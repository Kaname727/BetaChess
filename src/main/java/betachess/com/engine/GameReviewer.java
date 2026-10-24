package betachess.com.engine;

import betachess.com.engine.opening.OpeningBook;
import betachess.com.gamesystem.ChessGame;
import betachess.com.gamesystem.Move;
import betachess.com.gamesystem.MoveRecord;
import betachess.com.gamesystem.San;
import betachess.com.piece.PieceColor;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;

/**
 * Walks a game ply by ply with a dedicated Stockfish instance and produces
 * one {@link MoveAssessment} per move. Listener callbacks run on the EDT.
 */
public final class GameReviewer {

    public interface Listener {
        void onProgress(int completedPlies, int totalPlies);
        void onPlyAssessed(MoveAssessment assessment);
        void onComplete(List<MoveAssessment> assessments);
        void onError(String message);
    }

    /** How many alternative lines we ask Stockfish for at each "before" position. */
    public static final int TOP_MOVES = 5;

    private final String enginePath;
    private final int    depth;

    private volatile boolean cancelled;
    private Thread worker;

    public GameReviewer(String enginePath, int depth) {
        this.enginePath = enginePath;
        this.depth      = Math.max(6, depth);
    }

    public void start(ChessGame game, Listener listener) {
        List<MoveRecord> history = new ArrayList<>(game.getHistory());
        worker = new Thread(() -> run(history, listener), "game-reviewer");
        worker.setDaemon(true);
        worker.start();
    }

    public void cancel() { cancelled = true; }

    public boolean isRunning() {
        return worker != null && worker.isAlive();
    }

    // ---------------------------------------------------------------- core

    private void run(List<MoveRecord> history, Listener listener) {
        List<MoveAssessment> results = new ArrayList<>();
        try (UciEngine engine = new UciEngine(enginePath)) {
            engine.start();
            engine.newGame();

            ChessGame replay = new ChessGame();
            OpeningBook book = OpeningBook.getDefault();
            int total = history.size();

            for (int i = 0; i < total; i++) {
                if (cancelled) {
                    notifyError(listener, "Analysis cancelled");
                    return;
                }

                Move move = history.get(i).getMove();
                PieceColor mover = replay.getSideToMove();

                String fenBefore = FenSerializer.toFen(
                        replay.getBoard(), replay.getSideToMove(),
                        replay.getCastlingRights(), replay.getEnPassantTarget(),
                        replay.getHalfmoveClock(), replay.getFullmoveNumber());

                // MultiPV=5 for the "before" position (we show top choices).
                engine.setMultiPv(TOP_MOVES);
                List<EngineEvaluation> before = engine.analyzeBlocking(fenBefore, depth);
                if (before.isEmpty()) {
                    notifyError(listener, "Engine did not answer at ply " + (i + 1));
                    return;
                }

                String playedSan = San.of(replay.getBoard(), move);
                replay.makeMove(move);

                String fenAfter = FenSerializer.toFen(
                        replay.getBoard(), replay.getSideToMove(),
                        replay.getCastlingRights(), replay.getEnPassantTarget(),
                        replay.getHalfmoveClock(), replay.getFullmoveNumber());

                // MultiPV=1 for the "after" position — we only need the eval.
                engine.setMultiPv(1);
                List<EngineEvaluation> after = engine.analyzeBlocking(fenAfter, depth);
                if (after.isEmpty()) {
                    notifyError(listener, "Engine did not answer at ply " + (i + 1));
                    return;
                }

                int evalBefore = before.get(0).getScore().toCentipawns();
                int evalAfter  = -after.get(0).getScore().toCentipawns();

                MoveAssessment assessment = MoveAssessment.build(
                        i, move, playedSan, before, evalBefore, evalAfter);

                if (book.lookup(fenBefore) != null) {
                    assessment = assessment.asBook();
                }

                results.add(assessment);
                final MoveAssessment frozen = assessment;
                final int done = i + 1;
                SwingUtilities.invokeLater(() -> {
                    listener.onPlyAssessed(frozen);
                    listener.onProgress(done, total);
                });
            }

            final List<MoveAssessment> finalList = results;
            SwingUtilities.invokeLater(() -> listener.onComplete(finalList));
        } catch (Exception ex) {
            notifyError(listener, ex.getMessage() == null ? ex.toString() : ex.getMessage());
        }
    }

    private static void notifyError(Listener listener, String message) {
        SwingUtilities.invokeLater(() -> listener.onError(message));
    }
}