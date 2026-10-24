package betachess.com.GUI;

import betachess.com.gamesystem.ChessGame;
import betachess.com.gamesystem.GameSnapshot;
import betachess.com.gamesystem.Move;
import betachess.com.gamesystem.Position;
import betachess.com.gamesystem.San;
import betachess.com.gamesystem.record.GameRecord;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Read-only viewer for a {@link GameRecord}: shows the board at any ply,
 * with first / previous / next / last navigation and a clickable move list.
 */
public class ReviewFrame extends JFrame {

    private final GameRecord record;
    private final List<GameSnapshot> snapshots = new ArrayList<>();
    private final List<Move> moves = new ArrayList<>();
    private final List<String> notation = new ArrayList<>();

    private final BoardPanel boardPanel = new BoardPanel(null);
    private final DefaultListModel<String> moveModel = new DefaultListModel<>();
    private final JList<String> moveList = new JList<>(moveModel);
    private final JLabel positionLabel = new JLabel("Start position", SwingConstants.CENTER);

    private int currentPly = 0;

    public ReviewFrame(GameRecord record, String title) {
        super("BetaChess \u2013 " + title);
        this.record = record;

        buildPgnReplay();

        setLayout(new BorderLayout(6, 6));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildNavigation(), BorderLayout.SOUTH);

