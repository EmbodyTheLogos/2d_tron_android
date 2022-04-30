package com.example.a2d_tron_game;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface PlayerDAO {

    @Query("SELECT * FROM player")
    List<Player> getAll();

    @Query("SELECT * FROM player WHERE playerID LIKE :playerID")
    Player findByPlayerID(String playerID);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(Player... players);



    @Delete
    void delete(Player player);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insertPlayers(Player... players);

    @Update
    public void updateUsers(Player... players);
}
