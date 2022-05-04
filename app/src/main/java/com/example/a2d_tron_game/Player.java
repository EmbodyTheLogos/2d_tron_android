package com.example.a2d_tron_game;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Player {

    @PrimaryKey
    @NonNull
    public String playerID;

    @ColumnInfo(name = "player_name")
    public String playerName;

    @ColumnInfo(name = "is_local")
    public boolean isLocal;

}
