package com.zhutonghuo.zgamesbox.core;

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;

public final class NetSession {
    public interface GameListener {
        void onNet(int from, String action, String payload);
    }

    public interface StatusListener {
        void onStatus();
    }

    public static final int DEFAULT_PORT = 42317;

    private static final NetSession INSTANCE = new NetSession();
    public static NetSession get() { return INSTANCE; }

    private final Handler main = new Handler(Looper.getMainLooper());
    private final Random rnd = new Random();
    private final List<Peer> peers = Collections.synchronizedList(new ArrayList<Peer>());
    private ServerSocket server;
    private Thread acceptThread;
    private volatile boolean hosting, connected, verified;
    private volatile Socket clientSock;
    private volatile BufferedReader clientIn;
    private volatile PrintWriter clientOut;
    private volatile String clientIp = "", room = "", myName = "Player";
    private volatile int port = DEFAULT_PORT, mySeat = -1;
    private volatile GameListener gameListener;
    private volatile StatusListener statusListener;
    private volatile String activeGame;

    private static final class Peer {
        final Socket sock;
        final PrintWriter out;
        int seat = -1;
        String name = "";
        Thread reader;
        Peer(Socket s) throws IOException {
            sock = s;
            out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);
        }
    }

    private NetSession() {}

    public boolean isHosting() { return hosting; }
    public boolean isConnected() { return connected; }
    public boolean isOnline() { return connected && mySeat >= 0; }
    public boolean isVerified() { return verified; }
    public int seat() { return mySeat; }
    public int port() { return port; }
    public String room() { return room; }
    public String name() { return myName; }
    public String clientIp() { return clientIp; }
    public String activeGame() { return activeGame; }
    public int peerCount() { return isOnline() ? 1 + seatCount() : 0; }

    private int seatCount() {
        int n = 0;
        synchronized (peers) { for (Peer p : peers) if (p.seat >= 0) n++; }
        return n;
    }

    public String peerName(int seat) {
        if (seat == mySeat) return myName;
        synchronized (peers) {
            for (Peer p : peers) if (p.seat == seat) return p.name;
        }
        return "";
    }

    public String[] allNames() {
        String[] a = {"", "", ""};
        if (!isOnline()) return a;
        a[mySeat] = myName;
        synchronized (peers) {
            for (Peer p : peers) if (p.seat >= 0 && p.seat < 3) a[p.seat] = p.name;
        }
        return a;
    }

    public void setGameListener(GameListener l) { gameListener = l; }
    public void setStatusListener(StatusListener l) { statusListener = l; }
    public void setActiveGame(String g) { activeGame = g; }

    public static String localIPv4() {
        try {
            Enumeration<NetworkInterface> ni = NetworkInterface.getNetworkInterfaces();
            String fallback = "";
            while (ni.hasMoreElements()) {
                NetworkInterface n = ni.nextElement();
                if (!n.isUp() || n.isLoopback() || n.isVirtual()) continue;
                Enumeration<InetAddress> addrs = n.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress a = addrs.nextElement();
                    if (!(a instanceof Inet4Address) || a.isLoopbackAddress()) continue;
                    String h = a.getHostAddress();
                    if (h.startsWith("169.254.")) continue;
                    if (h.startsWith("192.168.") || h.startsWith("10.") || h.startsWith("172.")) return h;
                    if (fallback.isEmpty()) fallback = h;
                }
            }
            return fallback;
        } catch (Exception e) {
            return "";
        }
    }

    public boolean startHost(String name, int listenPort) {
        close();
        myName = name == null || name.trim().isEmpty() ? "player1" : name.trim();
        port = listenPort > 0 ? listenPort : DEFAULT_PORT;
        try {
            server = new ServerSocket(port);
            room = String.valueOf(1000 + rnd.nextInt(9000));
            hosting = true;
            connected = true;
            verified = true;
            mySeat = 0;
            peers.clear();
            acceptThread = new Thread(this::acceptLoop, "zg-accept");
            acceptThread.setDaemon(true);
            acceptThread.start();
            notifyStatus();
            return true;
        } catch (IOException e) {
            close();
            return false;
        }
    }

    private void acceptLoop() {
        while (hosting && server != null && !server.isClosed()) {
            try {
                Socket s = server.accept();
                s.setTcpNoDelay(true);
                Peer p = new Peer(s);
                synchronized (peers) { peers.add(p); }
                p.reader = new Thread(() -> peerReadLoop(p), "zg-peer");
                p.reader.setDaemon(true);
                p.reader.start();
            } catch (IOException e) {
                if (hosting) notifyStatus();
                break;
            }
        }
    }

    private int allocSeat() {
        boolean[] used = new boolean[3];
        used[mySeat] = true;
        synchronized (peers) {
            for (Peer p : peers) if (p.seat >= 0 && p.seat < 3) used[p.seat] = true;
        }
        for (int i = 0; i < 3; i++) if (!used[i]) return i;
        return -1;
    }

    private void peerReadLoop(Peer p) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(p.sock.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = in.readLine()) != null) {
                String[] f = line.split("\\|", -1);
                if (f.length == 0) continue;
                switch (f[0]) {
                    case "PING":
                        p.out.println("PONG");
                        break;
                    case "VERIFY":
                        if (f.length > 1 && room.equals(f[1])) p.out.println("OK");
                        else p.out.println("ERR|房间号错误");
                        break;
                    case "REGISTER": {
                        String want = f.length > 1 ? dec(f[1]) : "";
                        int seat = allocSeat();
                        if (seat < 0) { p.out.println("ERR|房间已满（最多3人）"); break; }
                        p.seat = seat;
                        p.name = want.isEmpty() ? "player" + (seat + 1) : want;
                        p.out.println("WELCOME|" + seat + "|" + enc(room) + "|" + enc(myName) + "|" + enc(String.join("|", allNames())));
                        broadcast("JOIN|" + seat + "|" + enc(p.name), p);
                        notifyStatus();
                        break;
                    }
                    case "GAME": {
                        if (p.seat < 0 || f.length < 5) break;
                        String msg = "GAME|" + f[1] + "|" + p.seat + "|" + f[3] + "|" + f[4];
                        deliver(f[1], p.seat, f[3], dec(f[4]));
                        broadcast(msg, p);
                        break;
                    }
                    case "LEAVE":
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException e) {
        } finally {
            if (p.seat >= 0) broadcast("LEFT|" + p.seat, p);
            synchronized (peers) { peers.remove(p); }
            try { p.sock.close(); } catch (IOException ignored) {}
            notifyStatus();
        }
    }

    public boolean ping(String ip, int port) {
        Socket s = null;
        try {
            s = new Socket(ip, port);
            s.setSoTimeout(1500);
            PrintWriter w = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);
            BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            w.println("PING");
            String resp = r.readLine();
            return "PONG".equals(resp);
        } catch (IOException e) {
            return false;
        } finally {
            if (s != null) try { s.close(); } catch (IOException ignored) {}
        }
    }

    public boolean join(String ip, int port, String roomCode, String name) {
        close();
        Socket s;
        try {
            s = new Socket(ip, port);
            s.setTcpNoDelay(true);
        } catch (IOException e) {
            return false;
        }
        try {
            clientSock = s;
            clientIn = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            clientOut = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);
            clientIp = ip;
            this.port = port;
            connected = true;
            clientOut.println("VERIFY|" + roomCode);
            String resp = readLine(s, 2500);
            if (!"OK".equals(resp)) { close(); return false; }
            verified = true;
            myName = name == null || name.trim().isEmpty() ? "" : name.trim();
            clientOut.println("REGISTER|" + enc(myName));
            String welcome = readLine(s, 2500);
            if (welcome == null || !welcome.startsWith("WELCOME|")) { close(); return false; }
            String[] f = welcome.split("\\|", -1);
            mySeat = Integer.parseInt(f[1]);
            room = dec(f[2]);
            if (myName.isEmpty()) myName = "player" + (mySeat + 1);
            if (f.length > 4) {
                String[] ns = dec(f[4]).split("\\|", -1);
                String[] z = {"", "", ""};
                for (int i = 0; i < 3 && i < ns.length; i++) z[i] = ns[i];
                z[mySeat] = myName;
                clientNames = z;
            }
            Thread t = new Thread(this::clientReadLoop, "zg-read");
            t.setDaemon(true);
            t.start();
            notifyStatus();
            return true;
        } catch (Exception e) {
            close();
            return false;
        }
    }

    private volatile String[] clientNames;

    private String readLine(Socket s, int timeout) throws IOException {
        s.setSoTimeout(timeout);
        try {
            return clientIn.readLine();
        } finally {
            s.setSoTimeout(0);
        }
    }

    private void clientReadLoop() {
        try {
            String line;
            while (clientIn != null && (line = clientIn.readLine()) != null) {
                String[] f = line.split("\\|", -1);
                if (f.length == 0) continue;
                switch (f[0]) {
                    case "WELCOME": {
                        String[] ns = f.length > 4 ? dec(f[4]).split("\\|", -1) : new String[0];
                        String[] z = {"", "", ""};
                        for (int i = 0; i < 3 && i < ns.length; i++) z[i] = ns[i];
                        z[mySeat] = myName;
                        clientNames = z;
                        notifyStatus();
                        break;
                    }
                    case "JOIN": {
                        if (f.length >= 3 && clientNames != null) {
                            int s = Integer.parseInt(f[1]);
                            if (s >= 0 && s < 3) clientNames[s] = dec(f[2]);
                        }
                        notifyStatus();
                        break;
                    }
                    case "LEFT": {
                        if (f.length >= 2 && clientNames != null) {
                            int s = Integer.parseInt(f[1]);
                            if (s >= 0 && s < 3) clientNames[s] = "";
                        }
                        notifyStatus();
                        break;
                    }
                    case "GAME":
                        if (f.length >= 5) deliver(f[1], Integer.parseInt(f[2]), f[3], dec(f[4]));
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException e) {
        } finally {
            if (connected) notifyStatus();
            connected = false;
        }
    }

    public String[] names() {
        if (isHosting()) return allNames();
        if (clientNames != null) return clientNames.clone();
        return allNames();
    }

    private void deliver(String gameId, int from, String action, String payload) {
        if (gameListener == null || gameId == null || !gameId.equals(activeGame)) return;
        final GameListener l = gameListener;
        main.post(() -> l.onNet(from, action, payload));
    }

    private void broadcast(String msg, Peer exclude) {
        synchronized (peers) {
            for (Peer p : peers) {
                if (p == exclude || p.seat < 0) continue;
                p.out.println(msg);
            }
        }
    }

    public void send(String action, String payload) {
        if (!isOnline()) return;
        String p = payload == null ? "" : payload;
        String msg = "GAME|" + (activeGame == null ? "" : activeGame) + "|" + mySeat + "|" + action + "|" + enc(p);
        if (hosting) {
            String fwd = "GAME|" + (activeGame == null ? "" : activeGame) + "|" + mySeat + "|" + action + "|" + enc(p);
            synchronized (peers) { for (Peer q : peers) if (q.seat >= 0) q.out.println(fwd); }
        } else if (clientOut != null) {
            clientOut.println(msg);
        }
    }

    public void leave() {
        if (connected && !hosting && clientOut != null) clientOut.println("LEAVE|" + mySeat);
        close();
    }

    public void close() {
        hosting = false;
        connected = false;
        verified = false;
        mySeat = -1;
        activeGame = null;
        clientNames = null;
        try { if (server != null) server.close(); } catch (IOException ignored) {}
        try { if (clientSock != null) clientSock.close(); } catch (IOException ignored) {}
        synchronized (peers) {
            for (Peer p : peers) try { p.sock.close(); } catch (IOException ignored) {}
            peers.clear();
        }
        server = null;
        clientSock = null;
        clientIn = null;
        clientOut = null;
        acceptThread = null;
        notifyStatus();
    }

    private void notifyStatus() {
        final StatusListener l = statusListener;
        if (l != null) main.post(l::onStatus);
    }

    private static String enc(String s) {
        if (s == null) return "";
        return android.util.Base64.encodeToString(s.getBytes(StandardCharsets.UTF_8), android.util.Base64.NO_WRAP);
    }

    private static String dec(String s) {
        if (s == null || s.isEmpty()) return "";
        try {
            return new String(android.util.Base64.decode(s, android.util.Base64.NO_WRAP), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }
}
