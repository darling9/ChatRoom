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
            //�洢���û�
            server.addClient(socket);
            BufferedReader reader= new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String msg;
            while ((msg= (reader).readLine())!=null){
                //�Ƿ��˳�
                if(server.isQuit(msg)){
                    break;
                }
                String forwardMessage="�ͻ���"+socket.getPort()+":"+msg+"\n";
                System.out.print(forwardMessage);
                //ת����Ϣ
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
