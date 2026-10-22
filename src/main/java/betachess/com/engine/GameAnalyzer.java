package betachess.com.engine;

import betachess.com.gamesystem.ChessGame;
import betachess.com.gamesystem.GameListener;
import betachess.com.piece.PieceColor;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Continuously analyses the current game position with a dedicated engine
 * process and exposes:
 *   – live eval / best move / depth of the current position
 *   – cumulative accuracy% for White and Black
 *
 * Register it as a {@link GameListener} on the active {@link ChessGame}.
 */
public final class GameAnalyzer implements GameListener {

    /** Immutable snapshot passed to the UI. */
    public static final class Snapshot {
        public final String  evalText;          // "+1.24", "-M3" …
        public final String  bestMoveUci;       // "e2e4" or ""
        public final int     depth;
        public final double  whiteAccuracy;
        public final double  blackAccuracy;
        public final int     whiteCentipawns;   // NEW: from White's POV, mate = ±10000
        public final boolean isMate;            // NEW

        Snapshot(String evalText, String bestMoveUci, int depth,
                 double whiteAccuracy, double blackAccuracy,
                 int whiteCentipawns, boolean isMate) {
            this.evalText        = evalText;
            this.bestMoveUci     = bestMoveUci;
            this.depth           = depth;
            this.whiteAccuracy   = whiteAccuracy;
            this.blackAccuracy   = blackAccuracy;
            this.whiteCentipawns = whiteCentipawns;
            this.isMate          = isMate;
        }
    }

    public interface Listener {
        void onAnalysis(Snapshot snapshot);
    }

    private final UciEngine engine;
    private final int depth;

    private final Map<Integer, EngineEvaluation> evalsByPly = new HashMap<>();
    private EngineEvaluation currentEval;
    private int currentPly = -1;
    private boolean freshSearch;

    private ChessGame game;
    private volatile boolean enabled;
    private volatile Listener listener;

    public GameAnalyzer(String executable, int depth) throws IOException {
        this.depth = Math.max(1, depth);
        this.engine = new UciEngine(executable);
        this.engine.setInfoListener(this::handleInfoLine);
        this.engine.start();
        this.engine.newGame();
    }

    public void setListener(Listener listener) { this.listener = listener; }
    public boolean isEnabled()                 { return enabled; }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            if (game != null) {
                onGameChanged(game);
            }
        } else {
            engine.stopSearch();
        }
    }

    // ------------------------------------------------------------ listener

    @Override
    public void onGameChanged(ChessGame game) {
        boolean switchedGame = (this.game != game);
        this.game = game;
        if (!enabled) {
            return;
        }

        int ply = game.getHistory().size();
        if (!switchedGame && ply == currentPly) {
            return;
        }

        if (switchedGame) {
            evalsByPly.clear();
        } else if (currentPly >= 0 && currentEval != null) {
            evalsByPly.put(currentPly, currentEval);
        }
        evalsByPly.keySet().removeIf(p -> p > ply);

        currentPly = ply;
        currentEval = null;
        freshSearch = true;
        publish();

        if (game.getStatus().isGameOver()) {
            return;
        }

        String fen = FenSerializer.toFen(
                game.getBoard(),
                game.getSideToMove(),
                game.getCastlingRights(),
                game.getEnPassantTarget(),
                game.getHalfmoveClock(),
                game.getFullmoveNumber());

        engine.stopSearch();
        engine.analyze(fen, depth);
    }

    private void handleInfoLine(String line) {
        if (!enabled) {
            return;
        }
        EngineEvaluation eval = UciInfoParser.parse(line);
        if (eval == null) {
            return;
        }
        if (freshSearch || currentEval == null || eval.getDepth() >= currentEval.getDepth()) {
            freshSearch = false;
            currentEval = eval;
            publish();
        }
    }

    // -------------------------------------------------------------- output

    private void publish() {
        Listener l = listener;
        if (l == null) {
            return;
        }
        Snapshot snapshot = buildSnapshot();
        SwingUtilities.invokeLater(() -> l.onAnalysis(snapshot));
    }

    private Snapshot buildSnapshot() {
        String evalText = "0.00";
        String bestMove = "";
        int depthSoFar = 0;
        int whiteCentipawns = 0;
        boolean isMate = false;

        if (currentEval != null) {
            depthSoFar = currentEval.getDepth();
            Score score = currentEval.getScore();
            PieceColor side = game != null ? game.getSideToMove() : PieceColor.WHITE;

            if (score.getType() == Score.Type.MATE) {
                isMate = true;
                int moves = score.getValue();
                if (side == PieceColor.BLACK) {
                    moves = -moves;
                }
                evalText = moves >= 0 ? "+M" + moves : "-M" + (-moves);
                whiteCentipawns = moves >= 0 ? 10000 : -10000;
            } else {
                int cpWhite = side == PieceColor.WHITE
                        ? score.toCentipawns()
                        : -score.toCentipawns();
                whiteCentipawns = cpWhite;
                evalText = String.format("%+.2f", cpWhite / 100.0);
            }
            if (currentEval.getBestMoveUci() != null) {
                bestMove = currentEval.getBestMoveUci();
            }
        }

        return new Snapshot(evalText, bestMove, depthSoFar,
                computeAccuracy(PieceColor.WHITE),
                computeAccuracy(PieceColor.BLACK),
                whiteCentipawns, isMate);
    }

    private double computeAccuracy(PieceColor color) {
        if (game == null) {
            return 100.0;
        }
        double total = 0;
        int count = 0;
        int historySize = game.getHistory().size();
        for (int ply = 0; ply < historySize; ply++) {
            PieceColor mover = (ply % 2 == 0) ? PieceColor.WHITE : PieceColor.BLACK;
            if (mover != color) {
                continue;
            }
            EngineEvaluation before = evalsByPly.get(ply);
            EngineEvaluation after = evalsByPly.get(ply + 1);
            if (before == null || after == null) {
                continue;
            }
            // Both to the mover's perspective:
            int cpBefore = before.getScore().toCentipawns();
            int cpAfter = -after.getScore().toCentipawns();
            total += AccuracyCalculator.accuracy(cpBefore, cpAfter);
            count++;
        }
        return count == 0 ? 100.0 : total / count;
    }

    public void shutdown() {
        enabled = false;
        engine.close();
    }
}