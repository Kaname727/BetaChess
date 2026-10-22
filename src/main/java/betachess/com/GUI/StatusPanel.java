package betachess.com.GUI;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Shows whose turn it is and offers the available actions.
 */
public class StatusPanel extends JPanel {

    private final JLabel turnLabel = new JLabel("White to move");
    private final JLabel statusLabel = new JLabel(" ");

    private final JButton newGameButton = new JButton("New game");
    private final JButton undoButton = new JButton("Undo");
    private final JButton drawButton = new JButton("Offer draw");
    private final JButton resignButton = new JButton("Resign");

    private GameActions actions;

    public StatusPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        turnLabel.setFont(turnLabel.getFont().deriveFont(Font.BOLD, 15f));
        turnLabel.setAlignmentX(LEFT_ALIGNMENT);
        statusLabel.setAlignmentX(LEFT_ALIGNMENT);

        add(turnLabel);
        add(statusLabel);
        add(Box.createVerticalStrut(8));
        add(buttonRow(newGameButton, undoButton));
        add(Box.createVerticalStrut(4));
        add(buttonRow(drawButton, resignButton));

        newGameButton.addActionListener(event -> fireNewGame());
        undoButton.addActionListener(event -> fireUndo());
        drawButton.addActionListener(event -> fireDraw());
        resignButton.addActionListener(event -> fireResign());
    }

    private JPanel buttonRow(JButton left, JButton right) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setAlignmentX(LEFT_ALIGNMENT);
        left.setAlignmentX(LEFT_ALIGNMENT);
        right.setAlignmentX(LEFT_ALIGNMENT);
        row.add(left);
        row.add(Box.createHorizontalStrut(6));
        row.add(right);
        row.add(Box.createHorizontalGlue());
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        return row;
    }

    public void setActions(GameActions actions) {
        this.actions = actions;
    }

    public void setTurnText(String text) {
        turnLabel.setText(text);
    }

    public void setStatusText(String text) {
        statusLabel.setText(text == null ? " " : text);
    }

    public void setUndoEnabled(boolean enabled) {
        undoButton.setEnabled(enabled);
    }

    public void setResignEnabled(boolean enabled) {
        resignButton.setEnabled(enabled);
        drawButton.setEnabled(enabled);
    }

    private void fireNewGame() {
        if (actions != null) {
            actions.onNewGameRequested();
        }
    }

    private void fireUndo() {
        if (actions != null) {
            actions.onUndoRequested();
        }
    }

    private void fireDraw() {
        if (actions != null) {
            actions.onDrawRequested();
        }
    }

    private void fireResign() {
        if (actions != null) {
            actions.onResignRequested();
        }
    }
}