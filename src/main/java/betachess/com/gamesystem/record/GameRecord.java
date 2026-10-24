package betachess.com.gamesystem.record;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable in-memory representation of a saved game.
 * A record is the payload PgnWriter serialises and PgnReader produces.
 */
public final class GameRecord {

    /** One half-move (ply): SAN text plus optional clock state after it. */
    public static final class Ply {
        private final String san;
        private final long   clockMillisAfterMove;   // -1 when unknown

        public Ply(String san, long clockMillisAfterMove) {
            this.san = san;
            this.clockMillisAfterMove = clockMillisAfterMove;
        }

        public String getSan() { return san; }
        public long   getClockMillisAfterMove() { return clockMillisAfterMove; }
    }

    private final Map<String, String> tags;
    private final List<Ply> plies;
    private final String result;

    public GameRecord(Map<String, String> tags, List<Ply> plies, String result) {
        this.tags   = Collections.unmodifiableMap(new LinkedHashMap<>(tags));
        this.plies  = Collections.unmodifiableList(plies);
        this.result = result;
    }

    public Map<String, String> getTags()  { return tags; }
    public List<Ply>           getPlies() { return plies; }
    public String              getResult(){ return result; }
    public String getTag(String name)     { return tags.get(name); }
    public int    plyCount()              { return plies.size(); }
}