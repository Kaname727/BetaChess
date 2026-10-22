package betachess.com.gamesystem;

import betachess.com.piece.PieceType;

/**
 * A single move. Castling and en passant are encoded implicitly
 * (king moves two files / pawn moves diagonally to an empty square),
 * which keeps the class serialisable as plain UCI text.
 */
public final class Move {

    private final Position from;
    private final Position to;
    private final PieceType promotion;

    public Move(Position from, Position to) {
        this(from, to, null);
    }

    public Move(Position from, Position to, PieceType promotion) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from/to must not be null");
        }
        if (from.equals(to)) {
            throw new IllegalArgumentException("A move must change square");
        }
        this.from = from;
        this.to = to;
        this.promotion = promotion;
    }

    public Position getFrom() {
        return from;
    }

    public Position getTo() {
        return to;
    }

    public PieceType getPromotion() {
        return promotion;
    }

    public boolean isPromotion() {
        return promotion != null;
    }

    /** e.g. "e2e4" or "e7e8q". */
    public String toUci() {
        StringBuilder text = new StringBuilder();
        text.append(from.toAlgebraic()).append(to.toAlgebraic());
        if (promotion != null) {
            text.append(promotion.letter());
        }
        return text.toString();
    }

    public static Move fromUci(String uci) {
        if (uci == null) {
            throw new IllegalArgumentException("Move must not be null");
        }
        String text = uci.trim().toLowerCase();
        if (text.length() != 4 && text.length() != 5) {
            throw new IllegalArgumentException("Bad move: " + uci);
        }
        Position from = Position.of(text.substring(0, 2));
        Position to = Position.of(text.substring(2, 4));
        PieceType promotion = text.length() == 5 ? promotionFromLetter(text.charAt(4)) : null;
        return new Move(from, to, promotion);
    }

    private static PieceType promotionFromLetter(char letter) {
        switch (letter) {
            case 'q': return PieceType.QUEEN;
            case 'r': return PieceType.ROOK;
            case 'b': return PieceType.BISHOP;
            case 'n': return PieceType.KNIGHT;
            default:  throw new IllegalArgumentException("Bad promotion piece: " + letter);
        }
    }

    @Override
    public String toString() {
        return toUci();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Move)) {
            return false;
        }
        Move that = (Move) other;
        return from.equals(that.from) && to.equals(that.to) && promotion == that.promotion;
    }

    @Override
    public int hashCode() {
        int result = from.hashCode();
        result = 31 * result + to.hashCode();
        result = 31 * result + (promotion != null ? promotion.hashCode() : 0);
        return result;
    }
}