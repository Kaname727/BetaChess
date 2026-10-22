package betachess.com.gamesystem;

/**
 * An immutable square on the board.
 * file: 0..7 -> a..h, rank: 0..7 -> 1..8.
 */
public final class Position {

    public static final int SIZE = 8;

    private final int file;
    private final int rank;

    public Position(int file, int rank) {
        if (!isOnBoard(file, rank)) {
            throw new IllegalArgumentException("Square off the board: " + file + "," + rank);
        }
        this.file = file;
        this.rank = rank;
    }

    public static Position of(int file, int rank) {
        return new Position(file, rank);
    }

    /** Parses algebraic notation such as "e4". */
    public static Position of(String algebraic) {
        if (algebraic == null || algebraic.length() != 2) {
            throw new IllegalArgumentException("Bad square: " + algebraic);
        }
        char fileChar = Character.toLowerCase(algebraic.charAt(0));
        char rankChar = algebraic.charAt(1);
        if (fileChar < 'a' || fileChar > 'h' || rankChar < '1' || rankChar > '8') {
            throw new IllegalArgumentException("Bad square: " + algebraic);
        }
        return new Position(fileChar - 'a', rankChar - '1');
    }

    public static boolean isOnBoard(int file, int rank) {
        return file >= 0 && file < SIZE && rank >= 0 && rank < SIZE;
    }

    public int file() {
        return file;
    }

    public int rank() {
        return rank;
    }

    /** Returns the offset square or null when it leaves the board. */
    public Position offset(int fileDelta, int rankDelta) {
        int newFile = file + fileDelta;
        int newRank = rank + rankDelta;
        return isOnBoard(newFile, newRank) ? new Position(newFile, newRank) : null;
    }

    public String toAlgebraic() {
        return "" + (char) ('a' + file) + (char) ('1' + rank);
    }

    @Override
    public String toString() {
        return toAlgebraic();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Position)) {
            return false;
        }
        Position that = (Position) other;
        return file == that.file && rank == that.rank;
    }

    @Override
    public int hashCode() {
        return file * SIZE + rank;
    }
}