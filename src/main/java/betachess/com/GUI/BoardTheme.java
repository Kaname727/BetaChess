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

    private BoardTheme() {
    }
}