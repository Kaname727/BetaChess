package betachess.com.GUI;

import betachess.com.engine.GameReviewer;
import betachess.com.engine.MoveAssessment;
import betachess.com.engine.MoveClassification;
import betachess.com.gamesystem.ChessGame;
import betachess.com.gamesystem.GameSnapshot;
import betachess.com.gamesystem.Move;
import betachess.com.gamesystem.MoveRecord;
import betachess.com.gamesystem.Position;
import betachess.com.gamesystem.San;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Standalone analysis window: steps through the game and shows Stockfish's
 * verdict on every ply. Can be opened from a live game or a PGN.
 */
public class AnalysisFrame extends JFrame implements GameReviewer.Listener {

    private static final int DEFAULT_DEPTH = 14;

    // --- game replay ---------------------------------------------------
    private final List<GameSnapshot> snapshots = new ArrayList<>();
    private final List<Move>         moves     = new ArrayList<>();
    private final List<String>       notation  = new ArrayList<>();
    private final List<MoveAssessment> assessments = new ArrayList<>();

    // --- UI ------------------------------------------------------------
    private final BoardPanel boardPanel = new BoardPanel(null);
    private final DefaultListModel<String> moveModel = new DefaultListModel<>();
    private final JList<String> moveList = new JList<>(moveModel);
    private final JLabel positionLabel       = new JLabel("Start position", SwingConstants.CENTER);
    private final JLabel evalLabel           = new JLabel("-");
    private final JLabel bestMoveLabel       = new JLabel("-");
    private final JLabel playedMoveLabel     = new JLabel("-");
    private final JLabel classificationLabel = new JLabel("-");
    private final JLabel cpLossLabel         = new JLabel("-");
    private final JProgressBar progressBar   = new JProgressBar();
    private final JButton analyzeBtn         = new JButton("Start analysis");
    private final JButton stopBtn            = new JButton("Stop");

    private final String headerTitle;
    private final String headerSubtitle;
    private final String enginePath;

    private GameReviewer reviewer;
    private int currentPly = 0;

    public AnalysisFrame(String headerTitle, String headerSubtitle,
                         ChessGame game, String enginePath) {
        super("BetaChess \u2013 Analysis");
        this.headerTitle    = headerTitle;
        this.headerSubtitle = headerSubtitle;
        this.enginePath     = enginePath;

        buildPositions(game);
        setLayout(new BorderLayout(6, 6));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildBottom(), BorderLayout.SOUTH);

