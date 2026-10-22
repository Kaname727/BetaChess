package betachess.com.GUI;

import betachess.com.engine.EnginePlayer;
import betachess.com.engine.opening.Opening;
import betachess.com.engine.opening.OpeningDetector;
import betachess.com.gamesystem.ChessGame;
import betachess.com.gamesystem.GameListener;
import betachess.com.gamesystem.GameStatus;
import betachess.com.gamesystem.Move;
import betachess.com.gamesystem.MoveRecord;
import betachess.com.gamesystem.Position;
import betachess.com.gamesystem.clock.ChessClock;
import betachess.com.gamesystem.clock.ClockTicker;
import betachess.com.gamesystem.clock.TimeControl;
import betachess.com.gamesystem.net.ChessClient;
import betachess.com.gamesystem.net.Protocol;
import betachess.com.piece.Piece;
import betachess.com.piece.PieceColor;
import betachess.com.piece.PieceType;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Glue between the pure game system, the Swing view, the clock, the engine
 * player and the network client. One instance per running game.
 */
public final class GameController
        implements BoardInteractionListener, GameListener, GameActions,
        ChessClient.Listener, ClockTicker.Listener {

    private final ChessGame game;
    private final GameView view;
    private final TimeControl timeControl;

    private ChessClient client;
    private EnginePlayer enginePlayer;
    private PieceColor localColor = PieceColor.WHITE;
    private Position selected;

    private ChessClock clock;
    private ClockTicker ticker;
    private int lastPly;
    private int lastAnnotatedPly = -1;

    public GameController(ChessGame game, GameView view, TimeControl timeControl) {
        this.game = game;
        this.view = view;
        this.timeControl = timeControl == null ? TimeControl.unlimited() : timeControl;
        if (!this.timeControl.isUnlimited()) {
            this.clock = new ChessClock(this.timeControl);
        }
    }

    // ------------------------------------------------------------ life cycle

    public void start() {
        game.addListener(this);
        if (clock != null) {
            ticker = new ClockTicker(clock, this);
            clock.start(game.getSideToMove());
            lastPly = game.getHistory().size();
            ticker.start();
        }
        refresh();
        pushClockToView();
    }

    public void connect(String host, int port) throws IOException {
        ChessClient newClient = new ChessClient(host, port, this);
        newClient.connect();
        this.client = newClient;
    }

    public boolean isNetworked() {
        return client != null;
    }

    public ChessGame getGame() {
        return game;
    }

    public TimeControl getTimeControl() {
        return timeControl;
    }

    public void setEnginePlayer(EnginePlayer enginePlayer) {
        this.enginePlayer = enginePlayer;
        if (enginePlayer != null) {
            enginePlayer.onGameChanged(game);
        }
    }

    public void dispose() {
        if (ticker != null) {
            ticker.stop();
            ticker = null;
        }
        game.removeListener(this);
        if (client != null) {
            client.close();
            client = null;
        }
        if (enginePlayer != null) {
            enginePlayer.shutdown();
            enginePlayer = null;
        }
    }

    // ------------------------------------------------------------ board input

    @Override
    public void onSquareSelected(Position position) {
        if (game.getStatus().isGameOver()) {
            return;
        }
        if (isNetworked() && game.getSideToMove() != localColor) {
            view.showStatus("It is not your turn.");
            return;
        }

        if (selected != null) {
            Move move = chooseMove(selected, position);
            if (move != null) {
                selected = null;
                play(move);
                return;
            }
        }

        Piece piece = game.getBoard().get(position);
        selected = (piece != null && piece.getColor() == game.getSideToMove())
                ? position : null;
        refresh();
    }

    private Move chooseMove(Position from, Position to) {
        List<Move> candidates = new ArrayList<Move>();
        for (Move move : game.getLegalMoves()) {
            if (move.getFrom().equals(from) && move.getTo().equals(to)) {
                candidates.add(move);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        Object[] options = {"Queen", "Rook", "Bishop", "Knight"};
        int choice = JOptionPane.showOptionDialog(view.dialogParent(),
                "Promote the pawn to:", "Promotion",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        PieceType type;
        switch (choice) {
            case 1:  type = PieceType.ROOK;   break;
            case 2:  type = PieceType.BISHOP; break;
            case 3:  type = PieceType.KNIGHT; break;
            default: type = PieceType.QUEEN;  break;
        }
        for (Move move : candidates) {
            if (move.getPromotion() == type) {
                return move;
            }
        }
        return candidates.get(0);
    }

    private void play(Move move) {
        if (!game.makeMove(move)) {
            view.showStatus("Illegal move.");
            refresh();
            return;
        }
        if (isNetworked()) {
            client.send(Protocol.MOVE, move.toUci());
        }
    }

    // --------------------------------------------------------------- actions

    @Override
    public void onNewGameRequested() {
        selected = null;
        if (isNetworked()) {
            client.send(Protocol.NEW_GAME, "");
        }
        game.reset();
    }

    @Override
    public void onUndoRequested() {
        if (isNetworked()) {
            view.showStatus("Undo is not available in online games.");
            return;
        }
        selected = null;
        game.undoLastMove();
    }

    @Override
    public void onClearRequested() {
        selected = null;
        view.clearBoardAnnotations();
        refresh();
    }

    @Override
    public void onDrawRequested() {
        if (game.getStatus().isGameOver()) {
            return;
        }
        if (isNetworked()) {
            client.send(Protocol.DRAW_OFFER, "");
            view.showStatus("Draw offered...");
            return;
        }
        int answer = JOptionPane.showConfirmDialog(view.dialogParent(),
                "Do both players agree to a draw?", "Draw",
                JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            game.finish(GameStatus.DRAW_AGREED);
        }
    }

    @Override
    public void onResignRequested() {
        if (game.getStatus().isGameOver()) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(view.dialogParent(),
                "Do you really want to resign?", "Resign",
                JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        if (isNetworked()) {
            client.send(Protocol.RESIGN, "");
        }
        game.finish(GameStatus.RESIGNED);
    }

    // ------------------------------------------------------------ game events

    @Override
    public void onGameChanged(ChessGame g) {
        int ply = g.getHistory().size();

        // Auto-clear annotations when the position actually changes.
        if (ply != lastAnnotatedPly) {
            view.clearBoardAnnotations();
            lastAnnotatedPly = ply;
        }

        if (clock != null) {
            if (ply == 0) {
                // fresh game (or network reset)
                clock.reset();
                clock.start(g.getSideToMove());
                lastPly = 0;
                if (ticker != null && !ticker.isRunning()) {
                    ticker.start();
                }
            } else if (ply > lastPly && clock.isRunning()) {
                clock.switchTo(g.getSideToMove());
                lastPly = ply;
            } else if (ply < lastPly) {
                if (clock.isRunning()) {
                    clock.stop();
                }
                if (!g.getStatus().isGameOver()) {
                    clock.start(g.getSideToMove());
                }
                lastPly = ply;
            }
        }

        refresh();
        pushClockToView();
    }

    @Override
    public void onGameFinished(ChessGame g, GameStatus status) {
        if (ticker != null) {
            ticker.stop();
        }
        if (clock != null) {
            clock.stop();
        }
        view.showGameOver(describeStatus());
        pushClockToView();
    }

    // ---------------------------------------------------------- clock events

    @Override
    public void onTick(ChessClock clock) {
        pushClockToView();
    }

    @Override
    public void onFlag(PieceColor flagged) {
        if (game.getStatus().isGameOver()) {
            return;
        }
        game.finish(GameStatus.TIMEOUT);
    }

    // ------------------------------------------------------- network events

    @Override
    public void onMessage(String type, String payload) {
        SwingUtilities.invokeLater(() -> handleNetworkMessage(type, payload));
    }

    private void handleNetworkMessage(String type, String payload) {
        switch (type) {
            case Protocol.START:
                if ("BLACK".equalsIgnoreCase(payload)) {
                    localColor = PieceColor.BLACK;
                    view.setFlipped(true);
                } else {
                    localColor = PieceColor.WHITE;
                    view.setFlipped(false);
                }
                view.showStatus("You play " + name(localColor) + ".");
                refresh();
                break;

            case Protocol.MOVE:
                try {
                    game.makeMove(Move.fromUci(payload));
                } catch (RuntimeException exception) {
                    view.showStatus("Opponent sent an invalid move: " + payload);
                }
                break;

            case Protocol.RESIGN:
                game.finish(GameStatus.RESIGNED);
                view.showStatus("Your opponent resigned.");
                break;

            case Protocol.DRAW_OFFER:
                int answer = JOptionPane.showConfirmDialog(view.dialogParent(),
                        "Your opponent offers a draw. Accept?", "Draw offer",
                        JOptionPane.YES_NO_OPTION);
                if (answer == JOptionPane.YES_OPTION) {
                    client.send(Protocol.DRAW_ACCEPT, "");
                    game.finish(GameStatus.DRAW_AGREED);
                } else {
                    client.send(Protocol.DRAW_DECLINE, "");
                }
                break;

            case Protocol.DRAW_ACCEPT:
                game.finish(GameStatus.DRAW_AGREED);
                break;

            case Protocol.DRAW_DECLINE:
                view.showStatus("Draw offer declined.");
                break;

            case Protocol.NEW_GAME:
                selected = null;
                game.reset();
                break;

            case Protocol.INFO:
                view.showStatus(payload);
                break;

            default:
                break;
        }
    }

    @Override
    public void onDisconnected(String reason) {
        SwingUtilities.invokeLater(() ->
                view.showStatus("Disconnected: " + reason));
    }

    // ----------------------------------------------------------------- render

    private void refresh() {
        List<Position> targets = Collections.emptyList();
        if (selected != null) {
            targets = new ArrayList<Position>();
            for (Move move : game.getLegalMovesFrom(selected)) {
                targets.add(move.getTo());
            }
        }

        Move lastMove = null;
        List<MoveRecord> history = game.getHistory();
        if (!history.isEmpty()) {
            lastMove = history.get(history.size() - 1).getMove();
        }

        Position checkSquare = game.isInCheck()
                ? game.getBoard().findKing(game.getSideToMove())
                : null;

        view.showBoard(game.getBoard(), selected, targets, lastMove, checkSquare);
        view.showTurn(name(game.getSideToMove()) + " to move");
        view.showStatus(describeStatus());

        List<String> notation = new ArrayList<String>();
        for (MoveRecord record : history) {
            notation.add(record.getNotation());
        }
        view.showMoves(notation);

        // Opening detection
        Opening opening = OpeningDetector.detect(game);
        if (opening != null) {
            view.showOpening(opening.getEco(), opening.getName());
        } else {
            view.showOpening("—", "Unknown opening");
        }

        // Let the engine decide whether it is its turn
        if (enginePlayer != null) {
            enginePlayer.onGameChanged(game);
        }
    }

    private void pushClockToView() {
        view.showClocks(clock);
    }

    private String describeStatus() {
        GameStatus status = game.getStatus();
        switch (status) {
            case CHECKMATE:
                return "Checkmate - " + name(game.getSideToMove().opposite()) + " wins";
            case STALEMATE:
                return "Stalemate - draw";
            case DRAW_FIFTY_MOVE:
                return "Draw - fifty move rule";
            case DRAW_INSUFFICIENT_MATERIAL:
                return "Draw - insufficient material";
            case DRAW_AGREED:
                return "Draw agreed";
            case RESIGNED:
                return "Resignation - " + name(game.getSideToMove().opposite()) + " wins";
            case TIMEOUT:
                return "Flag fell - " + name(game.getSideToMove().opposite()) + " wins on time";
            case CHECK:
                return name(game.getSideToMove()) + " is in check";
            default:
                if (isNetworked() && game.getSideToMove() != localColor) {
                    return "Waiting for your opponent...";
                }
                return "Your move";
        }
    }

    private static String name(PieceColor color) {
        return color == PieceColor.WHITE ? "White" : "Black";
    }
}