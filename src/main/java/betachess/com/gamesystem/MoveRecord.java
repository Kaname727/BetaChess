package betachess.com.gamesystem;

import betachess.com.piece.Piece;

/**
 * One entry of the move list, including everything needed to undo it.
 */
public final class MoveRecord {

    private final Move move;
    private final GameSnapshot snapshotBefore;
    private final Piece movedPiece;
    private final Piece capturedPiece;
    private final String notation;

    public MoveRecord(Move move, GameSnapshot snapshotBefore, Piece movedPiece,
                      Piece capturedPiece, String notation) {
        this.move = move;
        this.snapshotBefore = snapshotBefore;
        this.movedPiece = movedPiece;
        this.capturedPiece = capturedPiece;
        this.notation = notation;
    }

    public Move getMove() {
        return move;
    }

    public GameSnapshot getSnapshotBefore() {
        return snapshotBefore;
    }

    public Piece getMovedPiece() {
        return movedPiece;
    }

    public Piece getCapturedPiece() {
        return capturedPiece;
    }

    public String getNotation() {
        return notation;
    }

    @Override
    public String toString() {
        return notation;
    }
}