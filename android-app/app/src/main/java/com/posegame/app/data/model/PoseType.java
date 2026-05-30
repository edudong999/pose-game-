package com.posegame.app.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Pose type / 动作类型
 */
public class PoseType {

    @SerializedName("type")
    private String type;

    @SerializedName("name")
    private String name;

    @SerializedName("icon")
    private String icon;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}

class PoseTypeResponse {
    @SerializedName("list")
    private List<PoseType> list;

    public List<PoseType> getList() {
        return list;
    }

    public void setList(List<PoseType> list) {
        this.list = list;
    }
}