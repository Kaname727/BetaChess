package betachess.com.engine;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Locates the Stockfish executable.
 *
 * Search order (first hit wins):
 *   1. -Dbetachess.engine.path=<file | bare name>
 *   2. BETACHESS_ENGINE_PATH env var
 *   3. <cwd>/stockfish[.exe]
 *   4. <jar-directory>/stockfish[.exe]              <- fixes "launched from JAR"
 *   5. <jar-directory>/../stockfish[.exe]           <- project root next to target/
 *   6. -Dbetachess.engine.dir=<dir>
 *   7. PATH lookup
 *   8. platform default locations (apt, homebrew, Program Files …)
 *
 * Always returns an ABSOLUTE path so ProcessBuilder cannot misresolve it.
 */
public final class EngineConfig {

    public static final String PROPERTY_PATH = "betachess.engine.path";
    public static final String PROPERTY_DIR  = "betachess.engine.dir";
    public static final String ENV_PATH      = "BETACHESS_ENGINE_PATH";

    private static final String[] NAMES_UNIX = { "stockfish" };
    private static final String[] NAMES_WIN  = { "stockfish.exe", "stockfish.bat", "stockfish.cmd" };

    private EngineConfig() {
    }

    // ---------------------------------------------------------------- public

    public static String resolveExecutable() {
        List<String> tried = new ArrayList<>();

        // 1. -Dbetachess.engine.path
        String prop = System.getProperty(PROPERTY_PATH);
        if (prop != null && !prop.isBlank()) {
            File f = resolveFlexible(prop, tried);
            if (f == null && !prop.contains(File.separator)) {
                f = findOnPath(prop, tried);          // bare name given
            }
            if (f != null) {
                return f.getAbsolutePath();
            }
        }

        // 2. env var
        String env = System.getenv(ENV_PATH);
        if (env != null && !env.isBlank()) {
            File f = resolveFlexible(env, tried);
            if (f == null && !env.contains(File.separator)) {
                f = findOnPath(env, tried);
            }
            if (f != null) {
                return f.getAbsolutePath();
            }
        }

        // 3-6. directories: cwd, jar dir, jar's parent, -Dbetachess.engine.dir
        for (File dir : searchDirectories()) {
            File found = findInDirectory(dir, tried);
            if (found != null) {
                return found.getAbsolutePath();
            }
        }

        // 7. PATH
        File onPath = findOnPath(null, tried);
        if (onPath != null) {
            return onPath.getAbsolutePath();
        }

        // 8. platform defaults
        for (String candidate : platformCandidates()) {
            File f = new File(candidate);
            if (isExecutableFile(f)) {
                return f.getAbsolutePath();
            }
        }

        throw new IllegalStateException(buildErrorMessage(tried));
    }

    /** Human-readable diagnostics for the "Engine ▸ Engine info…" menu item. */
    public static List<String> diagnostics() {
        List<String> lines = new ArrayList<>();
        lines.add("os             = " + System.getProperty("os.name"));
        lines.add("user.dir       = " + System.getProperty("user.dir"));
        lines.add("jar directory  = " + applicationDirectory());
        lines.add("-D" + PROPERTY_PATH + " = " + System.getProperty(PROPERTY_PATH, "(unset)"));
        lines.add("-D" + PROPERTY_DIR  + " = " + System.getProperty(PROPERTY_DIR,  "(unset)"));
        lines.add("env " + ENV_PATH + "  = " + System.getenv(ENV_PATH));
        try {
            lines.add("resolved       = " + resolveExecutable());
        } catch (RuntimeException ex) {
            lines.add("resolved       = NOT FOUND");
            for (String l : ex.getMessage().split("\n")) {
                lines.add("  " + l);
            }
        }
        return lines;
    }

    // --------------------------------------------------------------- helpers

    private static File resolveFlexible(String raw, List<String> tried) {
        File asIs = new File(raw);
        if (asIs.isAbsolute()) {
            return check(asIs, tried);
        }
        File cwd = new File(System.getProperty("user.dir", "."), raw);
        if (check(cwd, tried) != null) {
            return cwd;
        }
        File appDir = applicationDirectory();
        if (appDir != null) {
            File next = new File(appDir, raw);
            if (check(next, tried) != null) {
                return next;
            }
        }
        return null;
    }

    private static File check(File file, List<String> tried) {
        tried.add(file.getAbsolutePath());
        return isExecutableFile(file) ? file : null;
    }

