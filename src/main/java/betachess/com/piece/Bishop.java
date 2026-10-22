package betachess.com.piece;

public final class Bishop extends Piece {

    private static final int[][] DIRECTIONS = {
            {-1, -1}, {1, -1}, {-1, 1}, {1, 1}
    };

    public Bishop(PieceColor color) {
        super(color);
    }

    @Override
    public PieceType getType() {
        return PieceType.BISHOP;
    }

    @Override
    public boolean isSliding() {
        return true;
    }

    @Override
    public int[][] moveDirections() {
        return DIRECTIONS;
    }
}