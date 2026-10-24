package betachess.com.engine;

import java.awt.Color;

/**
 * Verdict for a single played move. Colours mirror chess.com's game-review palette.
 */
public enum MoveClassification {

    BEST       ("\u2605", 0x81B64C),   // ★  green
    EXCELLENT  ("\u2713", 0x9CC650),   // ✓  light green
    GOOD       ("\u2713", 0xB8D26B),   // ✓  pale green
    BOOK       ("\u2658", 0xA87D33),   // ♘  brown (book move)
    INACCURACY ("?!",     0xF7C631),   //    yellow
    MISTAKE    ("?",      0xF7A631),   //    orange
    BLUNDER    ("??",     0xE54C4C);   //    red

    private final String symbol;
    private final int    rgb;

    MoveClassification(String symbol, int rgb) {
        this.symbol = symbol;
        this.rgb    = rgb;
    }

    public String getSymbol() { return symbol; }
    public Color  getColor()  { return new Color(rgb); }
}