    private static List<File> searchDirectories() {
        Set<File> dirs = new LinkedHashSet<>();

        File cwd = new File(System.getProperty("user.dir", "."));
        dirs.add(cwd);

        File appDir = applicationDirectory();
        if (appDir != null) {
            dirs.add(appDir);
            File parent = appDir.getParentFile();
            if (parent != null) {
                dirs.add(parent);         // e.g. project root next to target/
            }
        }

        String extra = System.getProperty(PROPERTY_DIR);
        if (extra != null && !extra.isBlank()) {
            File d = new File(extra);
            if (!d.isAbsolute()) {
                d = new File(cwd, extra);
            }
            dirs.add(d);
        }

        return new ArrayList<>(dirs);
    }

    private static File findInDirectory(File dir, List<String> tried) {
        if (dir == null || !dir.isDirectory()) {
            return null;
        }
        for (String name : executableNames()) {
            File candidate = new File(dir, name);
            tried.add(candidate.getAbsolutePath());
            if (isExecutableFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static File findOnPath(String specificName, List<String> tried) {
        String path = System.getenv("PATH");
        if (path == null || path.isBlank()) {
            return null;
        }
        List<String> names = new ArrayList<>();
        if (specificName != null) {
            names.add(specificName);
        }
        for (String n : executableNames()) {
            if (!names.contains(n)) {
                names.add(n);
            }
        }

        for (String entry : path.split(File.pathSeparator)) {
            if (entry.isBlank()) {
                continue;
            }
            for (String name : names) {
                File candidate = new File(entry, name);
                tried.add(candidate.getAbsolutePath() + " (PATH)");
                if (isExecutableFile(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static String[] executableNames() {
        return isWindows() ? NAMES_WIN : NAMES_UNIX;
    }

    private static boolean isExecutableFile(File file) {
        if (file == null || !file.isFile() || file.length() == 0) {
            return false;
        }
        String lower = file.getName().toLowerCase(Locale.ROOT);
        if (isWindows()) {
            // File.canExecute() is unreliable for *.exe on Windows – trust the extension.
            return lower.endsWith(".exe") || lower.endsWith(".bat") || lower.endsWith(".cmd")
                    || file.canExecute();
        }
        return file.canExecute();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
    }

    /** Directory that contains the running jar, or the classes folder under mvn/IDE. */
    private static File applicationDirectory() {
        try {
            URL location = EngineConfig.class.getProtectionDomain()
                    .getCodeSource().getLocation();
            if (location == null) {
                return null;
            }
            File source = new File(location.toURI());
            if (source.isDirectory()) {
                return source;
            }
            return source.getParentFile();
        } catch (Exception ex) {
            return null;
        }
    }

    private static List<String> platformCandidates() {
        List<String> out = new ArrayList<>();
        if (isWindows()) {
            out.add("C:\\Program Files\\Stockfish\\stockfish.exe");
            out.add("C:\\Program Files (x86)\\Stockfish\\stockfish.exe");
            out.add(System.getProperty("user.home") + "\\stockfish\\stockfish.exe");
            out.add(System.getProperty("user.home") + "\\Downloads\\stockfish.exe");
        } else if (isMac()) {
            out.add("/opt/homebrew/bin/stockfish");
            out.add("/usr/local/bin/stockfish");
        } else {
            out.add("/usr/games/stockfish");
            out.add("/usr/bin/stockfish");
            out.add("/usr/local/bin/stockfish");
            out.add("/snap/bin/stockfish");
        }
        return out;
    }

    private static String buildErrorMessage(List<String> tried) {
        StringBuilder msg = new StringBuilder();
        msg.append("Stockfish not found.\n\n");
        msg.append("Fix any of these:\n");
        msg.append("  1. Put stockfish.exe next to betachess.jar and try again\n");
        msg.append("  2. java -D").append(PROPERTY_PATH)
                .append("=/absolute/path/to/stockfish -jar betachess.jar\n");
        msg.append("  3. export ").append(ENV_PATH).append("=/absolute/path/to/stockfish\n");
        msg.append("  4. Install via your package manager so it lands on PATH\n\n");
        msg.append("Looked in:\n");
        int limit = Math.min(tried.size(), 25);
        for (int i = 0; i < limit; i++) {
            msg.append("  • ").append(tried.get(i)).append('\n');
        }
        if (tried.size() > limit) {
            msg.append("  … and ").append(tried.size() - limit).append(" more\n");
        }
        return msg.toString();
    }
}