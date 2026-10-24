package betachess.com.gamesystem;

import betachess.com.piece.Piece;
import betachess.com.piece.PieceColor;
import betachess.com.piece.PieceFactory;
import betachess.com.piece.PieceType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The complete state of a chess game: board, side to move, castling rights,
 * en passant target, clocks, history and result.
 */
public final class ChessGame {

    private Board board;
    private PieceColor sideToMove;
    private CastlingRights castlingRights;
    private Position enPassantTarget;
    private int halfmoveClock;
    private int fullmoveNumber;
    private GameStatus status;

    private final List<MoveRecord> history = new ArrayList<MoveRecord>();
    private final List<GameListener> listeners = new ArrayList<GameListener>();
    private List<Move> legalMovesCache;

    public ChessGame() {
        reset();
    }

    // ------------------------------------------------------------------ state

    public void reset() {
        board = Board.standard();
        sideToMove = PieceColor.WHITE;
        castlingRights = new CastlingRights();
        enPassantTarget = null;
        halfmoveClock = 0;
        fullmoveNumber = 1;
        history.clear();
        legalMovesCache = null;
        status = GameStatus.ONGOING;
        refreshStatus();
        fireGameChanged();
    }


    public Board getBoard() {
        return board;
    }

    public PieceColor getSideToMove() {
        return sideToMove;
    }

    public CastlingRights getCastlingRights() {
        return castlingRights;
    }

    public Position getEnPassantTarget() {
        return enPassantTarget;
    }

    public int getHalfmoveClock() {
        return halfmoveClock;
    }

    public int getFullmoveNumber() {
        return fullmoveNumber;
    }

    public GameStatus getStatus() {
        return status;
    }

    public List<MoveRecord> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public List<Move> getLegalMoves() {
        if (legalMovesCache == null) {
            legalMovesCache = MoveGenerator.legalMoves(board, sideToMove, castlingRights, enPassantTarget);
        }
        return Collections.unmodifiableList(legalMovesCache);
    }

    public List<Move> getLegalMovesFrom(Position from) {
        List<Move> result = new ArrayList<Move>();
        for (Move move : getLegalMoves()) {
            if (move.getFrom().equals(from)) {
                result.add(move);
            }
        }
        return result;
    }

    public boolean isInCheck() {
        return MoveGenerator.isInCheck(board, sideToMove);
    }

    // ------------------------------------------------------------------ moves

    public boolean makeMove(Move move) {
        if (move == null || status.isGameOver()) {
            return false;
        }
        if (!getLegalMoves().contains(move)) {
            return false;
        }

        Piece moving = board.get(move.getFrom());
        Piece captured = board.get(move.getTo());

        boolean enPassantCapture = moving.getType() == PieceType.PAWN
                && move.getFrom().file() != move.getTo().file()
                && captured == null;
        if (enPassantCapture) {
            captured = board.get(Position.of(move.getTo().file(), move.getFrom().rank()));
        }
        boolean castle = moving.getType() == PieceType.KING
                && Math.abs(move.getTo().file() - move.getFrom().file()) == 2;

        GameSnapshot before = snapshot();
        String notation = buildNotation(moving, move, captured != null, castle);

        castlingRights.update(board, move);
        MoveGenerator.applyMove(board, move);

        if (moving.getType() == PieceType.PAWN
                && Math.abs(move.getTo().rank() - move.getFrom().rank()) == 2) {
            enPassantTarget = Position.of(move.getFrom().file(),
                    (move.getFrom().rank() + move.getTo().rank()) / 2);
        } else {
            enPassantTarget = null;
        }

        if (moving.getType() == PieceType.PAWN || captured != null) {
            halfmoveClock = 0;
        } else {
            halfmoveClock++;
        }
        if (sideToMove == PieceColor.BLACK) {
            fullmoveNumber++;
        }
        sideToMove = sideToMove.opposite();
        legalMovesCache = null;

        history.add(new MoveRecord(move, before, moving, captured, notation));

        GameStatus previous = status;
        refreshStatus();
        fireGameChanged();
        if (!previous.isGameOver() && status.isGameOver()) {
            fireGameFinished();
        }
        return true;
    }

    public boolean undoLastMove() {
        if (history.isEmpty()) {
            return false;
        }
        MoveRecord record = history.remove(history.size() - 1);
        restore(record.getSnapshotBefore());
        legalMovesCache = null;
        refreshStatus();
        fireGameChanged();
        return true;
    }

    /** Ends the game from outside the rule engine (resignation, draw agreement…). */
    public void finish(GameStatus result) {
        if (result == null || !result.isGameOver() || status.isGameOver()) {
            return;
        }
        status = result;
        fireGameChanged();
        fireGameFinished();
    }

