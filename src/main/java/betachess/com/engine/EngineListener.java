package betachess.com.engine;

/**
 * Called on the engine's background thread when a move has been computed.
 * The implementation is responsible for hopping to the Swing EDT.
 */
public interface EngineListener {

    void onBestMove(String uciMove);

    default void onEngineError(String message) {
        // optional
    }
}