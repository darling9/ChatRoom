package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ChatHandler implements Runnable{
    private ChatServer server;
    private Socket socket;

    public ChatHandler(ChatServer server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }


    @Override
    public void run() {
        try {
            //存储新用户
            server.addClient(socket);
            BufferedReader reader= new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String msg;
            while ((msg= (reader).readLine())!=null){
                //是否退出
                if(server.isQuit(msg)){
                    break;
                }
                String forwardMessage="客户端"+socket.getPort()+":"+msg+"\n";
                System.out.print(forwardMessage);
                //转发消息
                server.forwardMessage(socket,forwardMessage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                server.removeClient(socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
