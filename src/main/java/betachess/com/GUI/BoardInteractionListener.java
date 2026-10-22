package betachess.com.GUI;

import betachess.com.gamesystem.Position;

/**
 * Implemented by whoever reacts to board clicks.
 */
public interface BoardInteractionListener {

    void onSquareSelected(Position position);
}