package betachess.com.piece;

/**
 * Creates pieces without exposing the concrete classes to the rest of the app.
 */
public final class PieceFactory {

    private PieceFactory() {
    }

    public static Piece create(PieceType type, PieceColor color) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        switch (type) {
            case KING:   return new King(color);
            case QUEEN:  return new Queen(color);
            case ROOK:   return new Rook(color);
            case BISHOP: return new Bishop(color);
            case KNIGHT: return new Knight(color);
            case PAWN:   return new Pawn(color);
            default:     throw new IllegalArgumentException("Unknown piece type: " + type);
        }
    }
}