package betachess.com.piece;

public final class Queen extends Piece {

    private static final int[][] DIRECTIONS = {
            {-1, -1}, {0, -1}, {1, -1},
            {-1,  0},          {1,  0},
            {-1,  1}, {0,  1}, {1,  1}
    };

    public Queen(PieceColor color) {
        super(color);
    }

    @Override
    public PieceType getType() {
        return PieceType.QUEEN;
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