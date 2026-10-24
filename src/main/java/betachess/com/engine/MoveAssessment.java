package betachess.com.engine;

import betachess.com.gamesystem.Move;
import betachess.com.piece.PieceColor;

/**
 * Everything Stockfish told us about one ply, plus the classification derived
 * from those numbers.
 */
public final class MoveAssessment {

    private final int                ply;             // 0-indexed
    private final Move               playedMove;
    private final String             playedSan;
    private final String             bestMoveUci;
    private final String             bestMoveSan;
    private final int                evalBefore;      // mover's POV, cp, ±10000 for mate
    private final int                evalAfter;       // mover's POV
    private final int                centipawnLoss;
    private final MoveClassification classification;

    private MoveAssessment(int ply, Move playedMove, String playedSan,
                           String bestMoveUci, String bestMoveSan,
                           int evalBefore, int evalAfter, int centipawnLoss,
                           MoveClassification classification) {
        this.ply            = ply;
        this.playedMove     = playedMove;
        this.playedSan      = playedSan;
        this.bestMoveUci    = bestMoveUci;
        this.bestMoveSan    = bestMoveSan;
        this.evalBefore     = evalBefore;
        this.evalAfter      = evalAfter;
        this.centipawnLoss  = centipawnLoss;
        this.classification = classification;
    }

    /**
     * Builds an assessment from the two eval snapshots.
     * Both evals are relative to the side that just moved (the mover's POV).
     */
    public static MoveAssessment build(int ply, Move playedMove, String playedSan,
                                       String bestMoveUci, String bestMoveSan,
                                       int evalBefore, int evalAfter,
                                       PieceColor moverColor) {
        int cpLoss = Math.max(0, evalBefore - evalAfter);
        MoveClassification cls;
        if (bestMoveUci != null && bestMoveUci.equals(playedMove.toUci()) && cpLoss <= 5) {
            cls = MoveClassification.BEST;
        } else if (cpLoss <= 5) {
            cls = MoveClassification.BEST;
        } else if (cpLoss <= 20) {
            cls = MoveClassification.EXCELLENT;
        } else if (cpLoss <= 50) {
            cls = MoveClassification.GOOD;
        } else if (cpLoss <= 100) {
            cls = MoveClassification.INACCURACY;
        } else if (cpLoss <= 250) {
            cls = MoveClassification.MISTAKE;
        } else {
            cls = MoveClassification.BLUNDER;
        }
        return new MoveAssessment(ply, playedMove, playedSan,
                bestMoveUci, bestMoveSan, evalBefore, evalAfter, cpLoss, cls);
    }

    /** A copy of this assessment re-tagged as a book move. */
    public MoveAssessment asBook() {
        return new MoveAssessment(ply, playedMove, playedSan,
                bestMoveUci, bestMoveSan, evalBefore, evalAfter, 0,
                MoveClassification.BOOK);
    }

    public int                getPly()             { return ply; }
    public Move               getPlayedMove()      { return playedMove; }
    public String             getPlayedSan()       { return playedSan; }
    public String             getBestMoveUci()     { return bestMoveUci; }
    public String             getBestMoveSan()     { return bestMoveSan; }
    public int                getEvalBefore()      { return evalBefore; }
    public int                getEvalAfter()       { return evalAfter; }
    public int                getCentipawnLoss()   { return centipawnLoss; }
    public MoveClassification getClassification()  { return classification; }
    public boolean            playedBestMove() {
        return bestMoveUci != null && bestMoveUci.equals(playedMove.toUci());
    }
}