        installKeyBindings();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1024, 760));
        pack();
        setLocationRelativeTo(null);

        gotoPly(0);
        startAnalysis();
    }

    // ------------------------------------------------------------ positions

    private void buildPositions(ChessGame game) {
        ChessGame replay = new ChessGame();
        snapshots.add(snapshotOf(replay));
        assessments.add(null);

        for (MoveRecord record : game.getHistory()) {
            Move move = record.getMove();
            String san = San.of(replay.getBoard(), move);
            replay.makeMove(move);
            moves.add(move);
            notation.add(san);
            snapshots.add(snapshotOf(replay));
            assessments.add(null);
        }

        for (int i = 0; i < notation.size(); i++) {
            boolean whiteMove = (i % 2 == 0);
            int n = i / 2 + 1;
            moveModel.addElement((whiteMove ? n + ". " : n + "... ") + notation.get(i));
        }
        moveList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        moveList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        moveList.setCellRenderer(new ClassificationRenderer());
        moveList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int idx = moveList.getSelectedIndex();
            if (idx >= 0 && idx + 1 != currentPly) gotoPly(idx + 1);
        });
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
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        JLabel title = new JLabel(headerTitle);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
        JLabel sub = new JLabel(headerSubtitle);
        sub.setFont(sub.getFont().deriveFont(Font.PLAIN, 11f));
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(title);
        left.add(Box.createVerticalStrut(2));
        left.add(sub);
        p.add(left, BorderLayout.WEST);
        return p;
    }

    // ------------------------------------------------------------ center

    private JSplitPane buildCenter() {
        JScrollPane moveScroll = new JScrollPane(moveList);
        moveScroll.setBorder(BorderFactory.createTitledBorder("Moves"));
        moveScroll.setPreferredSize(new Dimension(260, 0));

        JPanel right = new JPanel(new BorderLayout(0, 6));
        right.add(moveScroll, BorderLayout.CENTER);
        right.add(buildDetailPanel(), BorderLayout.NORTH);

        JPanel boardWrapper = new JPanel(new BorderLayout());
        boardWrapper.add(boardPanel, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, boardWrapper, right);
        split.setResizeWeight(1.0);
        split.setBorder(null);
        return split;
    }

    private JPanel buildDetailPanel() {
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createTitledBorder("Current move"));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(detailRow("Eval:",       evalLabel));
        p.add(detailRow("Played:",     playedMoveLabel));
        p.add(detailRow("Best:",       bestMoveLabel));
        p.add(detailRow("Verdict:",    classificationLabel));
        p.add(detailRow("CP loss:",    cpLossLabel));
        p.setPreferredSize(new Dimension(260, 170));
        return p;
    }

    private JPanel detailRow(String label, JLabel value) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel name = new JLabel(label);
        name.setFont(name.getFont().deriveFont(Font.PLAIN));
        value.setFont(value.getFont().deriveFont(Font.BOLD));
        row.add(name);
        row.add(Box.createHorizontalStrut(6));
        row.add(value);
        row.add(Box.createHorizontalGlue());
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        return row;
    }

    // ------------------------------------------------------------ bottom

    private JPanel buildBottom() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(BorderFactory.createEmptyBorder(4, 12, 8, 12));

        JToolBar nav = new JToolBar();
        nav.setFloatable(false);
        JButton first = new JButton("<<");
        JButton prev  = new JButton("<");
        JButton next  = new JButton(">");
        JButton last  = new JButton(">>");
        first.addActionListener(e -> gotoPly(0));
        prev .addActionListener(e -> gotoPly(currentPly - 1));
        next .addActionListener(e -> gotoPly(currentPly + 1));
        last .addActionListener(e -> gotoPly(snapshots.size() - 1));
        nav.add(first);
        nav.add(prev);
        nav.add(next);
        nav.add(last);
        nav.addSeparator();
        nav.add(positionLabel);

        analyzeBtn.addActionListener(e -> startAnalysis());
        stopBtn.addActionListener(e -> stopAnalysis());
        stopBtn.setEnabled(false);
        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(analyzeBtn);
        buttons.add(Box.createHorizontalStrut(6));
        buttons.add(stopBtn);
        buttons.add(Box.createHorizontalStrut(12));
        progressBar.setPreferredSize(new Dimension(220, 18));
        progressBar.setStringPainted(true);
        buttons.add(progressBar);
        buttons.add(Box.createHorizontalGlue());

        p.add(nav, BorderLayout.NORTH);
        p.add(buttons, BorderLayout.SOUTH);
        return p;
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

    // ----------------------------------------------------------- analysis

    private void startAnalysis() {
        if (reviewer != null && reviewer.isRunning()) return;
        if (notation.isEmpty()) {
            progressBar.setString("No moves to analyse");
            return;
        }
        // Reset any previous assessments
        for (int i = 0; i < assessments.size(); i++) assessments.set(i, null);
        progressBar.setValue(0);
        progressBar.setString("0 / " + notation.size());
        analyzeBtn.setEnabled(false);
        stopBtn.setEnabled(true);

        ChessGame game = new ChessGame();
        for (Move m : moves) game.makeMove(m);

        reviewer = new GameReviewer(enginePath, DEFAULT_DEPTH);
        reviewer.start(game, this);
    }

    private void stopAnalysis() {
        if (reviewer != null) reviewer.cancel();
        analyzeBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        progressBar.setString("Cancelled");
    }

    @Override public void onProgress(int completed, int total) {
        progressBar.setMaximum(total);
        progressBar.setValue(completed);
        progressBar.setString(completed + " / " + total);
    }

    @Override public void onPlyAssessed(MoveAssessment assessment) {
        assessments.set(assessment.getPly() + 1, assessment);
        moveList.repaint();
        if (currentPly == assessment.getPly() + 1) {
            updateDetail(assessment);
        }
    }

    @Override public void onComplete(List<MoveAssessment> all) {
        analyzeBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        progressBar.setString("Done");
    }

    @Override public void onError(String message) {
        analyzeBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        progressBar.setString(message);
    }

    // --------------------------------------------------------------- goto

    private void gotoPly(int ply) {
        currentPly = Math.max(0, Math.min(snapshots.size() - 1, ply));
        GameSnapshot s = snapshots.get(currentPly);
        Move lastMove = currentPly > 0 ? moves.get(currentPly - 1) : null;

        boardPanel.setBoard(s.getBoard());
        boardPanel.setSelected(null);
        boardPanel.setLegalTargets(Collections.emptyList());
        boardPanel.setLastMove(lastMove);
        boardPanel.setCheckSquare(
                MoveInCheck(s) ? s.getBoard().findKing(s.getSideToMove()) : null);
        boardPanel.clearArrows();

        // Draw arrows from the assessment of the move that just led here
        MoveAssessment justPlayed = currentPly > 0 ? assessments.get(currentPly) : null;
        if (justPlayed != null) {
            Move best = safeParse(justPlayed.getBestMoveUci());
            if (best != null) {
                Color arrow = justPlayed.playedBestMove()
                        ? BoardTheme.ARROW_GREEN
                        : BoardTheme.ARROW_BLUE;
                boardPanel.addArrow(best.getFrom(), best.getTo(), arrow);
            }
            if (!justPlayed.playedBestMove()) {
                boardPanel.addArrow(justPlayed.getPlayedMove().getFrom(),
                        justPlayed.getPlayedMove().getTo(), BoardTheme.ARROW_RED);
            }
        }

        boardPanel.repaint();

        if (currentPly == 0) {
            positionLabel.setText("Start position");
            moveList.clearSelection();
            clearDetail();
        } else {
            boolean whiteMove = ((currentPly - 1) % 2 == 0);
            int n = (currentPly - 1) / 2 + 1;
            positionLabel.setText("Move " + n + (whiteMove ? "." : "...")
                    + " " + notation.get(currentPly - 1)
                    + "   (" + currentPly + "/" + notation.size() + ")");
            moveList.setSelectedIndex(currentPly - 1);
            moveList.ensureIndexIsVisible(currentPly - 1);
            updateDetail(assessments.get(currentPly));
        }
    }

    private static boolean MoveInCheck(GameSnapshot s) {
        return betachess.com.gamesystem.MoveGenerator
                .isInCheck(s.getBoard(), s.getSideToMove());
    }

    private static Move safeParse(String uci) {
        if (uci == null) return null;
        try { return Move.fromUci(uci); }
        catch (RuntimeException ex) { return null; }
    }

    // ------------------------------------------------------------ detail

    private void updateDetail(MoveAssessment a) {
        if (a == null) {
            clearDetail();
            return;
        }
        evalLabel.setText(formatEval(a.getEvalBefore()));
        playedMoveLabel.setText(a.getPlayedSan());
        bestMoveLabel.setText(a.getBestMoveSan() == null ? "-" : a.getBestMoveSan());
        classificationLabel.setText(a.getClassification().name());
        classificationLabel.setForeground(a.getClassification().getColor());
        cpLossLabel.setText(a.getCentipawnLoss() + " cp");
        cpLossLabel.setForeground(a.getClassification().getColor());
    }

    private void clearDetail() {
        evalLabel.setText("-");
        playedMoveLabel.setText("-");
        bestMoveLabel.setText("-");
        classificationLabel.setText("-");
        classificationLabel.setForeground(Color.GRAY);
        cpLossLabel.setText("-");
        cpLossLabel.setForeground(Color.GRAY);
    }

    private static String formatEval(int cp) {
        if (Math.abs(cp) >= 10000) {
            int n = (Math.abs(cp) - 10000) / 10000 + 1;
            return (cp > 0 ? "+M" : "-M") + n;
        }
        return String.format("%+.2f", cp / 100.0);
    }

    // ------------------------------------------------------------ renderer

    private final class ClassificationRenderer extends javax.swing.DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            MoveAssessment a = (index >= 0 && index + 1 < assessments.size())
                    ? assessments.get(index + 1) : null;
            if (a != null) {
                String symbol = a.getClassification().getSymbol();
                setText(value + "  " + symbol);
                if (!isSelected) setForeground(a.getClassification().getColor());
            } else {
                setText(String.valueOf(value));
                if (!isSelected) setForeground(Color.DARK_GRAY);
            }
            return this;
        }
    }
}