package betachess.com.gamesystem.net;

/**
 * Tiny line based text protocol shared by server and clients.
 * Format: TYPE|payload (payload may be empty).
 */
public final class Protocol {

    public static final int DEFAULT_PORT = 5555;

    public static final String START        = "START";        // payload: WHITE / BLACK
    public static final String MOVE         = "MOVE";         // payload: UCI move
    public static final String NEW_GAME     = "NEW_GAME";     // payload: empty
    public static final String RESIGN       = "RESIGN";       // payload: empty
    public static final String DRAW_OFFER   = "DRAW_OFFER";   // payload: empty
    public static final String DRAW_ACCEPT  = "DRAW_ACCEPT";  // payload: empty
    public static final String DRAW_DECLINE = "DRAW_DECLINE"; // payload: empty
    public static final String INFO         = "INFO";         // payload: free text

    private static final char SEPARATOR = '|';

    private Protocol() {
    }

    public static String encode(String type, String payload) {
        return type + SEPARATOR + (payload == null ? "" : payload);
    }

    public static String typeOf(String line) {
        if (line == null) {
            return "";
        }
        int index = line.indexOf(SEPARATOR);
        return index < 0 ? line.trim() : line.substring(0, index).trim();
    }

    public static String payloadOf(String line) {
        if (line == null) {
            return "";
        }
        int index = line.indexOf(SEPARATOR);
        return index < 0 ? "" : line.substring(index + 1);
    }
}