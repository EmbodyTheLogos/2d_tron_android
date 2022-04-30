package com.example.a2d_tron_game;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class JoinGamePopUpWindows extends AppCompatActivity {

    Socket socket;
    final int HEADERSIZE = 10;
    SharedPreferences gameRoomInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Hide Title Bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_join_game_pop_up_windows);

        //Hide Navigation Bar
        ConstraintLayout mainLayout = (ConstraintLayout) findViewById(R.id.join_game_pop_up_constraint);
        mainLayout.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        // Initialize sharePreference to store game room information
        gameRoomInfo = getSharedPreferences("gameRoomInfo", Context.MODE_PRIVATE);


        // resize activity
//        DisplayMetrics dm = new DisplayMetrics();
//        getWindowManager().getDefaultDisplay().getMetrics(dm);
//
//        int width = dm.widthPixels;
//        int height = dm.heightPixels;
//
//        getWindow().setLayout((int) (width*0.7), (int) (height));
//
//        WindowManager.LayoutParams params = getWindow().getAttributes();
//        params.gravity = Gravity.CENTER;
//        params.x = 0;
//        params.y = -20;
//        getWindow().setAttributes(params);
    }

    public void joinButtonClicked(View view) {

        EditText gameRoomIDEditText = findViewById(R.id.join_game_room_id);
        String gameRoomID = gameRoomIDEditText.getText().toString();

        Runnable connectToServerSocket = new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket();
                    socket.connect(new InetSocketAddress("66.71.31.184", 5001), 1000);

                    // Create JSON object to send message
                    JSONObject json_message = new JSONObject();
                    try {
                        json_message.put("game_room_id", gameRoomID);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    String message = json_message.toString();
                    String headerFormat = "%-" + HEADERSIZE + "s";
                    String header = String.format(headerFormat, message.getBytes().length);
                    socket.getOutputStream().write((header + message).getBytes());


                    // Create JSONArray to receive message from game server
                    JSONArray gameRoomAddress = new JSONArray(receiveServerMessage(socket));
                    if (gameRoomAddress.getString(0).equals("None")) {
                        displayToast("Game room does not exist");
                    } else {
                        int gameRoomPort = gameRoomAddress.getInt(2);
                        String gameRoomIP = gameRoomAddress.getString(1);
                        String gameRoomID = gameRoomAddress.getString(0);

                        // TODO: save gameRoomIP and gameRoomPort to SharedPreferences
                        SharedPreferences.Editor editor = gameRoomInfo.edit();
                        editor.putString("gameRoomID", gameRoomID);
                        editor.putString("gameRoomIP", gameRoomIP);
                        editor.putInt("gameRoomPort", gameRoomPort);
                        editor.commit();

                        //Go to the lobby
                        Intent gameLobbyActivity = new Intent(JoinGamePopUpWindows.this, GameLobbyActivity.class);
                        JoinGamePopUpWindows.this.startActivity(gameLobbyActivity);
                    }

                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                    displayToast("Fail to connect to server");
                }
                // TODO: verify if room exist

                // TODO: If room exist, store the information about the game room in Room Database and go to Lobby


            }

        };
        new Thread(connectToServerSocket).start();
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
                    if (!serverHeader.toString().equals(""))
                    {
                        serverMessageSize = Integer.parseInt(serverHeader.toString().trim()); //trim() remove empty spaces
                        newMessage = false;
                        receiveMessageSize = 0;
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
        System.out.println(fullMessage.toString());
        return fullMessage.toString();
    }

    public void displayToast(String toastMessage) {
        Handler threadHandler = new Handler(Looper.getMainLooper());
        threadHandler.post(new Runnable() {
            @Override
            public void run() { //This thread need sometime to update the UI screen.
                Toast.makeText(getApplicationContext(), toastMessage,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

}