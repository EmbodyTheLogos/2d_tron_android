package com.example.a2d_tron_game;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.room.Room;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class GameLobbyActivity extends AppCompatActivity {

    Socket socket;
    final int HEADERSIZE = 10;
    SharedPreferences gameRoomInfo;
    String gameRoomID;
    String gameRoomIP;
    int gameRoomPort;
    volatile boolean doneWithLobby = false; //set this equal to true when the player leaves the game or when the game is started.
    volatile boolean canLeaveGame = false;

    Context context; // this is for getting the resource id by name

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Hide Title Bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();

        setContentView(R.layout.activity_game_lobby);

        //Hide Navigation Bar
        ConstraintLayout mainLayout = (ConstraintLayout) findViewById(R.id.game_lobby_main_constraint);
        mainLayout.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        context = getApplicationContext();

        gameRoomInfo = getSharedPreferences("gameRoomInfo", Context.MODE_PRIVATE);
        gameRoomID = gameRoomInfo.getString("gameRoomID", "");
        gameRoomIP = gameRoomInfo.getString("gameRoomIP", "");
        gameRoomPort = gameRoomInfo.getInt("gameRoomPort", 0);

        TextView textView = findViewById(R.id.game_room_id);
        textView.setText(gameRoomID);


        // By default, the start button is disabled. It will be enabled in joinGameRoom() if this player is the host.
        Button startGameButton = findViewById(R.id.start_game_button);
        startGameButton.setAlpha(.2f);
        startGameButton.setClickable(false);

        joinGameRoom();
    }

    @Override
    public void onBackPressed() {
        leaveGameRoom();
    }

    public void leaveGameRoom()
    {
        if (canLeaveGame) {
            Runnable leaveGameRoomThread = new Runnable() {
                @Override
                public void run() {
                    try {
                        // send leave game request to game server
                        String message = "leave";
                        String headerFormat = "%-" + HEADERSIZE + "s";
                        String header = String.format(headerFormat, message.getBytes().length);
                        socket.getOutputStream().write((header + message).getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    doneWithLobby = true;
                    finish();
                }
            };
            new Thread(leaveGameRoomThread).start();
        }
    }

    public void leaveGameButtonClicked(View view) {

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Leave Game?");
        alert.setMessage("Are you sure you want to leave the game?");
        alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                leaveGameRoom();
                finish();
            }
        });
        alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        alert.create().show();

    }

    public void startGameButtonClicked(View view)
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String message = "host_start";
                String headerFormat = "%-" + HEADERSIZE + "s";
                String header = String.format(headerFormat, message.getBytes().length);
                try {
                    socket.getOutputStream().write((header + message).getBytes());
                    Intent gamePlayActivity = new Intent(GameLobbyActivity.this, GamePlayActivity.class);
                    GameLobbyActivity.this.startActivity(gamePlayActivity);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    public void joinGameRoom() {
        Runnable joinGameRoomThread = new Runnable() {
            @Override
            public void run() {

                try {
                    socket = new Socket(gameRoomIP, gameRoomPort);

                    // load local player name from room database

                    AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                            AppDatabase.class, "player").build();

                    PlayerDAO playerDAO = db.playerDAO();

                    Player localPlayer = playerDAO.findByPlayerID("localPlayer");

                    // send player name to game server
                    String playerName = localPlayer.playerName;
                    String headerFormat = "%-" + HEADERSIZE + "s";
                    String header = String.format(headerFormat, playerName.getBytes().length);
                    socket.getOutputStream().write((header + playerName).getBytes());

                    // receive game room information from game server
                    String playerInfo = receiveServerMessage(socket);
                    JSONObject playerInfoJSON = new JSONObject(playerInfo);
                    String myPlayer = playerInfoJSON.getString("myPlayer");
                    String host = playerInfoJSON.getString("host");
                    JSONArray allPlayersNames = playerInfoJSON.getJSONArray("allPlayersNames");


                    // enable start button if you are the host
                    if (myPlayer.equals(host))
                    {
                        Button startGameButton = findViewById(R.id.start_game_button);
                        startGameButton.setAlpha(1);
                        startGameButton.setClickable(true);
                    }

                    // Update the UI
                    displayAllPlayers(allPlayersNames);

                    // delete all players with playerID != "localPlayer".
                    // Do this to clear out players from last game
                    for (int i = 0; i < 4; i++)
                    {
                        String playerID = "player" + String.valueOf(i+1);
                        Player player = new Player();
                        player.playerID = playerID;
                        playerDAO.delete(player);
                    }

                    // Save all players in database
                    for (int i =0; i < allPlayersNames.length(); i++)
                    {
                        if (!allPlayersNames.getString(i).equals("null"))
                        {
                            String playerID = "player" + String.valueOf(i+1);
                            Player player = new Player();
                            player.playerID = playerID;
                            player.playerName = allPlayersNames.getString(i);
                            if (playerID.equals(myPlayer))
                            {
                                player.isLocal = true;
                            }
                            else
                            {
                                player.isLocal = false;
                            }
                            playerDAO.insertAll(player);
                        }
                    }



                    // the player can leave game at this point
                    canLeaveGame = true;



                    // listen for any update or game start message: i.e. when new player joined or when a new left.
                    while (!doneWithLobby) {
                        String updatedInfo = receiveServerMessage(socket);
                        if (!updatedInfo.equals("host_start"))
                        {
                            System.out.println("updatedInfo" + updatedInfo);
                            JSONArray updateInfoJSON = new JSONArray(updatedInfo);
                            //displayToast("New message from server");

                            //Update the UI
                            displayAllPlayers(updateInfoJSON);

                            // Save all players in database
                            for (int i =0; i < allPlayersNames.length(); i++)
                            {
                                if (!allPlayersNames.getString(i).equals("null"))
                                {
                                    String playerID = "player" + String.valueOf(i+1);
                                    Player player = new Player();
                                    player.playerID = playerID;
                                    player.playerName = allPlayersNames.getString(i);
                                    if (playerID.equals(myPlayer))
                                    {
                                        player.isLocal = true;
                                    }
                                    else
                                    {
                                        player.isLocal = false;
                                    }
                                    playerDAO.insertAll(player);
                                }
                            }
                        }
                        else {
                            // tell the server's lobby to stop listening to this player
                            System.out.println(updatedInfo);
                            String message = "start";
                            headerFormat = "%-" + HEADERSIZE + "s";
                            header = String.format(headerFormat, message.getBytes().length);
                            socket.getOutputStream().write((header + message).getBytes());

                            // done with lobby
                            //displayToast("Start game");
                            doneWithLobby = true;
                            // start the game if you are non host
                            Intent gamePlayActivity = new Intent(GameLobbyActivity.this, GamePlayActivity.class);
                            GameLobbyActivity.this.startActivity(gamePlayActivity);
                        }
                    }

                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }

            }
        };
        new Thread(joinGameRoomThread).start();
    }

    //https://stackoverflow.com/questions/4427608/android-getting-resource-id-from-string
    public int getResId(String resName, String defType) {
        int resId = context.getResources().getIdentifier(
                resName,
                defType,
                context.getPackageName()
        );
        return resId;
    }

    public void displayAllPlayers(JSONArray allPlayersNames) {
        Handler threadHandler = new Handler(Looper.getMainLooper());
        threadHandler.post(new Runnable() {
            @Override
            public void run() { //This thread need sometime to update the UI screen.

                for (int i = 0; i < 4; i++)
                {
                    String imageViewName = "player" + String.valueOf(i+1) + "_image";
                    String textViewName = "player" + String.valueOf(i+1) + "_name";
                    int textViewID = getResId(textViewName, "id" );
                    int imageViewID = getResId(imageViewName, "id");
                    TextView playerName= findViewById(textViewID);
                    ImageView playerImage = findViewById(imageViewID);

                    String imageName = "player" + String.valueOf(i+1);
                    int imageViewResID = getResId(imageName, "drawable");

                    try {
                        if (!allPlayersNames.getString(i).equals("null"))
                        {
                            playerName.setText(allPlayersNames.getString(i));
                            playerImage.setImageResource(imageViewResID);
                        }
                        else
                        {
                            playerName.setText("");
                            playerImage.setImageResource(android.R.color.transparent);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }
        });

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