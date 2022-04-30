package com.example.a2d_tron_game;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.room.Room;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

public class WelcomeScreenActivity extends AppCompatActivity {

    final int HEADERSIZE = 10;
    Socket socket;
    SharedPreferences gameRoomInfo;
    long createGameButtonLastClicked = SystemClock.currentThreadTimeMillis();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide Title Bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_welcome_screen);

        //Hide Navigation Bar
        ConstraintLayout mainLayout = (ConstraintLayout) findViewById(R.id.welcome_screen_main_constraint);
        mainLayout.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        // Initialize sharePreference to store game room information
        gameRoomInfo = getSharedPreferences("gameRoomInfo", Context.MODE_PRIVATE);

        // load the local player name from database
        updateLocalPlayerName();

        // save the local player name to database
        saveLocalPlayerName();

//        // reset database
//        getApplicationContext().deleteDatabase("player");

    }

    public void createGameRoomButton(View view) {

        long createGameButtonCurrentClicked = System.currentTimeMillis();
        if (createGameButtonCurrentClicked - createGameButtonLastClicked > 1000) {
            createGameButtonLastClicked = createGameButtonCurrentClicked;
            Runnable connectToServerSocket = new Runnable() {
                @Override
                public void run() {
                    try {
                        socket = new Socket();
                        //"35.247.71.135",5000
                        socket.connect(new InetSocketAddress("66.71.31.184", 5000), 1000);


                        // Create JSONArray to receive message from game server
                        JSONArray gameRoomInfoJSONArray = new JSONArray(receiveServerMessage(socket));
                        int gameRoomPort = gameRoomInfoJSONArray.getInt(2);
                        String gameRoomIP = gameRoomInfoJSONArray.getString(1);
                        String gameRoomID = gameRoomInfoJSONArray.getString(0);
                        System.out.println("gameRoomIP" + gameRoomIP);

                        // store the information about the game room in SharedPreferences
                        SharedPreferences.Editor editor = gameRoomInfo.edit();
                        editor.putString("gameRoomID", gameRoomID);
                        editor.putString("gameRoomIP", gameRoomIP);
                        editor.putInt("gameRoomPort", gameRoomPort);
                        editor.commit();


                        //Go to the lobby
                        Intent lobbyActivity = new Intent(WelcomeScreenActivity.this, GameLobbyActivity.class);
                        WelcomeScreenActivity.this.startActivity(lobbyActivity);

                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                        displayToast("Fail to connect to server");
                    }

                }
            };
            new Thread(connectToServerSocket).start();
        }
    }

    public void joinGameRoomButton(View view) {
        Intent joinGamePopUpWindows = new Intent(WelcomeScreenActivity.this, JoinGamePopUpWindows.class);
        WelcomeScreenActivity.this.startActivity(joinGamePopUpWindows);
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
                    if (!serverHeader.toString().equals("")) {
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
    public void playWithBotButtonClicked(View view)
    {
        //Go to the local game
        Intent gamePlayActivity = new Intent(WelcomeScreenActivity.this, GamePlayActivity.class);
        WelcomeScreenActivity.this.startActivity(gamePlayActivity);
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

    public void saveLocalPlayerName()
    {
        // Initialize Room Database
        AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "player").build();

        PlayerDAO playerDAO = db.playerDAO();

        // Get the EditText for player's name
        EditText playerNameEditText = findViewById(R.id.player_name);

        // Set the player name to EditText
        Player localPlayer = new Player();

        playerNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String playerName = playerNameEditText.getText().toString();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        localPlayer.playerName = playerName;
                        localPlayer.playerID = "localPlayer";
                        playerDAO.insertAll(localPlayer);
                    }
                }).start();

            }

            @Override
            public void afterTextChanged(Editable editable) {


            }
        });
    }

    public void updateLocalPlayerName()
    {
        // Initialize Room Database
        AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "player").build();

        PlayerDAO playerDAO = db.playerDAO();

        new Thread(new Runnable() {
            @Override
            public void run() {
                // Get the EditText for player's name
                EditText playerNameEditText = findViewById(R.id.player_name);

                String localPlayerName;

                Player localPlayer = playerDAO.findByPlayerID("localPlayer");
                if (localPlayer != null)
                {
                    localPlayerName = localPlayer.playerName;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Set the player name to EditText
                            playerNameEditText.setText(localPlayerName);
                        }
                    });
                }
            }
        }).start();
    }
}