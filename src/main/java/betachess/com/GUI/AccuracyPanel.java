package betachess.com.GUI;

import betachess.com.engine.GameAnalyzer;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * Chess.com-style accuracy column: two compact cards stacked vertically
 * (White on top, Black below), each with a large colour-coded percentage
 * above a rounded progress bar.
 */
public class AccuracyPanel extends JPanel {

    private final AccuracyCard whiteCard = new AccuracyCard("White", "\u2654"); // ♔
    private final AccuracyCard blackCard = new AccuracyCard("Black", "\u265A"); // ♚

    public AccuracyPanel() {
        setOpaque(false);
        setLayout(new GridLayout(2, 1, 0, 6));
        add(whiteCard);
        add(blackCard);
    }

    public void update(GameAnalyzer.Snapshot snapshot) {
        whiteCard.setAccuracy(snapshot.whiteAccuracy);
        blackCard.setAccuracy(snapshot.blackAccuracy);
    }
    public void setValues(double white, double black) {
        whiteCard.setAccuracy(white);
        blackCard.setAccuracy(black);
    }

    public void clear() {
        whiteCard.reset();
        blackCard.reset();
    }

    // ------------------------------------------------------------------ colour

    private static Color colorFor(double accuracy) {
        if (accuracy >= 95) return new Color(0x81B64C);   // bright green
        if (accuracy >= 90) return new Color(0x9CC650);   // light green
        if (accuracy >= 80) return new Color(0xF7C631);   // yellow
        if (accuracy >= 70) return new Color(0xF7A631);   // orange
        if (accuracy >= 60) return new Color(0xE5712F);   // dark orange
        return new Color(0xE54C4C);                       // red
    }

    // ------------------------------------------------------------------ card

    private static final class AccuracyCard extends JPanel {

        private static final String PLACEHOLDER = "\u2014";   // em dash
        private static final int    CARD_HEIGHT = 74;

        private final JLabel     accLabel = new JLabel(PLACEHOLDER, SwingConstants.CENTER);
        private final AccuracyBar bar = new AccuracyBar();

        AccuracyCard(String name, String icon) {
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
            setLayout(new BorderLayout(0, 2));
            setPreferredSize(new Dimension(110, CARD_HEIGHT));

            JLabel nameLabel = new JLabel(icon + "  " + name);
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.PLAIN, 10f));
            nameLabel.setForeground(new Color(0x666666));

            accLabel.setFont(accLabel.getFont().deriveFont(Font.BOLD, 18f));
            accLabel.setForeground(new Color(0xBBBBBB));
            accLabel.setToolTipText("Accuracy");

            add(nameLabel, BorderLayout.NORTH);
            add(accLabel, BorderLayout.CENTER);
            add(bar, BorderLayout.SOUTH);
        }

        void setAccuracy(double accuracy) {
            accLabel.setText(String.format("%.1f%%", accuracy));
            accLabel.setForeground(colorFor(accuracy));
            bar.setValue(accuracy, colorFor(accuracy));
        }

        void reset() {
            accLabel.setText(PLACEHOLDER);
            accLabel.setForeground(new Color(0xBBBBBB));
            bar.setValue(0, new Color(0xCCCCCC));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0xFAFAFA));
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
            g2.setColor(new Color(0xE8E8E8));
            g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ------------------------------------------------------------------ bar

    private static final class AccuracyBar extends JComponent {

        private double value = 0;
        private Color  color = new Color(0xCCCCCC);

        AccuracyBar() {
            setPreferredSize(new Dimension(0, 5));
            setOpaque(false);
        }

        void setValue(double value, Color color) {
            this.value = Math.max(0, Math.min(100, value));
            this.color = color;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(new Color(0xE5E5E5));
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, h, h));
            int filled = (int) Math.round(w * value / 100.0);
            if (filled > 0) {
                g2.setColor(color);
                g2.fill(new RoundRectangle2D.Float(0, 0, filled, h, h, h));
            }
            g2.dispose();
        }
    }
}