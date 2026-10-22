package betachess.com.engine;

/**
 * An evaluation from the side-to-move perspective.
 * Either a centipawn value or a mate-in-N score.
 */
public final class Score {

    public enum Type { CENTIPAWNS, MATE }

    private final Type type;
    private final int value;

    private Score(Type type, int value) {
        this.type = type;
        this.value = value;
    }

    public static Score centipawns(int cp) { return new Score(Type.CENTIPAWNS, cp); }
    public static Score mate(int moves)    { return new Score(Type.MATE, moves); }
    public static Score none()             { return new Score(Type.CENTIPAWNS, 0); }

    public Type getType() { return type; }
    public int getValue() { return value; }

    /** For loss/accuracy math — mates collapse to ±10000 cp. */
    public int toCentipawns() {
        if (type == Type.MATE) {
            return value > 0 ? 10000 : -10000;
        }
        return value;
    }

    public Score negated() {
        return new Score(type, -value);
    }

    @Override
    public String toString() {
        if (type == Type.MATE) {
            return (value >= 0 ? "M" : "-M") + Math.abs(value);
        }
        return String.format("%+d", value);
    }
}