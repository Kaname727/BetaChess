package betachess.com.gamesystem.record;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * File management for saved games: default library folder, naming, dialogs.
 */
public final class GameRecordStore {

    private static final DateTimeFormatter FILE_STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private GameRecordStore() {
    }

    public static Path defaultDirectory() {
        String home = System.getProperty("user.home");
        return Paths.get(home, ".betachess", "games");
    }

    public static String suggestedFileName(GameRecord record) {
        String white = sanitize(record.getTag("White"));
        String black = sanitize(record.getTag("Black"));
        String stamp = LocalDateTime.now().format(FILE_STAMP);
        return white + "_vs_" + black + "_" + stamp + ".pgn";
    }

    public static Path promptForSavePath(Component parent, GameRecord record) {
        Path dir = defaultDirectory();
        try {
            java.nio.file.Files.createDirectories(dir);
        } catch (IOException ignored) {
            // the file chooser can still browse elsewhere
        }
        JFileChooser chooser = new JFileChooser(dir.toFile());
        chooser.setDialogTitle("Save game");
        chooser.setFileFilter(new FileNameExtensionFilter("PGN games (*.pgn)", "pgn"));
        chooser.setSelectedFile(new File(dir.toFile(), suggestedFileName(record)));

        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".pgn")) {
            file = new File(file.getParentFile(), file.getName() + ".pgn");
        }
        return file.toPath();
    }

    public static Path promptForOpenPath(Component parent) {
        Path dir = defaultDirectory();
        JFileChooser chooser = new JFileChooser(
                java.nio.file.Files.isDirectory(dir) ? dir.toFile() : null);
        chooser.setDialogTitle("Open saved game");
        chooser.setFileFilter(new FileNameExtensionFilter("PGN games (*.pgn)", "pgn"));

        if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        return chooser.getSelectedFile().toPath();
    }

    private static String sanitize(String raw) {
        if (raw == null || raw.isEmpty()) return "player";
        String s = raw.replaceAll("[^A-Za-z0-9_-]+", "_");
        return s.isEmpty() ? "player" : s;
    }
}