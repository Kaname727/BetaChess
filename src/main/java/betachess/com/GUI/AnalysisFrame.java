package betachess.com.GUI;

import betachess.com.engine.AccuracyCalculator;
import betachess.com.engine.EngineEvaluation;
import betachess.com.engine.GameReviewer;
import betachess.com.engine.MoveAssessment;
import betachess.com.engine.MoveClassification;
import betachess.com.engine.Score;
import betachess.com.gamesystem.ChessGame;
import betachess.com.gamesystem.GameSnapshot;
import betachess.com.gamesystem.Move;
import betachess.com.gamesystem.MoveRecord;
import betachess.com.gamesystem.San;
import betachess.com.piece.PieceColor;

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
 * Standalone analysis window. Steps through the game and shows Stockfish's
 * verdict on every ply, an eval bar, live accuracy, and the top-5 engine moves.
 */
public class AnalysisFrame extends JFrame implements GameReviewer.Listener {

    private static final int    DEFAULT_DEPTH = 14;
    private static final String PLACEHOLDER = "-";

    // --- replay --------------------------------------------------------
    private final List<GameSnapshot>   snapshots   = new ArrayList<>();
    private final List<Move>           moves       = new ArrayList<>();
    private final List<String>         notation    = new ArrayList<>();
    private final List<MoveAssessment> assessments = new ArrayList<>();

    // --- running accuracy accumulators ---------------------------------
    private double whiteAccSum   = 0;
    private int    whiteAccCount = 0;
    private double blackAccSum   = 0;
    private int    blackAccCount = 0;

    // --- widgets -------------------------------------------------------
    private final EvalBar       evalBar        = new EvalBar();
    private final BoardPanel    boardPanel     = new BoardPanel(null);
    private final AccuracyPanel accuracyPanel  = new AccuracyPanel();
    private final TopMovesPanel topMovesPanel  = new TopMovesPanel();
    private final DefaultListModel<String> moveModel = new DefaultListModel<>();
    private final JList<String> moveList       = new JList<>(moveModel);
    private final JLabel positionLabel       = new JLabel("Start position", SwingConstants.CENTER);
    private final JLabel evalLabel           = new JLabel(PLACEHOLDER);
    private final JLabel bestMoveLabel       = new JLabel(PLACEHOLDER);
    private final JLabel playedMoveLabel     = new JLabel(PLACEHOLDER);
    private final JLabel classificationLabel = new JLabel(PLACEHOLDER);
    private final JLabel cpLossLabel         = new JLabel(PLACEHOLDER);
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
        setMinimumSize(new Dimension(1120, 800));
        pack();
        setLocationRelativeTo(null);

        accuracyPanel.clear();
        evalBar.clear();
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
        // Board + eval bar
        JPanel boardWithEval = new JPanel(new BorderLayout(4, 0));
        boardWithEval.setOpaque(false);
        boardWithEval.add(evalBar, BorderLayout.WEST);
        boardWithEval.add(boardPanel, BorderLayout.CENTER);

        // Right column — accuracy card strip on top, then detail + top moves + move list
        JPanel rightTop = new JPanel();
        rightTop.setLayout(new BoxLayout(rightTop, BoxLayout.Y_AXIS));
        rightTop.add(accuracyPanel);
        rightTop.add(Box.createVerticalStrut(4));
        rightTop.add(buildDetailPanel());
        rightTop.add(Box.createVerticalStrut(4));
        rightTop.add(topMovesPanel);

        JScrollPane moveScroll = new JScrollPane(moveList);
        moveScroll.setBorder(BorderFactory.createTitledBorder("Moves"));

