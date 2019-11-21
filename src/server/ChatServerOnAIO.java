package server;

import javafx.scene.chart.Chart;
import sun.plugin2.ipc.IPCFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServerOnAIO {
    private static int DEFAULT_PORT = 8888;
    private static final String DEFAULT_HOST = "127.0.0.1";
    private final String QUIT = "quit";
    private final int BUFFER = 1024;
    private static final int POOL_SIZE = 8;
    private List<AcceptHandler.ClientHandler> clients;
    private AsynchronousChannelGroup channelGroup;
    private AsynchronousServerSocketChannel serverSocketChannel;
    Charset charset = Charset.forName("UTF-8");

    public ChatServerOnAIO() {

        this.clients = new ArrayList<>();
    }

    public boolean isQuit(String msg) {
        return QUIT.equals(msg);
    }

    public void close(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        try {
            ExecutorService executorService = Executors.newFixedThreadPool(POOL_SIZE);
            channelGroup = AsynchronousChannelGroup.withThreadPool(executorService);
            serverSocketChannel = AsynchronousServerSocketChannel.open(channelGroup);
            serverSocketChannel.bind(new InetSocketAddress(DEFAULT_HOST, DEFAULT_PORT));
            while (true) {
                serverSocketChannel.accept(null, new AcceptHandler());
                System.in.read();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(serverSocketChannel);
        }
    }

    private class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, Object> {

        @Override
        public void completed(AsynchronousSocketChannel result, Object attachment) {
            if (serverSocketChannel.isOpen()) {
                serverSocketChannel.accept(null, this);
            }
            if (result != null && result.isOpen()) {
                ClientHandler handler = new ClientHandler(result);
                ByteBuffer byteBuffer = ByteBuffer.allocate(BUFFER);
                //将客户端加入到在线用户列表
                addClient(handler);
                result.read(byteBuffer, byteBuffer, handler);
            }
        }


        @Override
        public void failed(Throwable exc, Object attachment) {
            System.out.println("连接失败：" + exc.getMessage());

        }

        private class ClientHandler implements CompletionHandler<Integer, Object> {

            private AsynchronousSocketChannel asynchronousSocketChannel;

            public ClientHandler(AsynchronousSocketChannel asynchronousSocketChannel) {
                this.asynchronousSocketChannel = asynchronousSocketChannel;
            }

            @Override
            public void completed(Integer result, Object attachment) {
                if(attachment!=null) {

                    if (result <= 0) {
                        //移除客户端
                        removeClient(this);
                    } else {
                        ByteBuffer byteBuffer = (ByteBuffer) attachment;
                        byteBuffer.flip();
                        String msg = receive(byteBuffer);
                        //System.out.println("客户端"+":" + msg);
                        forward(asynchronousSocketChannel, msg);
                        byteBuffer.clear();
                        if (isQuit(msg)) {
                            removeClient(this);
                        } else {
                            asynchronousSocketChannel.read(byteBuffer, byteBuffer, this);
                        }
                    }
                }

            }


            @Override
            public void failed(Throwable exc, Object attachment) {
                System.out.println("读取失败" + exc.getMessage());
            }
        }


    }

    private synchronized void forward(AsynchronousSocketChannel socketChannel, String msg) {
        for(AcceptHandler.ClientHandler handler:clients){
            try {
                InetSocketAddress address = (InetSocketAddress) handler.asynchronousSocketChannel.getRemoteAddress();
                if (!handler.asynchronousSocketChannel.equals(socketChannel)) {
                    msg="客户端"+address.getPort()+":"+msg;
                    ByteBuffer byteBuffer = charset.encode(msg);
                    handler.asynchronousSocketChannel.write(byteBuffer, null, handler);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }

    }

    private String receive(ByteBuffer byteBuffer) {
        return String.valueOf(charset.decode(byteBuffer));
    }

    private synchronized void addClient(AcceptHandler.ClientHandler handler) {
        clients.add(handler);
        InetSocketAddress address = null;
        try {
            address = (InetSocketAddress) handler.asynchronousSocketChannel.getRemoteAddress();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("客户端" + address.getPort() + "已上线");
    }

    private synchronized void removeClient(AcceptHandler.ClientHandler clientHandler)  {
        clients.remove(clientHandler);
        InetSocketAddress address = null;
        try {
            address = (InetSocketAddress) clientHandler.asynchronousSocketChannel.getRemoteAddress();
            close(clientHandler.asynchronousSocketChannel);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("客户端" + address.getPort() + "已下线");
    }

    public static void main(String[] args){
        ChatServerOnAIO chatServerOnAIO=new ChatServerOnAIO();
        chatServerOnAIO.start();
    }
}
