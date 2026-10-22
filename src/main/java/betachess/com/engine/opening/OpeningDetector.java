package betachess.com.engine.opening;

import betachess.com.engine.FenSerializer;
import betachess.com.gamesystem.ChessGame;
import betachess.com.gamesystem.GameSnapshot;
import betachess.com.gamesystem.MoveRecord;

import java.util.List;

/**
 * Identifies the opening currently on the board.
 *
 * <p>Lookup order:</p>
 * <ol>
 *   <li>the exact current position</li>
 *   <li>each earlier position in the move history, newest first</li>
 * </ol>
 *
 * <p>Step 2 is what makes transpositions and short prefixes work: if the
 * current position is not itself a named opening, the last position that
 * <em>is</em> named gives the best label for the line.</p>
 */
public final class OpeningDetector {

    private OpeningDetector() {
    }

    public static Opening detect(ChessGame game) {
        if (game == null) {
            return null;
        }
        OpeningBook book = OpeningBook.getDefault();
        if (book.size() == 0) {
            return null;
        }

        // 1. current position
        String currentFen = FenSerializer.toFen(
                game.getBoard(),
                game.getSideToMove(),
                game.getCastlingRights(),
                game.getEnPassantTarget(),
                game.getHalfmoveClock(),
                game.getFullmoveNumber());
        Opening exact = book.lookup(currentFen);
        if (exact != null) {
            return exact;
        }

        // 2. walk backwards through history
        List<MoveRecord> history = game.getHistory();
        for (int i = history.size() - 1; i >= 0; i--) {
            GameSnapshot snapshot = history.get(i).getSnapshotBefore();
            String fen = FenSerializer.toFen(
                    snapshot.getBoard(),
                    snapshot.getSideToMove(),
                    snapshot.getCastlingRights(),
                    snapshot.getEnPassantTarget(),
                    snapshot.getHalfmoveClock(),
                    snapshot.getFullmoveNumber());
            Opening fromHistory = book.lookup(fen);
            if (fromHistory != null) {
                return fromHistory;
            }
        }
        return null;
    }
}