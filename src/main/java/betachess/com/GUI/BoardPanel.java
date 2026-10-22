package betachess.com.GUI;

import betachess.com.gamesystem.Board;
import betachess.com.gamesystem.Move;
import betachess.com.gamesystem.Position;
import betachess.com.piece.Piece;
import betachess.com.piece.PieceColor;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;

/**
 * Draws the board, the pieces and all highlights, and forwards clicks.
 */
public class BoardPanel extends JPanel {

    private static final int SQUARE = BoardTheme.SQUARE_PIXELS;

    private BoardInteractionListener listener;
    private Board board = new Board();
    private Position selected;
    private List<Position> legalTargets = Collections.emptyList();
    private Move lastMove;
    private Position checkSquare;
    private boolean flipped;

    public BoardPanel(BoardInteractionListener listener) {
        this.listener = listener;
        setPreferredSize(new Dimension(BoardTheme.BOARD_PIXELS, BoardTheme.BOARD_PIXELS));
        setBackground(BoardTheme.LIGHT_SQUARE);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                handleClick(event.getX(), event.getY());
            }
        });
    }

    public void setListener(BoardInteractionListener listener) {
        this.listener = listener;
    }

    public void setBoard(Board board) {
        this.board = board;
    }

    public void setSelected(Position selected) {
        this.selected = selected;
    }

    public void setLegalTargets(List<Position> legalTargets) {
        this.legalTargets = legalTargets == null ? Collections.<Position>emptyList() : legalTargets;
    }

    public void setLastMove(Move lastMove) {
        this.lastMove = lastMove;
    }

    public void setCheckSquare(Position checkSquare) {
        this.checkSquare = checkSquare;
    }

    public void setFlipped(boolean flipped) {
        this.flipped = flipped;
    }

    // ------------------------------------------------------------------ input

    private void handleClick(int x, int y) {
        if (listener == null) {
            return;
        }
        Position position = positionAt(x, y);
        if (position != null) {
            listener.onSquareSelected(position);
        }
    }

    private Position positionAt(int x, int y) {
        int column = x / SQUARE;
        int row = y / SQUARE;
        if (column < 0 || column > 7 || row < 0 || row > 7) {
            return null;
        }
        int file = flipped ? 7 - column : column;
        int rank = flipped ? row : 7 - row;
        return Position.of(file, rank);
    }

    // ---------------------------------------------------------------- painting

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        drawSquares(g2);
        drawHighlights(g2);
        drawPieces(g2);
        drawCoordinates(g2);

        g2.dispose();
    }

    private Rectangle bounds(Position position) {
        int column = flipped ? 7 - position.file() : position.file();
        int row = flipped ? position.rank() : 7 - position.rank();
        return new Rectangle(column * SQUARE, row * SQUARE, SQUARE, SQUARE);
    }

    private void drawSquares(Graphics2D g2) {
        for (int file = 0; file < 8; file++) {
            for (int rank = 0; rank < 8; rank++) {
                Rectangle rectangle = bounds(Position.of(file, rank));
                boolean light = (file + rank) % 2 != 0;
                g2.setColor(light ? BoardTheme.LIGHT_SQUARE : BoardTheme.DARK_SQUARE);
                g2.fillRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
            }
        }
    }

    private void drawHighlights(Graphics2D g2) {
        if (lastMove != null) {
            fill(g2, bounds(lastMove.getFrom()), BoardTheme.LAST_MOVE_SQUARE);
            fill(g2, bounds(lastMove.getTo()), BoardTheme.LAST_MOVE_SQUARE);
        }
        if (checkSquare != null) {
            fill(g2, bounds(checkSquare), BoardTheme.CHECK_SQUARE);
        }
        if (selected != null) {
            fill(g2, bounds(selected), BoardTheme.SELECTED_SQUARE);
        }

        g2.setColor(BoardTheme.LEGAL_TARGET);
        for (Position target : legalTargets) {
            Rectangle rectangle = bounds(target);
            if (board.get(target) != null) {
                g2.setStroke(new BasicStroke(4f));
                g2.drawOval(rectangle.x + 6, rectangle.y + 6,
                        rectangle.width - 12, rectangle.height - 12);
            } else {
                int diameter = rectangle.width / 3;
                g2.fillOval(rectangle.x + (rectangle.width - diameter) / 2,
                        rectangle.y + (rectangle.height - diameter) / 2,
                        diameter, diameter);
            }
        }
        g2.setStroke(new BasicStroke(1f));
    }

    private void fill(Graphics2D g2, Rectangle rectangle, Color color) {
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 150));
        g2.fillRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
    }

    private void drawPieces(Graphics2D g2) {
        for (int file = 0; file < 8; file++) {
            for (int rank = 0; rank < 8; rank++) {
                Piece piece = board.get(file, rank);
                if (piece == null) {
                    continue;
                }
                Rectangle rectangle = bounds(Position.of(file, rank));
                Image image = PieceImages.get(piece, rectangle.width);
                if (image != null) {
                    g2.drawImage(image, rectangle.x, rectangle.y, rectangle.width, rectangle.height, null);
                } else {
                    drawFallbackGlyph(g2, piece, rectangle);
                }
            }
        }
    }

    private void drawFallbackGlyph(Graphics2D g2, Piece piece, Rectangle rectangle) {
        String glyph = PieceImages.fallbackGlyph(piece);
        g2.setFont(new Font("SansSerif", Font.PLAIN, (int) (rectangle.height * 0.78)));
        FontMetrics metrics = g2.getFontMetrics();
        int x = rectangle.x + (rectangle.width - metrics.stringWidth(glyph)) / 2;
        int y = rectangle.y + (rectangle.height + metrics.getAscent()) / 2 - 4;

        Color main = piece.getColor() == PieceColor.WHITE ? Color.WHITE : Color.BLACK;
        Color outline = piece.getColor() == PieceColor.WHITE ? Color.BLACK : Color.WHITE;

        g2.setColor(outline);
        g2.drawString(glyph, x - 1, y);
        g2.drawString(glyph, x + 1, y);
        g2.drawString(glyph, x, y - 1);
        g2.drawString(glyph, x, y + 1);
        g2.setColor(main);
        g2.drawString(glyph, x, y);
    }

    private void drawCoordinates(Graphics2D g2) {
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.setColor(new Color(0, 0, 0, 120));
        for (int i = 0; i < 8; i++) {
            int file = flipped ? 7 - i : i;
            Rectangle fileSquare = bounds(Position.of(file, flipped ? 7 : 0));
            g2.drawString(String.valueOf((char) ('a' + file)),
                    fileSquare.x + fileSquare.width - 12, fileSquare.y + fileSquare.height - 5);

            int rank = flipped ? i : 7 - i;
            Rectangle rankSquare = bounds(Position.of(flipped ? 7 : 0, rank));
            g2.drawString(String.valueOf(rank + 1), rankSquare.x + 4, rankSquare.y + 14);
        }
    }
}
