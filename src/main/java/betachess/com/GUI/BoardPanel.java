package betachess.com.GUI;

import betachess.com.gamesystem.Board;
import betachess.com.gamesystem.Move;
import betachess.com.gamesystem.Position;
import betachess.com.piece.Piece;
import betachess.com.piece.PieceColor;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Draws the board, pieces, highlights and user annotations (arrows /
 * square marks). Left-click selects; right-click annotates.
 *
 * <ul>
 *   <li>right-drag between squares → arrow (green)</li>
 *   <li>Shift + right-drag         → red arrow</li>
 *   <li>Alt + right-drag           → blue arrow</li>
 *   <li>Ctrl/Cmd + right-drag      → yellow arrow</li>
 *   <li>right-click on a square    → toggle square mark</li>
 *   <li>drawing an arrow that already exists toggles it off</li>
 * </ul>
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

    // ------------------------------------------------------- annotations
    private static final class Arrow {
        final Position from;
        final Position to;
        final Color color;
        Arrow(Position from, Position to, Color color) {
            this.from = from; this.to = to; this.color = color;
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Arrow)) return false;
            Arrow a = (Arrow) o;
            return from.equals(a.from) && to.equals(a.to);
        }
        @Override public int hashCode() {
            return 31 * from.hashCode() + to.hashCode();
        }
    }

    private final Set<Arrow> arrows = new LinkedHashSet<Arrow>();
    private final Set<Position> highlights = new LinkedHashSet<Position>();

    private Position rightDragStart;
    private Point rightDragPoint;
    private boolean rightDragging;
    private Color rightDragColor = BoardTheme.ARROW_GREEN;

    public BoardPanel(BoardInteractionListener listener) {
        this.listener = listener;
        setPreferredSize(new Dimension(BoardTheme.BOARD_PIXELS, BoardTheme.BOARD_PIXELS));
        setBackground(BoardTheme.LIGHT_SQUARE);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    handleLeftClick(e.getX(), e.getY());
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    beginRightDrag(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    endRightDrag(e);
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (rightDragStart != null) {
                    rightDragging = true;
                    rightDragPoint = e.getPoint();
                    repaint();
                }
            }
        });
    }

    // ------------------------------------------------------------------ setters

    public void setListener(BoardInteractionListener listener) { this.listener = listener; }
    public void setBoard(Board board)                         { this.board = board; }
    public void setSelected(Position selected)                { this.selected = selected; }
    public void setLegalTargets(List<Position> targets) {
        this.legalTargets = targets == null ? Collections.<Position>emptyList() : targets;
    }
    public void setLastMove(Move lastMove)                    { this.lastMove = lastMove; }
    public void setCheckSquare(Position checkSquare)          { this.checkSquare = checkSquare; }
    public void setFlipped(boolean flipped)                   { this.flipped = flipped; }

    /** Removes every user-drawn arrow and square mark. */
    public void clearAnnotations() {
        if (arrows.isEmpty() && highlights.isEmpty()) return;
        arrows.clear();
        highlights.clear();
        repaint();
    }

    public boolean hasAnnotations() {
        return !arrows.isEmpty() || !highlights.isEmpty();
    }

    // ------------------------------------------------------------------ input

    private void handleLeftClick(int x, int y) {
        if (listener == null) return;
        Position position = positionAt(x, y);
        if (position != null) listener.onSquareSelected(position);
    }

    private void beginRightDrag(MouseEvent e) {
        Position position = positionAt(e.getX(), e.getY());
        if (position == null) return;
        rightDragStart = position;
        rightDragPoint = e.getPoint();
        rightDragging = false;
        rightDragColor = colorFor(e);
    }

    private void endRightDrag(MouseEvent e) {
        if (rightDragStart == null) return;
        Position end = positionAt(e.getX(), e.getY());
        if (end == null) {
            rightDragStart = null;
            rightDragPoint = null;
            rightDragging = false;
            repaint();
            return;
        }

        if (!rightDragging || end.equals(rightDragStart)) {
            toggleHighlight(rightDragStart);
        } else {
            toggleArrow(rightDragStart, end, rightDragColor);
        }

        rightDragStart = null;
        rightDragPoint = null;
        rightDragging = false;
        repaint();
    }

    private void toggleHighlight(Position position) {
        if (!highlights.remove(position)) {
            highlights.add(position);
        }
    }

    private void toggleArrow(Position from, Position to, Color color) {
        Arrow sameDir = null;
        Arrow oppDir = null;
        for (Arrow a : arrows) {
            if (a.from.equals(from) && a.to.equals(to))      sameDir = a;
            else if (a.from.equals(to) && a.to.equals(from)) oppDir = a;
        }
        if (sameDir != null && sameDir.color.equals(color)) {
            arrows.remove(sameDir);           // clicking the same arrow again removes it
            return;
        }
        if (sameDir != null) arrows.remove(sameDir);
        if (oppDir != null) arrows.remove(oppDir);
        arrows.add(new Arrow(from, to, color));
    }

    private static Color colorFor(MouseEvent e) {
        if (e.isShiftDown())                       return BoardTheme.ARROW_RED;
        if (e.isAltDown())                         return BoardTheme.ARROW_BLUE;
        if (e.isControlDown() || e.isMetaDown())   return BoardTheme.ARROW_YELLOW;
        return BoardTheme.ARROW_GREEN;
    }

    private Position positionAt(int x, int y) {
        int column = x / SQUARE;
        int row = y / SQUARE;
        if (column < 0 || column > 7 || row < 0 || row > 7) return null;
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
        drawBoardHighlights(g2);
        drawLegalTargets(g2);
        drawUserHighlights(g2);
        drawPieces(g2);
        drawUserArrows(g2);
        drawDragArrow(g2);
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
                Rectangle r = bounds(Position.of(file, rank));
                boolean light = (file + rank) % 2 != 0;
                g2.setColor(light ? BoardTheme.LIGHT_SQUARE : BoardTheme.DARK_SQUARE);
                g2.fillRect(r.x, r.y, r.width, r.height);
            }
        }
    }

    private void drawBoardHighlights(Graphics2D g2) {
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
    }

    private void drawLegalTargets(Graphics2D g2) {
        g2.setColor(BoardTheme.LEGAL_TARGET);
        for (Position target : legalTargets) {
            Rectangle r = bounds(target);
            if (board.get(target) != null) {
                g2.setStroke(new BasicStroke(4f));
                g2.drawOval(r.x + 6, r.y + 6, r.width - 12, r.height - 12);
            } else {
                int diameter = r.width / 3;
                g2.fillOval(r.x + (r.width - diameter) / 2,
                        r.y + (r.height - diameter) / 2, diameter, diameter);
            }
        }
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawUserHighlights(Graphics2D g2) {
        for (Position p : highlights) {
            Rectangle r = bounds(p);
            int diameter = (int) (r.width * 0.82);
            int x = r.x + (r.width - diameter) / 2;
            int y = r.y + (r.height - diameter) / 2;
            g2.setColor(BoardTheme.ANNOTATION_HIGHLIGHT);
            g2.fillOval(x, y, diameter, diameter);
            g2.setStroke(new BasicStroke(SQUARE * 0.055f));
            g2.setColor(new Color(
                    BoardTheme.ARROW_GREEN.getRed(),
                    BoardTheme.ARROW_GREEN.getGreen(),
                    BoardTheme.ARROW_GREEN.getBlue(), 200));
            g2.drawOval(x, y, diameter, diameter);
        }
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawUserArrows(Graphics2D g2) {
        for (Arrow arrow : arrows) {
            Rectangle a = bounds(arrow.from);
            Rectangle b = bounds(arrow.to);
            drawArrowLine(g2,
                    a.x + a.width / 2, a.y + a.height / 2,
                    b.x + b.width / 2, b.y + b.height / 2,
                    arrow.color);
        }
    }

    private void drawDragArrow(Graphics2D g2) {
        if (rightDragStart == null || rightDragPoint == null || !rightDragging) return;
        Rectangle r = bounds(rightDragStart);
        int x1 = r.x + r.width / 2;
        int y1 = r.y + r.height / 2;

        Position over = positionAt(rightDragPoint.x, rightDragPoint.y);
        int x2, y2;
        if (over != null) {
            Rectangle t = bounds(over);
            x2 = t.x + t.width / 2;
            y2 = t.y + t.height / 2;
        } else {
            x2 = rightDragPoint.x;
            y2 = rightDragPoint.y;
        }
        if (x1 == x2 && y1 == y2) return;
        drawArrowLine(g2, x1, y1, x2, y2, rightDragColor);
    }

    private void drawPieces(Graphics2D g2) {
        for (int file = 0; file < 8; file++) {
            for (int rank = 0; rank < 8; rank++) {
                Piece piece = board.get(file, rank);
                if (piece == null) continue;
                Rectangle r = bounds(Position.of(file, rank));
                Image image = PieceImages.get(piece, r.width);
                if (image != null) {
                    g2.drawImage(image, r.x, r.y, r.width, r.height, null);
                } else {
                    drawFallbackGlyph(g2, piece, r);
                }
            }
        }
    }

    private void drawArrowLine(Graphics2D g2, int x1, int y1, int x2, int y2, Color color) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length < SQUARE * 0.15) return;

        double nx = dx / length;
        double ny = dy / length;

        double startOffset     = SQUARE * 0.30;
        double arrowHeadLength = SQUARE * 0.38;
        double arrowHeadWidth  = SQUARE * 0.24;

        int sx   = (int) Math.round(x1 + nx * startOffset);
        int sy   = (int) Math.round(y1 + ny * startOffset);
        int tipX = (int) Math.round(x2 - nx * SQUARE * 0.06);
        int tipY = (int) Math.round(y2 - ny * SQUARE * 0.06);
        int baseX = (int) Math.round(tipX - nx * arrowHeadLength);
        int baseY = (int) Math.round(tipY - ny * arrowHeadLength);

        double px = -ny;
        double py = nx;
        int w1X = (int) Math.round(baseX + px * arrowHeadWidth);
        int w1Y = (int) Math.round(baseY + py * arrowHeadWidth);
        int w2X = (int) Math.round(baseX - px * arrowHeadWidth);
        int w2Y = (int) Math.round(baseY - py * arrowHeadWidth);

        g2.setColor(color);
        g2.setStroke(new BasicStroke(SQUARE * 0.15f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
        g2.drawLine(sx, sy, baseX, baseY);

        g2.fillPolygon(new int[]{tipX, w1X, w2X}, new int[]{tipY, w1Y, w2Y}, 3);
        g2.setStroke(new BasicStroke(1f));
    }

    private void fill(Graphics2D g2, Rectangle r, Color color) {
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 150));
        g2.fillRect(r.x, r.y, r.width, r.height);
    }

    private void drawFallbackGlyph(Graphics2D g2, Piece piece, Rectangle r) {
        String glyph = PieceImages.fallbackGlyph(piece);
        g2.setFont(new Font("SansSerif", Font.PLAIN, (int) (r.height * 0.78)));
        FontMetrics metrics = g2.getFontMetrics();
        int x = r.x + (r.width - metrics.stringWidth(glyph)) / 2;
        int y = r.y + (r.height + metrics.getAscent()) / 2 - 4;

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
            Rectangle fs = bounds(Position.of(file, flipped ? 7 : 0));
            g2.drawString(String.valueOf((char) ('a' + file)),
                    fs.x + fs.width - 12, fs.y + fs.height - 5);

            int rank = flipped ? i : 7 - i;
            Rectangle rs = bounds(Position.of(flipped ? 7 : 0, rank));
            g2.drawString(String.valueOf(rank + 1), rs.x + 4, rs.y + 14);
        }
    }
}