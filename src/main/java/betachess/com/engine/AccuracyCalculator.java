package betachess.com.engine;

/**
 * Centipawn-loss → accuracy%, using the well-known logistic win-probability
 * curve and the fitting constants popularised by Lichess.
 *
 *   winPct(cp) = 50 + 50 * (2 / (1 + exp(-0.00368208 * cp)) - 1)
 *   accuracy%  = 103.1668 * exp(-0.04354 * loss) - 3.1669
 *
 * Both cp values are evaluated from the *same* mover's perspective.
 */
public final class AccuracyCalculator {

    private AccuracyCalculator() {
    }

    public static double accuracy(int centipawnsBefore, int centipawnsAfter) {
        double loss = Math.max(0, winPercent(centipawnsBefore) - winPercent(centipawnsAfter));
        double raw = 103.1668 * Math.exp(-0.04354 * loss) - 3.1669;
        return Math.max(0, Math.min(100, raw));
    }

    private static double winPercent(int cp) {
        int clamped = Math.max(-1000, Math.min(1000, cp));
        return 50 + 50 * (2.0 / (1 + Math.exp(-0.00368208 * clamped)) - 1);
    }
}