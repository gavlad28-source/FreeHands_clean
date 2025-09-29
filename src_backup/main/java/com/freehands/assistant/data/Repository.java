
package com.freehands.assistant.data;

import android.content.Context;

import com.freehands.assistant.data.local.AppDatabase;
import com.freehands.assistant.data.local.SampleItem;
import com.freehands.assistant.data.local.SampleItemDao;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Repository {
    private final SampleItemDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public Repository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        dao = db.sampleItemDao();
    }

    public void insert(SampleItem item) {
        executor.execute(() -> dao.insert(item));
    }

    public List<SampleItem> getAll() {
        return dao.getAll();
    }
}
