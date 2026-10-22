package betachess.com.piece;

/**
 * Every kind of piece that exists in chess.
 */
public enum PieceType {

    KING('k'),
    QUEEN('q'),
    ROOK('r'),
    BISHOP('b'),
    KNIGHT('n'),
    PAWN('p');

    private final char letter;

    PieceType(char letter) {
        this.letter = letter;
    }

    /** Lower case letter used by UCI notation and file names. */
    public char letter() {
        return letter;
    }
}