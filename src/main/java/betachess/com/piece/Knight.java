package betachess.com.piece;

public final class Knight extends Piece {

    private static final int[][] DIRECTIONS = {
            {-1, -2}, {1, -2}, {-2, -1}, {2, -1},
            {-2,  1}, {2,  1}, {-1,  2}, {1,  2}
    };

    public Knight(PieceColor color) {
        super(color);
    }

    @Override
    public PieceType getType() {
        return PieceType.KNIGHT;
    }

    @Override
    public boolean isSliding() {
        return false;
    }

    @Override
    public int[][] moveDirections() {
        return DIRECTIONS;
    }
}