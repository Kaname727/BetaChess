package betachess.com.gamesystem.clock;

/**
 * Immutable time-control config: initial clock per side + per-move increment.
 */
public final class TimeControl {

    private final long initialMillis;
    private final long incrementMillis;
    private final String label;

    private TimeControl(long initialMillis, long incrementMillis, String label) {
        this.initialMillis = initialMillis;
        this.incrementMillis = incrementMillis;
        this.label = label;
    }

    public static TimeControl unlimited() {
        return new TimeControl(0, 0, "No time limit");
    }

    public static TimeControl suddenDeath(int minutes) {
        return new TimeControl(minutes * 60_000L, 0, minutes + " min");
    }

    public static TimeControl increment(int minutes, int incrementSeconds) {
        return new TimeControl(minutes * 60_000L, incrementSeconds * 1000L,
                minutes + " min + " + incrementSeconds + "s");
    }

    /** Presets shown in the "New game" dialog. */
    public static TimeControl[] presets() {
        return new TimeControl[] {
                unlimited(),
                suddenDeath(10),
                increment(5, 5)
        };
    }
    /** Rebuild a TimeControl from a network message. */
    public static TimeControl custom(String label, long initialMillis, long incrementMillis) {
        return new TimeControl(initialMillis, incrementMillis, label == null ? "custom" : label);
    }

    public boolean isUnlimited()         { return initialMillis == 0; }
    public long    getInitialMillis()    { return initialMillis; }
    public long    getIncrementMillis()  { return incrementMillis; }
    public String  getLabel()            { return label; }

    @Override
    public String toString() { return label; }
}