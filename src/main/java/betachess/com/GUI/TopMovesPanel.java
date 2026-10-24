package betachess.com.GUI;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Collections;
import java.util.List;

/**
 * Five-row listing of the engine's preferred lines from the current position.
 */
public class TopMovesPanel extends JPanel {

    private static final int ROWS = 5;
    private static final Color PLAYED_COLOR = new Color(0x1565C0);

    public static final class Entry {
        public final String  san;
        public final String  evalText;
        public final boolean played;
        public final boolean best;

        public Entry(String san, String evalText, boolean played, boolean best) {
            this.san      = san;
            this.evalText = evalText;
            this.played   = played;
            this.best     = best;
        }
    }

    private final JLabel[] rankLabels   = new JLabel[ROWS];
    private final JLabel[] sanLabels    = new JLabel[ROWS];
    private final JLabel[] evalLabels   = new JLabel[ROWS];
    private final JLabel[] markerLabels = new JLabel[ROWS];
    private final JPanel[] rows         = new JPanel[ROWS];

    public TopMovesPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createTitledBorder("Top engine moves"));
        for (int i = 0; i < ROWS; i++) {
            rows[i] = buildRow(i);
            add(rows[i]);
        }
        setEntries(Collections.<Entry>emptyList());
    }

    private JPanel buildRow(int index) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        row.setPreferredSize(new Dimension(0, 22));

        JLabel rank = new JLabel((index + 1) + ".");
        rank.setFont(rank.getFont().deriveFont(Font.PLAIN, 11f));
        rank.setPreferredSize(new Dimension(18, 18));

        JLabel san = new JLabel("-");
        san.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));

        JLabel eval = new JLabel("-");
        eval.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        eval.setHorizontalAlignment(JLabel.RIGHT);
        eval.setPreferredSize(new Dimension(52, 18));

        JLabel marker = new JLabel(" ");
        marker.setFont(marker.getFont().deriveFont(Font.BOLD, 11f));
        marker.setForeground(PLAYED_COLOR);
        marker.setPreferredSize(new Dimension(14, 18));

        JPanel left = new JPanel(new BorderLayout(6, 0));
        left.setOpaque(false);
        left.add(rank, BorderLayout.WEST);
        left.add(san, BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout(4, 0));
        right.setOpaque(false);
        right.add(marker, BorderLayout.WEST);
        right.add(eval, BorderLayout.EAST);

        row.add(left, BorderLayout.CENTER);
        row.add(right, BorderLayout.EAST);

        rankLabels[index]   = rank;
        sanLabels[index]    = san;
        evalLabels[index]   = eval;
        markerLabels[index] = marker;
        return row;
    }

    public void setEntries(List<Entry> entries) {
        for (int i = 0; i < ROWS; i++) {
            if (i < entries.size()) {
                Entry e = entries.get(i);
                rankLabels[i].setText((i + 1) + ".");
                sanLabels[i].setText(e.san);
                evalLabels[i].setText(e.evalText);
                markerLabels[i].setText(e.played ? "\u2192 " : "  ");
                Color colour = e.best ? new Color(0x81B64C)
                        : (e.played ? PLAYED_COLOR : null);
                sanLabels[i].setForeground(colour == null
                        ? javax.swing.UIManager.getColor("Label.foreground")
                        : colour);
            } else {
                rankLabels[i].setText((i + 1) + ".");
                sanLabels[i].setText("-");
                evalLabels[i].setText("-");
                markerLabels[i].setText("  ");
                sanLabels[i].setForeground(javax.swing.UIManager.getColor("Label.foreground"));
            }
        }
        revalidate();
        repaint();
    }

    public void clear() {
        setEntries(Collections.<Entry>emptyList());
    }
}