package betachess.com.engine;

import betachess.com.gamesystem.ChessGame;
import betachess.com.gamesystem.GameStatus;
import betachess.com.gamesystem.Move;
import betachess.com.piece.PieceColor;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wires a UciEngine to a ChessGame. When enabled and it is the engine's
 * colour to move, it asks the engine for a move and applies it on the EDT.
 */
public final class EnginePlayer implements EngineListener {

    private final UciEngine engine;
    private final PieceColor engineColor;
    private final AtomicBoolean enabled = new AtomicBoolean(false);
    private final AtomicBoolean thinking = new AtomicBoolean(false);

    private volatile ChessGame game;

    public EnginePlayer(String executable, PieceColor engineColor) throws IOException {
        this.engine = new UciEngine(executable);
        this.engineColor = engineColor;
        this.engine.setListener(this);
        this.engine.start();
        this.engine.newGame();
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public PieceColor getEngineColor() {
        return engineColor;
    }

    /** Must be called whenever the game state changes. */
    public void onGameChanged(ChessGame game) {
        this.game = game;
        if (!enabled.get() || thinking.get()) {
            return;
        }
        if (game.getStatus().isGameOver()) {
            return;
        }
        if (game.getSideToMove() != engineColor) {
            return;
        }
        thinking.set(true);
        String fen = FenSerializer.toFen(
                game.getBoard(),
                game.getSideToMove(),
                game.getCastlingRights(),
                game.getEnPassantTarget(),
                game.getHalfmoveClock(),
                game.getFullmoveNumber());
        engine.go(fen);
    }

    public void newGame() {
        engine.newGame();
    }

    public void shutdown() {
        enabled.set(false);
        engine.close();
    }

    @Override
    public void onBestMove(String uciMove) {
        thinking.set(false);
        SwingUtilities.invokeLater(() -> {
            ChessGame current = game;
            if (current == null || current.getStatus().isGameOver()) {
                return;
            }
            if (current.getSideToMove() != engineColor) {
                return;
            }
            try {
                Move move = Move.fromUci(uciMove);
                if (!current.makeMove(move)) {
                    System.err.println("Engine returned illegal move: " + uciMove);
                }
            } catch (RuntimeException ex) {
                System.err.println("Bad engine move '" + uciMove + "': " + ex.getMessage());
            }
        });
    }

    @Override
    public void onEngineError(String message) {
        System.err.println("Engine error: " + message);
    }
}