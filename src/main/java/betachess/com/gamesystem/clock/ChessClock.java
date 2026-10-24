package betachess.com.gamesystem.clock;

import betachess.com.piece.PieceColor;

/**
 * Pure clock state. No threads: the caller drives time forward with
 * {@link #tick(long)} (or via {@link ClockTicker}). Testable without any UI.
 */
public final class ChessClock {

    private final TimeControl timeControl;
    private long whiteMillis;
    private long blackMillis;
    private PieceColor activeSide;
    private long lastTickNanos;
    private boolean running;

    public ChessClock(TimeControl timeControl) {
        this.timeControl = timeControl;
        this.whiteMillis = timeControl.getInitialMillis();
        this.blackMillis = timeControl.getInitialMillis();
    }
    public void syncAndSwitch(PieceColor mover, long postIncrementMillis, PieceColor newActive) {
        if (mover == PieceColor.WHITE) {
            whiteMillis = Math.max(0, postIncrementMillis);
        } else {
            blackMillis = Math.max(0, postIncrementMillis);
        }
        activeSide = newActive;
        lastTickNanos = System.nanoTime();
        running = true;
    }

    public void start(PieceColor side) {
        this.activeSide = side;
        this.lastTickNanos = System.nanoTime();
        this.running = true;
    }

    public void stop() {
        if (running) {
            tick(System.nanoTime());
            running = false;
        }
    }

    /** Adds the increment to the side that just moved and hands the clock over. */
    public void switchTo(PieceColor newSide) {
        if (!running || activeSide == null) {
            activeSide = newSide;
            return;
        }
        tick(System.nanoTime());
        if (activeSide == PieceColor.WHITE) {
            whiteMillis += timeControl.getIncrementMillis();
        } else {
            blackMillis += timeControl.getIncrementMillis();
        }
        activeSide = newSide;
        lastTickNanos = System.nanoTime();
    }

    /**
     * Advances time by (nowNanos - lastTickNanos).
     * @return the side whose flag fell, or null
     */
    public PieceColor tick(long nowNanos) {
        if (!running || activeSide == null) {
            return null;
        }
        long elapsed = (nowNanos - lastTickNanos) / 1_000_000L;
        lastTickNanos = nowNanos;
        if (elapsed <= 0) {
            return null;
        }
        if (activeSide == PieceColor.WHITE) {
            whiteMillis -= elapsed;
            if (whiteMillis <= 0) {
                whiteMillis = 0;
                running = false;
                return PieceColor.WHITE;
            }
        } else {
            blackMillis -= elapsed;
            if (blackMillis <= 0) {
                blackMillis = 0;
                running = false;
                return PieceColor.BLACK;
            }
        }
        return null;
    }

    public long getMillis(PieceColor color) {
        return Math.max(0, color == PieceColor.WHITE ? whiteMillis : blackMillis);
    }

    public PieceColor getActiveSide()      { return activeSide; }
    public boolean    isRunning()          { return running; }
    public TimeControl getTimeControl()    { return timeControl; }

    public void reset() {
        whiteMillis = timeControl.getInitialMillis();
        blackMillis = timeControl.getInitialMillis();
        activeSide = null;
        running = false;
    }
}