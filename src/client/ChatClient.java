package client;

import java.io.*;
import java.net.Socket;

public class ChatClient {
    private final String host="127.0.0.1";
    private final int port=8888;
    private final String QUIT="quit";

    private Socket socket;
    private BufferedWriter writer;
    private BufferedReader reader;

    public void send(String msg) throws IOException {
        if(!socket.isOutputShutdown()){
            writer.write(msg+"\n");
            writer.flush();
        }

    }
    public String receive() throws IOException {
        String msg="";
        if(!socket.isInputShutdown()){
            msg=reader.readLine();
        }
        return msg;
    }

    public boolean isQuit(String msg){
        return QUIT.equals(msg);
    }
    public void close(){
        if(writer!=null){
            try {
                System.out.println("客户端："+socket.getLocalPort()+"下线");
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void start(){
        try {
            socket=new Socket(host,port);
            reader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            //用户输入线程
            new Thread(new UserInputHandler(this)).start();
            String msg;
            while ((msg=receive())!=null){
                System.out.println(msg);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            close();
        }


    }

    public static void main(String[] args){
        ChatClient client=new ChatClient();
        client.start();
    }
}
