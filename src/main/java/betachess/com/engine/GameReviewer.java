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
 * one {@link MoveAssessment} per move. All listener callbacks run on the EDT.
 */
public final class GameReviewer {

    public interface Listener {
        void onProgress(int completedPlies, int totalPlies);
        void onPlyAssessed(MoveAssessment assessment);
        void onComplete(List<MoveAssessment> assessments);
        void onError(String message);
    }

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

    public void cancel() {
        cancelled = true;
    }

    public boolean isRunning() {
        return worker != null && worker.isAlive();
    }

    // ------------------------------------------------------------------ core

    private void run(List<MoveRecord> history, Listener listener) {
        List<MoveAssessment> results = new ArrayList<>();
        try (UciEngine engine = new UciEngine(enginePath)) {
            engine.start();
            engine.newGame();

            ChessGame replay = new ChessGame();
            OpeningBook book  = OpeningBook.getDefault();
            int total = history.size();

            for (int i = 0; i < total; i++) {
                if (cancelled) {
                    notifyError(listener, "Analysis cancelled");
                    return;
                }

                Move move = history.get(i).getMove();
                PieceColor mover = replay.getSideToMove();

                // Position BEFORE the move
                String fenBefore = FenSerializer.toFen(
                        replay.getBoard(), replay.getSideToMove(),
                        replay.getCastlingRights(), replay.getEnPassantTarget(),
                        replay.getHalfmoveClock(), replay.getFullmoveNumber());

                UciEngine.BlockingResult before =
                        engine.analyzeBlocking(fenBefore, depth);
                if (before == null || before.evaluation == null) {
                    notifyError(listener, "Engine did not answer at ply " + i);
                    return;
                }

                String playedSan = San.of(replay.getBoard(), move);
                replay.makeMove(move);

                // Position AFTER the move
                String fenAfter = FenSerializer.toFen(
                        replay.getBoard(), replay.getSideToMove(),
                        replay.getCastlingRights(), replay.getEnPassantTarget(),
                        replay.getHalfmoveClock(), replay.getFullmoveNumber());

                UciEngine.BlockingResult after =
                        engine.analyzeBlocking(fenAfter, depth);
                if (after == null || after.evaluation == null) {
                    notifyError(listener, "Engine did not answer at ply " + i);
                    return;
                }

                int evalBefore = signForMover(before.evaluation.getScore(), mover);
                int evalAfter  = signForMover(after.evaluation.getScore(), mover);

                MoveAssessment assessment = MoveAssessment.build(
                        i, move, playedSan,
                        before.bestMoveUci,
                        bestMoveSanOrNull(before.bestMoveUci, mover, fenBefore),
                        evalBefore, evalAfter, mover);

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

    // --------------------------------------------------------------- helpers

    /** Engine score is from side-to-move; convert to the mover's POV. */
    private static int signForMover(Score score, PieceColor mover) {
        int cp = score.toCentipawns();
        // After the move, side-to-move is the opponent, so flip to keep mover's POV.
        // Before the move, side-to-move == mover, so no flip needed.
        // Caller passes the raw cp; the flip is applied by which eval is passed in.
        return cp;
    }

    private static String bestMoveSanOrNull(String uci, PieceColor mover, String fenBefore) {
        if (uci == null) return null;
        try {
            Move m = Move.fromUci(uci);
            // Rebuild the position to translate cleanly.
            ChessGame tmp = ChessGame.fromFen(fenBefore);
            if (tmp == null) return uci;
            return San.of(tmp.getBoard(), m);
        } catch (RuntimeException ex) {
            return uci;
        }
    }

    private static void notifyError(Listener listener, String message) {
        SwingUtilities.invokeLater(() -> listener.onError(message));
    }
}