package ch.steinhagen.rest.tcp;

import java.io.*;
import java.net.*;

class TCPClient {
    public static void main(String[] argv) throws Exception {

        String modifiedSentence;
        try (Socket clientSocket = new Socket("localhost", 6789);) {
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            outToServer.writeBytes("Hello World!\n");
            modifiedSentence = inFromServer.readLine();
            System.err.println("FROM SERVER: " + modifiedSentence);
            clientSocket.close();
        }
        System.err.println("client is done");
    }
}
