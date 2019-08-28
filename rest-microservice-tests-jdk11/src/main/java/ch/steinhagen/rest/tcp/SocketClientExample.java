package ch.steinhagen.rest.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SocketClientExample {

    public void startClient() throws IOException, InterruptedException {

        InetSocketAddress hostAddress = new InetSocketAddress("localhost", 8090);
        try (SocketChannel client = SocketChannel.open(hostAddress);) {

            System.out.println("Client... started");

            String threadName = Thread.currentThread().getName();

            // Send messages to server
            String[] messages = new String[] { threadName + ": testMessage1", threadName + ": testMessage2",
                    threadName + ": testMessage3" };

            for (int i = 0; i < messages.length; i++) {
                byte[] message = messages[i].getBytes();
                ByteBuffer buffer = ByteBuffer.wrap(message);
                if (client.isOpen()) {
                    client.write(buffer);
                    System.out.println(messages[i]);
                    buffer.clear();
                    Thread.sleep(5000);
                }
            }
        }
    }

    public static void main(String[] argv) throws Exception {
        Runnable clientTask = () -> {
            try {
                Thread.sleep(1000);
                new SocketClientExample().startClient();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        new Thread(clientTask, "client-Custom").start();
    }
}
