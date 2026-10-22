package betachess.com.gamesystem;

import betachess.com.piece.Piece;
import betachess.com.piece.PieceColor;
import betachess.com.piece.PieceFactory;
import betachess.com.piece.PieceType;

/**
 * A plain 8x8 container of pieces. It knows nothing about rules.
 */
public final class Board {

    public static final int SIZE = Position.SIZE;

    private static final PieceType[] BACK_RANK = {
            PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
            PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK
    };

    /** squares[file][rank] */
    private final Piece[][] squares;

    public Board() {
        squares = new Piece[SIZE][SIZE];
    }

    private Board(Board other) {
        squares = new Piece[SIZE][SIZE];
        for (int file = 0; file < SIZE; file++) {
            System.arraycopy(other.squares[file], 0, squares[file], 0, SIZE);
        }
    }

    public Board copy() {
        return new Board(this);
    }

    /** The standard starting position. */
    public static Board standard() {
        Board board = new Board();
        for (int file = 0; file < SIZE; file++) {
            board.set(Position.of(file, 0), PieceFactory.create(BACK_RANK[file], PieceColor.WHITE));
            board.set(Position.of(file, 1), PieceFactory.create(PieceType.PAWN, PieceColor.WHITE));
            board.set(Position.of(file, 6), PieceFactory.create(PieceType.PAWN, PieceColor.BLACK));
            board.set(Position.of(file, 7), PieceFactory.create(BACK_RANK[file], PieceColor.BLACK));
        }
        return board;
    }

    public Piece get(Position position) {
        return squares[position.file()][position.rank()];
    }

    public Piece get(int file, int rank) {
        return Position.isOnBoard(file, rank) ? squares[file][rank] : null;
    }

    public void set(Position position, Piece piece) {
        squares[position.file()][position.rank()] = piece;
    }

    public boolean isEmpty(Position position) {
        return get(position) == null;
    }

    public Position findKing(PieceColor color) {
        for (int file = 0; file < SIZE; file++) {
            for (int rank = 0; rank < SIZE; rank++) {
                Piece piece = squares[file][rank];
                if (piece != null && piece.getType() == PieceType.KING && piece.getColor() == color) {
                    return Position.of(file, rank);
                }
            }
        }
        return null;
    }
}