package betachess.com.gamesystem;

import betachess.com.piece.Piece;
import betachess.com.piece.PieceColor;
import betachess.com.piece.PieceType;

/**
 * Tracks whether each side is still allowed to castle on each wing.
 */
public final class CastlingRights {

    private boolean whiteKingSide = true;
    private boolean whiteQueenSide = true;
    private boolean blackKingSide = true;
    private boolean blackQueenSide = true;

    public CastlingRights() {
    }

    private CastlingRights(CastlingRights other) {
        this.whiteKingSide = other.whiteKingSide;
        this.whiteQueenSide = other.whiteQueenSide;
        this.blackKingSide = other.blackKingSide;
        this.blackQueenSide = other.blackQueenSide;
    }

    public CastlingRights copy() {
        return new CastlingRights(this);
    }

    public boolean canCastle(PieceColor color, boolean kingSide) {
        if (color == PieceColor.WHITE) {
            return kingSide ? whiteKingSide : whiteQueenSide;
        }
        return kingSide ? blackKingSide : blackQueenSide;
    }

    public void revoke(PieceColor color, boolean kingSide) {
        if (color == PieceColor.WHITE) {
            if (kingSide) {
                whiteKingSide = false;
            } else {
                whiteQueenSide = false;
            }
        } else {
            if (kingSide) {
                blackKingSide = false;
            } else {
                blackQueenSide = false;
            }
        }
    }

    public void revokeAll(PieceColor color) {
        revoke(color, true);
        revoke(color, false);
    }

    /** Must be called with the board state *before* the move is applied. */
    public void update(Board board, Move move) {
        Piece moving = board.get(move.getFrom());
        if (moving == null) {
            return;
        }
        if (moving.getType() == PieceType.KING) {
            revokeAll(moving.getColor());
        } else if (moving.getType() == PieceType.ROOK) {
            revokeRookSquare(moving.getColor(), move.getFrom());
        }

        Piece captured = board.get(move.getTo());
        if (captured != null && captured.getType() == PieceType.ROOK) {
            revokeRookSquare(captured.getColor(), move.getTo());
        }
    }

    private void revokeRookSquare(PieceColor color, Position square) {
        if (square.equals(rookHomeSquare(color, true))) {
            revoke(color, true);
        } else if (square.equals(rookHomeSquare(color, false))) {
            revoke(color, false);
        }
    }

    public static Position rookHomeSquare(PieceColor color, boolean kingSide) {
        int rank = color == PieceColor.WHITE ? 0 : 7;
        return Position.of(kingSide ? 7 : 0, rank);
    }
}