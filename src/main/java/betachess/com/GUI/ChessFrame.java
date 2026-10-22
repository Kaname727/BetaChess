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

import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
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
    private final StatusPanel   statusPanel;
    private final ClockPanel    clockPanel;
    private final AnalysisPanel analysisPanel;
    private final MoveListPanel moveListPanel;
    private final OpeningPanel openingPanel;

    private GameController controller;
    private EnginePlayer   enginePlayer;
    private GameAnalyzer   analyzer;
    private JCheckBoxMenuItem engineToggleItem;
    private JCheckBoxMenuItem analysisToggleItem;
    private TimeControl currentTimeControl = TimeControl.unlimited();

    public ChessFrame() {
        super("BetaChess");

        boardPanel    = new BoardPanel(null);
        evalBar       = new EvalBar();
        statusPanel   = new StatusPanel();
        clockPanel    = new ClockPanel();
        analysisPanel = new AnalysisPanel();
        moveListPanel = new MoveListPanel();
        openingPanel = new OpeningPanel();

        clockPanel.setVisible(false);
        analysisPanel.setVisible(false);

        // ----- board area: eval bar on the left, board in the center ------
        JPanel boardArea = new JPanel(new BorderLayout(6, 0));
        boardArea.add(evalBar, BorderLayout.WEST);
        boardArea.add(boardPanel, BorderLayout.CENTER);

        JPanel boardWrapper = new JPanel(new GridBagLayout());
        boardWrapper.add(boardArea);

        // ----- side column -------------------------------------------------
        JPanel side = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0; side.add(statusPanel, gbc);
        gbc.gridy = 1; side.add(openingPanel, gbc);   // <-- new
        gbc.gridy = 2; side.add(clockPanel, gbc);
        gbc.gridy = 3; side.add(analysisPanel, gbc);
        gbc.gridy = 4;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        side.add(moveListPanel, gbc);
        side.setPreferredSize(new Dimension(250, 0));

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
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> dispose());
        gameMenu.add(localItem);
        gameMenu.addSeparator();
        gameMenu.add(hostItem);
        gameMenu.add(joinItem);
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

        return bar;
    }

    // --------------------------------------------------------------- sessions

    private void promptForNewGame() {
        TimeControl chosen = NewGameDialog.show(this, currentTimeControl);
        if (chosen == null) return;
        currentTimeControl = chosen;
        startLocalGame(chosen);
    }

    private void startLocalGame(TimeControl timeControl) {
        ChessGame game = new ChessGame();
        installController(new GameController(game, this, timeControl));
        if (engineToggleItem != null && engineToggleItem.isSelected() && enginePlayer != null) {
            controller.setEnginePlayer(enginePlayer);
        }
    }

    private void hostGame() {
        String input = JOptionPane.showInputDialog(this, "Port to listen on:",
                Protocol.DEFAULT_PORT);
        if (input == null) return;
        int port;
        try { port = Integer.parseInt(input.trim()); }
        catch (NumberFormatException ex) { showMessage("Please enter a valid port number."); return; }
        try { new ChessServer(port).start(); }
        catch (IOException ex) { showMessage("Could not start the server: " + ex.getMessage()); return; }
        showStatus("Server started on port " + port + " - waiting for an opponent...");
        connectToServer("localhost", port);
    }

    private void joinGame() {
        String host = JOptionPane.showInputDialog(this, "Server host:", "localhost");
        if (host == null || host.trim().isEmpty()) return;
        String portInput = JOptionPane.showInputDialog(this, "Server port:", Protocol.DEFAULT_PORT);
        if (portInput == null) return;
        int port;
        try { port = Integer.parseInt(portInput.trim()); }
        catch (NumberFormatException ex) { showMessage("Please enter a valid port number."); return; }
        connectToServer(host.trim(), port);
    }

    private void connectToServer(String host, int port) {
        if (enginePlayer != null) {
            int answer = JOptionPane.showConfirmDialog(this,
                    "Online play disables the local engine. Disable Stockfish now?",
                    "Engine", JOptionPane.YES_NO_OPTION);
            if (answer != JOptionPane.YES_OPTION) return;
            engineToggleItem.setSelected(false);
            toggleEngine(false);
        }
        ChessGame game = new ChessGame();
        GameController newController = new GameController(game, this, TimeControl.unlimited());
        try { newController.connect(host, port); }
        catch (IOException ex) {
            showMessage("Could not connect to " + host + ":" + port + "\n" + ex.getMessage());
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
    public void showOpening(String eco, String name) {
        openingPanel.setOpening(eco, name);
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
            evalBar.clear();                        // <-- reset to 50/50
            return;
        }
        try {
            if (analyzer == null) {
                analyzer = new GameAnalyzer(EngineConfig.resolveExecutable(), 16);
                analyzer.setListener(this::onAnalysisUpdate);   // <-- new handler
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

    /** Feeds both the numeric panel and the vertical eval bar. */
    private void onAnalysisUpdate(GameAnalyzer.Snapshot snapshot) {
        analysisPanel.update(snapshot);
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
    @Override public void showClocks(ChessClock clock)     { clockPanel.update(clock); }

    @Override
    public void setFlipped(boolean flipped) {
        boardPanel.setFlipped(flipped);
        boardPanel.repaint();
        evalBar.setFlipped(flipped);                          // <-- bar follows board
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