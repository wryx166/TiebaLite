package com.huanchengfly.tieba.post.models.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;


@Entity(tableName = "top_forum")
public class TopForum {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String forumId;


    public TopForum(String forumId) {
        this.forumId = forumId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getForumId() {
        return forumId;
    }

    public TopForum setForumId(String forumId) {
        this.forumId = forumId;
        return this;
    }
}
