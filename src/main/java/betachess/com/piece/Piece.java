package betachess.com.piece;

/**
 * Base class for every chess piece.
 *
 * <p>Pieces only know their colour and the geometry of their movement.
 * The actual legality of a move is decided by the game system, so this
 * package has no dependency on the board implementation.</p>
 */
public abstract class Piece {

    private final PieceColor color;

    protected Piece(PieceColor color) {
        if (color == null) {
            throw new IllegalArgumentException("A piece needs a colour");
        }
        this.color = color;
    }

    public final PieceColor getColor() {
        return color;
    }

    public abstract PieceType getType();

    /** True for queens, rooks and bishops. */
    public abstract boolean isSliding();

    /**
     * Movement steps as {fileDelta, rankDelta} pairs.
     * Non sliding pieces take a single step in each direction.
     */
    public abstract int[][] moveDirections();

    /** e.g. "wK" or "bP" – used to look up the matching image in the assets folder. */
    public final String assetKey() {
        String prefix = color == PieceColor.WHITE ? "w" : "b";
        return prefix + Character.toUpperCase(getType().letter());
    }

    @Override
    public String toString() {
        return (color == PieceColor.WHITE ? "White " : "Black ") + getType();
    }
}