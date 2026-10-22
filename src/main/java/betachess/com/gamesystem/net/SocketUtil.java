package betachess.com.gamesystem.net;

import java.io.Closeable;
import java.io.IOException;

/**
 * Small helpers shared by the server and the client.
 */
public final class SocketUtil {

    private SocketUtil() {
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignored) {
            // nothing sensible to do
        }
    }
}