package betachess.com.engine.opening;

/**
 * Reduces a full FEN to the four fields that define a position
 * (placement, side, castling, en passant). The halfmove and fullmove
 * counters are dropped — they are irrelevant for opening recognition.
 */
public final class FenNormalizer {

    private FenNormalizer() {
    }

    public static String normalize(String fen) {
        if (fen == null) {
            return "";
        }
        String trimmed = fen.trim();
        String[] parts = trimmed.split("\\s+");
        if (parts.length < 4) {
            return trimmed;
        }
        return parts[0] + " " + parts[1] + " " + parts[2] + " " + parts[3];
    }
}