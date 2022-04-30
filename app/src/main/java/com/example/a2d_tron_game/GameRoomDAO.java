package com.example.a2d_tron_game;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface GameRoomDAO {

    @Query("SELECT * FROM gameroom")
    List<GameRoom> getAll();

    @Insert
    void insertAll(GameRoom... gameRooms);

    @Delete
    void delete(GameRoom gameRoom);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insertRooms(GameRoom... gameRooms);

    @Update
    public void updateRooms(GameRoom... gameRooms);
}
