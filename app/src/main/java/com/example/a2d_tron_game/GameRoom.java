package com.example.a2d_tron_game;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class GameRoom {

    @PrimaryKey
    @NonNull
    public String gameRoomID;

    @ColumnInfo(name = "game_room_ip")
    public String gameRoomIP;

    @ColumnInfo(name = "game_room_port")
    public int gameRoomPort;
}
