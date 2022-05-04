package com.example.a2d_tron_game;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;

public class GameBoard {
    public final int GAME_BOARD_SIZE = 70;
    public int gameBoard[][] = new int[GAME_BOARD_SIZE][GAME_BOARD_SIZE];
    // Get screen dimensions in dp
    DisplayMetrics displayMetrics;
    float pxHeight;
    float dpHeight;
    private int positionSize; // the pixel size of each position in the gameBoard.

    public GameBoard() {
    }

    public synchronized int performOperationOnGameBoard(int row, int column, int value) {

        if (value == -9999) {
            //This is the getter method
            return gameBoard[row][column];

        } else {
            //This is the setter method
            gameBoard[row][column] = value;
            return -9999;
        }
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

    public void createUIGameBoardHelper(Context context, Activity activity) {

        //Scale the board according to screen dp and pixel.
        displayMetrics = context.getResources().getDisplayMetrics();
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
            TableLayout tl = (TableLayout) activity.findViewById(R.id.table_layout);
            /* Create a new row to be added. */
            TableRow tr = new TableRow(context);
            tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
            /* Create a Button to be the row-content. */

            for (int k = 0; k < GAME_BOARD_SIZE; k++) {
                ImageView b = new ImageView(context);
                if (gameBoard[i][k] == 1) {
                    b.setImageResource(R.drawable.wall);
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

    public void createUIGameBoard(Context context, Activity activity) {
        Handler threadHandler = new Handler(Looper.getMainLooper());
        threadHandler.post(new Runnable() {
            @Override
            public void run() { //This thread need sometime to update the UI screen.
                createUIGameBoardHelper(context, activity);
            }
        });

        System.out.println("finish initialize UI game board");
    }


    public boolean playerLose(int row, int column, String direction, int currentPosition) {

        switch (direction) {
            case "up":
                row--;
                break;
            case "down":
                row++;
                break;
            case "left":
                column--;
                break;
            case "right":
                column++;
                break;
        }

        try {
            if (performOperationOnGameBoard(row, column, -9999) != 0) {
                return true;
            }

        } catch (ArrayIndexOutOfBoundsException e) {
            return true;
        }
        return false;
    }
}
