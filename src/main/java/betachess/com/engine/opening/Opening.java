package betachess.com.engine.opening;

/**
 * One entry of the ECO opening book.
 */
public final class Opening {

    private final String eco;    // "B90", "C65", …
    private final String name;   // "Sicilian Defense: Najdorf Variation"

    public Opening(String eco, String name) {
        this.eco = eco;
        this.name = name;
    }

    public String getEco()  { return eco; }
    public String getName() { return name; }

    @Override
    public String toString() {
        return eco + " " + name;
    }
}