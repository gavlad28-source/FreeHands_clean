
package com.freehands.assistant.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sample_items")
public class SampleItem {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String title;
    public String content;
}
