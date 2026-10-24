package betachess.com.engine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thin UCI client.
 *
 * <p>Threading model:</p>
 * <ul>
 *   <li>one writer method ({@link #send(String)}, synchronized) for commands</li>
 *   <li>one daemon reader thread ({@code uci-reader}) that dispatches
 *       {@code info ...} lines to the current {@link InfoListener} and
 *       {@code bestmove ...} lines to the current {@link EngineListener}
 *       and to any waiting blocking caller</li>
 * </ul>
 *
 * <p>Two usage patterns:</p>
 * <ul>
 *   <li><b>Asynchronous</b> (playing engine, live analysis): {@link #setListener},
 *       {@link #setInfoListener}, then {@link #go(String)} / {@link #analyze(String,int)}.
 *       Results arrive on the reader thread.</li>
 *   <li><b>Blocking</b> (game review, one-off analysis): {@link #analyzeBlocking(String,int)}.
 *       Returns when Stockfish emits {@code bestmove}, with the deepest set of
 *       MultiPV evaluations collected along the way.</li>
 * </ul>
 */
public final class UciEngine implements AutoCloseable {

    /** Callback for {@code info ...} lines, on the reader thread. */
    public interface InfoListener {
        void onInfoLine(String line);
    }

    // ------------------------------------------------------------- state

    private final String executable;

    private Process        process;
    private BufferedWriter stdin;
    private Thread         reader;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong    searchId = new AtomicLong(0);

    /** Uses {@link betachess.com.engine.EngineListener} — the standalone interface. */
    private volatile EngineListener moveListener;
    private volatile InfoListener   infoListener;

    private volatile long   currentSearchId  = -1;
    private volatile String pendingBestMove;

    private final Object               blockingLock = new Object();
    private volatile BlockingCollector blocking;

    public UciEngine(String executable) {
        if (executable == null || executable.isBlank()) {
            throw new IllegalArgumentException("Engine executable must be provided");
        }
        this.executable = executable;
    }

    // ------------------------------------------------------------- lifecycle

    public synchronized void start() throws IOException {
        if (running.get()) {
            return;
        }
        ProcessBuilder builder = new ProcessBuilder(executable);
        builder.redirectErrorStream(true);
        process = builder.start();

        stdin = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        running.set(true);

        send("uci");
        await("uciok", 5_000);
        send("isready");
        await("readyok", 5_000);

        reader = new Thread(this::readLoop, "uci-reader");
        reader.setDaemon(true);
        reader.start();
    }

    @Override
    public synchronized void close() {
        if (!running.getAndSet(false)) {
            return;
        }
        try {
            send("quit");
        } catch (RuntimeException ignored) {
            // process may already be gone
        }
        if (reader != null) {
            reader.interrupt();
            reader = null;
        }
        if (process != null) {
            process.destroy();
            try {
                process.waitFor(2, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        stdin = null;
        process = null;
    }

    // ------------------------------------------------------------- listeners

    public void setListener(EngineListener listener)   { this.moveListener = listener; }
    public void setInfoListener(InfoListener listener) { this.infoListener = listener; }

    // ------------------------------------------------------------- commands

    public void newGame() {
        send("ucinewgame");
    }

    public void setSkillLevel(int level) {
        send("setoption name Skill Level value " + Math.max(0, Math.min(20, level)));
    }

    public void setMultiPv(int n) {
        send("setoption name MultiPV value " + Math.max(1, Math.min(10, n)));
    }

    public void go(String fen) {
        currentSearchId = searchId.incrementAndGet();
        pendingBestMove = null;
        send("position fen " + fen);
        send("go movetime 500");
    }

    public void analyze(String fen, int depth) {
        send("position fen " + fen);
        send("go depth " + Math.max(1, depth));
    }

    public void stopSearch() {
        send("stop");
    }

    // ------------------------------------------------------------- blocking

    private static final class BlockingCollector {
        final Map<Integer, EngineEvaluation> pvs = new HashMap<>();
        int                                    maxDepth = -1;
        final CountDownLatch                   done    = new CountDownLatch(1);
    }

    public List<EngineEvaluation> analyzeBlocking(String fen, int depth)
            throws InterruptedException {

        BlockingCollector collector = new BlockingCollector();
        synchronized (blockingLock) {
            if (blocking != null) {
                return new ArrayList<>();
            }
            blocking = collector;
        }

        InfoListener   oldInfo = this.infoListener;
        EngineListener oldMove = this.moveListener;

        this.infoListener = line -> {
            EngineEvaluation eval = UciInfoParser.parse(line);
            if (eval == null) {
                return;
            }
            if (eval.getDepth() > collector.maxDepth) {
                collector.pvs.clear();
                collector.maxDepth = eval.getDepth();
            }
            if (eval.getDepth() == collector.maxDepth) {
                collector.pvs.put(eval.getMultipv(), eval);
            }
        };
        this.moveListener = uci -> {
            if (collector.pvs.isEmpty() && uci != null) {
                collector.pvs.put(1, new EngineEvaluation(
                        collector.maxDepth, 1, Score.centipawns(0), uci));
            }
            collector.done.countDown();
        };

        try {
            send("position fen " + fen);
            send("go depth " + Math.max(1, depth));

            if (!collector.done.await(120, TimeUnit.SECONDS)) {
                send("stop");
                collector.done.await(5, TimeUnit.SECONDS);
            }

            List<EngineEvaluation> sorted = new ArrayList<>(collector.pvs.values());
            sorted.sort(Comparator.comparingInt(EngineEvaluation::getMultipv));
            return sorted;
        } finally {
            this.infoListener = oldInfo;
            this.moveListener = oldMove;
            synchronized (blockingLock) {
                if (blocking == collector) {
                    blocking = null;
                }
            }
        }
    }

    public String readBestMoveBlocking(String fen, long timeoutMillis)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        go(fen);
        while (pendingBestMove == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        return pendingBestMove;
    }

    // ------------------------------------------------------------- io

    private synchronized void send(String line) {
        if (stdin == null) {
            throw new IllegalStateException("Engine not started");
        }
        try {
            stdin.write(line);
            stdin.newLine();
            stdin.flush();
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot write to engine: " + ex.getMessage(), ex);
        }
    }

    private void readLoop() {
        try (BufferedReader out = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = out.readLine()) != null) {
                if (line.startsWith("bestmove")) {
                    String[] parts = line.split("\\s+");
                    String move = parts.length >= 2 ? parts[1] : null;
                    if (move != null && !"0000".equals(move)) {
                        pendingBestMove = move;
                    }
                    EngineListener listener = moveListener;
                    if (listener != null) {
                        if (move == null || "0000".equals(move)) {
                            listener.onBestMove("0000");
                        } else {
                            listener.onBestMove(move);
                        }
                    }
                } else if (line.startsWith("info ")) {
                    InfoListener listener = infoListener;
                    if (listener != null) {
                        listener.onInfoLine(line);
                    }
                }
            }
        } catch (IOException ex) {
            EngineListener listener = moveListener;
            if (listener != null) {
                listener.onEngineError(ex.getMessage() == null
                        ? "Engine I/O error" : ex.getMessage());
            }
        } finally {
            running.set(false);
        }
    }

    private void await(String token, long timeoutMillis) throws IOException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        BufferedReader out = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        String line;
        while (System.currentTimeMillis() < deadline) {
            line = out.readLine();
            if (line == null) {
                break;
            }
            if (line.equals(token)) {
                return;
            }
        }
        throw new IOException("Engine did not answer '" + token + "' in time");
    }

    // ------------------------------------------------------------- smoke test

    public static void main(String[] args) throws Exception {
        String fen = args.length > 0
                ? args[0]
                : "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

        try (UciEngine engine = new UciEngine(EngineConfig.resolveExecutable())) {
            engine.start();
            engine.newGame();
            engine.setSkillLevel(10);

            List<EngineEvaluation> pvs = engine.analyzeBlocking(fen, 12);
            if (pvs.isEmpty()) {
                System.out.println("No output from engine");
                return;
            }
            for (EngineEvaluation e : pvs) {
                System.out.printf("multipv %d  depth %d  %s  %s%n",
                        e.getMultipv(), e.getDepth(),
                        e.getScore(), e.getBestMoveUci());
            }
        }
    }
}