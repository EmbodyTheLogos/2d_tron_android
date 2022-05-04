package com.example.a2d_tron_game;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.room.Room;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.icu.text.TimeZoneFormat;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.net.InetSocketAddress;
import java.util.ArrayList;

import io.github.controlwear.virtual.joystick.android.JoystickView;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class GamePlayActivity extends AppCompatActivity {
    private Context gameActivityContext; // this allow non-activity class to access resources
    private final int GAME_BOARD_SIZE = 70;
    private Handler mainHandler = new Handler();
    private volatile String playerDirection = "up";
    private volatile int speed = 1; // speed of car.

    long playWithBotsStartTime = System.currentTimeMillis();


    SharedPreferences gameRoomInfo;
    String gameRoomID;
    String gameRoomIP;
    int gameRoomPort;
    volatile boolean gameOver = false;

    MovePlayerTest movePlayer;
    LocalGameServer localGameServer;

    /*
        Int values in board:
            0: Nothing in the board.
            1: Wall
            11: Player1's first half of head
            12: Player1's second half of head
            13: Player1's vertical tail
            14: Player1's horizontal tail.
            15: Player1's top_left_corner tail
            16: Player1's top_right_corner tail
            17: Player1's bottom_left_corner tail
            18: Player1's bottom_right_corner tail
            21 - 28: Player2's info
            31 - 38: Player3's info
            41 - 48: Player4's info
     */


    private int firstHalfOfHeadViewID = 11414;
    private int positionSize; // the pixel size of each position in the gameBoard.

    // Get screen dimensions in dp
    DisplayMetrics displayMetrics;
    float pxHeight;
    float dpHeight;


    boolean uiGameBoardCreated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameActivityContext = getApplicationContext();

        System.out.println("Oncreate called");


        // Hide Title Bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();

        setContentView(R.layout.activity_main);

        //Hide Navigation Bar
        ConstraintLayout mainLayout = (ConstraintLayout) findViewById(R.id.main_layout);
        mainLayout.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        //Initialize the Game UI Board
        //createUIGameBoard();

        gameRoomInfo = getSharedPreferences("gameRoomInfo", Context.MODE_PRIVATE);
        gameRoomID = gameRoomInfo.getString("gameRoomID", "");
        gameRoomIP = gameRoomInfo.getString("gameRoomIP", "");
        gameRoomPort = gameRoomInfo.getInt("gameRoomPort", 0);


        boolean playWithBot = gameRoomInfo.getBoolean("playWithBot", false);


        if (playWithBot) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // wait for previous socket to close


//                    try {
//                        Thread.sleep(3000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
                    for (int time = 3; time > 0; time--) {
                        Handler threadHandler = new Handler(Looper.getMainLooper());
                        int finalTime = time;
                        threadHandler.post(new Runnable() {
                            @Override
                            public void run() { //This thread need sometime to update the UI screen.
                                TextView startTimer = (TextView) findViewById(R.id.start_timer);

                                String text = String.valueOf(finalTime);
                                System.out.println("time: " + text);
                                startTimer.setText(text);
                            }
                        });

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    Handler threadHandler = new Handler(Looper.getMainLooper());
                    threadHandler.post(new Runnable() {
                        @Override
                        public void run() { //This thread need sometime to update the UI screen.
                            TextView startTimer = (TextView) findViewById(R.id.start_timer);
                            startTimer.setText("");
                        }
                    });

                    if (!gameOver) {
                        //Start game server on a background thread
                        localGameServer = new LocalGameServer();
                        localGameServer.initializeGameBoard();
                        localGameServer.run();

                    }
                }
            }).start();
        }





        //Start MovePlayer on background thread
        movePlayer = new MovePlayerTest(this, getApplicationContext(), playWithBot);
        movePlayer.run();



    }

    @Override
    public void onBackPressed() {
        long playWithBotsEndTime = System.currentTimeMillis();
        if (playWithBotsEndTime - playWithBotsStartTime > 4000) {
            if (movePlayer != null && localGameServer != null) {
                movePlayer.gameOver = true;
                localGameServer.gameOver = true;
            }
            gameOver = true;
            System.out.println("game over " + movePlayer.gameOver);
            finish();
        }
    }


    // Call this method every time the activity is created.
    // This will Programmatically creates Views to construct the UI game board.
