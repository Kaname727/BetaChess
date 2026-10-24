package betachess.com.engine;

import betachess.com.gamesystem.Move;
import betachess.com.piece.PieceColor;

import java.util.Collections;
import java.util.List;

/**
 * Everything Stockfish told us about one ply, plus the classification derived
 * from the two eval snapshots.
 */
public final class MoveAssessment {

    private final int                   ply;
    private final Move                  playedMove;
    private final String                playedSan;
    private final List<EngineEvaluation> topMoves;   // sorted by multipv, may be empty
    private final int                   evalBefore;
    private final int                   evalAfter;
    private final int                   centipawnLoss;
    private final MoveClassification    classification;

    private MoveAssessment(int ply, Move playedMove, String playedSan,
                           List<EngineEvaluation> topMoves,
                           int evalBefore, int evalAfter, int centipawnLoss,
                           MoveClassification classification) {
        this.ply            = ply;
        this.playedMove     = playedMove;
        this.playedSan      = playedSan;
        this.topMoves       = topMoves == null
                ? Collections.<EngineEvaluation>emptyList()
                : Collections.unmodifiableList(topMoves);
        this.evalBefore     = evalBefore;
        this.evalAfter      = evalAfter;
        this.centipawnLoss  = centipawnLoss;
        this.classification = classification;
    }

    /** Both evals must be from the mover's POV (see GameReviewer). */
    public static MoveAssessment build(int ply, Move playedMove, String playedSan,
                                       List<EngineEvaluation> topMoves,
                                       int evalBefore, int evalAfter) {
        int cpLoss = Math.max(0, evalBefore - evalAfter);
        String bestUci = topMoves.isEmpty() ? null : topMoves.get(0).getBestMoveUci();
        MoveClassification cls;
        if (cpLoss <= 5) {
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
        // Played the top engine choice exactly?  Always call it BEST.
        if (bestUci != null && bestUci.equals(playedMove.toUci())) {
            cls = MoveClassification.BEST;
        }
        return new MoveAssessment(ply, playedMove, playedSan, topMoves,
                evalBefore, evalAfter, cpLoss, cls);
    }

    public MoveAssessment asBook() {
        return new MoveAssessment(ply, playedMove, playedSan, topMoves,
                evalBefore, evalAfter, 0, MoveClassification.BOOK);
    }

    public int                    getPly()            { return ply; }
    public Move                   getPlayedMove()     { return playedMove; }
    public String                 getPlayedSan()      { return playedSan; }
    public List<EngineEvaluation> getTopMoves()       { return topMoves; }
    public int                    getEvalBefore()     { return evalBefore; }
    public int                    getEvalAfter()      { return evalAfter; }
    public int                    getCentipawnLoss()  { return centipawnLoss; }
    public MoveClassification     getClassification() { return classification; }

    public String getBestMoveUci() {
        return topMoves.isEmpty() ? null : topMoves.get(0).getBestMoveUci();
    }

    public boolean playedBestMove() {
        return getBestMoveUci() != null && getBestMoveUci().equals(playedMove.toUci());
    }
}