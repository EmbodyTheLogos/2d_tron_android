package com.example.a2d_tron_game;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;

import java.util.ArrayList;

import io.github.controlwear.virtual.joystick.android.JoystickView;

public class GamePlayActivity extends AppCompatActivity {


    private Context gameActivityContext; // this allow non-activity class to access resources
    private final int GAME_BOARD_SIZE = 70;
    private int gameBoard[][] = new int[GAME_BOARD_SIZE][GAME_BOARD_SIZE];
    private Handler mainHandler = new Handler();
    private volatile String playerDirection = "up";
    private volatile String playerPreviousDirection = playerDirection;
    private volatile int speed = 1; // speed of car.

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

    private boolean gameBoardInitialized = false;
    private int firstHalfOfHeadViewID = 11414;
    private int positionSize; // the pixel size of each position in the gameBoard.

    // Get screen dimensions in dp
    DisplayMetrics displayMetrics;
    float pxHeight;
    float dpHeight;


    boolean uiGameBoardCreated = false;


    // This allow multitouch function
//    ImageView boostSpeedButton;
//    int[] boostSpeedButtonX = new int[2];
//    int[] boostSpeedButtonY = new int[2];
//    boolean boostSpeedLocationAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameActivityContext = getApplicationContext();
        if (!gameBoardInitialized) {
            initializeGameBoard();
        }
        System.out.println("Oncreate called");
        for (int i = 0; i < gameBoard.length; i++) {
            for (int k = 0; k < gameBoard.length; k++) {
                System.out.print(gameBoard[i][k] + " ");
            }
            System.out.println();
        }
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
        createUIGameBoard();
        JoyStickThread joyStickThread = new JoyStickThread();
        new Thread(joyStickThread).start();

        MovePlayer movePlayer = new MovePlayer();
        new Thread(movePlayer).start();


//        mainLayout.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//
//                if (boostSpeedLocationAvailable) {
//                    int eventType = motionEvent.getActionMasked();
//                    if (eventType == MotionEvent.ACTION_DOWN) {
//                        System.out.println(motionEvent.getPointerCount());
//
//                        int touchX = (int) motionEvent.getX();
//                        int touchY = (int) motionEvent.getY();
//                        if (touchX >= boostSpeedButtonX[0] && touchX <= boostSpeedButtonX[1] && touchY >= boostSpeedButtonY[0] && touchY <= boostSpeedButtonY[1]) {
//                            boostSpeed();
//                            System.out.println("Speed " + speed);
//                            System.out.println("Speed boosted");
//                        }
//                        System.out.println("boostSpeedButtonX[0] " + boostSpeedButtonX[0]);
//                        System.out.println("boostSpeedButtonX[1] " + boostSpeedButtonX[1]);
//
//                        System.out.println("boostSpeedButtonY[0] " + boostSpeedButtonY[0]);
//                        System.out.println("boostSpeedButtonY[1] " + boostSpeedButtonY[1]);
//
//                        System.out.println("touchx " + touchX);
//                        System.out.println("touchY " + touchY);
//                    }
//
//                } else {
//                    setSpeedButtonLocation();
//                    boostSpeedLocationAvailable = true;
//                }
//
//                return true;
//            }
//        });
    }

//    public void setSpeedButtonLocation() {
//        // Find the position of the "button" after the UI Game Board is created
//        boostSpeedButton = (ImageView) findViewById(R.id.boost_speed);
//        int[] location = new int[2];
//        boostSpeedButton.getLocationOnScreen(location);
//        for (int i = 0; i < location.length; i++) {
//            System.out.println("Locations " + location[i]);
//        }
//        boostSpeedButtonX = new int[]{location[0], (int) (boostSpeedButton.getLeft() + (100 * displayMetrics.density))};
//        boostSpeedButtonY = new int[]{location[1], (int) (boostSpeedButton.getTop() + (200 * displayMetrics.density))};
//    }

    //Go through the board and set the int values associate with the player we want to delete to 0.
    //After each update, we then update the ImageView that associates with the position of the updated value.
//    public void deletePlayer(String player) {
//
//    }
//
//    public void movePlayers() {
//
//    }

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
        this.gameBoardInitialized = true;
    }

    //    //This will make sure the game board persists throughout the entire game.