//    public void createUIGameBoard() {
//        //Scale the board according to screen dp and pixel.
//        displayMetrics = getResources().getDisplayMetrics();
//        pxHeight = displayMetrics.heightPixels;
//        dpHeight = pxHeight / displayMetrics.density; //displayMetrics.density is how many pixels in a dp.
//
//
//        //positionSize = (int) (displayMetrics.density * (dpHeight / GAME_BOARD_SIZE));
//        positionSize = (int) pxHeight / GAME_BOARD_SIZE;
//
//        //System.out.println("positionSize" + positionSize);
//        for (int i = 0; i < GAME_BOARD_SIZE; i++) {
//            String row = String.valueOf(i);
//            if (i < 10) {
//                row = "0" + row;
//            }
//            /* Find Tablelayout defined in main.xml */
//            TableLayout tl = (TableLayout) findViewById(R.id.table_layout);
//            /* Create a new row to be added. */
//            TableRow tr = new TableRow(this);
//            tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
//            /* Create a Button to be the row-content. */
//
//            for (int k = 0; k < GAME_BOARD_SIZE; k++) {
//                ImageView b = new ImageView(this);
//                if (gameBoardData.gameBoard[i][k] == 1) {
//                    b.setImageResource(R.drawable.wall);
//                } else {
//                    b.setImageResource(R.drawable.block);
//                }
//
//                //b.setLayoutParams(new TableRow.LayoutParams(10, TableRow.LayoutParams.WRAP_CONTENT));
//                b.setLayoutParams(new TableRow.LayoutParams(positionSize, positionSize));
//
//                // ImageView id has this form: 1_row_column.
//                // Example 1: id = 10104 means we are at row 1 and column 4.
//                // Example 2: id = 12142 means we are at row 21 and column 42.
//                String column = String.valueOf(k);
//                if (k < 10) {
//                    column = "0" + column;
//                }
//                String stringId = "1" + row + column;
//                int id = Integer.parseInt(stringId);
//                b.setId(id);
//
//                /* Add ImageView to row. */
//                tr.addView(b);
//            }
//            tl.addView(tr, new TableLayout.LayoutParams(TableLayout.LayoutParams.FILL_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
//        }
//    }

//
//    public void deletePlayer(int playerCode) {
//
//        Handler threadHandler = new Handler(Looper.getMainLooper());
//        threadHandler.post(new Runnable() {
//            @Override
//            public void run() { //This thread need sometime to update the UI screen.
//                for (int row = 0; row < GAME_BOARD_SIZE; row++) {
//                    String stringRow = String.valueOf(row);
//                    if (row < 10) {
//                        stringRow = "0" + row;
//                    }
//                    for (int column = 0; column < GAME_BOARD_SIZE; column++) {
//                        if (gameBoardData.gameBoard[row][column] == playerCode) {
//                            String stringColumn = String.valueOf(column);
//                            if (column < 10) {
//                                stringColumn = "0" + column;
//                            }
//                            String stringId = "1" + stringRow + stringColumn;
//                            int id = Integer.parseInt(stringId);
//                            ImageView imageView = (ImageView) findViewById(id);
//                            imageView.setImageResource(R.drawable.block);
//                            imageView.setLayoutParams(new TableRow.LayoutParams(positionSize, positionSize));
//                            gameBoardData.gameBoard[row][column] = 1;
//                        }
//                    }
//                }
//            }
//        });
//    }

