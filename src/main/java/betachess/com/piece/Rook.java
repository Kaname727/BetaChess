package betachess.com.piece;

public final class Rook extends Piece {

    private static final int[][] DIRECTIONS = {
            {0, -1}, {-1, 0}, {1, 0}, {0, 1}
    };

    public Rook(PieceColor color) {
        super(color);
    }

    @Override
    public PieceType getType() {
        return PieceType.ROOK;
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