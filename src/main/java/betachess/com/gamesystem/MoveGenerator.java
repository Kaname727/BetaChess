package betachess.com.gamesystem;

import betachess.com.piece.Pawn;
import betachess.com.piece.Piece;
import betachess.com.piece.PieceColor;
import betachess.com.piece.PieceFactory;
import betachess.com.piece.PieceType;

import java.util.ArrayList;
import java.util.List;

/**
 * Stateless rule engine: generates moves and applies them to a board.
 */
public final class MoveGenerator {

    private static final PieceType[] PROMOTION_PIECES = {
            PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT
    };

    private MoveGenerator() {
    }

    // ------------------------------------------------------------------ moves

    public static List<Move> legalMoves(Board board, PieceColor side,
                                        CastlingRights rights, Position enPassantTarget) {
        List<Move> legal = new ArrayList<Move>();
        for (Move move : pseudoLegalMoves(board, side, rights, enPassantTarget)) {
            Board after = board.copy();
            applyMove(after, move);
            if (!isInCheck(after, side)) {
                legal.add(move);
            }
        }
        return legal;
    }

    public static List<Move> pseudoLegalMoves(Board board, PieceColor side,
                                              CastlingRights rights, Position enPassantTarget) {
        List<Move> moves = new ArrayList<Move>();
        for (int file = 0; file < Board.SIZE; file++) {
            for (int rank = 0; rank < Board.SIZE; rank++) {
                Position from = Position.of(file, rank);
                Piece piece = board.get(from);
                if (piece == null || piece.getColor() != side) {
                    continue;
                }
                if (piece.getType() == PieceType.PAWN) {
                    addPawnMoves(board, from, (Pawn) piece, enPassantTarget, moves);
                } else {
                    addDirectionalMoves(board, from, piece, moves);
                }
            }
        }
        addCastlingMoves(board, side, rights, moves);
        return moves;
    }

    private static void addDirectionalMoves(Board board, Position from, Piece piece, List<Move> moves) {
        for (int[] direction : piece.moveDirections()) {
            int file = from.file();
            int rank = from.rank();
            while (true) {
                file += direction[0];
                rank += direction[1];
                if (!Position.isOnBoard(file, rank)) {   // was: Board.isOnBoard(...)
                    break;
                }
                Position to = Position.of(file, rank);
                Piece occupant = board.get(to);
                if (occupant == null) {
                    moves.add(new Move(from, to));
                } else {
                    if (occupant.getColor() != piece.getColor()) {
                        moves.add(new Move(from, to));
                    }
                    break;
                }
                if (!piece.isSliding()) {
                    break;
                }
            }
        }
    }

    private static void addPawnMoves(Board board, Position from, Pawn pawn,
                                     Position enPassantTarget, List<Move> moves) {
        int direction = pawn.forward();

        Position oneForward = from.offset(0, direction);
        if (oneForward != null && board.isEmpty(oneForward)) {
            addPawnMove(moves, from, oneForward);
            Position twoForward = from.offset(0, direction * 2);
            if (from.rank() == pawn.startRank() && twoForward != null && board.isEmpty(twoForward)) {
                moves.add(new Move(from, twoForward));
            }
        }

        for (int fileDelta = -1; fileDelta <= 1; fileDelta += 2) {
            Position target = from.offset(fileDelta, direction);
            if (target == null) {
                continue;
            }
            Piece occupant = board.get(target);
            if (occupant != null && occupant.getColor() != pawn.getColor()) {
                addPawnMove(moves, from, target);
            } else if (occupant == null && target.equals(enPassantTarget)) {
                moves.add(new Move(from, target));
            }
        }
    }

    private static void addPawnMove(List<Move> moves, Position from, Position to) {
        if (to.rank() == 0 || to.rank() == Board.SIZE - 1) {
            for (PieceType type : PROMOTION_PIECES) {
                moves.add(new Move(from, to, type));
            }
        } else {
            moves.add(new Move(from, to));
        }
    }

