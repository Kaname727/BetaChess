package betachess.com.GUI;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

/**
 * Shows the ECO code and the name of the opening currently on the board.
 */
public class OpeningPanel extends JPanel {

    private final JLabel ecoLabel  = new JLabel("—");
    private final JLabel nameLabel = new JLabel(" ");

    public OpeningPanel() {
        setLayout(new BorderLayout(6, 0));
        setBorder(BorderFactory.createEmptyBorder(2, 0, 4, 0));

        ecoLabel.setFont(ecoLabel.getFont().deriveFont(Font.BOLD, 12f));
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.PLAIN, 12f));

        add(ecoLabel, BorderLayout.WEST);
        add(nameLabel, BorderLayout.CENTER);
    }

    public void setOpening(String eco, String name) {
        ecoLabel.setText(eco == null || eco.isEmpty() ? "—" : eco);
        nameLabel.setText(name == null || name.isEmpty() ? "Unknown opening" : name);
    }

    public void clear() {
        setOpening("—", "Unknown opening");
    }
}