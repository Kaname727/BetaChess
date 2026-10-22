package betachess.com.GUI;

import betachess.com.gamesystem.Board;
import betachess.com.gamesystem.Move;
import betachess.com.gamesystem.Position;
import betachess.com.gamesystem.clock.ChessClock;

import java.awt.Component;
import java.util.List;

/**
 * Everything a controller is allowed to do with the user interface.
 */
public interface GameView {

    void showBoard(Board board, Position selected, List<Position> legalTargets,
                   Move lastMove, Position checkSquare);

    void showTurn(String text);

    void showOpening(String eco, String name);

    void showStatus(String text);

    void showMoves(List<String> notation);

    void setFlipped(boolean flipped);

    void showMessage(String message);

    void showClocks(ChessClock clock);

    void showGameOver(String message);

    Component dialogParent();
}
