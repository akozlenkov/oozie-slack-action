package ru.hh.oozie.action;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class SlackMessage implements Serializable {
    @SerializedName("text")
    private String text;

    public SlackMessage(String text){
        super();
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String ts) {
        this.text = text;
    }
}
