package betachess.com.GUI;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

/**
 * Read-only list of the moves played so far (two plies per line).
 */
public class MoveListPanel extends JPanel {

    private final DefaultListModel<String> model = new DefaultListModel<String>();
    private final JList<String> list = new JList<String>(model);

    public MoveListPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Moves"));
        setPreferredSize(new Dimension(210, 0));

        list.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        add(new JScrollPane(list), BorderLayout.CENTER);
    }

    public void setMoves(List<String> moves) {
        model.clear();
        for (int index = 0; index < moves.size(); index += 2) {
            StringBuilder line = new StringBuilder();
            line.append(index / 2 + 1).append(". ").append(moves.get(index));
            if (index + 1 < moves.size()) {
                line.append("  ").append(moves.get(index + 1));
            }
            model.addElement(line.toString());
        }
        if (!model.isEmpty()) {
            list.ensureIndexIsVisible(model.size() - 1);
        }
    }
}