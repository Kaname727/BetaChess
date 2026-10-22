package betachess.com.GUI;

import betachess.com.assets.AssetLoader;
import betachess.com.piece.Piece;
import betachess.com.piece.PieceColor;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Caches the scaled piece images (assets are 128x128, display size varies).
 */
public final class PieceImages {

    private static final Map<String, Image> SCALED_CACHE = new HashMap<String, Image>();

    private PieceImages() {
    }

    /** Returns null when the PNG is missing – the caller then draws a glyph. */
    public static Image get(Piece piece, int size) {
        String key = piece.assetKey() + "@" + size;
        Image cached = SCALED_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        BufferedImage source = AssetLoader.loadPiece(piece.getType(), piece.getColor());
        if (source == null) {
            return null;
        }
        Image scaled = source.getScaledInstance(size, size, Image.SCALE_SMOOTH);
        SCALED_CACHE.put(key, scaled);
        return scaled;
    }

    /** Unicode fallback glyph for the given piece. */
    public static String fallbackGlyph(Piece piece) {
        int base = piece.getColor() == PieceColor.WHITE ? 0x2654 : 0x265A;
        switch (piece.getType()) {
            case KING:   return String.valueOf((char) base);
            case QUEEN:  return String.valueOf((char) (base + 1));
            case ROOK:   return String.valueOf((char) (base + 2));
            case BISHOP: return String.valueOf((char) (base + 3));
            case KNIGHT: return String.valueOf((char) (base + 4));
            case PAWN:   return String.valueOf((char) (base + 5));
            default:     return "?";
        }
    }
}