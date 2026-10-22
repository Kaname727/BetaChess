package betachess.com.engine;

/**
 * Parses "info ..." lines. Returns null when the line does not yet carry
 * a usable score (e.g. "info currmove ..." progress lines).
 */
public final class UciInfoParser {

    private UciInfoParser() {
    }

    public static EngineEvaluation parse(String line) {
        if (line == null || !line.startsWith("info ")) {
            return null;
        }
        int depth = 0;
        Score score = null;
        String bestMove = null;

        String[] tokens = line.split("\\s+");
        for (int i = 1; i < tokens.length; i++) {
            String token = tokens[i];
            switch (token) {
                case "depth":
                    if (i + 1 < tokens.length) {
                        try { depth = Integer.parseInt(tokens[++i]); }
                        catch (NumberFormatException ignored) { }
                    }
                    break;
                case "score":
                    if (i + 2 < tokens.length) {
                        String kind = tokens[++i];
                        try {
                            int value = Integer.parseInt(tokens[++i]);
                            score = "cp".equals(kind)
                                    ? Score.centipawns(value)
                                    : Score.mate(value);
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
        return new EngineEvaluation(depth, score, bestMove);
    }
}