package com.example.a2d_tron_game;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Player.class, GameRoom.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract PlayerDAO playerDAO();
    public abstract GameRoomDAO gameRoomDAO();
}