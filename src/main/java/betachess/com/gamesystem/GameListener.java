package betachess.com.gamesystem;

/**
 * Observer for anything interested in game state changes (GUI, network, AI…).
 */
public interface GameListener {

    void onGameChanged(ChessGame game);

    default void onGameFinished(ChessGame game, GameStatus status) {
        // optional callback
    }
}