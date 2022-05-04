package com.example.a2d_tron_game;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

// this class is for synchronizing "Play with Bots" options.
public class LocalGameServer {
    ArrayList<Socket> clientSockets = new ArrayList<Socket>();
    ServerSocket serverSocket;
    Activity activity;
    public final int GAME_BOARD_SIZE = 70;
    public int gameBoard[][] = new int[GAME_BOARD_SIZE][GAME_BOARD_SIZE];
    boolean gameOver;
    final int HEADERSIZE = 10;

    LocalGameServer() {
        try {
            serverSocket = new ServerSocket(5000);
            serverSocket.setReuseAddress(true);
            for (int i = 0; i < 4; i++) {
                Socket clientSocket = serverSocket.accept();
                clientSockets.add(clientSocket);
                System.out.println("client connected");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        gameOver = false;
        System.out.println("server constructor is done");
    }

    public void initializeGameBoard() {
        for (int row = 0; row < GAME_BOARD_SIZE; row++) {
            for (int column = 0; column < GAME_BOARD_SIZE; column++) {
                if (row == 0 || column == 0 || row == GAME_BOARD_SIZE - 1 || column == GAME_BOARD_SIZE - 1) {
                    // creating wall
                    this.gameBoard[row][column] = 1;
                } else {
                    this.gameBoard[row][column] = 0;
                }

            }
        }
    }

    public void run() {

        while (!gameOver) {
            Socket deletedSocket = null;
            for (Socket socket : clientSockets) {
                String receiveMessage = "";
                JSONArray receiveMessageJSONArray;
                try {
                    //socket.setSoTimeout(3000);
                    receiveMessage = receiveServerMessage(socket);

                    //System.out.println("receiveMessage " + receiveMessage);
                    receiveMessageJSONArray = new JSONArray(receiveMessage);
                    int row = receiveMessageJSONArray.getInt(0);
                    int column = receiveMessageJSONArray.getInt(1);
                    int playerCode = receiveMessageJSONArray.getInt(2);

                    // check if the player will live or die making the move
                    String sendMessage = "";
                    if (gameBoard[row][column] == 0) {
                        //System.out.println(gameBoard[row][column]);
                        gameBoard[row][column] = playerCode;
                        sendMessage = "ok";
                    } else {
                        deletePlayer(playerCode);
                        sendMessage = "boom";
                    }


                    //Send move status to player
                    String headerFormat = "%-" + HEADERSIZE + "s";
                    String header = String.format(headerFormat, sendMessage.getBytes().length);
                    socket.getOutputStream().write((header + sendMessage).getBytes());

                    if (sendMessage.equals("boom"))
                    {
                        deletedSocket = socket;
                        break;
                    }

                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                    deletedSocket = socket;
                    break;
                }
            }
            if(clientSockets.size() == 0)
            {
                break;
            }
            if (deletedSocket != null) {
                clientSockets.remove(deletedSocket);
            }
        }

        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Game Over");
    }

    public String receiveServerMessage(Socket socket) throws IOException {
        DataInputStream dis = new DataInputStream(socket.getInputStream());
        byte[] message;
        StringBuilder serverHeader = new StringBuilder();
        ByteArrayOutputStream fullMessage = new ByteArrayOutputStream();

        int receiveMessageSize = HEADERSIZE;
        boolean newMessage = true;

        int serverMessageSize = 0;

        long startTime = System.currentTimeMillis();
        while (true) {
            message = new byte[receiveMessageSize];
            dis.read(message, 0, receiveMessageSize);
            if (newMessage) {
                serverHeader.append(new String(message, StandardCharsets.UTF_8));
                if (serverHeader.length() < HEADERSIZE) //Make sure we receive the full header.
                {
                    receiveMessageSize = HEADERSIZE - serverHeader.length();
                } else {
                    try {
                        serverMessageSize = Integer.parseInt(serverHeader.toString().trim()); //trim() remove empty spaces
                        newMessage = false;
                        receiveMessageSize = 0;
                    } catch (NumberFormatException e) {
                        break;
                    }

                }

            } else {
                fullMessage.write(message);
                receiveMessageSize = serverMessageSize - fullMessage.size();
                if (fullMessage.size() == serverMessageSize) {
                    // newMessage = true;
                    //serverHeader = new StringBuilder();
                    break;
                }
            }

        }
        long endTime = System.currentTimeMillis();
        //System.out.println("receive message time " + (endTime - startTime));
        return fullMessage.toString();
    }


    public void deletePlayer(int playerCode) {
        for (int row = 0; row < GAME_BOARD_SIZE; row++) {
            for (int column = 0; column < GAME_BOARD_SIZE; column++) {
                if (gameBoard[row][column] == playerCode) {
                    gameBoard[row][column] = 0;
                }
            }
        }
    }
}



