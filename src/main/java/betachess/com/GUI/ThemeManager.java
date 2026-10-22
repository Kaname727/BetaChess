package betachess.com.GUI;

// remove this:
// import com.formdev.flatlaf.FlatAnimatedLafChange;

// add this instead:
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

/**
 * Switches the FlatLaf theme at runtime and repaints every window.
 * Uses FlatLaf's snapshot animation so the transition is not jarring.
 */
public final class ThemeManager {

    public enum Theme { LIGHT, DARK }

    private ThemeManager() {
    }

    public static void apply(Theme theme) {
        FlatAnimatedLafChange.showSnapshot();
        try {
            if (theme == Theme.DARK) {
                FlatDarkLaf.setup();
            } else {
                FlatLightLaf.setup();
            }
            FlatLaf.updateUI();
        } finally {
            FlatAnimatedLafChange.hideSnapshotWithAnimation();
        }
    }
}