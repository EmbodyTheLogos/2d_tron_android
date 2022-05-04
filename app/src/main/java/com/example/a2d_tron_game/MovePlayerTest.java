package com.example.a2d_tron_game;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.telephony.ims.RcsUceAdapter;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.room.Room;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import io.github.controlwear.virtual.joystick.android.JoystickView;

class MovePlayerTest {
    boolean playWithBot;
    GameBoard gameBoardData;
    Activity activity;
    Context context;
    boolean gameOver;
    volatile String playerDirection = "up";
    volatile int speed = 1; // speed of car.

    volatile int numOfAlivePlayer = 4;


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

    SharedPreferences gameRoomInfo;


    MovePlayerTest(Activity activity, Context context, boolean playWithBot) {
        myPlayerMove = new JSONObject();
        allPlayersMoves = new JSONObject();
        this.activity = activity;
        this.context = context;
        gameOver = false;
        gameBoardData = new GameBoard();
        this.playWithBot = playWithBot;
    }

    public void boostSpeedListener() {
        if (activity != null) {
            ImageButton speedBoostButton = activity.findViewById(R.id.boost_speed);

            speedBoostButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (speed == 1) {
                        speed = 2;
                    } else {
                        speed = 1;
                    }
                }
            });
        }
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

    public void run() {


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

        JoyStickThread joyStickThread = new JoyStickThread();
        new Thread(joyStickThread).start();
        gameBoardData.initializeGameBoard();
        System.out.println("finish initialize game board");
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





        // initilize all players' graphics
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
            GraphicPlayer graphicPlayer = new GraphicPlayer(playerOrder, direction, firstViewID, context);
            graphicPlayers.add(graphicPlayer);
        }


        // this is an offline game with bots
        if (playWithBot) {
            //Start player thread
            //(int myPlayerIndex, String myPlayerID, String serverIP, int serverPort)
            PlayerThread playerThread = new PlayerThread(0, "player1", "localhost", 5000, 4);
            new Thread(playerThread).start();
            new Thread(new BotThread(1)).start();
            new Thread(new BotThread(2)).start();
            new Thread(new BotThread(3)).start();

        } else {
            // This is an online game
            //load players from room database
            gameRoomInfo = context.getSharedPreferences("gameRoomInfo", Context.MODE_PRIVATE);
            String gameRoomID = gameRoomInfo.getString("gameRoomID", "");
            String gameRoomIP = gameRoomInfo.getString("gameRoomIP", "");
            int gameRoomPort = gameRoomInfo.getInt("gameRoomPort", 0);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    AppDatabase db = Room.databaseBuilder(context,
                            AppDatabase.class, "player").build();
                    PlayerDAO playerDAO = db.playerDAO();

                    List<Player> playerList = playerDAO.getAll();
                    String myPlayerID = "";
                    int myPlayerIndex = 0;
                    int numOfPlayers = playerList.size();
                    for (Player player : playerList)
                    {
                        if (!player.playerID.equals("localPlayer"))
                        {
                            if (player.isLocal)
                            {
                                String playerID = player.playerID;
                                int playerNumber = Integer.parseInt(String.valueOf(playerID.charAt(playerID.length()-1))) - 1;
                                PlayerThread playerThread = new PlayerThread(playerNumber,player.playerID, gameRoomIP, gameRoomPort, numOfPlayers);
                                new Thread(playerThread).start();
                                break;
                            }

                        }
                    }

                }
            }).start();
        }

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    Thread.sleep(3000);
//                } catch (InterruptedException e) {
//
//                }
//                boolean youLose2 = false;
//                boolean noDraw = true;
//                String direction = graphicPlayers.get(2).playerDirection;
//                while (!youLose2 || gameOver) {
//                    //Get the current row and column of the current position
//                    int currentPosition = graphicPlayers.get(2).firstHalfOfHeadViewID;
//                    String stringID = String.valueOf(currentPosition);
//                    String stringColumn = stringID.substring(3, 5);
//                    String stringRow = stringID.substring(1, 3);
//                    int row = Integer.parseInt(stringRow);
//                    int column = Integer.parseInt(stringColumn);
//
//                    youLose2 = gameBoardData.playerLose(row, column, direction, currentPosition);
//                    if ((direction.equals("up") || direction.equals("down")) && youLose2) {
//                        direction = "left";
//                        youLose2 = gameBoardData.playerLose(row, column, direction, currentPosition);
//                        if (youLose2) {
//                            direction = "right";
//                        }
//                        youLose2 = false;
//                    } else if ((direction.equals("left") || direction.equals("right")) && youLose2) {
//                        direction = "down";
//                        youLose2 = gameBoardData.playerLose(row, column, direction, currentPosition);
//                        if (youLose2) {
//                            direction = "up";
//                        }
//                        youLose2 = false;
//                    }
//                    youLose2 = updatePlayerMove(2, direction, 1, 0, youLose2, false);
//                }
//                gameOver = true;
//            }
//        }).start();

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    Thread.sleep(3000);
//                } catch (InterruptedException e) {
//
//                }
//                boolean youLose2 = false;
//                boolean noDraw = true;
//                String direction = graphicPlayers.get(1).playerDirection;
//                while (!youLose2 || gameOver) {
//                    //Get the current row and column of the current position
//                    int currentPosition = graphicPlayers.get(1).firstHalfOfHeadViewID;
//                    String stringID = String.valueOf(currentPosition);
//                    String stringColumn = stringID.substring(3, 5);
//                    String stringRow = stringID.substring(1, 3);
//                    int row = Integer.parseInt(stringRow);
//                    int column = Integer.parseInt(stringColumn);
//
//                    youLose2 = gameBoardData.playerLose(row, column, direction, currentPosition);
//                    if ((direction.equals("up") || direction.equals("down")) && youLose2) {
//                        direction = "left";
//                        youLose2 = gameBoardData.playerLose(row, column, direction, currentPosition);
//                        if (youLose2) {
//                            direction = "right";
//                        }
//                        youLose2 = false;
//                    } else if ((direction.equals("left") || direction.equals("right")) && youLose2) {
//                        direction = "down";
//                        youLose2 = gameBoardData.playerLose(row, column, direction, currentPosition);
//                        if (youLose2) {
//                            direction = "up";
//                        }
//                        youLose2 = false;
//                    }
//                    youLose2 = updatePlayerMove(1, direction, 1, 0, youLose2, false);
//                }
//                gameOver = true;
//            }
//        }).start();


