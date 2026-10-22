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
 * Live evaluation, engine best move (SAN) and search depth.
 * Accuracy has moved to the dedicated {@link AccuracyPanel}.
 */
public class AnalysisPanel extends JPanel {

    private final JLabel evalLabel  = new JLabel("-");
    private final JLabel bestLabel  = new JLabel("-");
    private final JLabel depthLabel = new JLabel("-");

    public AnalysisPanel() {
        setBorder(BorderFactory.createTitledBorder("Analysis"));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(row("Eval:",  evalLabel));
        add(row("Best:",  bestLabel));
        add(row("Depth:", depthLabel));
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
        bestLabel.setText(snapshot.bestMoveSan.isEmpty() ? "-" : snapshot.bestMoveSan);
        bestLabel.setToolTipText(snapshot.bestMoveUci.isEmpty()
                ? null : "UCI: " + snapshot.bestMoveUci);
        depthLabel.setText(snapshot.depth == 0 ? "-" : String.valueOf(snapshot.depth));
    }

    public void clear() {
        evalLabel.setText("-");
        bestLabel.setText("-");
        bestLabel.setToolTipText(null);
        depthLabel.setText("-");
    }
}