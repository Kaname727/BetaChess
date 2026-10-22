package betachess.com.engine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Locates the Stockfish executable.
 *
 * <p>Resolution order (first hit wins):</p>
 * <ol>
 *   <li>-Dbetachess.engine.path=&lt;file | bare name&gt;</li>
 *   <li>BETACHESS_ENGINE_PATH env var</li>
 *   <li>Bundled resource: /betachess/com/assets/stockfish/stockfish-windows-x86-64-universal.exe
 *       (direct file if running from classes; extracted to ~/.betachess/engines if from a jar)</li>
 *   <li>&lt;cwd&gt;/stockfish[.exe]</li>
 *   <li>&lt;jar-dir&gt;/stockfish[.exe]</li>
 *   <li>&lt;jar-dir&gt;/../stockfish[.exe]</li>
 *   <li>-Dbetachess.engine.dir=&lt;dir&gt;</li>
 *   <li>PATH lookup</li>
 *   <li>Platform default install locations</li>
 * </ol>
 *
 * <p>Always returns an ABSOLUTE path so ProcessBuilder cannot misresolve it.</p>
 */
public final class EngineConfig {

    public static final String PROPERTY_PATH            = "betachess.engine.path";
    public static final String PROPERTY_DIR             = "betachess.engine.dir";
    public static final String PROPERTY_DISABLE_BUNDLED = "betachess.engine.disable-bundled";
    public static final String ENV_PATH                 = "BETACHESS_ENGINE_PATH";

    /**
     * Classpath locations of bundled Stockfish builds.
     * Add more entries to support other platforms.
     */
    private static final String[] BUNDLED_RESOURCES = {
            "/betachess/com/assets/stockfish/stockfish-windows-x86-64-universal.exe",
            // "/betachess/com/assets/stockfish/stockfish-macos-m1-apple-silicon",
            // "/betachess/com/assets/stockfish/stockfish-ubuntu-x86-64-avx2",
    };

    private static final String[] NAMES_UNIX = { "stockfish" };
    private static final String[] NAMES_WIN  = { "stockfish.exe", "stockfish.bat", "stockfish.cmd" };

    private EngineConfig() {
    }

    // ---------------------------------------------------------------- public

    public static String resolveExecutable() {
        List<String> tried = new ArrayList<>();

        // 1. explicit -D override
        String prop = System.getProperty(PROPERTY_PATH);
        if (prop != null && !prop.isBlank()) {
            File f = resolveFlexible(prop, tried);
            if (f == null && !prop.contains(File.separator)) {
                f = findOnPath(prop, tried);
            }
            if (f != null) {
                return f.getAbsolutePath();
            }
        }

        // 2. env override
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

        // 3. bundled resource (extracted if inside the jar)
        if (!Boolean.getBoolean(PROPERTY_DISABLE_BUNDLED)) {
            File bundled = locateBundledEngine(tried);
            if (bundled != null) {
                return bundled.getAbsolutePath();
            }
        }

        // 4-7. directories: cwd, jar dir, jar parent, -Dbetachess.engine.dir
        for (File dir : searchDirectories()) {
            File found = findInDirectory(dir, tried);
            if (found != null) {
                return found.getAbsolutePath();
            }
        }

        // 8. PATH
        File onPath = findOnPath(null, tried);
        if (onPath != null) {
            return onPath.getAbsolutePath();
        }

        // 9. platform defaults
        for (String candidate : platformCandidates()) {
            File f = new File(candidate);
            if (isExecutableFile(f)) {
                return f.getAbsolutePath();
            }
        }

        throw new IllegalStateException(buildErrorMessage(tried));
    }

