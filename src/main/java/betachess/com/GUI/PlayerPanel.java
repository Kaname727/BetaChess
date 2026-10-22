package betachess.com.GUI;

import betachess.com.gamesystem.clock.ChessClock;
import betachess.com.piece.PieceColor;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Chess.com-style player plate shown above and below the board.
 *
 * <ul>
 *   <li>drawn crown icon in the piece colour</li>
 *   <li>"White" / "Black" label</li>
 *   <li>clock time (hidden when the game has no time control)</li>
 *   <li>active-side highlight driven by {@link #setActive(boolean)}</li>
 * </ul>
 */
public class PlayerPanel extends JPanel {

    private static final int HEIGHT = 44;

    private final CrownIcon icon       = new CrownIcon();
    private final JLabel    nameLabel  = new JLabel("White");
    private final JLabel    clockLabel = new JLabel("--:--");

    private PieceColor color = PieceColor.WHITE;
    private boolean    active;

    public PlayerPanel(PieceColor color) {
        this.color = color;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        setLayout(new BorderLayout(8, 0));
        setPreferredSize(new Dimension(0, HEIGHT));

        icon.setPreferredSize(new Dimension(28, 28));
        icon.setColor(color);

        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 13f));

        clockLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
        clockLabel.setHorizontalAlignment(JLabel.RIGHT);
        clockLabel.setVisible(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        left.add(icon);
        left.add(nameLabel);

        add(left, BorderLayout.WEST);
        add(clockLabel, BorderLayout.EAST);

        refreshName();
    }

    public PieceColor getColor() {
        return color;
    }

    public void setColor(PieceColor color) {
        if (this.color == color) {
            return;
        }
        this.color = color;
        icon.setColor(color);
        refreshName();
        repaint();
    }

    public void setActive(boolean active) {
        if (this.active == active) {
            return;
        }
        this.active = active;
        repaint();
    }

    /** Reads the remaining time for this panel's colour; pass null to hide the clock. */
    public void updateClock(ChessClock clock) {
        if (clock == null) {
            clockLabel.setVisible(false);
            return;
        }
        clockLabel.setVisible(true);
        clockLabel.setText(format(clock.getMillis(color)));
    }

    private void refreshName() {
        nameLabel.setText(color == PieceColor.WHITE ? "White" : "Black");
    }

    private static String format(long millis) {
        long ms = Math.max(0, millis);
        if (ms < 20_000) {
            return String.format("%d.%d", ms / 1000, (ms % 1000) / 100);
        }
        long totalSeconds = ms / 1000;
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth() - 1;
        int h = getHeight() - 1;

        boolean dark = isDarkTheme();
        Color accent = UIManager.getColor("Component.accentColor");
        if (accent == null) {
            accent = new Color(0x1565C0);
        }

        Color bg;
        Color border;
        if (active) {
            bg     = dark ? blend(accent, Color.BLACK, 0.70) : blend(accent, Color.WHITE, 0.88);
            border = accent;
        } else {
            bg     = dark ? new Color(0x2D2D2D) : new Color(0xEEEEEE);
            border = dark ? new Color(0x3D3D3D) : new Color(0xDADADA);
        }

        g2.setColor(bg);
        g2.fillRoundRect(0, 0, w, h, 10, 10);
        g2.setColor(border);
        g2.drawRoundRect(0, 0, w, h, 10, 10);
        g2.dispose();
        super.paintComponent(g);
    }

    private static boolean isDarkTheme() {
        Color bg = UIManager.getColor("Panel.background");
        if (bg == null) {
            return false;
        }
        double lum = 0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue();
        return lum < 128;
    }

    private static Color blend(Color a, Color b, double ratioB) {
        double ratioA = 1 - ratioB;
        return new Color(
                (int) (a.getRed()   * ratioA + b.getRed()   * ratioB),
                (int) (a.getGreen() * ratioA + b.getGreen() * ratioB),
                (int) (a.getBlue()  * ratioA + b.getBlue()  * ratioB));
    }

    // -------------------------------------------------------------- crown

    private static final class CrownIcon extends JComponent {

        private Color color = PieceColor.WHITE == null ? Color.WHITE : Color.WHITE;

        void setColor(PieceColor pieceColor) {
            this.color = pieceColor == PieceColor.WHITE ? Color.WHITE : Color.BLACK;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth() - 4;
            int h = getHeight() - 4;
            int ox = 2;
            int oy = 2;

            int[] xs = {
                    ox,
                    (int) (ox + w * 0.15),
                    (int) (ox + w * 0.30),
                    (int) (ox + w * 0.50),
                    (int) (ox + w * 0.70),
                    (int) (ox + w * 0.85),
                    ox + w,
                    ox + w,
                    ox
            };
            int[] ys = {
                    (int) (oy + h * 0.50),
                    oy,
                    (int) (oy + h * 0.50),
                    oy,
                    (int) (oy + h * 0.50),
                    oy,
                    (int) (oy + h * 0.50),
                    oy + h,
                    oy + h
            };

            Color fill   = color == Color.WHITE ? new Color(0xF5F5F5) : new Color(0x212121);
            Color stroke = color == Color.WHITE ? new Color(0x757575) : new Color(0xBDBDBD);

            g2.setColor(fill);
            g2.fillPolygon(xs, ys, xs.length);
            g2.setColor(stroke);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawPolygon(xs, ys, xs.length);
            g2.dispose();
        }
    }
}