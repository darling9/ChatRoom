package client;

import server.ChatServerOnNIO;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Set;

public class ChatClientOnNIO extends ChatClient{
    private static final String DEFAULT_HOST="127.0.0.1";
    private static final int DEFAULT_PORT=8888;
    private final String QUIT="quit";
    private final int BUFFER=1024;
    private ByteBuffer readBuffer = ByteBuffer.allocate(BUFFER);
    private ByteBuffer writeBuffer = ByteBuffer.allocate(BUFFER);
    private Charset charset=Charset.forName("UTF-8");
    private Selector selector;
    private SocketChannel socketChannel;
    private String host;
    private int port;

    public ChatClientOnNIO(String host,int port){
        this.host=host;
        this.port=port;
    }

    public ChatClientOnNIO(){
        this(DEFAULT_HOST,DEFAULT_PORT);
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
    public void start() {
        try {
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            selector = Selector.open();
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
            socketChannel.connect(new InetSocketAddress(host, port));

            while (true) {
                selector.select();
                Set<SelectionKey> selectionKeySet = selector.selectedKeys();
                for (SelectionKey key : selectionKeySet) {
                    handle(key);
                }
                selectionKeySet.clear();
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            close(selector);
        }
    }

    private void handle(SelectionKey key) throws IOException {
        //Á¬½Ó¾ÍÐ÷
        if(key.isConnectable()){
            SocketChannel socketChannel= (SocketChannel) key.channel();
            if(socketChannel.isConnectionPending()){
                socketChannel.finishConnect();
                new Thread(new UserInputHandler(this)).start();
            }
            socketChannel.register(selector,SelectionKey.OP_READ);
        }else if(key.isReadable()){
            SocketChannel socketChannel= (SocketChannel) key.channel();
            String msg=receive(socketChannel);
            if(msg.isEmpty()){
                close(selector);
            }else {
                System.out.println(msg);
            }
        }
    }

    public void send(String msg) throws IOException {
        if(!msg.isEmpty()){
            writeBuffer.clear();
            writeBuffer.put(charset.encode(msg));
            writeBuffer.flip();
            while (writeBuffer.hasRemaining()){
                socketChannel.write(writeBuffer);
            }
            if(isQuit(msg)){
                close(selector);
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
    public static void main(String[] args){
        ChatClientOnNIO client=new ChatClientOnNIO(DEFAULT_HOST,8088);
        client.start();
    }

}
