
package com.freehands.assistant.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SampleItemDao {
    @Insert
    long insert(SampleItem item);

    @Query("SELECT * FROM sample_items")
    List<SampleItem> getAll();
}