//    public void boostSpeed(View view) {
//        if (speed == 1) {
//            speed = 2;
//        } else {
//            speed = 1;
//        }
//    }

    class JoyStickThread implements Runnable {

        @Override
        public void run() {

            //Creating virtual joystick as the controller of the game.
            JoystickView joystick = (JoystickView) findViewById(R.id.joystickView);
            joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
                @Override
                public void onMove(int angle, int strength) {

                    // do whatever you want
                    if (strength >= 70) {
                        if (angle >= 45 && angle <= 135) //90 - 45 and 90 + 45
                        {
                            //move up
                            if (!playerDirection.equals("down")) {
                                playerDirection = "up";
                                //System.out.println("GraphicPlayer direction: " + playerDirection);
                            }

                        } else if (angle >= 225 && angle <= 315) // 270 - 45 and 270 + 45
                        {
                            //move down
                            if (!playerDirection.equals("up")) {
                                playerDirection = "down";
                                //System.out.println("GraphicPlayer direction: " + playerDirection);
                            }
                        } else if (angle > 135 && angle < 225) //180 - 45 and 180 + 45
                        {
                            //move left
                            if (!playerDirection.equals("right")) {
                                playerDirection = "left";
                                // System.out.println("GraphicPlayer direction: " + playerDirection);
                            }
                        } else if ((angle < 45 && angle >= 0) || (angle > 315)) //0 + 45 or 360 - 45
                        {
                            //move right
                            if (!playerDirection.equals("left")) {
                                playerDirection = "right";
                                //System.out.println("GraphicPlayer direction: " + playerDirection);
                            }
                        }

                    }
                }
            });

        }
    }

    class MovePlayer implements Runnable {
        GameBoard gameBoardData;
        Activity activity;
        Context context;

        //TODO: load players from room databas

        //GraphicPlayer player1 = new GraphicPlayer("player1", "up", firstHalfOfHeadViewID, gameActivityContext);
        ArrayList<GraphicPlayer> graphicPlayers = new ArrayList<GraphicPlayer>();
        JSONObject myPlayerMove;
        JSONObject allPlayersMoves;
        final int HEADERSIZE = 10;
        Socket socket;
        DataOutputStream sendMessageToServer;
        boolean localGame = true;


        byte[] message = new byte[100];

        boolean youLose = false; //indicate whether the player lose or not.

        MovePlayer(Activity activity, Context context) {
            myPlayerMove = new JSONObject();
            allPlayersMoves = new JSONObject();
            this.activity = activity;
            this.context = context;
        }


        public void displayYouLoseToast() {
            Handler threadHandler = new Handler(Looper.getMainLooper());
            threadHandler.post(new Runnable() {
                @Override
                public void run() { //This thread need sometime to update the UI screen.
                    Toast.makeText(context, "You lose",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        public void displayToast(String toastMessage) {
            Handler threadHandler = new Handler(Looper.getMainLooper());
            threadHandler.post(new Runnable() {
                @Override
                public void run() { //This thread need sometime to update the UI screen.
                    Toast.makeText(context, toastMessage,
                            Toast.LENGTH_SHORT).show();
                }
            });
        }


        @Override
        public void run() {

            // load players from room database
//            AppDatabase db = Room.databaseBuilder(getApplicationContext(),
//                    AppDatabase.class, "player").build();
//
//            PlayerDAO playerDAO = db.playerDAO();
//
//            List<Player> playerList = playerDAO.getAll();
//            String myPlayerID = "";
//            int myPlayerIndex = 0;
//
//            // initialize the graphics and position for each player.
//            int i = 0;
//            for (Player player : playerList) {
//                if (!player.playerID.equals("localPlayer")) {
//                    String direction = "";
//                    int firstViewID = 0;
//                    switch (player.playerID) {
//                        case "player1":
//                            direction = "down";
//                            firstViewID = 12035;
//                            break;
//                        case "player2":
//                            direction = "up";
//                            firstViewID = 15036;
//                            break;
//                        case "player3":
//                            direction = "right";
//                            firstViewID = 13520;
//                            break;
//                        case "player4":
//                            direction = "left";
//                            firstViewID = 13650;
//                            break;
//                    }
//
//                    GraphicPlayer graphicPlayer = new GraphicPlayer(player.playerID, direction, firstViewID, gameActivityContext);
//                    graphicPlayers.add(graphicPlayer);
//
//                    if (player.isLocal) {
//                        myPlayerID = player.playerID;
//                        myPlayerIndex = i;
//                    }
//                    i++;
//                }
//            }


            //Socket s=new Socket("35.247.71.135",5000);


            gameBoardData = new GameBoard();
            gameBoardData.initializeGameBoard();
            gameBoardData.createUIGameBoard(context, activity);


//            boolean socketConnected = false;
//            // initialize socket
//            while (!socketConnected) {
//                try {
//                    socket = new Socket();
//                    socket.connect(new InetSocketAddress(gameRoomIP, gameRoomPort));
//                    socketConnected = true;
//                    displayYouLoseToast();
//                    System.out.println(gameRoomIP + gameRoomPort);
//                    socket.setTcpNoDelay(true);
//                    socket.setTcpNoDelay(true);
//                    sendMessageToServer = new DataOutputStream(socket.getOutputStream());
//
//                    //dis = new DataInputStream(socket.getInputStream());
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {

            }

            // initilize all players
            for (int i = 0; i < 4; i++) {
                String playerOrder = "player" + String.valueOf(i + 1);
                String direction = "";
                int firstViewID = 0;
                switch (playerOrder) {
                    case "player1":
                        direction = "down";
                        firstViewID = 12035;
                        break;
                    case "player2":
                        direction = "up";
                        firstViewID = 15036;
                        break;
                    case "player3":
                        direction = "right";
                        firstViewID = 13520;
                        break;
                    case "player4":
                        direction = "left";
                        firstViewID = 13650;
                        break;
                }
                GraphicPlayer graphicPlayer = new GraphicPlayer(playerOrder, direction, firstViewID, gameActivityContext);
                graphicPlayers.add(graphicPlayer);
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    boolean youLose1 = false;

                    while (!gameOver) {
                        if (youLose1) {
                            displayToast("You lose!");
                        }
                        youLose1 = updatePlayerMove(0, playerDirection, speed, 0, youLose1, false);
                    }
                    gameOver = true;
                    if (!youLose1) {
                        displayToast("You win!");
                    }
                }
            }).start();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    boolean youLose2 = false;
                    boolean noDraw = true;
                    String direction = graphicPlayers.get(2).playerDirection;
                    while (!youLose2 || gameOver) {
                        //Get the current row and column of the current position
                        int currentPosition = graphicPlayers.get(2).firstHalfOfHeadViewID;
                        String stringID = String.valueOf(currentPosition);
                        String stringColumn = stringID.substring(3, 5);
                        String stringRow = stringID.substring(1, 3);
                        int row = Integer.parseInt(stringRow);
                        int column = Integer.parseInt(stringColumn);

                        youLose2 = gameBoardData.playerLose(row, column, direction, currentPosition);
                        if ((direction.equals("up") || direction.equals("down")) && youLose2) {
                            direction = "left";
                            youLose2 = gameBoardData.playerLose(row, column, direction, currentPosition);
                            if (youLose2) {
                                direction = "right";
                            }
                            youLose2 = false;
                        } else if ((direction.equals("left") || direction.equals("right")) && youLose2) {
                            direction = "down";
                            youLose2 = gameBoardData.playerLose(row, column, direction, currentPosition);
                            if (youLose2) {
                                direction = "up";
                            }
                            youLose2 = false;
                        }
                        youLose2 = updatePlayerMove(2, direction, 1, 0, youLose2, false);
                    }
                    gameOver = true;
                }
            }).start();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    boolean youLose2 = false;
                    boolean noDraw = true;
                    String direction = graphicPlayers.get(1).playerDirection;
                    while (!youLose2 || gameOver) {
                        //Get the current row and column of the current position
                        int currentPosition = graphicPlayers.get(1).firstHalfOfHeadViewID;
                        String stringID = String.valueOf(currentPosition);
                        String stringColumn = stringID.substring(3, 5);
                        String stringRow = stringID.substring(1, 3);
                        int row = Integer.parseInt(stringRow);
                        int column = Integer.parseInt(stringColumn);

                        youLose2 = gameBoardData.playerLose(row, column, direction, currentPosition);
                        if ((direction.equals("up") || direction.equals("down")) && youLose2) {
                            direction = "left";
                            youLose2 = gameBoardData.playerLose(row, column, direction, currentPosition);
                            if (youLose2) {
                                direction = "right";
                            }
                            youLose2 = false;
                        } else if ((direction.equals("left") || direction.equals("right")) && youLose2) {
                            direction = "down";
                            youLose2 = gameBoardData.playerLose(row, column, direction, currentPosition);
                            if (youLose2) {
                                direction = "up";
                            }
                            youLose2 = false;
                        }
                        youLose2 = updatePlayerMove(1, direction, 1, 0, youLose2, false);
                    }
                    gameOver = true;
                }
            }).start();


