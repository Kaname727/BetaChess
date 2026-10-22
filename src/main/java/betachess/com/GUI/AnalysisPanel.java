package betachess.com.GUI;

import betachess.com.engine.GameAnalyzer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Small panel showing the live evaluation, the suggested best move (SAN)
 * and the cumulative accuracy for both sides.
 */
public class AnalysisPanel extends JPanel {

    private final JLabel evalLabel  = new JLabel("-");
    private final JLabel bestLabel  = new JLabel("-");
    private final JLabel depthLabel = new JLabel("-");
    private final JLabel whiteAcc   = new JLabel("100.0%");
    private final JLabel blackAcc   = new JLabel("100.0%");

    public AnalysisPanel() {
        setBorder(BorderFactory.createTitledBorder("Analysis"));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(row("Eval:",  evalLabel));
        add(row("Best:",  bestLabel));
        add(row("Depth:", depthLabel));
        add(row("White:", whiteAcc));
        add(row("Black:", blackAcc));
        add(Box.createVerticalGlue());
    }

    private JPanel row(String label, JLabel value) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setAlignmentX(LEFT_ALIGNMENT);
        JLabel name = new JLabel(label);
        name.setFont(name.getFont().deriveFont(Font.PLAIN));
        value.setFont(value.getFont().deriveFont(Font.BOLD));
        value.setAlignmentX(LEFT_ALIGNMENT);
        row.add(name);
        row.add(Box.createHorizontalStrut(6));
        row.add(value);
        row.add(Box.createHorizontalGlue());
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        return row;
    }

    public void update(GameAnalyzer.Snapshot snapshot) {
        evalLabel.setText(snapshot.evalText);

        // Display SAN ("e4", "Bb5+", "O-O") with UCI in the tooltip.
        bestLabel.setText(snapshot.bestMoveSan.isEmpty() ? "-" : snapshot.bestMoveSan);
        bestLabel.setToolTipText(snapshot.bestMoveUci.isEmpty()
                ? null : "UCI: " + snapshot.bestMoveUci);

        depthLabel.setText(snapshot.depth == 0 ? "-" : String.valueOf(snapshot.depth));
        whiteAcc.setText(String.format("%.1f%%", snapshot.whiteAccuracy));
        blackAcc.setText(String.format("%.1f%%", snapshot.blackAccuracy));
    }

    public void clear() {
        evalLabel.setText("-");
        bestLabel.setText("-");
        bestLabel.setToolTipText(null);
        depthLabel.setText("-");
        whiteAcc.setText("100.0%");
        blackAcc.setText("100.0%");
    }
}