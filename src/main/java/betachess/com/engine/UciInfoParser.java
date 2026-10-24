package betachess.com.engine;

/**
 * Parses "info ..." lines. Returns null when the line has no usable score.
 * Extracts depth, multipv number (default 1), score, and PV head move.
 */
public final class UciInfoParser {

    private UciInfoParser() {
    }

    public static EngineEvaluation parse(String line) {
        if (line == null || !line.startsWith("info ")) {
            return null;
        }
        int depth   = 0;
        int multipv = 1;
        Score score = null;
        String bestMove = null;

        String[] tokens = line.split("\\s+");
        for (int i = 1; i < tokens.length; i++) {
            switch (tokens[i]) {
                case "depth":
                    if (i + 1 < tokens.length) {
                        try { depth = Integer.parseInt(tokens[++i]); }
                        catch (NumberFormatException ignored) { }
                    }
                    break;
                case "multipv":
                    if (i + 1 < tokens.length) {
                        try { multipv = Integer.parseInt(tokens[++i]); }
                        catch (NumberFormatException ignored) { }
                    }
                    break;
                case "score":
                    if (i + 2 < tokens.length) {
                        String kind = tokens[++i];
                        try {
                            int v = Integer.parseInt(tokens[++i]);
                            score = "cp".equals(kind) ? Score.centipawns(v) : Score.mate(v);
                        } catch (NumberFormatException ignored) { }
                    }
                    break;
                case "pv":
                    if (i + 1 < tokens.length) {
                        bestMove = tokens[i + 1];
                    }
                    break;
                default:
                    break;
            }
        }
        if (score == null) {
            return null;
        }
        return new EngineEvaluation(depth, multipv, score, bestMove);
    }
}