//            String currentDirection;
//            long startTime = 0;
//            long endTime = 0;
//            int currentSpeed;
//            // initilize playerDirection
//            playerDirection = graphicPlayers.get(0).playerDirection;
//            while (!youLose) {
//                currentDirection = playerDirection;
//                currentSpeed = speed; //currentSpeed decrease to 1 when turning
////                //TODO: send your move to game server
//                try {
//                    String stringID = String.valueOf(graphicPlayers.get(myPlayerIndex).firstHalfOfHeadViewID);
//                    String stringColumn = stringID.substring(3, 5);
//                    String stringRow = stringID.substring(1, 3);
//                    int row = Integer.parseInt(stringRow);
//                    int column = Integer.parseInt(stringColumn);
//
//                    String position = "";
//                    switch (currentDirection) {
//                        case "up":
//                            switch (graphicPlayers.get(myPlayerIndex).previousPlayerDirection) {
//                                case "down":
//                                case "up":
//                                    row--;
//                                    position = "(" + row + "," + column + ")";
//                                    if (currentSpeed == 2) {
//                                        if (gameBoard[row][column] == 0)
//                                        {
//                                            row--;
//                                            position = position + "+(" + row + "," + column + ")";
//                                        }
//                                    }
//                                    break;
//                                case "left":
//                                case "right":
//                                    currentSpeed = 1;
//                                    row--;
//                                    position = "(" + row + "," + column + ")";
//                                    break;
//                            }
//                            break;
//                        case "down":
//                            switch (graphicPlayers.get(myPlayerIndex).previousPlayerDirection) {
//                                case "down":
//                                case "up":
//                                    row++;
//                                    position = "(" + row + "," + column + ")";
//                                    if (currentSpeed == 2) {
//                                        if (gameBoard[row][column] == 0) {
//                                            row++;
//                                            position = position + "+(" + row + "," + column + ")";
//                                        }
//                                    }
//
//                                    myPlayerMove.put("position", position);
//                                    break;
//                                case "left":
//                                case "right":
//                                    currentSpeed = 1;
//                                    row++;
//                                    position = "(" + row + "," + column + ")";
//                                    break;
//                            }
//                            break;
//
//                        case "left":
//                            switch (graphicPlayers.get(myPlayerIndex).previousPlayerDirection) {
//                                case "left":
//                                case "right":
//                                    column--;
//                                    position = "(" + row + "," + column + ")";
//                                    if (currentSpeed == 2) {
//                                        if (gameBoard[row][column] == 0) {
//                                            column--;
//                                            position = position + "+(" + row + "," + column + ")";
//                                        }
//                                    }
//                                    break;
//                                case "up":
//                                case "down":
//                                    currentSpeed = 1;
//                                    column--;
//                                    position = "(" + row + "," + column + ")";
//                                    break;
//
//                            }
//                            break;
//                        case "right":
//                            switch (graphicPlayers.get(myPlayerIndex).previousPlayerDirection) {
//                                case "left":
//                                case "right":
//                                    column++;
//                                    position = "(" + row + "," + column + ")";
//                                    if (currentSpeed == 2) {
//                                        if (gameBoard[row][column] == 0) {
//                                            column++;
//                                            position = position + "+(" + row + "," + column + ")";
//                                        }
//                                    }
//                                    break;
//                                case "up":
//                                case "down":
//                                    currentSpeed = 1;
//                                    column++;
//                                    position = "(" + row + "," + column + ")";
//                                    break;
//                            }
//                            break;
//                    }
//
//
//                    myPlayerMove.put("position", position);
//                    myPlayerMove.put("playerID", myPlayerID);
//                    myPlayerMove.put("speed", currentSpeed);
//                    myPlayerMove.put("direction", currentDirection);
////
////
//                    // Send move to server
//                    String myPlayerMoveString = myPlayerMove.toString();
//                    String headerFormat = "%-" + HEADERSIZE + "s";
//                    String header = String.format(headerFormat, myPlayerMoveString.length());
//                    myPlayerMoveString = header + myPlayerMoveString;
//                    //System.out.println("playerDirection " + playerDirection);
//                    sendMessageToServer.write(myPlayerMoveString.getBytes());
//                    sendMessageToServer.flush();
//
//
//                    //TODO: receive move from all graphicPlayers to game server
//                    startTime = System.currentTimeMillis();
//                    String message = receiveServerMessage();
//                    //receiveServerMessage();
////                    int receiveMessageSize = Integer.parseInt(new String(message, StandardCharsets.UTF_8).trim());
////                    message = new byte[receiveMessageSize];
////                    dis.read(message);
////                    System.out.println(message.toString());
//                    endTime = System.currentTimeMillis();
//                    //System.out.println("receive time " + (endTime - startTime));
//
//                } catch (JSONException | IOException e) {
//                    e.printStackTrace();
//                }
//
//
//
//                //TODO: when speed is 2, we move 2 cells. If the 2nd cell is an obstacle, set the speed to 1 to display the car properly.
//                int latencyTime = (int) (endTime - startTime);
//                if (youLose) {
//                    if (currentSpeed == 2) {
//                        currentSpeed = 1;
//                        updatePlayerMove(myPlayerIndex, currentDirection, currentSpeed, latencyTime);
//                    }
//                    break;
//                }
//                updatePlayerMove(myPlayerIndex, currentDirection, currentSpeed, latencyTime);
//
//            } // end while
//            displayYouLoseToast();
        }

