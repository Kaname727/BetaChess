package betachess.com.engine;

/**
 * One snapshot parsed from a UCI "info ..." line.
 * The score is from the perspective of the side to move.
 */
public final class EngineEvaluation {

    private final int depth;
    private final Score score;
    private final String bestMoveUci;   // first move of the PV, may be null

    public EngineEvaluation(int depth, Score score, String bestMoveUci) {
        this.depth = depth;
        this.score = score;
        this.bestMoveUci = bestMoveUci;
    }

    public int getDepth()          { return depth; }
    public Score getScore()        { return score; }
    public String getBestMoveUci() { return bestMoveUci; }
}