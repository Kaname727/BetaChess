package betachess.com.GUI;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Vertical evaluation bar. White portion grows from the bottom (or the top
 * when the board is flipped to Black's perspective). Width is fixed, height
 * follows the board via BorderLayout.WEST.
 */
public class EvalBar extends JComponent {

    private static final int PREFERRED_WIDTH = 26;

    private static final Color WHITE_LIVE   = new Color(0xF7F7F7);
    private static final Color WHITE_DIM    = new Color(0xE0E0E0);
    private static final Color BLACK_FILL   = new Color(0x1B1B1B);
    private static final Color MID_LINE     = new Color(0x9E9E9E);
    private static final Color BORDER       = new Color(0x616161);

    private double whiteFraction = 0.5;   // 0..1
    private boolean flipped;
    private boolean live;

    public EvalBar() {
        setPreferredSize(new Dimension(PREFERRED_WIDTH, 0));
        setOpaque(false);
        setToolTipText("Evaluation: 0.00 (analysis off)");
    }

    public void setFlipped(boolean flipped) {
        if (this.flipped != flipped) {
            this.flipped = flipped;
            repaint();
        }
    }

    /** @param centipawns positive = White is better, from White's POV. */
    public void setCentipawns(int centipawns) {
        int clamped = Math.max(-1000, Math.min(1000, centipawns));
        whiteFraction = 1.0 / (1.0 + Math.exp(-clamped / 400.0));
        live = true;
        setToolTipText(String.format("Evaluation: %+.2f", centipawns / 100.0));
        repaint();
    }

    public void setMate(boolean whiteWins) {
        whiteFraction = whiteWins ? 1.0 : 0.0;
        live = true;
        setToolTipText("Evaluation: " + (whiteWins ? "White mates" : "Black mates"));
        repaint();
    }

    public void clear() {
        whiteFraction = 0.5;
        live = false;
        setToolTipText("Evaluation: 0.00 (analysis off)");
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            g2.dispose();
            return;
        }

        // solid black base
        g2.setColor(BLACK_FILL);
        g2.fillRect(0, 0, w, h);

        // white slice
        int whiteHeight = (int) Math.round(h * whiteFraction);
        int whiteY = flipped ? 0 : h - whiteHeight;
        g2.setColor(live ? WHITE_LIVE : WHITE_DIM);
        g2.fillRect(0, whiteY, w, whiteHeight);

        // midline hint
        g2.setColor(MID_LINE);
        g2.drawLine(0, h / 2, w, h / 2);

        // border
        g2.setColor(BORDER);
        g2.drawRect(0, 0, w - 1, h - 1);

        g2.dispose();
    }
}