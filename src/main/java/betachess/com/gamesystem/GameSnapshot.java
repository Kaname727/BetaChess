package betachess.com.gamesystem;

import betachess.com.piece.PieceColor;

/**
 * Immutable copy of everything needed to restore a position (undo, rewind, sync).
 */
public final class GameSnapshot {

    private final Board board;
    private final CastlingRights castlingRights;
    private final Position enPassantTarget;
    private final PieceColor sideToMove;
    private final int halfmoveClock;
    private final int fullmoveNumber;

    public GameSnapshot(Board board, CastlingRights castlingRights, Position enPassantTarget,
                        PieceColor sideToMove, int halfmoveClock, int fullmoveNumber) {
        this.board = board;
        this.castlingRights = castlingRights;
        this.enPassantTarget = enPassantTarget;
        this.sideToMove = sideToMove;
        this.halfmoveClock = halfmoveClock;
        this.fullmoveNumber = fullmoveNumber;
    }

    public Board getBoard() {
        return board;
    }

    public CastlingRights getCastlingRights() {
        return castlingRights;
    }

    public Position getEnPassantTarget() {
        return enPassantTarget;
    }

    public PieceColor getSideToMove() {
        return sideToMove;
    }

    public int getHalfmoveClock() {
        return halfmoveClock;
    }

    public int getFullmoveNumber() {
        return fullmoveNumber;
    }
}