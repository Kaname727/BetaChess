package betachess.com.piece;

/**
 * The two colours that can be on the board.
 */
public enum PieceColor {

    WHITE,
    BLACK;

    public PieceColor opposite() {
        return this == WHITE ? BLACK : WHITE;
    }
}