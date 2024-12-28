package com.example.myapplication;

import android.view.View;

public class EmailMessage{
    public String sender;
    public String message;
    public String subject;
    public String time;
    public String type;
    public Long uid;
    
    public EmailMessage(
            String sender,
            String message,
            String subject,
            String time,
            String type,
            Long uid){
        this.sender = sender;
        this.message = message;
        this.subject = subject;
        this.time = time;
        this.type = type;
        this.uid = uid;
    }


}
