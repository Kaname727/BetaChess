package betachess.com.GUI;

import java.awt.Color;

/**
 * Central place for all board colours and dimensions.
 */
public final class BoardTheme {

    public static final int BOARD_PIXELS = 640;
    public static final int SQUARE_PIXELS = BOARD_PIXELS / 8;

    public static final Color LIGHT_SQUARE     = new Color(0xF0D9B5);
    public static final Color DARK_SQUARE      = new Color(0xB58863);
    public static final Color SELECTED_SQUARE  = new Color(0x4A90D9);
    public static final Color LEGAL_TARGET     = new Color(0x2E7D32);
    public static final Color LAST_MOVE_SQUARE = new Color(0xFFD54F);
    public static final Color CHECK_SQUARE     = new Color(0xE53935);
    public static final Color ARROW_GREEN  = new Color(0x15, 0x78, 0x1B, 200);
    public static final Color ARROW_RED    = new Color(0x88, 0x20, 0x20, 200);
    public static final Color ARROW_BLUE   = new Color(0x00, 0x30, 0x88, 200);
    public static final Color ARROW_YELLOW = new Color(0xE6, 0x8F, 0x00, 200);
    public static final Color ANNOTATION_HIGHLIGHT = new Color(0x15, 0x78, 0x1B, 80);

    private BoardTheme() {
    }
}