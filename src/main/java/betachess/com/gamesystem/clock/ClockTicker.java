package betachess.com.gamesystem.clock;

import betachess.com.piece.PieceColor;

import javax.swing.Timer;

/**
 * Drives a {@link ChessClock} on the Swing EDT. Fires 10x per second.
 */
public final class ClockTicker {

    public interface Listener {
        void onTick(ChessClock clock);
        void onFlag(PieceColor flaggedSide);
    }

    private final ChessClock clock;
    private final Listener listener;
    private final Timer timer;

    public ClockTicker(ChessClock clock, Listener listener) {
        this.clock = clock;
        this.listener = listener;
        this.timer = new Timer(100, e -> tick());
        this.timer.setCoalesce(true);
    }

    private void tick() {
        PieceColor flagged = clock.tick(System.nanoTime());
        listener.onTick(clock);
        if (flagged != null) {
            timer.stop();
            listener.onFlag(flagged);
        }
    }

    public void start() {
        if (!timer.isRunning()) {
            timer.start();
        }
    }

    public void stop() {
        timer.stop();
    }

    public boolean isRunning() {
        return timer.isRunning();
    }
}