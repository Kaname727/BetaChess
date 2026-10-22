package betachess.com.engine.opening;

import betachess.com.gamesystem.ChessGame;
import betachess.com.gamesystem.Move;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads the ECO opening book from the classpath and answers position lookups.
 *
 * <p>Resource format ({@code openings.tsv}):</p>
 * <pre>
 * # eco    name                                    uci-moves | FEN:…
 * B90      Sicilian Defense: Najdorf Variation     e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 …
 * C65      Ruy Lopez: Berlin Defense               FEN:r1bqkb1r/…
 * </pre>
 *
 * <p>UCI lines are replayed once at startup to compute the exact position;
 * FEN lines are used as-is (and normalised). Both forms end up in the same
 * {@code Map<String, Opening>} keyed by the four-field EPD.</p>
 */
public final class OpeningBook {

    private static final String RESOURCE = "/betachess/com/assets/openings/openings.tsv";

    private static volatile OpeningBook instance;

    private final Map<String, Opening> byPosition = new HashMap<>();
    private final String resourcePath;

    private OpeningBook(String resourcePath) {
        this.resourcePath = resourcePath;
        load();
    }

    /** Lazily created singleton backed by the bundled resource. */
    public static OpeningBook getDefault() {
        OpeningBook local = instance;
        if (local == null) {
            synchronized (OpeningBook.class) {
                local = instance;
                if (local == null) {
                    local = new OpeningBook(RESOURCE);
                    instance = local;
                }
            }
        }
        return local;
    }

    /** For tests / alternative datasets. */
    public static OpeningBook fromResource(String classpathResource) {
        return new OpeningBook(classpathResource);
    }

    public Opening lookup(String fen) {
        if (fen == null || fen.isEmpty()) {
            return null;
        }
        return byPosition.get(FenNormalizer.normalize(fen));
    }

    public int size() {
        return byPosition.size();
    }

    public Map<String, Opening> asMap() {
        return Collections.unmodifiableMap(byPosition);
    }

    // ---------------------------------------------------------------- loading

    private void load() {
        InputStream stream = OpeningBook.class.getResourceAsStream(resourcePath);
        if (stream == null) {
            System.err.println("[openings] resource not found: " + resourcePath
                    + " – opening detection disabled");
            return;
        }

        int loaded = 0;
        int skipped = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (parseLine(line)) {
                    loaded++;
                } else {
                    skipped++;
                }
            }
        } catch (IOException exception) {
            System.err.println("[openings] failed to read " + resourcePath
                    + ": " + exception.getMessage());
        }
        System.out.println("[openings] loaded " + loaded + " entries"
                + (skipped > 0 ? " (" + skipped + " skipped)" : ""));
    }

    private boolean parseLine(String line) {
        String[] columns = line.split("\t", 3);
        if (columns.length < 3) {
            return false;
        }
        String eco = columns[0].trim();
        String name = columns[1].trim();
        String payload = columns[2].trim();

        String fen;
        if (payload.regionMatches(true, 0, "FEN:", 0, 4)) {
            fen = payload.substring(4).trim();
        } else {
            fen = replayUci(payload);
        }
        if (fen == null || fen.isEmpty()) {
            return false;
        }
        byPosition.put(FenNormalizer.normalize(fen), new Opening(eco, name));
        return true;
    }

    /** Replays a space-separated UCI move list and returns the resulting FEN. */
    private String replayUci(String uciMoves) {
        try {
            ChessGame game = new ChessGame();
            for (String token : uciMoves.trim().split("\\s+")) {
                if (token.isEmpty()) {
                    continue;
                }
                if (!game.makeMove(Move.fromUci(token))) {
                    return null;             // illegal or malformed entry
                }
            }
            return fenOf(game);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String fenOf(ChessGame game) {
        return betachess.com.engine.FenSerializer.toFen(
                game.getBoard(),
                game.getSideToMove(),
                game.getCastlingRights(),
                game.getEnPassantTarget(),
                game.getHalfmoveClock(),
                game.getFullmoveNumber());
    }
}