    private static void addCastlingMoves(Board board, PieceColor side,
                                         CastlingRights rights, List<Move> moves) {
        if (rights == null) {
            return;
        }
        int rank = side == PieceColor.WHITE ? 0 : 7;
        Position kingSquare = Position.of(4, rank);
        Piece king = board.get(kingSquare);
        if (king == null || king.getType() != PieceType.KING || king.getColor() != side) {
            return;
        }

        PieceColor enemy = side.opposite();
        if (isSquareAttacked(board, kingSquare, enemy)) {
            return;
        }

        if (rights.canCastle(side, true)) {
            Position f = Position.of(5, rank);
            Position g = Position.of(6, rank);
            Piece rook = board.get(Position.of(7, rank));
            if (isFriendlyRook(rook, side) && board.isEmpty(f) && board.isEmpty(g)
                    && !isSquareAttacked(board, f, enemy)
                    && !isSquareAttacked(board, g, enemy)) {
                moves.add(new Move(kingSquare, g));
            }
        }

        if (rights.canCastle(side, false)) {
            Position b = Position.of(1, rank);
            Position c = Position.of(2, rank);
            Position d = Position.of(3, rank);
            Piece rook = board.get(Position.of(0, rank));
            if (isFriendlyRook(rook, side) && board.isEmpty(b) && board.isEmpty(c) && board.isEmpty(d)
                    && !isSquareAttacked(board, d, enemy)
                    && !isSquareAttacked(board, c, enemy)) {
                moves.add(new Move(kingSquare, c));
            }
        }
    }

    private static boolean isFriendlyRook(Piece piece, PieceColor side) {
        return piece != null && piece.getType() == PieceType.ROOK && piece.getColor() == side;
    }

    // -------------------------------------------------------------- apply move

    /** Applies a move without any validation (castling and en passant included). */
    public static void applyMove(Board board, Move move) {
        Position from = move.getFrom();
        Position to = move.getTo();
        Piece moving = board.get(from);
        if (moving == null) {
            return;
        }

        boolean isPawn = moving.getType() == PieceType.PAWN;
        if (isPawn && from.file() != to.file() && board.get(to) == null) {
            // en passant capture
            board.set(Position.of(to.file(), from.rank()), null);
        }

        board.set(from, null);
        board.set(to, moving);

        if (moving.getType() == PieceType.KING && Math.abs(to.file() - from.file()) == 2) {
            int rank = from.rank();
            if (to.file() == 6) {
                board.set(Position.of(5, rank), board.get(Position.of(7, rank)));
                board.set(Position.of(7, rank), null);
            } else if (to.file() == 2) {
                board.set(Position.of(3, rank), board.get(Position.of(0, rank)));
                board.set(Position.of(0, rank), null);
            }
        }

        if (move.isPromotion()) {
            board.set(to, PieceFactory.create(move.getPromotion(), moving.getColor()));
        }
    }

    // ------------------------------------------------------------------ checks

    public static boolean isInCheck(Board board, PieceColor color) {
        Position king = board.findKing(color);
        return king != null && isSquareAttacked(board, king, color.opposite());
    }

    public static boolean isSquareAttacked(Board board, Position square, PieceColor byColor) {
        for (int file = 0; file < Board.SIZE; file++) {
            for (int rank = 0; rank < Board.SIZE; rank++) {
                Piece piece = board.get(file, rank);
                if (piece == null || piece.getColor() != byColor) {
                    continue;
                }
                if (attacks(board, Position.of(file, rank), piece, square)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean attacks(Board board, Position from, Piece piece, Position target) {
        int fileDelta = target.file() - from.file();
        int rankDelta = target.rank() - from.rank();
        if (fileDelta == 0 && rankDelta == 0) {
            return false;
        }

        switch (piece.getType()) {
            case PAWN: {
                int direction = piece.getColor() == PieceColor.WHITE ? 1 : -1;
                return rankDelta == direction && Math.abs(fileDelta) == 1;
            }
            case KNIGHT:
                return (Math.abs(fileDelta) == 1 && Math.abs(rankDelta) == 2)
                        || (Math.abs(fileDelta) == 2 && Math.abs(rankDelta) == 1);
            case KING:
                return Math.max(Math.abs(fileDelta), Math.abs(rankDelta)) == 1;
            case BISHOP:
                return fileDelta != 0 && Math.abs(fileDelta) == Math.abs(rankDelta)
                        && isPathClear(board, from, target);
            case ROOK:
                return (fileDelta == 0 || rankDelta == 0) && isPathClear(board, from, target);
            case QUEEN:
                if (fileDelta != 0 && rankDelta != 0 && Math.abs(fileDelta) != Math.abs(rankDelta)) {
                    return false;
                }
                return isPathClear(board, from, target);
            default:
                return false;
        }
    }

    private static boolean isPathClear(Board board, Position from, Position target) {
        int fileStep = Integer.signum(target.file() - from.file());
        int rankStep = Integer.signum(target.rank() - from.rank());
        int file = from.file() + fileStep;
        int rank = from.rank() + rankStep;
        while (file != target.file() || rank != target.rank()) {
            if (!Position.isOnBoard(file, rank)) {   // was: Board.isOnBoard(...)
                return false;
            }
            if (board.get(file, rank) != null) {
                return false;
            }
            file += fileStep;
            rank += rankStep;
        }
        return true;
    }
}