//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    Thread.sleep(3000);
//                } catch (InterruptedException e) {
//
//                }
//                boolean youLose2 = false;
//                boolean noDraw = true;
//                String direction = graphicPlayers.get(3).playerDirection;
//                while (!youLose2 || gameOver) {
//                    //Get the current row and column of the current position
//                    int currentPosition = graphicPlayers.get(3).firstHalfOfHeadViewID;
//                    String stringID = String.valueOf(currentPosition);
//                    String stringColumn = stringID.substring(3, 5);
//                    String stringRow = stringID.substring(1, 3);
//                    int row = Integer.parseInt(stringRow);
//                    int column = Integer.parseInt(stringColumn);
//
//                    youLose2 = gameBoardData.playerLose(row, column, direction, currentPosition);
//                    if ((direction.equals("up") || direction.equals("down")) && youLose2) {
//                        direction = "left";
//                        youLose2 = gameBoardData.playerLose(row, column, direction, currentPosition);
//                        if (youLose2) {
//                            direction = "right";
//                        }
//                        youLose2 = false;
//                    } else if ((direction.equals("left") || direction.equals("right")) && youLose2) {
//                        direction = "down";
//                        youLose2 = gameBoardData.playerLose(row, column, direction, currentPosition);
//                        if (youLose2) {
//                            direction = "up";
//                        }
//                        youLose2 = false;
//                    }
//                    youLose2 = updatePlayerMove(3, direction, 1, 0, youLose2, false);
//                }
//                gameOver = true;
//            }
//        }).start();


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


    public boolean updatePlayerMove(int playerNumber, String direction, int currentSpeed, long latencyTime, boolean youLose, boolean noDraw) {
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

        //gameBoard[row][column] = playerCode;
        gameBoardData.gameBoard[row][column] = playerCode;


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
                            gameBoardData.performOperationOnGameBoard(row + 1, column, playerCode);
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
                if (row < gameBoardData.GAME_BOARD_SIZE - currentSpeed) {
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
                            gameBoardData.performOperationOnGameBoard(row - 1, column, playerCode);
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
                            gameBoardData.performOperationOnGameBoard(row, column + 1, playerCode);
                        } else {
                            youLose = true;
                        }

                        graphicPlayers.get(playerNumber).secondHalfOfHalfViewID -= 1;
                    }
                    //speed = tempSpeed;
                }
                break;

            case "right":
                if (column < gameBoardData.GAME_BOARD_SIZE - currentSpeed) {
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
                            gameBoardData.performOperationOnGameBoard(row, column - 1, playerCode);
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

        if (youLose || gameBoardData.performOperationOnGameBoard(row, column, -9999) != 0 || row >= gameBoardData.GAME_BOARD_SIZE || column >= gameBoardData.GAME_BOARD_SIZE || row < 0 || column < 0) {
            //System.out.println(youLose);
            youLose = true;
            return youLose;
        }


        //gameBoardData.gameBoard[row][column] = playerCode;
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
            ImageView imageView = (ImageView) activity.findViewById(graphicPlayers.get(playerNumber).firstHalfOfHeadViewID);
            ImageView imageView2 = (ImageView) activity.findViewById(graphicPlayers.get(playerNumber).secondHalfOfHalfViewID);
            ImageView imageView3 = (ImageView) activity.findViewById(graphicPlayers.get(playerNumber).lastTailViewID);
            ImageView imageView4 = (ImageView) activity.findViewById(graphicPlayers.get(playerNumber).previousFirstHalfOfHeadViewID);

            //Communicate with UI thread to change image resource
//            graphicPlayers.get(playerNumber).handlerRunning = true;
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
                    //graphicPlayers.get(playerNumber).handlerRunning = false;
                }
            });

