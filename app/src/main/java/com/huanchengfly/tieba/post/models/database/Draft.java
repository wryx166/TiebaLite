package com.huanchengfly.tieba.post.models.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "draft")
public class Draft {
    @PrimaryKey
    @NonNull
    private String hash;
    private String content;

    public Draft(@NonNull String hash, String content) {
        this.hash = hash;
        this.content = content;
    }

    @NonNull
    public String getHash() {
        return hash;
    }

    public Draft setHash(String hash) {
        this.hash = hash;
        return this;
    }

    public String getContent() {
        return content;
    }

    public Draft setContent(String content) {
        this.content = content;
        return this;
    }
}
