package betachess.com.engine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thin UCI client: one writer (synchronized), one daemon reader that routes
 * "info …" to an InfoListener and "bestmove …" to an EngineListener.
 */
public final class UciEngine implements AutoCloseable {

    public interface InfoListener {
        void onInfoLine(String line);
    }

    private final String executable;
    private Process process;
    private BufferedWriter stdin;
    private Thread reader;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong searchId = new AtomicLong(0);

    private volatile EngineListener moveListener;
    private volatile InfoListener infoListener;
    private volatile long currentSearchId = -1;
    private volatile String pendingBestMove;

    public UciEngine(String executable) {
        this.executable = executable;
    }

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

    public void setListener(EngineListener listener)   { this.moveListener = listener; }
    public void setInfoListener(InfoListener listener) { this.infoListener = listener; }

    public void newGame()              { send("ucinewgame"); }
    public void setSkillLevel(int lvl) { send("setoption name Skill Level value "
            + Math.max(0, Math.min(20, lvl))); }

    /** Sends position + go depth N. Callers that care about bestmove use a listener. */
    public void analyze(String fen, int depth) {
        send("position fen " + fen);
        send("go depth " + depth);
    }

    /** Sends position + go movetime; used by the playing EnginePlayer. */
    public void go(String fen) {
        currentSearchId = searchId.incrementAndGet();
        pendingBestMove = null;
        send("position fen " + fen);
        send("go movetime 500");
    }

    public void stopSearch() {
        send("stop");
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

    @Override
    public synchronized void close() {
        if (!running.getAndSet(false)) {
            return;
        }
        try {
            send("quit");
        } catch (RuntimeException ignored) {
            // already gone
        }
        if (process != null) {
            process.destroy();
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ------------------------------------------------------------------ io

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
                    if (parts.length >= 2 && !"0000".equals(parts[1])) {
                        pendingBestMove = parts[1];
                        EngineListener listener = moveListener;
                        if (listener != null) {
                            listener.onBestMove(parts[1]);
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
                listener.onEngineError(ex.getMessage());
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
}