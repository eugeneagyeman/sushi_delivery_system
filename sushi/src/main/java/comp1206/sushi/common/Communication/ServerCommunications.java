package comp1206.sushi.common.Communication;

import comp1206.sushi.common.*;
import comp1206.sushi.server.Server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;


public class ServerCommunications extends Thread implements Serializable {
    private static ServerSocket serverSocket;


    protected static transient ObjectOutputStream outputStream;
    protected static transient ObjectInputStream inputStream;
    static transient Server server;
    transient ClientListener clientListener;

    public ServerCommunications(Server aServer) throws IOException {
        serverSocket = new ServerSocket(3000);
        server = aServer;
        this.start();
    }

    public void run() {
        while (true) {
            try {
                System.out.println("Waiting...");
                System.out.println("Waiting for client on port " +
                        serverSocket.getLocalPort() + "...");

                Socket serverSocket = ServerCommunications.serverSocket.accept();
                outputStream = new ObjectOutputStream(serverSocket.getOutputStream());
                inputStream = new ObjectInputStream(serverSocket.getInputStream());

                for (Dish dishes : server.getDishes()) {
                    outputStream.writeObject(dishes);
                }

                for (Postcode postcode : server.getPostcodes()) {
                    outputStream.writeObject(postcode);
                }

                for (User user : server.getUsers()) {
                    outputStream.writeObject(user);
                }

                clientListener = new ClientListener();
                this.clientListener.start();

            } catch (SocketTimeoutException s) {
                System.out.println("Socket timed out!");
                break;
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public void sendMsg(Object obj, boolean sendToAll) throws IOException {
        outputStream.writeObject(obj);
        if (sendToAll) {
            for (ServerCommunications connectedClients : clientListener.getActiveClients()) {
                connectedClients.getOutputStream().writeObject(obj);
            }
        }
    }

    public ObjectOutputStream getOutputStream() {
        return outputStream;
    }

    public static ObjectInputStream getInputStream() {
        return inputStream;
    }

    protected class ClientListener extends Thread implements Serializable{
        ArrayList<ServerCommunications> activeClients = new ArrayList<>();

        public ArrayList<ServerCommunications> getActiveClients() {
            return activeClients;
        }

        public void run() {
            while (true) {
                receiveMsg();
            }
        }

        void receiveMsg() {
            try {
                Object obj = ServerCommunications.getInputStream().readObject();
                if (obj instanceof Order) {
                    Order receivedOrder = (Order) obj;
                    receivedOrder.setStatus("Received");
                    String notify = String.format("%s:Received", receivedOrder.getOrderID());
                    ServerCommunications.sendMsg(notify);

                    server.getOrders().add(receivedOrder);
                    server.getOrderQueue().add(receivedOrder);
                } else if (obj instanceof User) {
                    server.getUsers().add((User) obj);
                } else if (obj instanceof String) {
                    parseReceivedMsg((String) obj);
                }
            } catch (EOFException ignored) {

            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

    }

    public static void sendMsg(Object obj) throws IOException {
        ServerCommunications.outputStream.writeObject(obj);

    }

    private void parseReceivedMsg(String obj) {

        Integer orderID = Integer.valueOf(obj.split(":")[0]);
        String updateString = obj.split(":")[1];

        for (Order order : server.getOrders()) {
            if (order.getOrderID()==(orderID)) {
                order.setStatus(updateString);
            }
        }

    }


}
