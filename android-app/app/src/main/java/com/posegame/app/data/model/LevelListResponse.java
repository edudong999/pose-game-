package com.posegame.app.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Level list response wrapper
 */
public class LevelListResponse {

    @SerializedName("list")
    private List<Level> list;

    @SerializedName("total")
    private int total;

    public List<Level> getList() {
        return list;
    }

    public void setList(List<Level> list) {
        this.list = list;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}