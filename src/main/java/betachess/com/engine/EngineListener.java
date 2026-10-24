package betachess.com.engine;

/**
 * Callback for {@code bestmove ...} lines, invoked on the engine's reader thread.
 * The implementation is responsible for hopping to the Swing EDT.
 */
public interface EngineListener {

    void onBestMove(String uciMove);

    default void onEngineError(String message) {
        // optional
    }
}