        installKeyBindings();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(900, 700));

        gotoPly(0);
    }

    // ------------------------------------------------------- record replay

    private void buildPgnReplay() {
        ChessGame replay = new ChessGame();
        snapshots.add(snapshotOf(replay));
        for (GameRecord.Ply ply : record.getPlies()) {
            Move move = findMove(replay, ply.getSan());
            if (move == null) {
                System.err.println("[review] cannot replay: " + ply.getSan());
                break;
            }
            String san = San.of(replay.getBoard(), move);
            replay.makeMove(move);
            moves.add(move);
            notation.add(san);
            snapshots.add(snapshotOf(replay));
        }
    }

    private static Move findMove(ChessGame game, String san) {
        String target = normalize(san);
        for (Move move : game.getLegalMoves()) {
            if (normalize(San.of(game.getBoard(), move)).equals(target)) {
                return move;
            }
        }
        return null;
    }

    private static String normalize(String san) {
        return san.trim()
                .replace("/", "=")
                .replace("0-0-0", "O-O-O")
                .replace("0-0", "O-O")
                .replaceAll("[+#!?]+", "");
    }

    private static GameSnapshot snapshotOf(ChessGame game) {
        return new GameSnapshot(
                game.getBoard().copy(),
                game.getCastlingRights().copy(),
                game.getEnPassantTarget(),
                game.getSideToMove(),
                game.getHalfmoveClock(),
                game.getFullmoveNumber());
    }

    // ------------------------------------------------------------ header

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JLabel titleLabel = new JLabel(
                record.getTag("White") + "  vs  " + record.getTag("Black"));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 15f));

        StringBuilder subtitle = new StringBuilder();
        subtitle.append("Result: ").append(record.getResult());
        if (record.getTag("TimeControl") != null) {
            subtitle.append("   \u2022   Time: ").append(record.getTag("TimeControl"));
        }
        if (record.getTag("Date") != null) {
            subtitle.append("   \u2022   ").append(record.getTag("Date"));
        }
        JLabel subtitleLabel = new JLabel(subtitle.toString());
        subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(Font.PLAIN, 11f));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(titleLabel);
        left.add(Box.createVerticalStrut(2));
        left.add(subtitleLabel);
        panel.add(left, BorderLayout.WEST);

        return panel;
    }

    // ------------------------------------------------------------ center

    private JSplitPane buildCenter() {
        populateMoveList();

        JScrollPane listScroll = new JScrollPane(moveList);
        listScroll.setBorder(BorderFactory.createTitledBorder("Moves"));
        listScroll.setPreferredSize(new Dimension(220, 0));

        JPanel boardWrapper = new JPanel(new BorderLayout());
        boardWrapper.add(boardPanel, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, boardWrapper, listScroll);
        split.setResizeWeight(1.0);
        split.setBorder(null);
        return split;
    }

    private void populateMoveList() {
        moveModel.clear();
        for (int i = 0; i < notation.size(); i++) {
            boolean whiteMove = (i % 2 == 0);
            int moveNumber = i / 2 + 1;
            String prefix = whiteMove ? (moveNumber + ". ") : (moveNumber + "... ");
            moveModel.addElement(prefix + notation.get(i));
        }
        moveList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        moveList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        moveList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int index = moveList.getSelectedIndex();
            if (index >= 0 && index + 1 != currentPly) {
                gotoPly(index + 1);
            }
        });
    }

    // ------------------------------------------------------------ navigation

    private JToolBar buildNavigation() {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);

        JButton startBtn = new JButton("<<");
        JButton prevBtn  = new JButton("<");
        JButton nextBtn  = new JButton(">");
        JButton endBtn   = new JButton(">>");

        startBtn.addActionListener(e -> gotoPly(0));
        prevBtn.addActionListener (e -> gotoPly(currentPly - 1));
        nextBtn.addActionListener (e -> gotoPly(currentPly + 1));
        endBtn.addActionListener  (e -> gotoPly(snapshots.size() - 1));

        bar.add(startBtn);
        bar.add(prevBtn);
        bar.add(nextBtn);
        bar.add(endBtn);
        bar.addSeparator();
        bar.add(positionLabel);

        return bar;
    }

    private void installKeyBindings() {
        JPanel root = (JPanel) getContentPane();
        bind(root, "HOME",  e -> gotoPly(0));
        bind(root, "END",   e -> gotoPly(snapshots.size() - 1));
        bind(root, "LEFT",  e -> gotoPly(currentPly - 1));
        bind(root, "RIGHT", e -> gotoPly(currentPly + 1));
    }

    private static void bind(JPanel root, String key, java.awt.event.ActionListener action) {
        String name = "action-" + key;
        root.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(key), name);
        root.getActionMap().put(name, new javax.swing.AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.actionPerformed(e); }
        });
    }

    // ------------------------------------------------------------- gotoPly

    private void gotoPly(int ply) {
        int clamped = Math.max(0, Math.min(snapshots.size() - 1, ply));
        if (clamped == currentPly && snapshots.size() > 1) {
            // still repaint (harmless); guards against off-by-one edge cases
        }
        currentPly = clamped;

        GameSnapshot snap = snapshots.get(currentPly);
        Move lastMove = currentPly > 0 ? moves.get(currentPly - 1) : null;
        Position checkSquare = snap.getSideToMove() != null && inCheck(snap)
                ? snap.getBoard().findKing(snap.getSideToMove()) : null;

        boardPanel.setBoard(snap.getBoard());
        boardPanel.setSelected(null);
        boardPanel.setLegalTargets(Collections.emptyList());
        boardPanel.setLastMove(lastMove);
        boardPanel.setCheckSquare(checkSquare);
        boardPanel.repaint();

        if (currentPly == 0) {
            positionLabel.setText("Start position");
            moveList.clearSelection();
        } else {
            boolean whiteMove = ((currentPly - 1) % 2 == 0);
            int moveNumber = (currentPly - 1) / 2 + 1;
            positionLabel.setText("Move " + moveNumber + (whiteMove ? "." : "...")
                    + " " + notation.get(currentPly - 1)
                    + "   (" + currentPly + "/" + notation.size() + ")");
            moveList.setSelectedIndex(currentPly - 1);
            moveList.ensureIndexIsVisible(currentPly - 1);
        }
    }

    private boolean inCheck(GameSnapshot snap) {
        return betachess.com.gamesystem.MoveGenerator
                .isInCheck(snap.getBoard(), snap.getSideToMove());
    }
}