    public static ChessGame fromFen(String fen) {
        try {
            String[] parts = fen.trim().split("\\s+");
            ChessGame g = new ChessGame();
            Board b = new Board();
            int rank = 7;
            int file = 0;
            for (char c : parts[0].toCharArray()) {
                if (c == '/') { rank--; file = 0; }
                else if (Character.isDigit(c)) file += c - '0';
                else {
                    PieceColor color = Character.isUpperCase(c) ? PieceColor.WHITE : PieceColor.BLACK;
                    PieceType type = PieceType.fromLetter(Character.toLowerCase(c));
                    b.set(Position.of(file, rank), PieceFactory.create(type, color));
                    file++;
                }
            }
            g.board = b;
            g.sideToMove = "b".equals(parts[1]) ? PieceColor.BLACK : PieceColor.WHITE;
            g.castlingRights = new CastlingRights();
            if (!parts[2].contains("K")) g.castlingRights.revoke(PieceColor.WHITE, true);
            if (!parts[2].contains("Q")) g.castlingRights.revoke(PieceColor.WHITE, false);
            if (!parts[2].contains("k")) g.castlingRights.revoke(PieceColor.BLACK, true);
            if (!parts[2].contains("q")) g.castlingRights.revoke(PieceColor.BLACK, false);
            g.enPassantTarget = parts[3].equals("-") ? null : Position.of(parts[3]);
            g.halfmoveClock   = Integer.parseInt(parts[4]);
            g.fullmoveNumber  = Integer.parseInt(parts[5]);
            return g;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    // ------------------------------------------------------------- internals

    private GameSnapshot snapshot() {
        return new GameSnapshot(board.copy(), castlingRights.copy(), enPassantTarget,
                sideToMove, halfmoveClock, fullmoveNumber);
    }

    private void restore(GameSnapshot snapshot) {
        board = snapshot.getBoard();
        castlingRights = snapshot.getCastlingRights();
        enPassantTarget = snapshot.getEnPassantTarget();
        sideToMove = snapshot.getSideToMove();
        halfmoveClock = snapshot.getHalfmoveClock();
        fullmoveNumber = snapshot.getFullmoveNumber();
    }

    private void refreshStatus() {
        List<Move> legal = getLegalMoves();
        boolean inCheck = MoveGenerator.isInCheck(board, sideToMove);

        if (legal.isEmpty()) {
            status = inCheck ? GameStatus.CHECKMATE : GameStatus.STALEMATE;
        } else if (halfmoveClock >= 100) {
            status = GameStatus.DRAW_FIFTY_MOVE;
        } else if (hasInsufficientMaterial()) {
            status = GameStatus.DRAW_INSUFFICIENT_MATERIAL;
        } else if (inCheck) {
            status = GameStatus.CHECK;
        } else {
            status = GameStatus.ONGOING;
        }
    }

    private boolean hasInsufficientMaterial() {
        int bishops = 0;
        int knights = 0;
        for (int file = 0; file < Board.SIZE; file++) {
            for (int rank = 0; rank < Board.SIZE; rank++) {
                Piece piece = board.get(file, rank);
                if (piece == null) {
                    continue;
                }
                switch (piece.getType()) {
                    case KING:
                        break;
                    case BISHOP:
                        bishops++;
                        break;
                    case KNIGHT:
                        knights++;
                        break;
                    default:
                        return false;
                }
            }
        }
        return (bishops == 0 && knights == 0)
                || (bishops == 1 && knights == 0)
                || (bishops == 0 && knights == 1);
    }

    private String buildNotation(Piece moving, Move move, boolean capture, boolean castle) {
        if (castle) {
            return move.getTo().file() == 6 ? "O-O" : "O-O-O";
        }
        StringBuilder notation = new StringBuilder();
        if (moving.getType() == PieceType.PAWN) {
            if (capture) {
                notation.append((char) ('a' + move.getFrom().file()));
            }
        } else {
            notation.append(Character.toUpperCase(moving.getType().letter()));
        }
        if (capture) {
            notation.append('x');
        }
        notation.append(move.getTo().toAlgebraic());
        if (move.isPromotion()) {
            notation.append('=').append(Character.toUpperCase(move.getPromotion().letter()));
        }
        return notation.toString();
    }

    // -------------------------------------------------------------- listeners

    public void addListener(GameListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(GameListener listener) {
        listeners.remove(listener);
    }

    private void fireGameChanged() {
        for (GameListener listener : new ArrayList<GameListener>(listeners)) {
            listener.onGameChanged(this);
        }
    }

    private void fireGameFinished() {
        for (GameListener listener : new ArrayList<GameListener>(listeners)) {
            listener.onGameFinished(this, status);
        }
    }
}