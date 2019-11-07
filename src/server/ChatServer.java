package server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    private int DEFAULT_PORT = 8888;
    private final String QUIT = "quit";

    private ServerSocket serverSocket;

    private HashMap<Integer, Writer> clients;

    private ExecutorService executorService;

    public ChatServer() {

        this.executorService= Executors.newFixedThreadPool(10);
        this.clients = new HashMap<>();
    }

    public synchronized void addClient(Socket socket) throws IOException {
        if (socket != null) {
            clients.put(socket.getPort(), new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
            System.out.println("客户端：" + socket.getPort() + "已上线");
        }
    }

    public synchronized void removeClient(Socket socket) throws IOException {
        if (socket != null) {
            if (clients.containsKey(socket.getPort())) {
                clients.get(socket.getPort()).close();
            }
            clients.remove(socket.getPort());
            System.out.println("客户端：" + socket.getPort() + "已断开连接");
        }

    }

    public synchronized void forwardMessage(Socket socket, String message) throws IOException {
        for (int port : clients.keySet()) {
            if (port != socket.getPort()) {
                Writer writer = clients.get(port);
                writer.write(message);
                writer.flush();
            }
        }
    }


    private void start() {
        try {
            serverSocket = new ServerSocket(DEFAULT_PORT);
            System.out.println("启动服务器，监听" + DEFAULT_PORT + "端口");

            while (true) {
                Socket socket = serverSocket.accept();
                //new Thread(new ChatHandler(this,socket)).start();
                executorService.execute(new ChatHandler(this,socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            close();
        }
    }

    public synchronized void close() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isQuit(String msg){
        return QUIT.equals(msg);
    }

    public static void main(String[] args){
        ChatServer server=new ChatServer();
        server.start();
    }
}
