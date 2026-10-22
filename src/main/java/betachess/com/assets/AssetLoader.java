package betachess.com.assets;

import betachess.com.piece.PieceColor;
import betachess.com.piece.PieceType;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Loads the optional 128x128 piece images.
 *
 * Lookup order for a piece with key "wK":
 *   1. exact classpath resource /betachess/com/assets/wK.png
 *   2. case-insensitive match against the assets folder (wK.PNG, wk.png, …)
 *   3. give up (GUI draws a Unicode glyph)
 *
 * Enable diagnostics with -Dbetachess.assets.verbose=true.
 */
public final class AssetLoader {

    private static final String PIECE_DIRECTORY          = "betachess/com/assets/";
    private static final String PIECE_DIRECTORY_RESOURCE = "/" + PIECE_DIRECTORY;

    private static final boolean VERBOSE =
            Boolean.getBoolean("betachess.assets.verbose");

    private static final Map<String, BufferedImage> IMAGE_CACHE = new HashMap<>();
    private static final Set<String> MISSING = new HashSet<>();

    /** lower-case base name (no extension) -> classpath resource path */
    private static Map<String, String> directoryIndex;

    private AssetLoader() {
    }

    public static BufferedImage loadPiece(PieceType type, PieceColor color) {
        if (type == null || color == null) {
            return null;
        }
        String key = (color == PieceColor.WHITE ? "w" : "b")
                + Character.toUpperCase(type.letter());

        if (MISSING.contains(key)) {
            return null;
        }
        BufferedImage cached = IMAGE_CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        BufferedImage image = load(key);
        if (image == null) {
            MISSING.add(key);
        } else {
            IMAGE_CACHE.put(key, image);
        }
        return image;
    }

    private static BufferedImage load(String key) {
        // 1. exact name
        String exactPath = PIECE_DIRECTORY_RESOURCE + key + ".png";
        BufferedImage direct = readQuietly(exactPath);
        if (direct != null) {
            log("loaded " + key + " from " + exactPath);
            return direct;
        }

        // 2. case-insensitive via directory index
        String indexedPath = indexDirectory().get(key.toLowerCase());
        if (indexedPath != null) {
            BufferedImage viaIndex = readQuietly(indexedPath);
            if (viaIndex != null) {
                log("loaded " + key + " from " + indexedPath + " (case-insensitive fallback)");
                return viaIndex;
            }
        }

        // 3. nothing worked
        log("MISSING asset for " + key
                + " – tried " + exactPath
                + "; directory index contains " + indexDirectory().keySet());
        return null;
    }

    private static BufferedImage readQuietly(String resourcePath) {
        InputStream stream = AssetLoader.class.getResourceAsStream(resourcePath);
        if (stream == null) {
            log("not on classpath: " + resourcePath);
            return null;
        }
        try {
            BufferedImage image = ImageIO.read(stream);
            if (image == null) {
                log("ImageIO could not decode " + resourcePath
                        + " (is it really a PNG?)");
            }
            return image;
        } catch (IOException exception) {
            log("ImageIO failed for " + resourcePath + ": " + exception.getMessage());
            return null;
        } finally {
            try {
                stream.close();
            } catch (IOException ignored) {
                // ignore
            }
        }
    }

    // ------------------------------------------------------------- indexing

    private static synchronized Map<String, String> indexDirectory() {
        if (directoryIndex != null) {
            return directoryIndex;
        }
        directoryIndex = new HashMap<>();

        URL location;
        try {
            location = AssetLoader.class.getProtectionDomain().getCodeSource().getLocation();
        } catch (SecurityException exception) {
            log("cannot read code source: " + exception.getMessage());
            return directoryIndex;
        }
        if (location == null) {
            log("code source is null");
            return directoryIndex;
        }

        try {
            File source = new File(location.toURI());
            if (source.isDirectory()) {
                indexFileDirectory(new File(source, PIECE_DIRECTORY));
            } else {
                indexJar(source);
            }
        } catch (URISyntaxException | IOException exception) {
            log("could not index assets: " + exception.getMessage());
        }
        return directoryIndex;
    }

    private static void indexFileDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files == null) {
            log("cannot list directory " + directory.getAbsolutePath());
            return;
        }
        for (File file : files) {
            if (file.isFile()) {
                registerIndexEntry(file.getName(), PIECE_DIRECTORY_RESOURCE + file.getName());
            }
        }
    }

    private static void indexJar(File jar) throws IOException {
        try (JarFile jarFile = new JarFile(jar)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !name.startsWith(PIECE_DIRECTORY)) {
                    continue;
                }
                String simpleName = name.substring(PIECE_DIRECTORY.length());
                if (simpleName.contains("/")) {
                    continue;
                }
                registerIndexEntry(simpleName, "/" + name);
            }
        }
    }

    private static void registerIndexEntry(String fileName, String resourcePath) {
        int dot = fileName.lastIndexOf('.');
        String base = (dot < 0 ? fileName : fileName.substring(0, dot)).toLowerCase();
        if (base.isEmpty()) {
            return;
        }
        directoryIndex.put(base, resourcePath);
        if (VERBOSE) {
            System.out.println("[assets] indexed '" + base + "' -> " + resourcePath);
        }
    }

    private static void log(String message) {
        if (VERBOSE) {
            System.out.println("[assets] " + message);
        }
    }
}