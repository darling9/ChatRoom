package client;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class ChatClientOnAIO extends ChatClient{
    private static final String DEFAULT_HOST="127.0.0.1";
    private static final int DEFAULT_PORT=8888;
    private final String QUIT="quit";
    private final int BUFFER=1024;
    private ByteBuffer readBuffer = ByteBuffer.allocate(BUFFER);
    private ByteBuffer writeBuffer = ByteBuffer.allocate(BUFFER);
    private Charset charset=Charset.forName("UTF-8");
    private AsynchronousSocketChannel socketChannel;



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
        while (true) {
            try {
                socketChannel = AsynchronousSocketChannel.open();

                Future<Void> future = socketChannel.connect(new InetSocketAddress(DEFAULT_HOST, DEFAULT_PORT));
                future.get();
                new Thread(new UserInputHandler(this)).start();
                Future<Integer> resultFuture = socketChannel.read(readBuffer);
                int result = resultFuture.get();
                if (result <= 0) {
                    System.out.println("Á¬½Ó¶Ï¿ª");
                    close(socketChannel);
                } else {
                    readBuffer.flip();
                    String msg = String.valueOf(charset.decode(readBuffer));
                    readBuffer.clear();
                    System.out.println(msg);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    public void send(String msg){
        if(!msg.isEmpty()){

            Future<Integer> future=socketChannel.write(charset.encode(msg));
            try {
                future.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

        }
    }
    public static void main(String[] args){
        ChatClientOnAIO chatClientOnAIO=new ChatClientOnAIO();
        chatClientOnAIO.start();
    }
}
