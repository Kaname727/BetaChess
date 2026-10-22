package betachess.com.gamesystem;

import betachess.com.piece.Piece;
import betachess.com.piece.PieceColor;
import betachess.com.piece.PieceType;

/**
 * Standard Algebraic Notation (SAN) generator.
 *
 * <p>Handles:</p>
 * <ul>
 *   <li>castling ({@code O-O}, {@code O-O-O}), including checking castle</li>
 *   <li>piece moves ({@code Nf3}, {@code Bb5})</li>
 *   <li>pawns ({@code e4}, {@code exd5})</li>
 *   <li>captures ({@code Nxe5})</li>
 *   <li>disambiguation ({@code Nbd2}, {@code R1a3}, {@code Qh4e1})</li>
 *   <li>en passant ({@code exd6})</li>
 *   <li>promotion ({@code e8=Q})</li>
 *   <li>check suffix ({@code +})</li>
 * </ul>
 *
 * <p>The check suffix is optional because checking whether the move
 * delivers <em>mate</em> would require a full legal-move generation; only
 * {@code +} is appended here.</p>
 */
public final class San {

    private San() {
    }

    /** Basic SAN with check suffix when applicable. */
    public static String of(Board board, Move move) {
        return of(board, move, true);
    }

    public static String of(Board board, Move move, boolean appendCheckSuffix) {
        Piece moving = board.get(move.getFrom());
        if (moving == null) {
            // Defensive fallback: not a legal move for this position.
            return move.toUci();
        }
        PieceType type = moving.getType();
        String base;

        // Castling
        if (type == PieceType.KING
                && Math.abs(move.getTo().file() - move.getFrom().file()) == 2) {
            base = move.getTo().file() == 6 ? "O-O" : "O-O-O";
        } else {
            boolean capture = board.get(move.getTo()) != null
                    || (type == PieceType.PAWN
                    && move.getFrom().file() != move.getTo().file());
            StringBuilder sb = new StringBuilder();
            if (type == PieceType.PAWN) {
                if (capture) {
                    sb.append((char) ('a' + move.getFrom().file())).append('x');
                }
                sb.append(move.getTo().toAlgebraic());
                if (move.isPromotion()) {
                    sb.append('=').append(Character.toUpperCase(move.getPromotion().letter()));
                }
            } else {
                sb.append(Character.toUpperCase(type.letter()));
                sb.append(disambiguation(board, move, moving));
                if (capture) {
                    sb.append('x');
                }
                sb.append(move.getTo().toAlgebraic());
            }
            base = sb.toString();
        }

        if (appendCheckSuffix) {
            Board after = board.copy();
            MoveGenerator.applyMove(after, move);
            if (MoveGenerator.isInCheck(after, moving.getColor().opposite())) {
                base = base + "+";
            }
        }
        return base;
    }

    // ------------------------------------------------------------ disambiguation

    private static String disambiguation(Board board, Move move, Piece moving) {
        PieceType type = moving.getType();
        if (type == PieceType.PAWN || type == PieceType.KING) {
            return "";
        }
        PieceColor color = moving.getColor();
        boolean ambiguous = false;
        boolean sameFile = false;
        boolean sameRank = false;

        for (int f = 0; f < Board.SIZE; f++) {
            for (int r = 0; r < Board.SIZE; r++) {
                Position from = Position.of(f, r);
                if (from.equals(move.getFrom())) {
                    continue;
                }
                Piece other = board.get(from);
                if (other == null || other.getType() != type || other.getColor() != color) {
                    continue;
                }
                if (!canReach(board, from, move.getTo(), other)) {
                    continue;
                }
                ambiguous = true;
                if (f == move.getFrom().file()) {
                    sameFile = true;
                }
                if (r == move.getFrom().rank()) {
                    sameRank = true;
                }
            }
        }

        if (!ambiguous) {
            return "";
        }
        // SAN rules: prefer file, fall back to rank, then full square.
        if (!sameFile) {
            return String.valueOf((char) ('a' + move.getFrom().file()));
        }
        if (!sameRank) {
            return String.valueOf((char) ('1' + move.getFrom().rank()));
        }
        return move.getFrom().toAlgebraic();
    }

    /** Geometric reachability (ignores own-king-in-check, which is fine for SAN). */
    private static boolean canReach(Board board, Position from, Position to, Piece piece) {
        for (int[] dir : piece.moveDirections()) {
            int f = from.file();
            int r = from.rank();
            while (true) {
                f += dir[0];
                r += dir[1];
                if (!Position.isOnBoard(f, r)) {
                    break;
                }
                Position p = Position.of(f, r);
                if (p.equals(to)) {
                    return true;
                }
                if (board.get(p) != null) {
                    break;
                }
                if (!piece.isSliding()) {
                    break;
                }
            }
        }
        return false;
    }
}