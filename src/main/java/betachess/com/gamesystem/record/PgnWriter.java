package betachess.com.gamesystem.record;

import betachess.com.gamesystem.ChessGame;
import betachess.com.gamesystem.GameStatus;
import betachess.com.gamesystem.Move;
import betachess.com.gamesystem.MoveRecord;
import betachess.com.gamesystem.San;
import betachess.com.gamesystem.clock.TimeControl;
import betachess.com.piece.PieceColor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a {@link GameRecord} from a live {@link ChessGame} and serialises
 * it to PGN text.
 */
public final class PgnWriter {

    private PgnWriter() {
    }

    /** Replays the game's history to produce a self-consistent record. */
    public static GameRecord toRecord(ChessGame game,
                                      String whiteName,
                                      String blackName,
                                      TimeControl timeControl,
                                      String site) {
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("Event", "BetaChess");
        tags.put("Site", site == null ? "Local" : site);
        tags.put("Date", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        tags.put("Round", "-");
        tags.put("White", whiteName == null ? "White" : whiteName);
        tags.put("Black", blackName == null ? "Black" : blackName);
        tags.put("Result", resultFor(game));
        String tc = formatTimeControl(timeControl);
        if (tc != null) {
            tags.put("TimeControl", tc);
        }

        // Replay to build canonical SAN for every ply.
        ChessGame replay = new ChessGame();
        List<GameRecord.Ply> plies = new ArrayList<>();
        for (MoveRecord record : game.getHistory()) {
            Move move = record.getMove();
            String san = San.of(replay.getBoard(), move);
            replay.makeMove(move);
            plies.add(new GameRecord.Ply(san, -1));
        }
        return new GameRecord(tags, plies, tags.get("Result"));
    }

    /** Serialises the record to PGN text. */
    public static String write(GameRecord record) {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, String> e : record.getTags().entrySet()) {
            out.append('[').append(e.getKey()).append(" \"")
                    .append(escape(e.getValue())).append("\"]\n");
        }
        out.append('\n');

        List<GameRecord.Ply> plies = record.getPlies();
        int moveNumber = 1;
        for (int i = 0; i < plies.size(); i++) {
            boolean whiteMove = (i % 2 == 0);
            if (whiteMove) {
                out.append(moveNumber).append(". ");
            } else if (i == 0) {
                out.append(moveNumber).append("... ");
            }
            out.append(plies.get(i).getSan());
            long clk = plies.get(i).getClockMillisAfterMove();
            if (clk >= 0) {
                out.append(" {[%clk ").append(formatClock(clk)).append("]}");
            }
            out.append(' ');
            if (!whiteMove) {
                moveNumber++;
            }
        }
        out.append(record.getResult()).append('\n');
        return out.toString();
    }

    public static void write(GameRecord record, Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(path, write(record).getBytes(StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------- helpers

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String formatClock(long millis) {
        long totalSeconds = millis / 1000;
        return String.format("%d:%02d:%02d",
                totalSeconds / 3600, (totalSeconds % 3600) / 60, totalSeconds % 60);
    }

    private static String formatTimeControl(TimeControl tc) {
        if (tc == null || tc.isUnlimited()) {
            return "-";
        }
        return (tc.getInitialMillis() / 1000) + "+" + (tc.getIncrementMillis() / 1000);
    }

    private static String resultFor(ChessGame game) {
        GameStatus status = game.getStatus();
        switch (status) {
            case CHECKMATE:
            case RESIGNED:
            case TIMEOUT:
                return game.getSideToMove().opposite() == PieceColor.WHITE ? "1-0" : "0-1";
            case STALEMATE:
            case DRAW_FIFTY_MOVE:
            case DRAW_INSUFFICIENT_MATERIAL:
            case DRAW_AGREED:
                return "1/2-1/2";
            default:
                return "*";
        }
    }
}