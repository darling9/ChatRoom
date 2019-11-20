package server;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CharServerOnAIO {
    private static int DEFAULT_PORT = 8888;
    private static final String DEFAULT_HOST="127.0.0.1";
    private final String QUIT = "quit";
    private final int BUFFER=1024;
    private static final int POOL_SIZE=8;
    private AsynchronousChannelGroup channelGroup;
    private AsynchronousServerSocketChannel serverSocketChannel;

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

    public void start(){
        try {
            ExecutorService executorService= Executors.newFixedThreadPool(POOL_SIZE);
            channelGroup=AsynchronousChannelGroup.withThreadPool(executorService);
            serverSocketChannel=AsynchronousServerSocketChannel.open(channelGroup);
            serverSocketChannel.bind(new InetSocketAddress(DEFAULT_HOST,DEFAULT_PORT));
            while (true){
                serverSocketChannel.accept(null,new AcceptHandler());
                System.in.read();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            close(serverSocketChannel);
        }
    }

    private class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel,Object> {

        @Override
        public void completed(AsynchronousSocketChannel result, Object attachment) {
            if(serverSocketChannel.isOpen()){
                serverSocketChannel.accept(null,this);
            }
            if(result!=null && result.isOpen()){
                ByteBuffer byteBuffer = ByteBuffer.allocate(BUFFER);
                //TODO:将客户端加入到在线用户列表
                result.read(byteBuffer,byteBuffer,new ClientHandler());
            }
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            System.out.println("连接失败："+exc.getMessage());

        }
    }
}
