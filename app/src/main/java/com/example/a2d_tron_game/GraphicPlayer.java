package com.example.a2d_tron_game;
import android.content.Context;
import android.graphics.drawable.Drawable;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import java.lang.reflect.Field;

public class GraphicPlayer {

    Context context;
    String playerDirection;
    String previousPlayerDirection;
    // Drawables for the head of the player.
    Drawable headUp1;
    Drawable headUp2;
    Drawable headDown1;
    Drawable headDown2;
    Drawable headLeft1;
    Drawable headLeft2;
    Drawable headRight1;
    Drawable headRight2;

    // Drawables for the tail of the player.
    Drawable verticalTail;
    Drawable horizontalTail;
    Drawable topLeftCornerTail;
    Drawable topRightCornerTail;
    Drawable bottomLeftCornerTail;
    Drawable bottomRightCornerTail;

    /*
            The above Drawables are used to load in images into memory to save time.
            Once they are loaded in memory, they will not change.
     */

    /*
            The below Drawables will be set to the appropriate Drawables above
     */
    Drawable firstHalfOfHeadDrawable;
    Drawable secondHalfOfHeadDrawable;
    Drawable lastTailDrawable;  // The part of the tail that is next to the head.

    int firstHalfOfHeadViewID; //The view id for a current position is of this form: 1_00_00 (i.e. 1_row_column).
    int previousFirstHalfOfHeadViewID = firstHalfOfHeadViewID;
    int secondHalfOfHalfViewID;
    int lastTailViewID;


    String turnDirection = ""; // Determine how the car will turn (ex: "leftToUp")
    String previousTurnDirection = ""; // We need this to know when to draw the corners of the tail since the tail follow the head, it needs to know what the head previous did.

    boolean handlerRunning;

    public GraphicPlayer(String playerOrder, String playerDirection, int initialPositionID, Context context) {
        this.firstHalfOfHeadViewID = initialPositionID;
        this.context = context;

        String headUp1Name = playerOrder + "_head_up1";
        String headUp2Name = playerOrder + "_head_up2";
        String headDown1Name = playerOrder + "_head_down1";
        String headDown2Name = playerOrder + "_head_down2";
        String headLeft1Name = playerOrder + "_head_left1";
        String headLeft2Name = playerOrder + "_head_left2";
        String headRight1Name = playerOrder + "_head_right1";
        String headRight2Name = playerOrder + "_head_right2";
        String verticalTailName = playerOrder + "_vertical_tail";
        String horizontalTailName = playerOrder + "_horizontal_tail";
        String topLeftCornerTailName = playerOrder + "_top_left_corner_tail";
        String topRightCornerTailName = playerOrder + "_top_right_corner_tail";
        String bottomLeftCornerTailName = playerOrder + "_bottom_left_corner_tail";
        String bottomRightCornerTailName = playerOrder + "_bottom_right_corner_tail";


        //Set the Drawable for the player.
        this.headUp1 = context.getDrawable(getResId(headUp1Name));
        this.headUp2 = context.getDrawable(getResId(headUp2Name));
        this.headDown1 = context.getDrawable(getResId(headDown1Name));
        this.headDown2 = context.getDrawable(getResId(headDown2Name));
        this.headLeft1 = context.getDrawable(getResId(headLeft1Name));
        this.headLeft2 = context.getDrawable(getResId(headLeft2Name));
        this.headRight1 = context.getDrawable(getResId(headRight1Name));
        this.headRight2 = context.getDrawable(getResId(headRight2Name));
        this.verticalTail = context.getDrawable(getResId(verticalTailName));
        this.horizontalTail = context.getDrawable(getResId(horizontalTailName));
        this.topLeftCornerTail = context.getDrawable(getResId(topLeftCornerTailName));
        this.topRightCornerTail = context.getDrawable(getResId(topRightCornerTailName));
        this.bottomLeftCornerTail = context.getDrawable(getResId(bottomLeftCornerTailName));
        this.bottomRightCornerTail = context.getDrawable(getResId(bottomRightCornerTailName));
        this.firstHalfOfHeadDrawable = headUp1;
        this.secondHalfOfHeadDrawable = headUp2;
        this.lastTailDrawable = verticalTail;  // The part of the tail that is next to the head.
        System.out.println("horizontalTail" + horizontalTail);

        //Set the initial value for important integer ID's
        this.previousFirstHalfOfHeadViewID = firstHalfOfHeadViewID;
        this.secondHalfOfHalfViewID = previousFirstHalfOfHeadViewID;
        this.lastTailViewID = secondHalfOfHalfViewID;

        //Set the initial direction for player
        this.playerDirection = playerDirection;
        this.previousPlayerDirection = this.playerDirection;
        System.out.println("finished initialize player");

        handlerRunning = false;
    }


    //https://stackoverflow.com/questions/4427608/android-getting-resource-id-from-string
    public int getResId(String resName) {
        int resId = context.getResources().getIdentifier(
                resName,
                "drawable",
                context.getPackageName()
        );
        return resId;
    }

}
