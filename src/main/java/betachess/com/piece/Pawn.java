package betachess.com.piece;

/**
 * Pawns are special: they are generated explicitly by the move generator,
 * so they expose their direction helpers instead of generic offsets.
 */
public final class Pawn extends Piece {

    private static final int[][] NO_DIRECTIONS = new int[0][0];

    public Pawn(PieceColor color) {
        super(color);
    }

    @Override
    public PieceType getType() {
        return PieceType.PAWN;
    }

    @Override
    public boolean isSliding() {
        return false;
    }

    @Override
    public int[][] moveDirections() {
        return NO_DIRECTIONS;
    }

    /** +1 for white, -1 for black. */
    public int forward() {
        return getColor() == PieceColor.WHITE ? 1 : -1;
    }

    /** Rank index (0 based) on which the pawn starts and may move two squares. */
    public int startRank() {
        return getColor() == PieceColor.WHITE ? 1 : 6;
    }
}