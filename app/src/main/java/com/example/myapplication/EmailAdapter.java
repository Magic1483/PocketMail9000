package com.example.myapplication;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

public class EmailAdapter extends ArrayAdapter<EmailMessage> {
    private Context ctx;
    private List<EmailMessage> emails;


    public EmailAdapter(Context ctx, List<EmailMessage> emails){
        super(ctx,0,emails);
        this.ctx = ctx;
        this.emails = emails;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        EmailMessage email = getItem(position);

        if (convertView == null){
            convertView = LayoutInflater.from(ctx).inflate(R.layout.message,parent,false);
        }

        ImageView emailIcon = convertView.findViewById(R.drawable.ic_launcher_foreground);
        TextView senderText = convertView.findViewById(R.id.senderText);
        TextView messageText = convertView.findViewById(R.id.messageText);
        TextView timeText = convertView.findViewById(R.id.timeText);

        senderText.setText(email.sender);
        messageText.setText(email.subject);
        timeText.setText(email.time);


        return convertView;
    }
}