    /** Diagnostics for the "Engine ▸ Engine info…" menu item. */
    public static List<String> diagnostics() {
        List<String> lines = new ArrayList<>();
        lines.add("os             = " + System.getProperty("os.name"));
        lines.add("user.dir       = " + System.getProperty("user.dir"));
        lines.add("jar directory  = " + applicationDirectory());
        lines.add("bundled        = " + bundledStatus());
        lines.add("cache dir      = " + engineCacheDirectory());
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

    // --------------------------------------------------------------- bundled

    private static File locateBundledEngine(List<String> tried) {
        for (String resourcePath : BUNDLED_RESOURCES) {
            URL url = EngineConfig.class.getResource(resourcePath);
            tried.add("classpath:" + resourcePath);
            if (url == null) {
                continue;
            }

            // Dev mode (mvn exec:java / IDE): resource is a real file, use it directly.
            if ("file".equals(url.getProtocol())) {
                try {
                    File direct = new File(url.toURI());
                    if (ensureExecutable(direct)) {
                        return direct;
                    }
                } catch (URISyntaxException | IllegalArgumentException ignored) {
                    // fall through to extraction
                }
            }

            // Packaged jar: extract once and reuse.
            try {
                File extracted = extractResource(resourcePath);
                if (extracted != null) {
                    return extracted;
                }
            } catch (IOException ex) {
                tried.add("  (extraction failed: " + ex.getMessage() + ")");
            }
        }
        return null;
    }

    private static File extractResource(String resourcePath) throws IOException {
        String fileName = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
        File cacheDir = engineCacheDirectory();
        if (!cacheDir.isDirectory() && !cacheDir.mkdirs()) {
            throw new IOException("Cannot create cache directory: " + cacheDir);
        }
        File target = new File(cacheDir, fileName);

        // Extract to a temp file first, then compare / move.
        File tmp = File.createTempFile("betachess-extract-", suffixOf(fileName), cacheDir);
        long extractedSize;
        try {
            try (InputStream in = EngineConfig.class.getResourceAsStream(resourcePath);
                 OutputStream out = new FileOutputStream(tmp)) {
                if (in == null) {
                    throw new IOException("Resource disappeared: " + resourcePath);
                }
                extractedSize = 0;
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) > 0) {
                    out.write(buffer, 0, read);
                    extractedSize += read;
                }
            }
            ensureExecutable(tmp);

            // Reuse an identical cached copy (same size, executable).
            if (target.isFile() && target.length() == extractedSize && ensureExecutable(target)) {
                return target;
            }

            // Replace (or install for the first time).
            if (target.exists() && !target.delete()) {
                // Locked by a currently-running engine → fall back to a unique name.
                File unique = new File(cacheDir,
                        stripExtension(fileName) + "-" + System.nanoTime() + suffixOf(fileName));
                if (!tmp.renameTo(unique)) {
                    throw new IOException("Cannot install extracted engine");
                }
                ensureExecutable(unique);
                return unique;
            }
            if (!tmp.renameTo(target)) {
                throw new IOException("Cannot install extracted engine");
            }
            ensureExecutable(target);
            return target;
        } finally {
            if (tmp.exists()) {
                tmp.delete();
            }
        }
    }

    private static File engineCacheDirectory() {
        String home = System.getProperty("user.home");
        File dir = new File(home, ".betachess/engines");
        if (!dir.isDirectory() && !dir.mkdirs()) {
            File tmp = new File(System.getProperty("java.io.tmpdir"), "betachess-engines");
            tmp.mkdirs();
            return tmp;
        }
        return dir;
    }

    private static boolean ensureExecutable(File file) {
        if (file == null || !file.isFile() || file.length() == 0) {
            return false;
        }
        if (isWindows()) {
            String lower = file.getName().toLowerCase(Locale.ROOT);
            return lower.endsWith(".exe") || lower.endsWith(".bat") || lower.endsWith(".cmd")
                    || file.canExecute();
        }
        if (file.canExecute()) {
            return true;
        }
        //noinspection ResultOfMethodCallIgnored
        file.setExecutable(true, false);
        return file.canExecute();
    }

    private static String bundledStatus() {
        if (Boolean.getBoolean(PROPERTY_DISABLE_BUNDLED)) {
            return "disabled (-D" + PROPERTY_DISABLE_BUNDLED + "=true)";
        }
        for (String path : BUNDLED_RESOURCES) {
            if (EngineConfig.class.getResource(path) != null) {
                return "found (" + path + ")";
            }
        }
        return "not present in jar";
    }

    private static String suffixOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot);
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? fileName : fileName.substring(0, dot);
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
                dirs.add(parent);
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

    private static File applicationDirectory() {
        try {
            URL location = EngineConfig.class.getProtectionDomain()
                    .getCodeSource().getLocation();
            if (location == null) {
                return null;
            }
            File source = new File(location.toURI());
            return source.isDirectory() ? source : source.getParentFile();
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
        msg.append("  1. Put stockfish-windows-x86-64-universal.exe at\n")
                .append("       src/main/resources/betachess/com/assets/stockfish/\n")
                .append("     (or anywhere on your PATH)\n");
        msg.append("  2. java -D").append(PROPERTY_PATH)
                .append("=/absolute/path/to/stockfish -jar betachess.jar\n");
        msg.append("  3. export ").append(ENV_PATH).append("=/absolute/path/to/stockfish\n\n");
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