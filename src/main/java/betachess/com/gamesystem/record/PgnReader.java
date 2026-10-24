package betachess.com.gamesystem.record;

import betachess.com.gamesystem.ChessGame;
import betachess.com.gamesystem.Move;
import betachess.com.gamesystem.San;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses PGN text into a {@link GameRecord}.
 *
 * <p>Supported:</p>
 * <ul>
 *   <li>tag pairs {@code [Key "Value"]}</li>
 *   <li>move numbers {@code 1.}, {@code 1...}</li>
 *   <li>SAN moves with optional check/mate/annotation suffixes</li>
 *   <li>{@code { ... }} comments, including {@code [%clk h:mm:ss]}</li>
 *   <li>{@code ; ... } end-of-line comments and {@code ( ... )} variations (ignored)</li>
 *   <li>result tokens {@code 1-0}, {@code 0-1}, {@code 1/2-1/2}, {@code *}</li>
 * </ul>
 *
 * <p>Not supported: {@code FEN} tag with a non-standard start. If present,
 * a warning is emitted and the standard start is used.</p>
 */
public final class PgnReader {

    private PgnReader() {
    }

    public static GameRecord read(Path path) throws IOException {
        String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        return read(text);
    }

    public static GameRecord read(String text) throws IOException {
        Map<String, String> tags = new LinkedHashMap<>();
        String movesSection = splitTagsAndMoves(text, tags);

        String fen = tags.get("FEN");
        if (fen != null && !isStandardStart(fen)) {
            throw new IOException("Non-standard start positions are not supported");
        }

        List<String> tokens = tokenise(movesSection);
        List<GameRecord.Ply> plies = new ArrayList<>();
        ChessGame replay = new ChessGame();
        for (String token : tokens) {
            Move move = findMove(replay, token);
            if (move == null) {
                throw new IOException("Unrecognised or illegal SAN: " + token);
            }
            String canonical = San.of(replay.getBoard(), move);
            replay.makeMove(move);
            plies.add(new GameRecord.Ply(canonical, -1));
        }

        String result = tags.get("Result");
        if (result == null) {
            result = "*";
        }
        return new GameRecord(tags, plies, result);
    }

    // ------------------------------------------------------------ tag section

    private static String splitTagsAndMoves(String text, Map<String, String> tags) {
        StringBuilder moves = new StringBuilder();
        int i = 0;
        int length = text.length();
        while (i < length) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (c == '[') {
                int end = text.indexOf(']', i);
                if (end < 0) break;
                String line = text.substring(i + 1, end);
                int sp = line.indexOf(' ');
                if (sp > 0) {
                    String key = line.substring(0, sp).trim();
                    String rawValue = line.substring(sp + 1).trim();
                    if (rawValue.startsWith("\"")) rawValue = rawValue.substring(1);
                    if (rawValue.endsWith("\"")) rawValue = rawValue.substring(0, rawValue.length() - 1);
                    tags.put(key, rawValue);
                }
                i = end + 1;
                continue;
            }
            break;
        }
        // Everything after the tag section is the move section
        moves.append(text, Math.min(i, length), length);
        return moves.toString();
    }

    private static boolean isStandardStart(String fen) {
        return fen.startsWith("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR");
    }

    // ------------------------------------------------------------ move section

    private static List<String> tokenise(String text) {
        List<String> out = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        int variationDepth = 0;
        boolean inBraceComment = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (inBraceComment) {
                if (c == '}') inBraceComment = false;
                continue;
            }
            if (c == ';') {
                while (i < text.length() && text.charAt(i) != '\n') i++;
                continue;
            }
            if (c == '{') { inBraceComment = true; continue; }
            if (c == '(') { variationDepth++; continue; }
            if (c == ')') { if (variationDepth > 0) variationDepth--; continue; }
            if (variationDepth > 0) continue;

            if (Character.isWhitespace(c)) {
                flush(buf, out);
                continue;
            }
            buf.append(c);
        }
        flush(buf, out);
        return out;
    }

    private static void flush(StringBuilder buf, List<String> out) {
        if (buf.length() == 0) return;
        String token = buf.toString();
        buf.setLength(0);

        if (token.matches("\\d+\\.+")) return;   // move number
        if (token.equals("1-0") || token.equals("0-1")
                || token.equals("1/2-1/2") || token.equals("*")) {
            return;                               // result token
        }
        out.add(token);
    }

    // ---------------------------------------------------------- SAN matching

    private static Move findMove(ChessGame game, String san) {
        String target = normalize(san);
        for (Move move : game.getLegalMoves()) {
            String candidate = normalize(San.of(game.getBoard(), move));
            if (candidate.equals(target)) {
                return move;
            }
        }
        return null;
    }

    private static String normalize(String san) {
        String s = san.trim()
                .replace("/", "=")                    // e8/Q → e8=Q
                .replace("0-0-0", "O-O-O")
                .replace("0-0", "O-O")
                .replace("e.p.", "")
                .replaceAll("[+#!?]+", "");
        return s;
    }
}