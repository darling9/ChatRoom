package server;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Set;

public class ChatServerOnNIO {
    private static int DEFAULT_PORT = 8888;
    private final String QUIT = "quit";
    private final int BUFFER=1024;
    private ServerSocketChannel channel;
    private Selector selector;
    private ByteBuffer readBuffer = ByteBuffer.allocate(BUFFER);
    private ByteBuffer writeBuffer = ByteBuffer.allocate(BUFFER);
    private Charset charset=Charset.forName("UTF-8");
    private int port;

    public ChatServerOnNIO(){
        this(DEFAULT_PORT);
    }
    public ChatServerOnNIO(int port){
        this.port=port;
    }


    private void start(){

        try {
            channel = ServerSocketChannel.open();
            channel.configureBlocking(false);
            channel.bind(new InetSocketAddress(port));

            selector=Selector.open();
            channel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("服务器启动，监听端口:"+port);

            while (true) {
                selector.select();
                Set<SelectionKey> selectionKeys=selector.selectedKeys();
                for(SelectionKey key:selectionKeys){
                    handle(key);
                }
                selectionKeys.clear();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }finally {

            close(selector);
        }
    }

    private void handle(SelectionKey key) throws IOException {
        //accept事件-客户端连接
        if(key.isAcceptable()){
            ServerSocketChannel serverSocketChannel= (ServerSocketChannel) key.channel();
            SocketChannel socketChannel=serverSocketChannel.accept();
            socketChannel.configureBlocking(false);
            socketChannel.register(selector,SelectionKey.OP_READ);
            System.out.println("客户端"+socketChannel.socket().getPort()+"连接");
        }
        //read事件-客户端发送信息
        else if(key.isReadable()){
            SocketChannel socketChannel= (SocketChannel) key.channel();
            String msg=receive(socketChannel);
            if(msg.isEmpty()){
                //客户端异常
                key.cancel();
                selector.wakeup();
            }else {
                System.out.println("客户端"+socketChannel.socket().getPort()+":"+msg);
                forward(socketChannel,msg);
            }
            if(isQuit(msg)){
                key.cancel();
                selector.wakeup();
                System.out.println("客户端"+socketChannel.socket().getPort()+"已断开");
            }

        }
    }

    private void forward(SocketChannel socketChannel, String msg) throws IOException {
        for(SelectionKey key:selector.keys()){
            Channel channel= key.channel();
            if(!socketChannel.equals(channel)&& !(channel instanceof ServerSocketChannel) && key.isValid()){
                writeBuffer.clear();
                writeBuffer.put(charset.encode("客户端"+socketChannel.socket().getPort()+":"+msg));
                writeBuffer.flip();
                while (writeBuffer.hasRemaining()){
                    ((SocketChannel)channel).write(writeBuffer);
                }
            }
        }
    }

    private String receive(SocketChannel socketChannel) throws IOException {
        readBuffer.clear();
        while (socketChannel.read(readBuffer)>0){
            readBuffer.flip();
        }
        return String.valueOf(charset.decode(readBuffer));


    }

    public boolean isQuit(String msg){
        return QUIT.equals(msg);
    }
    public void close(Closeable closeable) {
        try {
            if ( closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args){
        ChatServerOnNIO server=new ChatServerOnNIO(8088);
        server.start();
    }
}
