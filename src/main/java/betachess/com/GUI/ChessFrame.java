package betachess.com.GUI;

import betachess.com.engine.EngineConfig;
import betachess.com.engine.EnginePlayer;
import betachess.com.engine.GameAnalyzer;
import betachess.com.gamesystem.Board;
import betachess.com.gamesystem.ChessGame;
import betachess.com.gamesystem.Move;
import betachess.com.gamesystem.Position;
import betachess.com.gamesystem.clock.ChessClock;
import betachess.com.gamesystem.clock.TimeControl;
import betachess.com.gamesystem.net.ChessServer;
import betachess.com.gamesystem.net.Protocol;
import betachess.com.piece.PieceColor;
import betachess.com.gamesystem.record.GameRecord;
import betachess.com.gamesystem.record.GameRecordStore;
import betachess.com.gamesystem.record.PgnReader;
import betachess.com.gamesystem.record.PgnWriter;
import betachess.com.gamesystem.San;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.util.List;

public class ChessFrame extends JFrame implements GameView {

    private final BoardPanel    boardPanel;
    private final EvalBar       evalBar;
    private final PlayerPanel   topPlayer;
    private final PlayerPanel   bottomPlayer;
    private final StatusPanel   statusPanel;
    private final OpeningPanel  openingPanel;
    private final AnalysisPanel analysisPanel;
    private final AccuracyPanel accuracyPanel;
    private final MoveListPanel moveListPanel;

    private GameController controller;
    private EnginePlayer   enginePlayer;
    private GameAnalyzer   analyzer;
    private JCheckBoxMenuItem engineToggleItem;
    private JCheckBoxMenuItem analysisToggleItem;
    private TimeControl currentTimeControl = TimeControl.unlimited();

    private PieceColor topColor    = PieceColor.BLACK;
    private PieceColor bottomColor = PieceColor.WHITE;
    private ChessClock currentClock;

    public ChessFrame() {
        super("BetaChess");

        boardPanel    = new BoardPanel(null);
        evalBar       = new EvalBar();
        topPlayer     = new PlayerPanel(PieceColor.BLACK);
        bottomPlayer  = new PlayerPanel(PieceColor.WHITE);
        statusPanel   = new StatusPanel();
        openingPanel  = new OpeningPanel();
        analysisPanel = new AnalysisPanel();
        accuracyPanel = new AccuracyPanel();
        moveListPanel = new MoveListPanel();

        analysisPanel.setVisible(false);

        // ----- board: eval bar + board, flanked by player plates -----
        JPanel boardWithEval = new JPanel(new BorderLayout(6, 0));
        boardWithEval.setOpaque(false);
        boardWithEval.add(evalBar, BorderLayout.WEST);
        boardWithEval.add(boardPanel, BorderLayout.CENTER);

        JPanel boardArea = new JPanel(new BorderLayout(0, 6));
        boardArea.setOpaque(false);
        boardArea.add(topPlayer, BorderLayout.NORTH);
        boardArea.add(boardWithEval, BorderLayout.CENTER);
        boardArea.add(bottomPlayer, BorderLayout.SOUTH);

        JPanel boardWrapper = new JPanel(new GridBagLayout());
        boardWrapper.add(boardArea);

        // ----- bottom of side column: accuracy | move list ---------
        JPanel accuracyWrapper = new JPanel(new BorderLayout());
        accuracyWrapper.setOpaque(false);
        accuracyWrapper.setPreferredSize(new Dimension(120, 0));
        accuracyWrapper.add(accuracyPanel, BorderLayout.NORTH);

        JPanel lowerRow = new JPanel(new BorderLayout(8, 0));
        lowerRow.setOpaque(false);
        lowerRow.add(accuracyWrapper, BorderLayout.WEST);
        lowerRow.add(moveListPanel, BorderLayout.CENTER);

        // ----- side column -----------------------------------------
        JPanel side = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0; side.add(statusPanel,   gbc);
        gbc.gridy = 1; side.add(openingPanel,  gbc);
        gbc.gridy = 2; side.add(analysisPanel, gbc);
        gbc.gridy = 3;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        side.add(lowerRow, gbc);
        side.setPreferredSize(new Dimension(380, 0));

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        content.add(boardWrapper, BorderLayout.CENTER);
        content.add(side, BorderLayout.EAST);

        setContentPane(content);
        setJMenuBar(buildMenuBar());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);