        JPanel right = new JPanel(new BorderLayout(0, 6));
        right.add(rightTop, BorderLayout.NORTH);
        right.add(moveScroll, BorderLayout.CENTER);
        right.setPreferredSize(new Dimension(320, 0));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, boardWithEval, right);
        split.setResizeWeight(1.0);
        split.setBorder(null);
        return split;
    }

    private JPanel buildDetailPanel() {
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createTitledBorder("Current move"));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(detailRow("Eval:",    evalLabel));
        p.add(detailRow("Played:",  playedMoveLabel));
        p.add(detailRow("Best:",    bestMoveLabel));
        p.add(detailRow("Verdict:", classificationLabel));
        p.add(detailRow("CP loss:", cpLossLabel));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
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

        // Navigation row
        JToolBar nav = new JToolBar();
        nav.setFloatable(false);
        JButton first = new JButton("<<");
        JButton prev  = new JButton("<");
        JButton next  = new JButton(">");
        JButton last  = new JButton(">>");
        JButton clear = new JButton("Clear annotations");
        first.addActionListener(e -> gotoPly(0));
        prev .addActionListener(e -> gotoPly(currentPly - 1));
        next .addActionListener(e -> gotoPly(currentPly + 1));
        last .addActionListener(e -> gotoPly(snapshots.size() - 1));
        clear.addActionListener(e -> {
            boardPanel.clearAnnotations();
            boardPanel.repaint();
        });

        nav.add(first);
        nav.add(prev);
        nav.add(next);
        nav.add(last);
        nav.addSeparator();
        nav.add(positionLabel);
        nav.addSeparator();
        nav.add(clear);

        // Analyze controls
        analyzeBtn.addActionListener(e -> startAnalysis());
        stopBtn.addActionListener(e -> stopAnalysis());
        stopBtn.setEnabled(false);

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.X_AXIS));
        controls.add(analyzeBtn);
        controls.add(Box.createHorizontalStrut(6));
        controls.add(stopBtn);
        controls.add(Box.createHorizontalStrut(12));
        progressBar.setPreferredSize(new Dimension(220, 18));
        progressBar.setStringPainted(true);
        controls.add(progressBar);
        controls.add(Box.createHorizontalGlue());

        p.add(nav, BorderLayout.NORTH);
        p.add(controls, BorderLayout.SOUTH);
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
        for (int i = 0; i < assessments.size(); i++) assessments.set(i, null);
        whiteAccSum = 0; whiteAccCount = 0;
        blackAccSum = 0; blackAccCount = 0;
        accuracyPanel.clear();
        topMovesPanel.clear();
        evalBar.clear();

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

    @Override public void onPlyAssessed(MoveAssessment a) {
        assessments.set(a.getPly() + 1, a);
        moveList.repaint();

        if (a.getClassification() != MoveClassification.BOOK) {
            double acc = AccuracyCalculator.accuracy(a.getEvalBefore(), a.getEvalAfter());
            boolean whiteMover = (a.getPly() % 2 == 0);
            if (whiteMover) { whiteAccSum += acc; whiteAccCount++; }
            else            { blackAccSum += acc; blackAccCount++; }
        }
        updateAccuracyDisplay();

        if (currentPly == a.getPly() + 1) {
            updateDetail(a);
            updateEvalBar(currentPly);
            updateTopMoves(currentPly);
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

    private void updateAccuracyDisplay() {
        double w = whiteAccCount == 0 ? 100.0 : whiteAccSum / whiteAccCount;
        double b = blackAccCount == 0 ? 100.0 : blackAccSum / blackAccCount;
        if (whiteAccCount == 0 && blackAccCount == 0) {
            accuracyPanel.clear();
        } else {
            accuracyPanel.setValues(w, b);
        }
    }

    // --------------------------------------------------------------- goto

    private void gotoPly(int ply) {
        currentPly = Math.max(0, Math.min(snapshots.size() - 1, ply));
        GameSnapshot s = snapshots.get(currentPly);
        Move lastMove = currentPly > 0 ? moves.get(currentPly - 1) : null;

        boardPanel.setBoard(s.getBoard());
        boardPanel.setSelected(null);
        boardPanel.setLegalTargets(Collections.<betachess.com.gamesystem.Position>emptyList());
        boardPanel.setLastMove(lastMove);
        boardPanel.setCheckSquare(
                betachess.com.gamesystem.MoveGenerator.isInCheck(s.getBoard(), s.getSideToMove())
                        ? s.getBoard().findKing(s.getSideToMove()) : null);
        boardPanel.clearArrows();

        MoveAssessment justPlayed = currentPly > 0 ? assessments.get(currentPly) : null;
        if (justPlayed != null) {
            Move best = safeParse(justPlayed.getBestMoveUci());
            if (best != null) {
                Color arrow = justPlayed.playedBestMove()
                        ? BoardTheme.ARROW_GREEN : BoardTheme.ARROW_BLUE;
                boardPanel.addArrow(best.getFrom(), best.getTo(), arrow);
            }
            if (!justPlayed.playedBestMove()) {
                boardPanel.addArrow(
                        justPlayed.getPlayedMove().getFrom(),
                        justPlayed.getPlayedMove().getTo(),
                        BoardTheme.ARROW_RED);
            }
        }
        boardPanel.repaint();

        updateEvalBar(currentPly);
        updateTopMoves(currentPly);

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

    private static Move safeParse(String uci) {
        if (uci == null) return null;
        try { return Move.fromUci(uci); }
        catch (RuntimeException ex) { return null; }
    }

    // -------------------------------------------------------------- eval bar

    private void updateEvalBar(int ply) {
        int eval = evalAtPly(ply);
        if (eval == Integer.MIN_VALUE) {
            evalBar.clear();
        } else if (Math.abs(eval) >= 10000) {
            evalBar.setMate(eval > 0);
        } else {
            evalBar.setCentipawns(eval);
        }
    }

    /** White-POV eval at the given position, or Integer.MIN_VALUE if unknown. */
    private int evalAtPly(int ply) {
        if (ply == 0) {
            if (assessments.size() > 1 && assessments.get(1) != null) {
                return assessments.get(1).getEvalBefore();
            }
            return Integer.MIN_VALUE;
        }
        if (ply < assessments.size() && assessments.get(ply) != null) {
            MoveAssessment a = assessments.get(ply);
            boolean whiteMover = (a.getPly() % 2 == 0);
            return whiteMover ? a.getEvalAfter() : -a.getEvalAfter();
        }
        return Integer.MIN_VALUE;
    }

    // ------------------------------------------------------------- top moves

    private void updateTopMoves(int ply) {
        // Top moves for position at `ply` are the "before" analysis of ply+1
        if (ply + 1 >= assessments.size() || assessments.get(ply + 1) == null) {
            topMovesPanel.clear();
            return;
        }
        MoveAssessment next = assessments.get(ply + 1);
        List<EngineEvaluation> top = next.getTopMoves();
        if (top.isEmpty()) {
            topMovesPanel.clear();
            return;
        }
        GameSnapshot before = snapshots.get(ply);
        Move played = next.getPlayedMove();

        List<TopMovesPanel.Entry> entries = new ArrayList<>();
        for (int i = 0; i < top.size() && i < 5; i++) {
            EngineEvaluation e = top.get(i);
            String san = uciToSan(e.getBestMoveUci(), before);
            boolean isPlayed = played != null && e.getBestMoveUci() != null
                    && e.getBestMoveUci().equals(played.toUci());
            entries.add(new TopMovesPanel.Entry(san, formatScore(e.getScore()),
                    isPlayed, i == 0));
        }
        topMovesPanel.setEntries(entries);
    }

    private static String uciToSan(String uci, GameSnapshot snap) {
        if (uci == null) return PLACEHOLDER;
        try {
            Move m = Move.fromUci(uci);
            return San.of(snap.getBoard(), m);
        } catch (RuntimeException ex) {
            return uci;
        }
    }

    private static String formatScore(Score score) {
        if (score == null) return PLACEHOLDER;
        if (score.getType() == Score.Type.MATE) {
            int m = score.getValue();
            return (m >= 0 ? "M" : "-M") + Math.abs(m);
        }
        return String.format("%+.2f", score.toCentipawns() / 100.0);
    }

    // ------------------------------------------------------------ detail

    private void updateDetail(MoveAssessment a) {
        if (a == null) { clearDetail(); return; }
        evalLabel.setText(formatEval(a.getEvalBefore()));
        playedMoveLabel.setText(a.getPlayedSan());
        String best = a.getBestMoveUci();
        GameSnapshot before = snapshots.get(a.getPly());
        bestMoveLabel.setText(best == null ? PLACEHOLDER : uciToSan(best, before));
        classificationLabel.setText(a.getClassification().name());
        classificationLabel.setForeground(a.getClassification().getColor());
        cpLossLabel.setText(a.getCentipawnLoss() + " cp");
        cpLossLabel.setForeground(a.getClassification().getColor());
    }

    private void clearDetail() {
        evalLabel.setText(PLACEHOLDER);
        playedMoveLabel.setText(PLACEHOLDER);
        bestMoveLabel.setText(PLACEHOLDER);
        classificationLabel.setText(PLACEHOLDER);
        classificationLabel.setForeground(Color.GRAY);
        cpLossLabel.setText(PLACEHOLDER);
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
                setText(value + "  " + a.getClassification().getSymbol());
                if (!isSelected) setForeground(a.getClassification().getColor());
            } else {
                setText(String.valueOf(value));
                if (!isSelected) setForeground(Color.DARK_GRAY);
            }
            return this;
        }
    }
}