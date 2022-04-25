package com.example.a2d_tron_game;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class WelcomeScreenActivity extends AppCompatActivity {


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

    }

    public void createGameRoomButton(View view) {
        Runnable connectToServerSocket = new Runnable() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket("127.0.0.1", 5000);
                    //TODO: send create game room message

                    //TODO: store the information about the game room in Room Database

                    //Go to the lobby
                    Intent lobbyActivity = new Intent(WelcomeScreenActivity.this, GameLobbyActivity.class);
                    WelcomeScreenActivity.this.startActivity(lobbyActivity);

                } catch (IOException e) {
                    e.printStackTrace();
                    displayConnectionFailedToast();
                }

            }
        };
        new Thread(connectToServerSocket).start();
    }

    public void joinGameRoomButton(View view) {
        Runnable connectToServerSocket = new Runnable() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket("127.0.0.1", 5001);

                } catch (IOException e) {
                    e.printStackTrace();
                    displayConnectionFailedToast();
                }
                // TODO: verify if room exist

                // TODO: If room exist, store the information about the game room in Room Database and go to Lobby

                //Go to the lobby
                Intent lobbyActivity = new Intent(WelcomeScreenActivity.this, GameLobbyActivity.class);
                WelcomeScreenActivity.this.startActivity(lobbyActivity);

            }
        };
        new Thread(connectToServerSocket).start();
    }

    public void displayConnectionFailedToast() {
        Handler threadHandler = new Handler(Looper.getMainLooper());
        threadHandler.post(new Runnable() {
            @Override
            public void run() { //This thread need sometime to update the UI screen.
                Toast.makeText(getApplicationContext(), "Fail to connect to game server",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}