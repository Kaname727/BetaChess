package betachess.com.GUI;

/**
 * Buttons of the status panel talk to the controller through this interface.
 */
public interface GameActions {

    void onNewGameRequested();
    void onUndoRequested();
    void onClearRequested();
    void onDrawRequested();
    void onResignRequested();
}