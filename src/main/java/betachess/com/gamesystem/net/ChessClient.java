package betachess.com.gamesystem.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * TCP client used by the GUI to talk to the relay server.
 * All callbacks are delivered on the reader thread.
 */
public final class ChessClient {

    public interface Listener {
        void onMessage(String type, String payload);

        void onDisconnected(String reason);
    }

    private final String host;
    private final int port;
    private final Listener listener;

    private Socket socket;
    private PrintWriter out;
    private Thread reader;
    private volatile boolean running;

    public ChessClient(String host, int port, Listener listener) {
        this.host = host;
        this.port = port;
        this.listener = listener;
    }

    public void connect() throws IOException {
        socket = new Socket(host, port);
        out = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        running = true;
        reader = new Thread(this::readLoop, "chess-client-reader");
        reader.setDaemon(true);
        reader.start();
    }

    private void readLoop() {
        String reason = "Connection closed";
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while (running && (line = in.readLine()) != null) {
                listener.onMessage(Protocol.typeOf(line), Protocol.payloadOf(line));
            }
        } catch (IOException exception) {
            reason = exception.getMessage() == null ? "Connection lost" : exception.getMessage();
        } finally {
            running = false;
            if (listener != null) {
                listener.onDisconnected(reason);
            }
        }
    }

    public void send(String type, String payload) {
        if (out != null && running) {
            out.println(Protocol.encode(type, payload));
        }
    }

    public boolean isConnected() {
        return running && socket != null && !socket.isClosed();
    }

    public void close() {
        running = false;
        SocketUtil.closeQuietly(socket);
    }
}