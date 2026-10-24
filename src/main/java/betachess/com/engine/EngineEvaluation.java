package betachess.com.engine;

/**
 * One snapshot parsed from a UCI "info ..." line.
 * Score is from the side-to-move's perspective.
 */
public final class EngineEvaluation {

    private final int    depth;
    private final int    multipv;    // 1 = best line
    private final Score  score;
    private final String bestMoveUci;

    public EngineEvaluation(int depth, int multipv, Score score, String bestMoveUci) {
        this.depth       = depth;
        this.multipv     = multipv;
        this.score       = score;
        this.bestMoveUci = bestMoveUci;
    }

    public int    getDepth()       { return depth; }
    public int    getMultipv()     { return multipv; }
    public Score  getScore()       { return score; }
    public String getBestMoveUci() { return bestMoveUci; }
}