//    //This method traverse through the board[][] array and update the view accordingly.
//    public void restoreBoard()
//    {
//
//    }
//
//    // Call this method every time the activity is created.
//    // This will Programmatically creates Views to construct the UI game board.
    public void createUIGameBoard() {
        //Scale the board according to screen dp and pixel.
        displayMetrics = getResources().getDisplayMetrics();
        pxHeight = displayMetrics.heightPixels;
        dpHeight = pxHeight / displayMetrics.density; //displayMetrics.density is how many pixels in a dp.


        //positionSize = (int) (displayMetrics.density * (dpHeight / GAME_BOARD_SIZE));
        positionSize = (int) pxHeight / GAME_BOARD_SIZE;

        //System.out.println("positionSize" + positionSize);
        for (int i = 0; i < GAME_BOARD_SIZE; i++) {
            String row = String.valueOf(i);
            if (i < 10) {
                row = "0" + row;
            }
            /* Find Tablelayout defined in main.xml */
            TableLayout tl = (TableLayout) findViewById(R.id.table_layout);
            /* Create a new row to be added. */
            TableRow tr = new TableRow(this);
            tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
            /* Create a Button to be the row-content. */

            for (int k = 0; k < GAME_BOARD_SIZE; k++) {
                ImageView b = new ImageView(this);
                if (gameBoard[i][k] == 1) {
                    b.setImageResource(R.drawable.player2);
                } else {
                    b.setImageResource(R.drawable.block);
                }

                //b.setLayoutParams(new TableRow.LayoutParams(10, TableRow.LayoutParams.WRAP_CONTENT));
                b.setLayoutParams(new TableRow.LayoutParams(positionSize, positionSize));

                // ImageView id has this form: 1_row_column.
                // Example 1: id = 10104 means we are at row 1 and column 4.
                // Example 2: id = 12142 means we are at row 21 and column 42.
                String column = String.valueOf(k);
                if (k < 10) {
                    column = "0" + column;
                }
                String stringId = "1" + row + column;
                int id = Integer.parseInt(stringId);
                b.setId(id);

                /* Add ImageView to row. */
                tr.addView(b);
            }
            tl.addView(tr, new TableLayout.LayoutParams(TableLayout.LayoutParams.FILL_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
        }
    }


    public void deletePlayer() {
        for (int row = 0; row < GAME_BOARD_SIZE; row++) {
            String stringRow = String.valueOf(row);
            if (row < 10) {
                stringRow = "0" + row;
            }
            for (int column = 0; column < GAME_BOARD_SIZE; column++) {
                if (gameBoard[row][column] == 10) {
                    String stringColumn = String.valueOf(column);
                    if (column < 10) {
                        stringColumn = "0" + column;
                    }
                    String stringId = "1" + stringRow + stringColumn;
                    int id = Integer.parseInt(stringId);
                    ImageView imageView = (ImageView) findViewById(id);
                    imageView.setImageResource(R.drawable.block);
                    imageView.setLayoutParams(new TableRow.LayoutParams(positionSize, positionSize));
                    gameBoard[row][column] = 1;
                }
            }
        }

    }

    public void boostSpeed(View view) {
        if (speed == 1) {
            speed = 2;
        } else {
            speed = 1;
        }
        System.out.println("Speed in boostSpeed " + speed);
    }

    class JoyStickThread implements Runnable {

        @Override
        public void run() {
            //Creating virtual joystick as the controller of the game.
            JoystickView joystick = (JoystickView) findViewById(R.id.joystickView);
            joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
                @Override
                public void onMove(int angle, int strength) {
                    playerPreviousDirection = playerDirection;
                    // do whatever you want
                    if (strength >= 70) {
                        if (angle >= 45 && angle <= 135) //90 - 45 and 90 + 45
                        {
                            //move up
                            if (!playerDirection.equals("down")) {
                                playerDirection = "up";
                                //System.out.println("Player direction: " + playerDirection);
                            }

                        } else if (angle >= 225 && angle <= 315) // 270 - 45 and 270 + 45
                        {
                            //move down
                            if (!playerDirection.equals("up")) {
                                playerDirection = "down";
                                //System.out.println("Player direction: " + playerDirection);
                            }
                        } else if (angle > 135 && angle < 225) //180 - 45 and 180 + 45
                        {
                            //move left
                            if (!playerDirection.equals("right")) {
                                playerDirection = "left";
                                // System.out.println("Player direction: " + playerDirection);
                            }
                        } else if ((angle < 45 && angle >= 0) || (angle > 315)) //0 + 45 or 360 - 45
                        {
                            //move right
                            if (!playerDirection.equals("left")) {
                                playerDirection = "right";
                                //System.out.println("Player direction: " + playerDirection);
                            }
                        }

                    }
                }
            });

        }
    }

    class MovePlayer implements Runnable {


        //Player player1 = new Player("player1", "up", firstHalfOfHeadViewID, gameActivityContext);
        ArrayList<Player> players = new ArrayList<Player>();
        boolean youLose = false; //indicate whether the player lose or not.

        MovePlayer()
        {
            players.add(new Player("player1", "up", firstHalfOfHeadViewID, gameActivityContext));
            System.out.println("Move player initilized");
        }

        public void displayYouLoseToast() {
            Handler threadHandler = new Handler(Looper.getMainLooper());
            threadHandler.post(new Runnable() {
                @Override
                public void run() { //This thread need sometime to update the UI screen.
                    deletePlayer();
                    Toast.makeText(getApplicationContext(), "You lose",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }


        @Override
        public void run() {

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {

            }

            // This is used for when the speed is 2 (i.e. when the player skip a cell as they move, and there is obstacle in that cell, then youLose will set to true).

            while (!youLose) {
                updatePlayerMove(0, playerDirection);
            } // end while
        }


        public void updatePlayerMove(int playerNumber, String direction) {
            players.get(playerNumber).previousTurnDirection = players.get(playerNumber).turnDirection; // This help us know how our car turned so we know to update the UI appropriately (i.e. taking care of corners of tail).
            players.get(playerNumber).secondHalfOfHalfViewID = players.get(playerNumber).previousFirstHalfOfHeadViewID;

            //Get the current row and column of firstHalfOfHeadViewID
            String stringID = String.valueOf(players.get(playerNumber).firstHalfOfHeadViewID);
            String stringColumn = stringID.substring(3, 5);
            String stringRow = stringID.substring(1, 3);
            int row = Integer.parseInt(stringRow);
            int column = Integer.parseInt(stringColumn);

            gameBoard[row][column] = 10;

            // Update the UI appropriately according to the playerDirection.
            //players.get(playerNumber).playerDirection = playerDirection;
            players.get(playerNumber).playerDirection = direction;
            switch (players.get(playerNumber).playerDirection) {
                case "up":
                    if (row > speed - 1) {
                        //when we turn, we want to make the speed equal to 1
                        int tempSpeed = speed;
                        if (players.get(playerNumber).secondHalfOfHeadDrawable == players.get(playerNumber).headLeft2) { //We are turning
                            players.get(playerNumber).turnDirection = "leftToUp";
                            players.get(playerNumber).lastTailDrawable = players.get(playerNumber).horizontalTail;
                            tempSpeed = 1;
                        } else if (players.get(playerNumber).secondHalfOfHeadDrawable == players.get(playerNumber).headRight2) { //We are turning
                            players.get(playerNumber).turnDirection = "rightToUp";
                            players.get(playerNumber).lastTailDrawable = players.get(playerNumber).horizontalTail;
                            tempSpeed = 1;
                        } else { // Player not turning
                            players.get(playerNumber).lastTailDrawable = players.get(playerNumber).verticalTail;
                            players.get(playerNumber).turnDirection = "";
                        }
                        players.get(playerNumber).firstHalfOfHeadDrawable = players.get(playerNumber).headUp1;
                        players.get(playerNumber).secondHalfOfHeadDrawable = players.get(playerNumber).headUp2;
                        row -= tempSpeed;
                        //secondHalfOfViewID will increase/decrease an additional unit if the speed is 2.
                        if (tempSpeed == 2) {

                            // when the speed is 2, the player's head skips a cell in gameBoard.
                            // If there is something in that skipped cell, then the play dies.
                            // This prevents the player from running through walls and other players without dying.
                            if (gameBoard[row + 1][column] == 0) {
                                gameBoard[row + 1][column] = 10;
                            } else {
                                youLose = true;
                            }
                            players.get(playerNumber).secondHalfOfHalfViewID -= 100;
                        }
                        //speed = tempSpeed;
                    }
                    break;
                case "down":
                    if (row < GAME_BOARD_SIZE - speed) {
                        int tempSpeed = speed;
                        if (players.get(playerNumber).secondHalfOfHeadDrawable == players.get(playerNumber).headLeft2) {

                            players.get(playerNumber).turnDirection = "leftToDown";
                            players.get(playerNumber).lastTailDrawable = players.get(playerNumber).horizontalTail;
                            tempSpeed = 1;

                        } else if (players.get(playerNumber).secondHalfOfHeadDrawable == players.get(playerNumber).headRight2) {
                            players.get(playerNumber).turnDirection = "rightToDown";
                            players.get(playerNumber).lastTailDrawable = players.get(playerNumber).horizontalTail;
                            tempSpeed = 1;
                        } else { // Player not turning
                            players.get(playerNumber).lastTailDrawable = players.get(playerNumber).verticalTail;
                            players.get(playerNumber).turnDirection = "";
                        }
                        players.get(playerNumber).firstHalfOfHeadDrawable = players.get(playerNumber).headDown1;
                        players.get(playerNumber).secondHalfOfHeadDrawable = players.get(playerNumber).headDown2;

                        row += tempSpeed;

                        //secondHalfOfViewID will increase/decrease an additional unit if the speed is 2.
                        if (tempSpeed == 2) {
                            if (gameBoard[row - 1][column] == 0) {
                                gameBoard[row - 1][column] = 10;
                            } else {
                                youLose = true;
                            }

                            players.get(playerNumber).secondHalfOfHalfViewID += 100;
                        }
                        //speed = tempSpeed;
                    }
                    break;
                case "left":
                    if (column > (speed - 1)) {
                        int tempSpeed = speed;
                        if (players.get(playerNumber).secondHalfOfHeadDrawable == players.get(playerNumber).headUp2) {
                            players.get(playerNumber).turnDirection = "upToLeft";
                            players.get(playerNumber).lastTailDrawable = players.get(playerNumber).verticalTail;
                            tempSpeed = 1;

                        } else if (players.get(playerNumber).secondHalfOfHeadDrawable == players.get(playerNumber).headDown2) {
                            players.get(playerNumber).turnDirection = "downToLeft";
                            players.get(playerNumber).lastTailDrawable = players.get(playerNumber).verticalTail;
                            tempSpeed = 1;
                        } else { // Player not turning
                            players.get(playerNumber).lastTailDrawable = players.get(playerNumber).horizontalTail;
                            players.get(playerNumber).turnDirection = "";
                        }
                        players.get(playerNumber).firstHalfOfHeadDrawable = players.get(playerNumber).headLeft1;
                        players.get(playerNumber).secondHalfOfHeadDrawable = players.get(playerNumber).headLeft2;
                        column -= tempSpeed;

                        //secondHalfOfViewID will increase/decrease an additional unit if the speed is 2.
                        if (tempSpeed == 2) {
                            if (gameBoard[row][column + 1] == 0) {
                                gameBoard[row][column + 1] = 10;
                            } else {
                                youLose = true;
                            }

                            players.get(playerNumber).secondHalfOfHalfViewID -= 1;
                        }
                        //speed = tempSpeed;
                    }
                    break;

                case "right":
                    if (column < GAME_BOARD_SIZE - speed) {
                        int tempSpeed = speed;
                        if (players.get(playerNumber).secondHalfOfHeadDrawable == players.get(playerNumber).headUp2) {
                            players.get(playerNumber).turnDirection = "upToRight";
                            players.get(playerNumber).lastTailDrawable = players.get(playerNumber).verticalTail;
                            tempSpeed = 1;

                        } else if (players.get(playerNumber).secondHalfOfHeadDrawable == players.get(playerNumber).headDown2) {
                            players.get(playerNumber).turnDirection = "downToRight";
                            players.get(playerNumber).lastTailDrawable = players.get(playerNumber).verticalTail;
                            tempSpeed = 1;
                        } else { // Player not turning
                            players.get(playerNumber).lastTailDrawable = players.get(playerNumber).horizontalTail;
                            players.get(playerNumber).turnDirection = "";
                        }
                        players.get(playerNumber).firstHalfOfHeadDrawable = players.get(playerNumber).headRight1;
                        players.get(playerNumber).secondHalfOfHeadDrawable = players.get(playerNumber).headRight2;

                        column += tempSpeed;

                        //secondHalfOfViewID will increase/decrease an additional unit if the speed is 2.
                        if (tempSpeed == 2) {
                            if (gameBoard[row][column - 1] == 0) {
                                gameBoard[row][column - 1] = 10;
                            } else {
                                youLose = true;
                            }
                            players.get(playerNumber).secondHalfOfHalfViewID += 1;
                        }
                        //speed = tempSpeed;
                    }
                    break;
            }

            // Check to see if we need to change the tail to one of the tail corners.
            if (players.get(playerNumber).lastTailDrawable == players.get(playerNumber).horizontalTail || players.get(playerNumber).lastTailDrawable == players.get(playerNumber).verticalTail) {
                switch (players.get(playerNumber).previousTurnDirection) {
                    case "leftToUp":
                    case "downToRight":
                        players.get(playerNumber).lastTailDrawable = players.get(playerNumber).bottomLeftCornerTail;
                        //System.out.println(previousTurnDirection);
                        break;
                    case "leftToDown":
                    case "upToRight":
                        players.get(playerNumber).lastTailDrawable = players.get(playerNumber).topLeftCornerTail;
                        //System.out.println(previousTurnDirection);
                        break;
                    case "rightToUp":
                    case "downToLeft":
                        players.get(playerNumber).lastTailDrawable = players.get(playerNumber).bottomRightCornerTail;
                        //System.out.println(previousTurnDirection);
                        break;
                    case "rightToDown":
                    case "upToLeft":
                        players.get(playerNumber).lastTailDrawable = players.get(playerNumber).topRightCornerTail;
                        //System.out.println(previousTurnDirection);
                        break;
                }
            }

            // Check if the player lose or not

            if (youLose || gameBoard[row][column] != 0 || row >= GAME_BOARD_SIZE || column >= GAME_BOARD_SIZE || row < 0 || column < 0) {
                displayYouLoseToast();
                System.out.println(youLose);
                youLose = true;
                return;
            }

            //gameBoard[row][column] = 10;


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
            players.get(playerNumber).firstHalfOfHeadViewID = Integer.parseInt(stringID);

            // Prepare views to update animation on UI.
            ImageView imageView = (ImageView) findViewById(players.get(playerNumber).firstHalfOfHeadViewID);
            ImageView imageView2 = (ImageView) findViewById(players.get(playerNumber).secondHalfOfHalfViewID);
            ImageView imageView3 = (ImageView) findViewById(players.get(playerNumber).lastTailViewID);
            ImageView imageView4 = (ImageView) findViewById(players.get(playerNumber).previousFirstHalfOfHeadViewID);


//                handlerRunning = true;
            //Communicate with UI thread to change image resource
            Handler threadHandler = new Handler(Looper.getMainLooper());
            threadHandler.post(new Runnable() {
                @Override
                public void run() { //This thread need sometime to update the UI screen.
                    // Change image for ImageView
                    imageView.setImageDrawable(players.get(playerNumber).firstHalfOfHeadDrawable);
                    imageView2.setImageDrawable(players.get(playerNumber).secondHalfOfHeadDrawable);
                    imageView3.setImageDrawable(players.get(playerNumber).lastTailDrawable);

                    // This take care of when the speed is 2.
                    // When the speed is 2, everything jump two cells.
                    // This means we need to update the cell that has been jumped over.
                    if (players.get(playerNumber).secondHalfOfHalfViewID != players.get(playerNumber).previousFirstHalfOfHeadViewID) {
                        if (players.get(playerNumber).secondHalfOfHeadDrawable == players.get(playerNumber).headLeft2 || players.get(playerNumber).secondHalfOfHeadDrawable == players.get(playerNumber).headRight2) {
                            imageView4.setImageDrawable(players.get(playerNumber).horizontalTail);
                        } else {
                            imageView4.setImageDrawable(players.get(playerNumber).verticalTail);
                        }
                    }
//                        handlerRunning = false;
                }
            });

            System.out.println(players.get(playerNumber).turnDirection);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {

            }
            players.get(playerNumber).lastTailViewID = players.get(playerNumber).secondHalfOfHalfViewID; //We want to know where the second half of the head was so we can update the tail appropriately next time.
            players.get(playerNumber).previousFirstHalfOfHeadViewID = players.get(playerNumber).firstHalfOfHeadViewID; // We need this to take care of when the speed is 2.
        }
    }


//    class MovePlayer implements Runnable {
//
//        // Drawables for the head of the player.
//        Drawable headUp1 = getDrawable(R.drawable.head_up1);
//        Drawable headUp2 = getDrawable(R.drawable.head_up2);
//        Drawable headDown1 = getDrawable(R.drawable.head_down1);
//        Drawable headDown2 = getDrawable(R.drawable.head_down2);
//        Drawable headLeft1 = getDrawable(R.drawable.head_left1);
//        Drawable headLeft2 = getDrawable(R.drawable.head_left2);
//        Drawable headRight1 = getDrawable(R.drawable.head_right1);
//        Drawable headRight2 = getDrawable(R.drawable.head_right2);
//
//        // Drawables for the tail of the player.
//        Drawable verticalTail = getDrawable(R.drawable.vertical_tail);
//        Drawable horizontalTail = getDrawable(R.drawable.horizontal_tail);
//        Drawable top_left_corner_tail = getDrawable(R.drawable.top_left_corner_tail);
//        Drawable top_right_corner_tail = getDrawable(R.drawable.top_right_corner_tail);
//        Drawable bottom_left_corner_tail = getDrawable(R.drawable.bottom_left_corner_tail);
//        Drawable bottom_right_corner_tail = getDrawable(R.drawable.bottom_right_corner_tail);
//
//        /*
//            The above Drawables are used to load in images into memory to save time.
//            Once they are loaded in memory, they will not change.
//        */
//
//
//        /*
//            The below Drawables will be set to the appropriate Drawables above
//         */
//        Drawable firstHalfOfHeadDrawable = headUp1;
//        Drawable secondHalfOfHeadDrawable = headUp2;
//        Drawable lastTailDrawable = verticalTail;  // The part of the tail that is next to the head.
//
//        int previousFirstHalfOfHeadViewID = firstHalfOfHeadViewID;
//        int secondHalfOfHalfViewID = previousFirstHalfOfHeadViewID;
//        int lastTailViewID = secondHalfOfHalfViewID;
//
//
//        String turnDirection = ""; // Determine how the car will turn (ex: "leftToUp")
//        String previousTurnDirection = ""; // We need this to know when to draw the corners of the tail since the tail follow the head, it needs to know what the head previous did.
//
//        public void displayYouLoseToast()
//        {
//            Handler threadHandler = new Handler(Looper.getMainLooper());
//            threadHandler.post(new Runnable() {
//                @Override
//                public void run() { //This thread need sometime to update the UI screen.
//                    deletePlayer();
//                    Toast.makeText(getApplicationContext(), "You lose",
//                            Toast.LENGTH_SHORT).show();
//                }
//            });
//        }
//
//        @Override
//        public void run() {
//            try {
//                Thread.sleep(3000);
//            } catch (InterruptedException e) {
//
//            }
//            boolean youLose = false; //indicate whether the player lose or not.
//            // This is used for when the speed is 2 (i.e. when the player skip a cell as they move, and there is obstacle in that cell, then youLose will set to true).
//
//            while (true) {
////                while (handlerRunning) {
////                    //Do nothing here. We want to ensure all the changes are made to the UI before we continue making a new move.
////                    //This will ensure all of our images are displayed correctly.
////                }
//                previousTurnDirection = turnDirection; // This help us know how our car turned so we know to update the UI appropriately (i.e. taking care of corners of tail).
//                secondHalfOfHalfViewID = previousFirstHalfOfHeadViewID;
//
//                //Get the current row and column of firstHalfOfHeadViewID
//                String stringID = String.valueOf(firstHalfOfHeadViewID);
//                String stringColumn = stringID.substring(3, 5);
//                String stringRow = stringID.substring(1, 3);
//                int row = Integer.parseInt(stringRow);
//                int column = Integer.parseInt(stringColumn);
//
//                gameBoard[row][column] = 10;
//
//                // Update the UI appropriately according to the playerDirection.
//                switch (playerDirection) {
//                    case "up":
//                        if (row > speed - 1) {
//                            //when we turn, we want to make the speed equal to 1
//                            int tempSpeed = speed;
//                            if (secondHalfOfHeadDrawable == headLeft2) { //We are turning
//                                turnDirection = "leftToUp";
//                                lastTailDrawable = horizontalTail;
//                                tempSpeed = 1;
//                            } else if (secondHalfOfHeadDrawable == headRight2) { //We are turning
//                                turnDirection = "rightToUp";
//                                lastTailDrawable = horizontalTail;
//                                tempSpeed = 1;
//                            } else { // Player not turning
//                                lastTailDrawable = verticalTail;
//                                turnDirection = "";
//                            }
//                            firstHalfOfHeadDrawable = headUp1;
//                            secondHalfOfHeadDrawable = headUp2;
//                            row -= tempSpeed;
//                            //secondHalfOfViewID will increase/decrease an additional unit if the speed is 2.
//                            if (tempSpeed == 2) {
//
//                                // when the speed is 2, the player's head skips a cell in gameBoard.
//                                // If there is something in that skipped cell, then the play dies.
//                                // This prevents the player from running through walls and other players without dying.
//                                if (gameBoard[row+1][column] == 0)
//                                {
//                                    gameBoard[row+1][column] = 10;
//                                }
//                                else
//                                {
//                                    youLose = true;
//                                }
//                                secondHalfOfHalfViewID -= 100;
//                            }
//                            //speed = tempSpeed;
//                        }
//                        break;
//                    case "down":
//                        if (row < GAME_BOARD_SIZE - speed) {
//                            int tempSpeed = speed;
//                            if (secondHalfOfHeadDrawable == headLeft2) {
//
//                                turnDirection = "leftToDown";
//                                lastTailDrawable = horizontalTail;
//                                tempSpeed = 1;
//
//                            } else if (secondHalfOfHeadDrawable == headRight2) {
//                                turnDirection = "rightToDown";
//                                lastTailDrawable = horizontalTail;
//                                tempSpeed = 1;
//                            } else { // Player not turning
//                                lastTailDrawable = verticalTail;
//                                turnDirection = "";
//                            }
//                            firstHalfOfHeadDrawable = headDown1;
//                            secondHalfOfHeadDrawable = headDown2;
//
//                            row += tempSpeed;
//
//                            //secondHalfOfViewID will increase/decrease an additional unit if the speed is 2.
//                            if (tempSpeed == 2) {
//                                if (gameBoard[row-1][column] == 0)
//                                {
//                                    gameBoard[row-1][column] = 10;
//                                }
//                                else
//                                {
//                                    youLose = true;
//                                }
//
//                                secondHalfOfHalfViewID += 100;
//                            }
//                            //speed = tempSpeed;
//                        }
//                        break;
//                    case "left":
//                        if (column > (speed - 1)) {
//                            int tempSpeed = speed;
//                            if (secondHalfOfHeadDrawable == headUp2) {
//                                turnDirection = "upToLeft";
//                                lastTailDrawable = verticalTail;
//                                tempSpeed = 1;
//
//                            } else if (secondHalfOfHeadDrawable == headDown2) {
//                                turnDirection = "downToLeft";
//                                lastTailDrawable = verticalTail;
//                                tempSpeed = 1;
//                            } else { // Player not turning
//                                lastTailDrawable = horizontalTail;
//                                turnDirection = "";
//                            }
//                            firstHalfOfHeadDrawable = headLeft1;
//                            secondHalfOfHeadDrawable = headLeft2;
//                            column -= tempSpeed;
//
//                            //secondHalfOfViewID will increase/decrease an additional unit if the speed is 2.
//                            if (tempSpeed == 2) {
//                                if(gameBoard[row][column+1] == 0)
//                                {
//                                    gameBoard[row][column+1] = 10;
//                                }
//                                else
//                                {
//                                    youLose = true;
//                                }
//
//                                secondHalfOfHalfViewID -= 1;
//                            }
//                            //speed = tempSpeed;
//                        }
//                        break;
//
//                    case "right":
//                        if (column < GAME_BOARD_SIZE - speed) {
//                            int tempSpeed = speed;
//                            if (secondHalfOfHeadDrawable == headUp2) {
//                                turnDirection = "upToRight";
//                                lastTailDrawable = verticalTail;
//                                tempSpeed = 1;
//
//                            } else if (secondHalfOfHeadDrawable == headDown2) {
//                                turnDirection = "downToRight";
//                                lastTailDrawable = verticalTail;
//                                tempSpeed = 1;
//                            } else { // Player not turning
//                                lastTailDrawable = horizontalTail;
//                                turnDirection = "";
//                            }
//                            firstHalfOfHeadDrawable = headRight1;
//                            secondHalfOfHeadDrawable = headRight2;
//
//                            column += tempSpeed;
//
//                            //secondHalfOfViewID will increase/decrease an additional unit if the speed is 2.
//                            if (tempSpeed == 2) {
//                                if (gameBoard[row][column-1] == 0)
//                                {
//                                    gameBoard[row][column-1] = 10;
//                                }
//                                else
//                                {
//                                    youLose = true;
//                                }
//                                secondHalfOfHalfViewID += 1;
//                            }
//                            //speed = tempSpeed;
//                        }
//                        break;
//                }
//
//                // Check to see if we need to change the tail to one of the tail corners.
//                if (lastTailDrawable == horizontalTail || lastTailDrawable == verticalTail) {
//                    switch (previousTurnDirection) {
//                        case "leftToUp":
//                        case "downToRight":
//                            lastTailDrawable = bottom_left_corner_tail;
//                            //System.out.println(previousTurnDirection);
//                            break;
//                        case "leftToDown":
//                        case "upToRight":
//                            lastTailDrawable = top_left_corner_tail;
//                            //System.out.println(previousTurnDirection);
//                            break;
//                        case "rightToUp":
//                        case "downToLeft":
//                            lastTailDrawable = bottom_right_corner_tail;
//                            //System.out.println(previousTurnDirection);
//                            break;
//                        case "rightToDown":
//                        case "upToLeft":
//                            lastTailDrawable = top_right_corner_tail;
//                            //System.out.println(previousTurnDirection);
//                            break;
//                    }
//                }
//
//                // Check if the player lose or not
//
//                if (youLose || gameBoard[row][column] != 0 || row >= GAME_BOARD_SIZE || column >= GAME_BOARD_SIZE || row < 0 || column < 0)
//                {
//                    displayYouLoseToast();
//                    System.out.println(youLose);
//                    break;
//                }
//
//                //gameBoard[row][column] = 10;
//
//
//                // Make sure view_id is of the form 1_00_00.
//                if (column < 10) {
//                    stringColumn = "0" + String.valueOf(column);
//                } else {
//                    stringColumn = String.valueOf(column);
//                }
//                if (row < 10) {
//                    stringRow = "0" + String.valueOf(row);
//                } else {
//                    stringRow = String.valueOf(row);
//                }
//                stringID = "1" + stringRow + stringColumn;
//                firstHalfOfHeadViewID = Integer.parseInt(stringID);
//
//                // Prepare views to update animation on UI.
//                ImageView imageView = (ImageView) findViewById(firstHalfOfHeadViewID);
//                ImageView imageView2 = (ImageView) findViewById(secondHalfOfHalfViewID);
//                ImageView imageView3 = (ImageView) findViewById(lastTailViewID);
//                ImageView imageView4 = (ImageView) findViewById(previousFirstHalfOfHeadViewID);
//
//
////                handlerRunning = true;
//                //Communicate with UI thread to change image resource
//                Handler threadHandler = new Handler(Looper.getMainLooper());
//                threadHandler.post(new Runnable() {
//                    @Override
//                    public void run() { //This thread need sometime to update the UI screen.
//                        // Change image for ImageView
//                        imageView.setImageDrawable(firstHalfOfHeadDrawable);
//                        imageView2.setImageDrawable(secondHalfOfHeadDrawable);
//                        imageView3.setImageDrawable(lastTailDrawable);
//
//                        // This take care of when the speed is 2.
//                        // When the speed is 2, everything jump two cells.
//                        // This means we need to update the cell that has been jumped over.
//                        if (secondHalfOfHalfViewID != previousFirstHalfOfHeadViewID) {
//                            if (secondHalfOfHeadDrawable == headLeft2 || secondHalfOfHeadDrawable == headRight2) {
//                                imageView4.setImageDrawable(horizontalTail);
//                            } else {
//                                imageView4.setImageDrawable(verticalTail);
//                            }
//                        }
////                        handlerRunning = false;
//                    }
//                });
//
//                System.out.println(turnDirection);
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//
//                }
//                lastTailViewID = secondHalfOfHalfViewID; //We want to know where the second half of the head was so we can update the tail appropriately next time.
//                previousFirstHalfOfHeadViewID = firstHalfOfHeadViewID; // We need this to take care of when the speed is 2.
//            } // end while
//        }
//    }

    public void buttonClicked(View view) throws InterruptedException {
        for (int i = 0; i < GAME_BOARD_SIZE; i++) {
            ImageView imageView = (ImageView) findViewById(firstHalfOfHeadViewID);
            // Increase column by 1
            String stringID = String.valueOf(firstHalfOfHeadViewID);
            String stringColumn = stringID.substring(3, 5);
            String stringRow = stringID.substring(1, 3);

            int row = Integer.parseInt(stringRow);
            int column = Integer.parseInt(stringColumn);
            if (column < GAME_BOARD_SIZE) {
                gameBoard[row][column] = 1;
                column += 1;
                if (column < 10) {
                    stringColumn = "0" + String.valueOf(column);
                } else {
                    stringColumn = String.valueOf(column);
                }
                stringID = stringID.substring(0, 3) + stringColumn;
                firstHalfOfHeadViewID = Integer.parseInt(stringID);
                //System.out.println(stringID);
                // Change image for ImageView
                imageView.setImageResource(R.drawable.player1);
                imageView.setLayoutParams(new TableRow.LayoutParams(positionSize, positionSize));
                //gameBoard[row][column] = 1;
                //System.out.println("sleep???");
            }
            Thread.sleep(300);
        }
        //System.out.println("Button clicked");
    }
    //TODO: On Friday, create a virtual joystick. Create images for the heads. Create images for the tails.

    //TODO: On Saturday, handle multitouch events. Add a "Boost" button that allow player to move two blocks.

    //TODO: On Sunday, create Player class. Move a player automatically, and control that player's direction with the joystick.

    //TODO: On Monday, add multiple players and allow them to move automatically. Figure out the int values (i.e. player's heads and tails) for each player in the board[][]

    //TODO: On Tuesday, create socket and connect to the server using background thread. Figure out how to handle communication.

    //TODO: On Wednesday, make "Create Game"/"Join Game" Activity that interact with the server.

    //TODO: On Thursday, create "Lobby" activity with "Start" and "Leave" buttons. Receive game information from server (number of players, who is the host, players' information).

    //TODO: On Saturday, create Room Database to store game information received from server (i.e. player's name and player's position).

    //TODO: On Sunday, handle game communication between server and clients in the same room. Synchronize start game of every player in the same room.

    //TODO: On Monday, finalize the project. Check for all requirements Dr. Blum wants.

    //TODO: On Tuesday, write the report and prepare presentation.

    //TODO: On Wednesday, submit project and continue to prepare for presentation.
}

