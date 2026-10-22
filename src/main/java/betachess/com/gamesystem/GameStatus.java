package betachess.com.gamesystem;

/**
 * The lifecycle state of a game.
 */
public enum GameStatus {

    ONGOING,
    CHECK,
    CHECKMATE,
    STALEMATE,
    DRAW_FIFTY_MOVE,
    DRAW_INSUFFICIENT_MATERIAL,
    DRAW_AGREED,
    RESIGNED;

    public boolean isGameOver() {
        switch (this) {
            case CHECKMATE:
            case STALEMATE:
            case DRAW_FIFTY_MOVE:
            case DRAW_INSUFFICIENT_MATERIAL:
            case DRAW_AGREED:
            case RESIGNED:
                return true;
            default:
                return false;
        }
    }
}