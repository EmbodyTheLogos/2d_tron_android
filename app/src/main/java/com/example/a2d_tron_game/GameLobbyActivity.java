package com.example.a2d_tron_game;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class GameLobbyActivity extends AppCompatActivity {

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


        //TODO: Load information from Room Database and connect to game server.

        //TODO: listen to update from server if a new player connect or disconnect.
    }

    //TODO: request room information from server when there is a new player joined or left.

    //TODO: display that information.

    //TODO: determine who is the host.
}