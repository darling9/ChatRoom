package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class UserInputHandler implements Runnable {
    private ChatClient client;

    public UserInputHandler(ChatClient client) {
        this.client = client;
    }

    @Override
    public void run() {

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            while (true) {
                String msg = reader.readLine();
                client.send(msg);
                if(client.isQuit(msg)){
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
