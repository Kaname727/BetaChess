package betachess.com.GUI;

import betachess.com.gamesystem.clock.TimeControl;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.Component;

/**
 * Modal "New game" dialog: choose a time control.
 * Returns null when the user cancels.
 */
public final class NewGameDialog {

    private NewGameDialog() {
    }

    public static TimeControl show(Component parent, TimeControl current) {
        TimeControl[] presets = TimeControl.presets();
        JRadioButton[] buttons = new JRadioButton[presets.length];
        ButtonGroup group = new ButtonGroup();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        panel.add(new JLabel("Choose a time control:"));
        panel.add(new JLabel(" "));

        boolean selectedAny = false;
        for (int i = 0; i < presets.length; i++) {
            buttons[i] = new JRadioButton(presets[i].getLabel());
            if (current != null && presets[i].getLabel().equals(current.getLabel())) {
                buttons[i].setSelected(true);
                selectedAny = true;
            }
            group.add(buttons[i]);
            panel.add(buttons[i]);
        }
        if (!selectedAny) {
            buttons[0].setSelected(true);
        }

        int answer = JOptionPane.showConfirmDialog(parent, panel, "New game",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) {
            return null;
        }
        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i].isSelected()) {
                return presets[i];
            }
        }
        return presets[0];
    }
}