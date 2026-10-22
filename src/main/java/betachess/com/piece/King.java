package betachess.com.piece;

public final class King extends Piece {

    private static final int[][] DIRECTIONS = {
            {-1, -1}, {0, -1}, {1, -1},
            {-1,  0},          {1,  0},
            {-1,  1}, {0,  1}, {1,  1}
    };

    public King(PieceColor color) {
        super(color);
    }

    @Override
    public PieceType getType() {
        return PieceType.KING;
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