//        public boolean playerLoseView(int currentPosition, String direction)
//        {
//
//        }


        public boolean updatePlayerMove(int playerNumber, String direction, int currentSpeed, int latencyTime, boolean youLose, boolean noDraw) {
            long startTime = System.currentTimeMillis();
            int playerCode = (playerNumber + 1) * 10;

            graphicPlayers.get(playerNumber).previousTurnDirection = graphicPlayers.get(playerNumber).turnDirection; // This help us know how our car turned so we know to update the UI appropriately (i.e. taking care of corners of tail).
            graphicPlayers.get(playerNumber).secondHalfOfHalfViewID = graphicPlayers.get(playerNumber).previousFirstHalfOfHeadViewID;

            //Get the current row and column of firstHalfOfHeadViewID
            String stringID = String.valueOf(graphicPlayers.get(playerNumber).firstHalfOfHeadViewID);
            String stringColumn = stringID.substring(3, 5);
            String stringRow = stringID.substring(1, 3);
            int row = Integer.parseInt(stringRow);
            int column = Integer.parseInt(stringColumn);


            gameBoardData.performOperationOnGameBoard(row, column, playerCode);
            //gameBoard[row][column] = playerCode;

            // Update the UI appropriately according to the playerDirection.
            //graphicPlayers.get(playerNumber).playerDirection = playerDirection;
            graphicPlayers.get(playerNumber).playerDirection = direction;
            switch (graphicPlayers.get(playerNumber).playerDirection) {
                case "up":
                    if (row > currentSpeed - 1) {
                        //when we turn, we want to make the speed equal to 1
                        int tempSpeed = currentSpeed;
                        if (graphicPlayers.get(playerNumber).secondHalfOfHeadDrawable == graphicPlayers.get(playerNumber).headLeft2) { //We are turning
                            graphicPlayers.get(playerNumber).turnDirection = "leftToUp";
                            graphicPlayers.get(playerNumber).lastTailDrawable = graphicPlayers.get(playerNumber).horizontalTail;
                            tempSpeed = 1;
                        } else if (graphicPlayers.get(playerNumber).secondHalfOfHeadDrawable == graphicPlayers.get(playerNumber).headRight2) { //We are turning
                            graphicPlayers.get(playerNumber).turnDirection = "rightToUp";
                            graphicPlayers.get(playerNumber).lastTailDrawable = graphicPlayers.get(playerNumber).horizontalTail;
                            tempSpeed = 1;
                        } else { // GraphicPlayer not turning
                            graphicPlayers.get(playerNumber).lastTailDrawable = graphicPlayers.get(playerNumber).verticalTail;
                            graphicPlayers.get(playerNumber).turnDirection = "";
                        }
                        graphicPlayers.get(playerNumber).firstHalfOfHeadDrawable = graphicPlayers.get(playerNumber).headUp1;
                        graphicPlayers.get(playerNumber).secondHalfOfHeadDrawable = graphicPlayers.get(playerNumber).headUp2;
                        row -= tempSpeed;
                        //secondHalfOfViewID will increase/decrease an additional unit if the speed is 2.
                        if (tempSpeed == 2) {

                            // when the speed is 2, the player's head skips a cell in gameBoard.
                            // If there is something in that skipped cell, then the play dies.
                            // This prevents the player from running through walls and other graphicPlayers without dying.
                            if (gameBoardData.performOperationOnGameBoard(row + 1, column, -9999) == 0) {
                                gameBoardData.performOperationOnGameBoard(row + 1, column, 10);
                                //gameBoard[row + 1][column] = 10;
                            } else {
                                youLose = true;
                            }
                            graphicPlayers.get(playerNumber).secondHalfOfHalfViewID -= 100;
                        }
                        //speed = tempSpeed;
                    }
                    break;
                case "down":
                    if (row < GAME_BOARD_SIZE - currentSpeed) {
                        int tempSpeed = currentSpeed;
                        if (graphicPlayers.get(playerNumber).secondHalfOfHeadDrawable == graphicPlayers.get(playerNumber).headLeft2) {

                            graphicPlayers.get(playerNumber).turnDirection = "leftToDown";
                            graphicPlayers.get(playerNumber).lastTailDrawable = graphicPlayers.get(playerNumber).horizontalTail;
                            tempSpeed = 1;

                        } else if (graphicPlayers.get(playerNumber).secondHalfOfHeadDrawable == graphicPlayers.get(playerNumber).headRight2) {
                            graphicPlayers.get(playerNumber).turnDirection = "rightToDown";
                            graphicPlayers.get(playerNumber).lastTailDrawable = graphicPlayers.get(playerNumber).horizontalTail;
                            tempSpeed = 1;
                        } else { // GraphicPlayer not turning
                            graphicPlayers.get(playerNumber).lastTailDrawable = graphicPlayers.get(playerNumber).verticalTail;
                            graphicPlayers.get(playerNumber).turnDirection = "";
                        }
                        graphicPlayers.get(playerNumber).firstHalfOfHeadDrawable = graphicPlayers.get(playerNumber).headDown1;
                        graphicPlayers.get(playerNumber).secondHalfOfHeadDrawable = graphicPlayers.get(playerNumber).headDown2;

                        row += tempSpeed;

                        //secondHalfOfViewID will increase/decrease an additional unit if the speed is 2.
                        if (tempSpeed == 2) {
                            if (gameBoardData.performOperationOnGameBoard(row - 1, column, -9999) == 0) {
                                //gameBoard[row - 1][column] = 10;
                                gameBoardData.performOperationOnGameBoard(row - 1, column, 10);
                            } else {
                                youLose = true;
                            }

                            graphicPlayers.get(playerNumber).secondHalfOfHalfViewID += 100;
                        }
                        //speed = tempSpeed;
                    }
                    break;
                case "left":
                    if (column > (currentSpeed - 1)) {
                        int tempSpeed = currentSpeed;
                        if (graphicPlayers.get(playerNumber).secondHalfOfHeadDrawable == graphicPlayers.get(playerNumber).headUp2) {
                            graphicPlayers.get(playerNumber).turnDirection = "upToLeft";
                            graphicPlayers.get(playerNumber).lastTailDrawable = graphicPlayers.get(playerNumber).verticalTail;
                            tempSpeed = 1;

                        } else if (graphicPlayers.get(playerNumber).secondHalfOfHeadDrawable == graphicPlayers.get(playerNumber).headDown2) {
                            graphicPlayers.get(playerNumber).turnDirection = "downToLeft";
                            graphicPlayers.get(playerNumber).lastTailDrawable = graphicPlayers.get(playerNumber).verticalTail;
                            tempSpeed = 1;
                        } else { // GraphicPlayer not turning
                            graphicPlayers.get(playerNumber).lastTailDrawable = graphicPlayers.get(playerNumber).horizontalTail;
                            graphicPlayers.get(playerNumber).turnDirection = "";
                        }
                        graphicPlayers.get(playerNumber).firstHalfOfHeadDrawable = graphicPlayers.get(playerNumber).headLeft1;
                        graphicPlayers.get(playerNumber).secondHalfOfHeadDrawable = graphicPlayers.get(playerNumber).headLeft2;
                        column -= tempSpeed;

                        //secondHalfOfViewID will increase/decrease an additional unit if the speed is 2.
                        if (tempSpeed == 2) {
                            if (gameBoardData.performOperationOnGameBoard(row, column + 1, -9999) == 0) {
                                //gameBoard[row][column + 1] = 10;
                                gameBoardData.performOperationOnGameBoard(row, column + 1, 10);
                            } else {
                                youLose = true;
                            }

                            graphicPlayers.get(playerNumber).secondHalfOfHalfViewID -= 1;
                        }
                        //speed = tempSpeed;
                    }
                    break;

                case "right":
                    if (column < GAME_BOARD_SIZE - currentSpeed) {
                        int tempSpeed = currentSpeed;
                        if (graphicPlayers.get(playerNumber).secondHalfOfHeadDrawable == graphicPlayers.get(playerNumber).headUp2) {
                            graphicPlayers.get(playerNumber).turnDirection = "upToRight";
                            graphicPlayers.get(playerNumber).lastTailDrawable = graphicPlayers.get(playerNumber).verticalTail;
                            tempSpeed = 1;

                        } else if (graphicPlayers.get(playerNumber).secondHalfOfHeadDrawable == graphicPlayers.get(playerNumber).headDown2) {
                            graphicPlayers.get(playerNumber).turnDirection = "downToRight";
                            graphicPlayers.get(playerNumber).lastTailDrawable = graphicPlayers.get(playerNumber).verticalTail;
                            tempSpeed = 1;
                        } else { // GraphicPlayer not turning
                            graphicPlayers.get(playerNumber).lastTailDrawable = graphicPlayers.get(playerNumber).horizontalTail;
                            graphicPlayers.get(playerNumber).turnDirection = "";
                        }
                        graphicPlayers.get(playerNumber).firstHalfOfHeadDrawable = graphicPlayers.get(playerNumber).headRight1;
                        graphicPlayers.get(playerNumber).secondHalfOfHeadDrawable = graphicPlayers.get(playerNumber).headRight2;

                        column += tempSpeed;

                        //secondHalfOfViewID will increase/decrease an additional unit if the speed is 2.
                        if (tempSpeed == 2) {
                            if (gameBoardData.performOperationOnGameBoard(row, column - 1, -9999) == 0) {
                                //gameBoard[row][column - 1] = 10;
                                gameBoardData.performOperationOnGameBoard(row, column - 1, 10);
                            } else {
                                youLose = true;
                            }
                            graphicPlayers.get(playerNumber).secondHalfOfHalfViewID += 1;
                        }
                        //speed = tempSpeed;
                    }
                    break;
            }

            // Check to see if we need to change the tail to one of the tail corners.
            if (graphicPlayers.get(playerNumber).lastTailDrawable == graphicPlayers.get(playerNumber).horizontalTail || graphicPlayers.get(playerNumber).lastTailDrawable == graphicPlayers.get(playerNumber).verticalTail) {
                switch (graphicPlayers.get(playerNumber).previousTurnDirection) {
                    case "leftToUp":
                    case "downToRight":
                        graphicPlayers.get(playerNumber).lastTailDrawable = graphicPlayers.get(playerNumber).bottomLeftCornerTail;
                        //System.out.println(previousTurnDirection);
                        break;
                    case "leftToDown":
                    case "upToRight":
                        graphicPlayers.get(playerNumber).lastTailDrawable = graphicPlayers.get(playerNumber).topLeftCornerTail;
                        //System.out.println(previousTurnDirection);
                        break;
                    case "rightToUp":
                    case "downToLeft":
                        graphicPlayers.get(playerNumber).lastTailDrawable = graphicPlayers.get(playerNumber).bottomRightCornerTail;
                        //System.out.println(previousTurnDirection);
                        break;
                    case "rightToDown":
                    case "upToLeft":
                        graphicPlayers.get(playerNumber).lastTailDrawable = graphicPlayers.get(playerNumber).topRightCornerTail;
                        //System.out.println(previousTurnDirection);
                        break;
                }
            }

            // Check if the player lose or not

            if (youLose || gameBoardData.performOperationOnGameBoard(row, column, -9999) != 0 || row >= GAME_BOARD_SIZE || column >= GAME_BOARD_SIZE || row < 0 || column < 0) {
                //System.out.println(youLose);
                youLose = true;

                if (playerNumber == 0) {
                    displayYouLoseToast();
                    //deletePlayer(playerCode);
                } else {
                    if (!noDraw) {
                        // deletePlayer(playerCode);
                    }

                }
                return youLose;
            }

            //gameBoard[row][column] = 10;

            if (!noDraw) {
                // Make sure view_id is of the form 1_00_00.
                if (column < 10) {
                    stringColumn = "0" + String.valueOf(column);
                } else {
                    stringColumn = String.valueOf(column);
                }
                if (row < 10) {
                    stringRow = "0" + String.valueOf(row);
                } else {
                    stringRow = String.valueOf(row);
                }
                stringID = "1" + stringRow + stringColumn;
                graphicPlayers.get(playerNumber).firstHalfOfHeadViewID = Integer.parseInt(stringID);


                // Prepare views to update animation on UI.
                ImageView imageView = (ImageView) findViewById(graphicPlayers.get(playerNumber).firstHalfOfHeadViewID);
                ImageView imageView2 = (ImageView) findViewById(graphicPlayers.get(playerNumber).secondHalfOfHalfViewID);
                ImageView imageView3 = (ImageView) findViewById(graphicPlayers.get(playerNumber).lastTailViewID);
                ImageView imageView4 = (ImageView) findViewById(graphicPlayers.get(playerNumber).previousFirstHalfOfHeadViewID);


//                handlerRunning = true;
                //Communicate with UI thread to change image resource
                Handler threadHandler = new Handler(Looper.getMainLooper());
                threadHandler.post(new Runnable() {
                    @Override
                    public void run() { //This thread need sometime to update the UI screen.
                        // Change image for ImageView
                        imageView.setImageDrawable(graphicPlayers.get(playerNumber).firstHalfOfHeadDrawable);
                        imageView2.setImageDrawable(graphicPlayers.get(playerNumber).secondHalfOfHeadDrawable);
                        imageView3.setImageDrawable(graphicPlayers.get(playerNumber).lastTailDrawable);

                        // This take care of when the speed is 2.
                        // When the speed is 2, everything jump two cells.
                        // This means we need to update the cell that has been jumped over.
                        if (graphicPlayers.get(playerNumber).secondHalfOfHalfViewID != graphicPlayers.get(playerNumber).previousFirstHalfOfHeadViewID) {
                            if (graphicPlayers.get(playerNumber).secondHalfOfHeadDrawable == graphicPlayers.get(playerNumber).headLeft2 || graphicPlayers.get(playerNumber).secondHalfOfHeadDrawable == graphicPlayers.get(playerNumber).headRight2) {
                                imageView4.setImageDrawable(graphicPlayers.get(playerNumber).horizontalTail);
                            } else {
                                imageView4.setImageDrawable(graphicPlayers.get(playerNumber).verticalTail);
                            }
                        }
//                        handlerRunning = false;
                    }
                });

                long endTime = System.currentTimeMillis();
                latencyTime += (endTime - startTime);
                try {
                    if (latencyTime < 100) {
                        Thread.sleep(100 - latencyTime);
                    }

                } catch (InterruptedException e) {

                }
                graphicPlayers.get(playerNumber).lastTailViewID = graphicPlayers.get(playerNumber).secondHalfOfHalfViewID; //We want to know where the second half of the head was so we can update the tail appropriately next time.
                graphicPlayers.get(playerNumber).previousFirstHalfOfHeadViewID = graphicPlayers.get(playerNumber).firstHalfOfHeadViewID; // We need this to take care of when the speed is 2.
                graphicPlayers.get(playerNumber).previousPlayerDirection = graphicPlayers.get(playerNumber).playerDirection;

            }

            return youLose;
        }

        public String receiveServerMessage() throws IOException {
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
    }

    //TODO: On Friday, create a virtual joystick. Create images for the heads. Create images for the tails.

    //TODO: On Saturday, handle multitouch events. Add a "Boost" button that allow player to move two blocks.

    //TODO: On Sunday, create GraphicPlayer class. Move a player automatically, and control that player's direction with the joystick.

    //TODO: On Monday, add multiple graphicPlayers and allow them to move automatically. Figure out the int values (i.e. player's heads and tails) for each player in the board[][]

    //TODO: On Tuesday, create socket and connect to the server using background thread. Figure out how to handle communication.

    //TODO: On Wednesday, make "Create Game"/"Join Game" Activity that interact with the server.

    //TODO: On Thursday, create "Lobby" activity with "Start" and "Leave" buttons. Receive game information from server (number of graphicPlayers, who is the host, graphicPlayers' information).

    //TODO: On Saturday, create Room Database to store game information received from server (i.e. player's name and player's position).

    //TODO: On Sunday, handle game communication between server and clients in the same room. Synchronize start game of every player in the same room.

    //TODO: On Monday, finalize the project. Check for all requirements Dr. Blum wants.

    //TODO: On Tuesday, write the report and prepare presentation.

    //TODO: On Wednesday, submit project and continue to prepare for presentation.
}

