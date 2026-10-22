package betachess.com.engine;

import betachess.com.gamesystem.Board;
import betachess.com.gamesystem.CastlingRights;
import betachess.com.gamesystem.Position;
import betachess.com.piece.Piece;
import betachess.com.piece.PieceColor;

/**
 * Converts the game state held by ChessGame / Board into a FEN string.
 */
public final class FenSerializer {

    private FenSerializer() {
    }

    public static String toFen(Board board, PieceColor sideToMove,
                               CastlingRights rights, Position enPassantTarget,
                               int halfmoveClock, int fullmoveNumber) {
        StringBuilder fen = new StringBuilder();

        // 1. piece placement, rank 8 → rank 1
        for (int rank = 7; rank >= 0; rank--) {
            int empty = 0;
            for (int file = 0; file < 8; file++) {
                Piece piece = board.get(file, rank);
                if (piece == null) {
                    empty++;
                } else {
                    if (empty > 0) {
                        fen.append(empty);
                        empty = 0;
                    }
                    char letter = piece.getType().letter();
                    fen.append(piece.getColor() == PieceColor.WHITE
                            ? Character.toUpperCase(letter)
                            : letter);
                }
            }
            if (empty > 0) {
                fen.append(empty);
            }
            if (rank > 0) {
                fen.append('/');
            }
        }

        // 2. side to move
        fen.append(' ').append(sideToMove == PieceColor.WHITE ? 'w' : 'b');

        // 3. castling rights
        StringBuilder castling = new StringBuilder();
        if (rights.canCastle(PieceColor.WHITE, true))  castling.append('K');
        if (rights.canCastle(PieceColor.WHITE, false)) castling.append('Q');
        if (rights.canCastle(PieceColor.BLACK, true))  castling.append('k');
        if (rights.canCastle(PieceColor.BLACK, false)) castling.append('q');
        fen.append(' ').append(castling.length() == 0 ? "-" : castling);

        // 4. en passant target
        fen.append(' ').append(enPassantTarget == null ? "-" : enPassantTarget.toAlgebraic());

        // 5. halfmove & fullmove counters
        fen.append(' ').append(halfmoveClock);
        fen.append(' ').append(fullmoveNumber);

        return fen.toString();
    }
}