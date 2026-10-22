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

    private final JLabel turnLabel   = new JLabel("White to move");
    private final JLabel statusLabel = new JLabel(" ");

    private final JButton newGameButton = new JButton("New game");
    private final JButton undoButton    = new JButton("Undo");
    private final JButton clearButton   = new JButton("Clear");     // NEW
    private final JButton drawButton    = new JButton("Offer draw");
    private final JButton resignButton  = new JButton("Resign");


    private GameActions actions;

    public StatusPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        turnLabel.setFont(turnLabel.getFont().deriveFont(Font.BOLD, 15f));
        turnLabel.setAlignmentX(LEFT_ALIGNMENT);
        statusLabel.setAlignmentX(LEFT_ALIGNMENT);

        clearButton.setToolTipText("Clear arrows, square highlights and selection");

        add(turnLabel);
        add(statusLabel);
        add(Box.createVerticalStrut(8));
        add(buttonRow(newGameButton, undoButton, clearButton));    // NEW layout
        add(Box.createVerticalStrut(4));
        add(buttonRow(drawButton, resignButton));

        newGameButton.addActionListener(event -> fireNewGame());
        undoButton.addActionListener(event -> fireUndo());
        clearButton.addActionListener(event -> fireClear());   // only if you have a Clear button
        drawButton.addActionListener(event -> fireDraw());
        resignButton.addActionListener(event -> fireResign());
    }

    /** One horizontal row of buttons, any number of them. */
    private JPanel buttonRow(JButton... buttons) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setAlignmentX(LEFT_ALIGNMENT);
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setAlignmentX(LEFT_ALIGNMENT);
            if (i > 0) {
                row.add(Box.createHorizontalStrut(6));
            }
            row.add(buttons[i]);
        }
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
        System.err.println("[StatusPanel] New game clicked, actions=" + actions);
        if (actions != null) actions.onNewGameRequested();
    }
    private void fireUndo() {
        System.err.println("[StatusPanel] Undo clicked, actions=" + actions);
        if (actions != null) actions.onUndoRequested();
    }
    private void fireClear() {
        System.err.println("[StatusPanel] Clear clicked, actions=" + actions);
        if (actions != null) actions.onClearRequested();
    }
    private void fireDraw() {
        System.err.println("[StatusPanel] Draw clicked, actions=" + actions);
        if (actions != null) actions.onDrawRequested();
    }
    private void fireResign() {
        System.err.println("[StatusPanel] Resign clicked, actions=" + actions);
        if (actions != null) actions.onResignRequested();
    }
}