//            while(graphicPlayers.get(playerNumber).handlerRunning)
//            {
//
//            }


            long endTime = System.currentTimeMillis();
            latencyTime += (endTime - startTime);
            try {
                if (latencyTime < 100) {
                    Thread.sleep(100 - latencyTime);
                }
//                Thread.sleep(100);


            } catch (InterruptedException e) {

            }
            graphicPlayers.get(playerNumber).lastTailViewID = graphicPlayers.get(playerNumber).secondHalfOfHalfViewID; //We want to know where the second half of the head was so we can update the tail appropriately next time.
            graphicPlayers.get(playerNumber).previousFirstHalfOfHeadViewID = graphicPlayers.get(playerNumber).firstHalfOfHeadViewID; // We need this to take care of when the speed is 2.
            graphicPlayers.get(playerNumber).previousPlayerDirection = graphicPlayers.get(playerNumber).playerDirection;

        }

        return youLose;
    }

    public void deletePlayer(int playerCode) {

        Handler threadHandler = new Handler(Looper.getMainLooper());
        threadHandler.post(new Runnable() {
            @Override
            public void run() { //This thread need sometime to update the UI screen.
                for (int row = 0; row < gameBoardData.GAME_BOARD_SIZE; row++) {
                    String stringRow = String.valueOf(row);
                    if (row < 10) {
                        stringRow = "0" + row;
                    }
                    for (int column = 0; column < gameBoardData.GAME_BOARD_SIZE; column++) {
                        if (gameBoardData.gameBoard[row][column] == playerCode) {
                            String stringColumn = String.valueOf(column);
                            if (column < 10) {
                                stringColumn = "0" + column;
                            }
                            String stringId = "1" + stringRow + stringColumn;
                            int id = Integer.parseInt(stringId);
                            ImageView imageView = (ImageView) activity.findViewById(id);
                            imageView.setImageResource(R.drawable.block);
                            //imageView.setLayoutParams(new TableRow.LayoutParams(positionSize, positionSize));
                            //gameBoardData.gameBoard[row][column] = 0;
                            gameBoardData.performOperationOnGameBoard(row, column, 0);
                        }
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
        return fullMessage.toString();
    }

    class PlayerThread implements Runnable {

        int myPlayerIndex;
        String myPlayerID;
        String serverIP;
        int serverPort;
        Socket socket;
        int numOfPlayer;

        public PlayerThread(int myPlayerIndex, String myPlayerID, String serverIP, int serverPort, int numOfPlayer) {
            this.myPlayerIndex = myPlayerIndex;
            this.myPlayerID = myPlayerID;
            this.serverIP = serverIP;
            this.serverPort = serverPort;
            this.numOfPlayer = numOfPlayer;
        }

        @Override
        public void run() {

            try {
                Thread.sleep(3500);
            } catch (InterruptedException e) {

            }

            try {
                socket = new Socket(serverIP, serverPort);
                System.out.println("connect successfully");
            } catch (IOException e) {
                e.printStackTrace();
            }


            String onlineServerMessage;
            JSONArray onlineServerMessageJSONArray = new JSONArray();
            String currentDirection;
            long startTime = 0;
            long endTime = 0;
            int currentSpeed;
            // initilize playerDirection
            playerDirection = graphicPlayers.get(0).playerDirection;
            while (!youLose && !gameOver) {
                currentDirection = playerDirection;
                currentSpeed = speed; //currentSpeed decrease to 1 when turning
//                //TODO: send your move to game server
                try {
                    String stringID = String.valueOf(graphicPlayers.get(myPlayerIndex).firstHalfOfHeadViewID);
                    String stringColumn = stringID.substring(3, 5);
                    String stringRow = stringID.substring(1, 3);
                    int row = Integer.parseInt(stringRow);
                    int column = Integer.parseInt(stringColumn);

                    String position = "";
                    switch (currentDirection) {
                        case "up":
                            switch (graphicPlayers.get(myPlayerIndex).previousPlayerDirection) {
                                case "down":
                                case "up":
                                    row--;
                                    position = "[" + row + "," + column + "]";
                                    if (currentSpeed == 2) {
                                        if (gameBoardData.gameBoard[row][column] == 0) {
                                            row--;
                                            position = position + "+[" + row + "," + column + "]";
                                        }
                                    }
                                    break;
                                case "left":
                                case "right":
                                    currentSpeed = 1;
                                    row--;
                                    position = "[" + row + "," + column + "]";
                                    break;
                            }
                            break;
                        case "down":
                            switch (graphicPlayers.get(myPlayerIndex).previousPlayerDirection) {
                                case "down":
                                case "up":
                                    row++;
                                    position = "[" + row + "," + column + "]";
                                    if (currentSpeed == 2) {
                                        if (gameBoardData.gameBoard[row][column] == 0) {
                                            row++;
                                            position = position + "+[" + row + "," + column + "]";
                                        }
                                    }

                                    myPlayerMove.put("position", position);
                                    break;
                                case "left":
                                case "right":
                                    currentSpeed = 1;
                                    row++;
                                    position = "[" + row + "," + column + "]";
                                    break;
                            }
                            break;

                        case "left":
                            switch (graphicPlayers.get(myPlayerIndex).previousPlayerDirection) {
                                case "left":
                                case "right":
                                    column--;
                                    position = "[" + row + "," + column + "]";
                                    if (currentSpeed == 2) {
                                        if (gameBoardData.gameBoard[row][column] == 0) {
                                            column--;
                                            position = position + "+[" + row + "," + column + "]";
                                        }
                                    }
                                    break;
                                case "up":
                                case "down":
                                    currentSpeed = 1;
                                    column--;
                                    position = "[" + row + "," + column + "]";
                                    break;

                            }
                            break;
                        case "right":
                            switch (graphicPlayers.get(myPlayerIndex).previousPlayerDirection) {
                                case "left":
                                case "right":
                                    column++;
                                    position = "[" + row + "," + column + "]";
                                    if (currentSpeed == 2) {
                                        if (gameBoardData.gameBoard[row][column] == 0) {
                                            column++;
                                            position = position + "+[" + row + "," + column + "]";
                                        }
                                    }
                                    break;
                                case "up":
                                case "down":
                                    currentSpeed = 1;
                                    column++;
                                    position = "[" + row + "," + column + "]";
                                    break;
                            }
                            break;
                    }

                    String headerFormat = "%-" + HEADERSIZE + "s";

                    if (playWithBot) {
                        // Send move to server
                        String sendMessage = "[" + row + "," + column + "," + 10 + "]";
                        String header = String.format(headerFormat, sendMessage.getBytes().length);
                        socket.getOutputStream().write((header + sendMessage).getBytes());


                        // receive move status from server
                        String receiveMessage = receiveServerMessage(socket);
                        if (receiveMessage.equals("boom")) {
                            youLose = true;
                            break;
                        }
                        updatePlayerMove(0, playerDirection, speed, 0, youLose, false);

                    } else {
                        // This is an online game

                        System.out.println("this is an online game");
                        myPlayerMove.put("position", position);
                        myPlayerMove.put("playerID", myPlayerID);
                        myPlayerMove.put("speed", currentSpeed);
                        myPlayerMove.put("direction", currentDirection);
//
//
                        // Send move to server
                        String myPlayerMoveString = myPlayerMove.toString();
                        System.out.println(myPlayerMoveString);
                        String header = String.format(headerFormat, myPlayerMoveString.getBytes().length);
                        //System.out.println("playerDirection " + playerDirection);
                        socket.getOutputStream().write((header + myPlayerMoveString).getBytes());

                        // receive move status from server
                        onlineServerMessage = receiveServerMessage(socket);
                        onlineServerMessageJSONArray = new JSONArray(onlineServerMessage);
                        System.out.println(onlineServerMessageJSONArray);


                        //TODO: receive move from all graphicPlayers to game server
                        for (int i = 0; i < onlineServerMessageJSONArray.length(); i++)
                        {
                            JSONObject playerMove = onlineServerMessageJSONArray.getJSONObject(i);
                            if (!playerMove.equals("None"))
                            {
                                String playerDirection = playerMove.getString("direction");
                                JSONArray playerPositionStatus = playerMove.getJSONArray("position");
                                int playerSpeed = 0;
                                for (int k = 0; k < playerPositionStatus.length(); k++)
                                {
                                    if (playerPositionStatus.getString(k).equals("ok"))
                                    {
                                        playerSpeed++;
                                    }
                                    else
                                    {
                                        break;
                                    }
                                }
                                if (playerSpeed > 0)
                                {
                                    updatePlayerMove(i, playerDirection, playerSpeed, 0, youLose, false);

                                }

                            }
                        }

                    }





                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }
                if (numOfAlivePlayer == 1) {
                    break;
                }


                //TODO: when speed is 2, we move 2 cells. If the 2nd cell is an obstacle, set the speed to 1 to display the car properly.
//                int latencyTime = (int) (endTime - startTime);
//                if (youLose) {
//                    if (currentSpeed == 2) {
//                        currentSpeed = 1;
//                        updatePlayerMove(myPlayerIndex, playerDirection, speed, 0, youLose, false);
//                    }
//                    break;
//                }

            } // end while
            numOfAlivePlayer--;
            if (youLose) {
                displayYouLoseToast();
                deletePlayer(10);
            } else if (!gameOver) {
                displayToast("You Win!");
            }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    class BotThread implements Runnable {
        int playerNumber;
        Socket socket;

        public BotThread(int playerNumber) {
            this.playerNumber = playerNumber;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(3500);
            } catch (InterruptedException e) {

            }
            try {
                socket = new Socket("localhost", 5000);
            } catch (IOException e) {
                e.printStackTrace();
            }


            boolean youLose2 = false;
            boolean noDraw = true;
            String direction = graphicPlayers.get(playerNumber).playerDirection;
            int playerCode = (playerNumber + 1) * 10;
            while (!youLose2 && !gameOver) {
                //Get the current row and column of the current position
                int currentPosition = graphicPlayers.get(playerNumber).firstHalfOfHeadViewID;
                long latencyTime = System.currentTimeMillis();
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
                try {
                    //Send move to local game server
                    String sendMessage = "[" + row + "," + column + "," + playerCode + "]";
                    String headerFormat = "%-" + HEADERSIZE + "s";
                    String header = String.format(headerFormat, sendMessage.getBytes().length);
                    System.out.println(header);

                    socket.getOutputStream().write((header + sendMessage).getBytes());
                    String receiveMessage = receiveServerMessage(socket);
                    System.out.println("why " + receiveMessage);
                    if (receiveMessage.equals("boom")) {
                        youLose2 = true;
                        break;
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (numOfAlivePlayer == 1) {
                    break;
                }
                latencyTime = System.currentTimeMillis() - latencyTime;
                youLose2 = updatePlayerMove(playerNumber, direction, 1, latencyTime, youLose2, false);
            }
            System.out.println("player is done");
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            numOfAlivePlayer--;
            if (youLose2) {
                deletePlayer(playerCode);
            }


        }
    }

    class JoyStickThread implements Runnable {

        @Override
        public void run() {

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {

            }


            boostSpeedListener();

            //Creating virtual joystick as the controller of the game.
            JoystickView joystick = (JoystickView) activity.findViewById(R.id.joystickView);
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
}