package betachess.com.gamesystem.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Very small relay server: it pairs two connecting players and forwards
 * every line one player sends to the other one.
 */
public final class ChessServer {

    private final int port;
    private ServerSocket serverSocket;
    private Thread acceptThread;
    private volatile boolean running;

    public ChessServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        acceptThread = new Thread(this::acceptLoop, "chess-server-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    public void stop() {
        running = false;
        SocketUtil.closeQuietly(serverSocket);
    }

    public int getPort() {
        return port;
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket first = serverSocket.accept();
                sendInfo(first, "Waiting for an opponent...");
                Socket second = serverSocket.accept();
                new Session(first, second).start();
            } catch (IOException exception) {
                if (running) {
                    System.err.println("Server error: " + exception.getMessage());
                }
            }
        }
    }

    private static void sendInfo(Socket socket, String text) {
        try {
            PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            out.println(Protocol.encode(Protocol.INFO, text));
        } catch (IOException ignored) {
            SocketUtil.closeQuietly(socket);
        }
    }

    /** A pair of players that relay messages to each other. */
    private static final class Session {

        private final Socket white;
        private final Socket black;
        private final PrintWriter whiteOut;
        private final PrintWriter blackOut;

        Session(Socket white, Socket black) throws IOException {
            this.white = white;
            this.black = black;
            this.whiteOut = new PrintWriter(
                    new OutputStreamWriter(white.getOutputStream(), StandardCharsets.UTF_8), true);
            this.blackOut = new PrintWriter(
                    new OutputStreamWriter(black.getOutputStream(), StandardCharsets.UTF_8), true);
        }

        void start() {
            whiteOut.println(Protocol.encode(Protocol.START, "WHITE"));
            blackOut.println(Protocol.encode(Protocol.START, "BLACK"));

            Thread whitePump = new Thread(() -> pump(white, blackOut), "chess-server-white");
            Thread blackPump = new Thread(() -> pump(black, whiteOut), "chess-server-black");
            whitePump.setDaemon(true);
            blackPump.setDaemon(true);
            whitePump.start();
            blackPump.start();
        }

        private void pump(Socket from, PrintWriter to) {
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(from.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = in.readLine()) != null) {
                    to.println(line);
                }
            } catch (IOException ignored) {
                // connection lost
            }
            to.println(Protocol.encode(Protocol.INFO, "Opponent disconnected"));
            SocketUtil.closeQuietly(white);
            SocketUtil.closeQuietly(black);
        }
    }

    /** Allows running the relay standalone: java betachess.com.gamesystem.net.ChessServer [port] */
    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : Protocol.DEFAULT_PORT;
        ChessServer server = new ChessServer(port);
        server.start();
        System.out.println("BetaChess server listening on port " + port);
    }
}