        startLocalGame(currentTimeControl);
    }

    // ------------------------------------------------------------------ menu

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu gameMenu = new JMenu("Game");
        JMenuItem localItem = new JMenuItem("New local game...");
        localItem.addActionListener(e -> promptForNewGame());
        JMenuItem hostItem = new JMenuItem("Host online game...");
        hostItem.addActionListener(e -> hostGame());
        JMenuItem joinItem = new JMenuItem("Join online game...");
        joinItem.addActionListener(e -> joinGame());
        JMenuItem clearAnnotationsItem = new JMenuItem("Clear annotations");
        clearAnnotationsItem.addActionListener(e -> boardPanel.clearAnnotations());
        JMenuItem exitItem = new JMenuItem("Exit");
        JMenuItem saveItem = new JMenuItem("Save game...");
        saveItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        saveItem.addActionListener(e -> saveCurrentGame());

        JMenuItem openItem = new JMenuItem("Open saved game...");
        openItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        JMenuItem analyzeCurrentItem = new JMenuItem("Analyze current game...");
        analyzeCurrentItem.addActionListener(e -> analyzeCurrentGame());

        JMenuItem analyzePgnItem = new JMenuItem("Open & analyze PGN...");
        analyzePgnItem.addActionListener(e -> openAndAnalyzePgn());

        gameMenu.add(analyzeCurrentItem);
        gameMenu.add(analyzePgnItem);
        openItem.addActionListener(e -> openSavedGame());

        gameMenu.add(localItem);
        gameMenu.add(hostItem);
        gameMenu.add(joinItem);
        gameMenu.addSeparator();
        gameMenu.add(saveItem);
        gameMenu.add(openItem);
        gameMenu.addSeparator();
        gameMenu.add(clearAnnotationsItem);
        gameMenu.addSeparator();
        gameMenu.add(exitItem);
        exitItem.addActionListener(e -> dispose());
        gameMenu.add(localItem);
        gameMenu.addSeparator();
        gameMenu.add(hostItem);
        gameMenu.add(joinItem);
        gameMenu.addSeparator();
        gameMenu.add(clearAnnotationsItem);
        gameMenu.addSeparator();
        gameMenu.add(exitItem);
        bar.add(gameMenu);

        JMenu engineMenu = new JMenu("Engine");
        engineToggleItem = new JCheckBoxMenuItem("Enable Stockfish (Black)");
        engineToggleItem.addActionListener(e -> toggleEngine(engineToggleItem.isSelected()));
        analysisToggleItem = new JCheckBoxMenuItem("Show analysis");
        analysisToggleItem.addActionListener(e -> toggleAnalysis(analysisToggleItem.isSelected()));
        JMenuItem infoItem = new JMenuItem("Engine info...");
        infoItem.addActionListener(e -> showMessage(
                String.join("\n", EngineConfig.diagnostics())));
        engineMenu.add(engineToggleItem);
        engineMenu.add(analysisToggleItem);
        engineMenu.addSeparator();
        engineMenu.add(infoItem);
        bar.add(engineMenu);

        JMenu viewMenu = new JMenu("View");
        JRadioButtonMenuItem lightItem = new JRadioButtonMenuItem("Light theme", true);
        JRadioButtonMenuItem darkItem  = new JRadioButtonMenuItem("Dark theme");
        ButtonGroup themeGroup = new ButtonGroup();
        themeGroup.add(lightItem);
        themeGroup.add(darkItem);
        lightItem.addActionListener(e -> ThemeManager.apply(ThemeManager.Theme.LIGHT));
        darkItem.addActionListener(e -> ThemeManager.apply(ThemeManager.Theme.DARK));
        viewMenu.add(lightItem);
        viewMenu.add(darkItem);
        bar.add(viewMenu);

        return bar;
    }

    // --------------------------------------------------------------- sessions

    private void promptForNewGame() {
        TimeControl chosen = NewGameDialog.show(this, currentTimeControl);
        if (chosen == null) return;
        currentTimeControl = chosen;
        startLocalGame(chosen);
    }
    private void saveCurrentGame() {
        if (controller == null) return;
        ChessGame game = controller.getGame();

        GameRecord record = PgnWriter.toRecord(
                game,
                whiteName(),
                blackName(),
                controller.getTimeControl(),
                controller.isNetworked() ? "Online" : "Local");

        if (record.plyCount() == 0) {
            int answer = JOptionPane.showConfirmDialog(this,
                    "No moves have been played yet. Save anyway?", "Save game",
                    JOptionPane.YES_NO_OPTION);
            if (answer != JOptionPane.YES_OPTION) return;
        }

        java.nio.file.Path path = GameRecordStore.promptForSavePath(this, record);
        if (path == null) return;

        try {
            PgnWriter.write(record, path);
            showStatus("Saved to " + path.getFileName());
        } catch (IOException ex) {
            showMessage("Could not save: " + ex.getMessage());
        }
    }

    private void analyzeCurrentGame() {
        if (controller == null) return;
        ChessGame game = controller.getGame();
        if (game.getHistory().isEmpty()) {
            showMessage("No moves to analyse yet.");
            return;
        }
        String title = whiteName() + "  vs  " + blackName();
        String sub   = "Result: " + game.getStatus()
                + "   \u2022   Time: " + controller.getTimeControl().getLabel();
        try {
            AnalysisFrame frame = new AnalysisFrame(title, sub, game,
                    EngineConfig.resolveExecutable());
            frame.setVisible(true);
        } catch (Exception ex) {
            showMessage("Could not start analysis: " + ex.getMessage());
        }
    }

    private void openAndAnalyzePgn() {
        java.nio.file.Path path = GameRecordStore.promptForOpenPath(this);
        if (path == null) return;
        try {
            GameRecord record = PgnReader.read(path);
            ChessGame replay = new ChessGame();
            for (GameRecord.Ply ply : record.getPlies()) {
                Move m = findMoveForSan(replay, ply.getSan());
                if (m == null) break;
                replay.makeMove(m);
            }
            String title = record.getTag("White") + "  vs  " + record.getTag("Black");
            String sub   = "Result: " + record.getResult();
            AnalysisFrame frame = new AnalysisFrame(title, sub, replay,
                    EngineConfig.resolveExecutable());
            frame.setVisible(true);
        } catch (Exception ex) {
            showMessage("Could not open file: " + ex.getMessage());
        }
    }

    private static Move findMoveForSan(ChessGame game, String san) {
        String target = san.replace("/", "=")
                .replace("0-0-0", "O-O-O")
                .replace("0-0", "O-O")
                .replaceAll("[+#!?]+", "");
        for (Move move : game.getLegalMoves()) {
            if (San.of(game.getBoard(), move).replaceAll("[+#!?]+", "").equals(target)) {
                return move;
            }
        }
        return null;
    }

    private void openSavedGame() {
        java.nio.file.Path path = GameRecordStore.promptForOpenPath(this);
        if (path == null) return;

        try {
            GameRecord record = PgnReader.read(path);
            ReviewFrame frame = new ReviewFrame(record, path.getFileName().toString());
            frame.setVisible(true);
        } catch (IOException ex) {
            showMessage("Could not open: " + ex.getMessage());
        }
    }

    private String whiteName() {
        if (controller == null) return "White";
        if (controller.isNetworked()) {
            return controller.getLocalColor() == PieceColor.WHITE ? "You" : "Opponent";
        }
        if (enginePlayer != null && enginePlayer.getEngineColor() == PieceColor.WHITE) {
            return "Stockfish";
        }
        return "White";
    }

    private String blackName() {
        if (controller == null) return "Black";
        if (controller.isNetworked()) {
            return controller.getLocalColor() == PieceColor.BLACK ? "You" : "Opponent";
        }
        if (enginePlayer != null && enginePlayer.getEngineColor() == PieceColor.BLACK) {
            return "Stockfish";
        }
        return "Black";
    }

    private void startLocalGame(TimeControl timeControl) {
        ChessGame game = new ChessGame();
        installController(new GameController(game, this, timeControl));
        if (engineToggleItem != null && engineToggleItem.isSelected() && enginePlayer != null) {
            controller.setEnginePlayer(enginePlayer);
        }
    }

    private void hostGame() {
        // 1. Pick a time control
        TimeControl chosen = NewGameDialog.show(this, currentTimeControl);
        if (chosen == null) return;
        currentTimeControl = chosen;

        // 2. Pick a port
        String input = JOptionPane.showInputDialog(this, "Port to listen on:",
                Protocol.DEFAULT_PORT);
        if (input == null) return;
        int port;
        try { port = Integer.parseInt(input.trim()); }
        catch (NumberFormatException ex) {
            showMessage("Please enter a valid port number.");
            return;
        }

        // 3. Start the relay
        try { new ChessServer(port).start(); }
        catch (IOException ex) {
            showMessage("Could not start the server: " + ex.getMessage());
            return;
        }
        showStatus("Server started on port " + port
                + " (" + chosen.getLabel() + ") - waiting for an opponent...");
        connectToServer("localhost", port, chosen);
    }

    private void joinGame() {
        String host = JOptionPane.showInputDialog(this, "Server host:", "localhost");
        if (host == null || host.trim().isEmpty()) return;
        String portInput = JOptionPane.showInputDialog(this, "Server port:",
                Protocol.DEFAULT_PORT);
        if (portInput == null) return;
        int port;
        try { port = Integer.parseInt(portInput.trim()); }
        catch (NumberFormatException ex) {
            showMessage("Please enter a valid port number.");
            return;
        }
        // Time control is unknown until the host announces it.
        connectToServer(host.trim(), port, TimeControl.unlimited());
    }

    private void connectToServer(String host, int port, TimeControl tc) {
        if (enginePlayer != null) {
            int answer = JOptionPane.showConfirmDialog(this,
                    "Online play disables the local engine. Disable Stockfish now?",
                    "Engine", JOptionPane.YES_NO_OPTION);
            if (answer != JOptionPane.YES_OPTION) return;
            engineToggleItem.setSelected(false);
            toggleEngine(false);
        }
        ChessGame game = new ChessGame();
        GameController newController = new GameController(game, this, tc);
        try { newController.connect(host, port); }
        catch (IOException ex) {
            showMessage("Could not connect to " + host + ":" + port
                    + "\n" + ex.getMessage());
            return;
        }
        installController(newController);
    }

    private void installController(GameController newController) {
        if (controller != null) {
            if (analyzer != null) controller.getGame().removeListener(analyzer);
            controller.dispose();
        }
        controller = newController;
        boardPanel.setListener(controller);
        statusPanel.setActions(controller);
        statusPanel.setUndoEnabled(!controller.isNetworked());
        controller.start();

        if (analyzer != null && analyzer.isEnabled()) {
            controller.getGame().addListener(analyzer);
            analyzer.onGameChanged(controller.getGame());
        }
    }

    @Override
    public void dispose() {
        shutdownEngine();
        shutdownAnalyzer();
        if (controller != null) controller.dispose();
        super.dispose();
    }

    // --------------------------------------------------------- engine wiring

    private void toggleEngine(boolean enable) {
        if (controller == null) { engineToggleItem.setSelected(false); return; }
        if (enable) {
            try {
                EnginePlayer player = new EnginePlayer(
                        EngineConfig.resolveExecutable(), PieceColor.BLACK);
                player.setEnabled(true);
                shutdownEngine();
                enginePlayer = player;
                controller.setEnginePlayer(player);
                showStatus("Stockfish enabled as Black.");
            } catch (Exception ex) {
                engineToggleItem.setSelected(false);
                showMessage("Could not start Stockfish:\n" + ex.getMessage());
            }
        } else {
            shutdownEngine();
            controller.setEnginePlayer(null);
            showStatus("Stockfish disabled.");
        }
    }

    private void shutdownEngine() {
        if (enginePlayer != null) { enginePlayer.shutdown(); enginePlayer = null; }
    }

    private void toggleAnalysis(boolean enable) {
        analysisPanel.setVisible(enable);
        if (!enable) {
            if (analyzer != null) analyzer.setEnabled(false);
            analysisPanel.clear();
            accuracyPanel.clear();
            evalBar.clear();
            return;
        }
        try {
            if (analyzer == null) {
                analyzer = new GameAnalyzer(EngineConfig.resolveExecutable(), 16);
                analyzer.setListener(this::onAnalysisUpdate);
            }
        } catch (IOException ex) {
            analysisToggleItem.setSelected(false);
            analysisPanel.setVisible(false);
            showMessage("Could not start analysis engine:\n" + ex.getMessage());
            return;
        }
        analyzer.setEnabled(true);
        if (controller != null) {
            controller.getGame().addListener(analyzer);
            analyzer.onGameChanged(controller.getGame());
        }
    }

    private void onAnalysisUpdate(GameAnalyzer.Snapshot snapshot) {
        analysisPanel.update(snapshot);
        accuracyPanel.update(snapshot);
        if (snapshot.isMate) {
            evalBar.setMate(snapshot.whiteCentipawns > 0);
        } else {
            evalBar.setCentipawns(snapshot.whiteCentipawns);
        }
    }

    private void shutdownAnalyzer() {
        if (analyzer != null) { analyzer.shutdown(); analyzer = null; }
    }

    // ------------------------------------------------------------- GameView

    @Override
    public void showBoard(Board board, Position selected, List<Position> legalTargets,
                          Move lastMove, Position checkSquare) {
        boardPanel.setBoard(board);
        boardPanel.setSelected(selected);
        boardPanel.setLegalTargets(legalTargets);
        boardPanel.setLastMove(lastMove);
        boardPanel.setCheckSquare(checkSquare);
        boardPanel.repaint();
    }

    @Override public void showTurn(String text)            { statusPanel.setTurnText(text); }
    @Override public void showStatus(String text)          { statusPanel.setStatusText(text); }
    @Override public void showMoves(List<String> notation) { moveListPanel.setMoves(notation); }
    @Override public void showOpening(String eco, String name) { openingPanel.setOpening(eco, name); }

    @Override
    public void showClocks(ChessClock clock) {
        currentClock = clock;
        topPlayer.updateClock(clock);
        bottomPlayer.updateClock(clock);
    }

    @Override
    public void showActiveSide(PieceColor side) {
        topPlayer.setActive(side != null && side == topColor);
        bottomPlayer.setActive(side != null && side == bottomColor);
    }

    @Override
    public void clearBoardAnnotations() {
        boardPanel.clearAnnotations();
    }

    @Override
    public void setFlipped(boolean flipped) {
        boardPanel.setFlipped(flipped);
        boardPanel.repaint();
        evalBar.setFlipped(flipped);

        topColor    = flipped ? PieceColor.WHITE : PieceColor.BLACK;
        bottomColor = flipped ? PieceColor.BLACK : PieceColor.WHITE;
        topPlayer.setColor(topColor);
        bottomPlayer.setColor(bottomColor);

        // Refresh clock labels because each panel now reads the other side's time
        if (currentClock != null) {
            topPlayer.updateClock(currentClock);
            bottomPlayer.updateClock(currentClock);
        }
    }

    @Override public void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "BetaChess",
                JOptionPane.INFORMATION_MESSAGE);
    }

    @Override public void showGameOver(String message) {
        JOptionPane.showMessageDialog(this, message, "Game over",
                JOptionPane.INFORMATION_MESSAGE);
    }

    @Override public Component dialogParent() { return this; }
}