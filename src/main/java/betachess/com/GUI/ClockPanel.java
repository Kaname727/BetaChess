package betachess.com.GUI;

import betachess.com.gamesystem.clock.ChessClock;
import betachess.com.piece.PieceColor;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Two clock labels. Hidden when no time control is active (pass a null clock).
 */
public class ClockPanel extends JPanel {

    private static final Color ACTIVE_FG   = new Color(0x1565C0);
    private static final Color INACTIVE_FG = new Color(0x616161);

    private final JLabel whiteLabel = new JLabel("--:--", SwingConstants.CENTER);
    private final JLabel blackLabel = new JLabel("--:--", SwingConstants.CENTER);

    public ClockPanel() {
        setLayout(new GridLayout(1, 2, 6, 0));
        setBorder(BorderFactory.createEmptyBorder(2, 0, 6, 0));
        add(buildSide("White", whiteLabel));
        add(buildSide("Black", blackLabel));
    }

    private JPanel buildSide(String name, JLabel time) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel caption = new JLabel(name, SwingConstants.CENTER);
        caption.setFont(caption.getFont().deriveFont(Font.PLAIN, 11f));
        caption.setForeground(INACTIVE_FG);
        panel.add(caption, BorderLayout.NORTH);
        panel.add(time, BorderLayout.CENTER);
        return panel;
    }

    public void update(ChessClock clock) {
        if (clock == null) {
            setVisible(false);
            return;
        }
        setVisible(true);
        whiteLabel.setText(format(clock.getMillis(PieceColor.WHITE)));
        blackLabel.setText(format(clock.getMillis(PieceColor.BLACK)));

        boolean whiteActive = clock.isRunning() && clock.getActiveSide() == PieceColor.WHITE;
        boolean blackActive = clock.isRunning() && clock.getActiveSide() == PieceColor.BLACK;
        style(whiteLabel, whiteActive);
        style(blackLabel, blackActive);
    }

    private void style(JLabel label, boolean active) {
        Font base = label.getFont().deriveFont(Font.PLAIN, 20f);
        label.setFont(active ? base.deriveFont(Font.BOLD) : base);
        label.setForeground(active ? ACTIVE_FG : INACTIVE_FG);
    }

    private static String format(long millis) {
        long ms = Math.max(0, millis);
        if (ms < 20_000) {
            return String.format("%d.%d", ms / 1000, (ms % 1000) / 100);
        }
        long totalSeconds = ms / 1000;
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }
}