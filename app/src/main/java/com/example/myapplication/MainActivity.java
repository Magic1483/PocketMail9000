package com.example.myapplication;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import jakarta.mail.MessagingException;


public class MainActivity extends Activity {
    private List<EmailMessage> emailList;
    private EmailAdapter adapter;
    private ListView emailListView;



    private void getEmails(){
        // get 1st 10 emails
        SharedPreferences shared = getSharedPreferences("config",MODE_PRIVATE);

        String imap_host = shared.getString("imap_host","false");
        String email = shared.getString("email","false");
        String pass = shared.getString("password","false");
        int limit = shared.getInt("limit",10);

        if (emailList.size() == 0){
            new Thread(()->{
                List<EmailMessage> emails = new EmailHandler()
                        .getEmails(email,pass,imap_host,limit);
                Log.i("EMAIL HANDLER M",String.valueOf(emails.size()));



                runOnUiThread(() -> {
                    emailList.addAll(emails);
                    adapter.notifyDataSetChanged();

                    TextView footerText = findViewById(R.id.FooterText);
                    footerText.setText(String.format("Pocketmail 9000: %s items recieved", emailList.size() ));
                });
            }).start();
        }

    }

    private void NewMsgHandler(){
        setContentView(R.layout.write_message);


        TextView back = findViewById(R.id.back_btn);
        TextView send = findViewById(R.id.send_message);
        EditText msg_to = findViewById(R.id.msg_to);
        EditText msg_text = findViewById(R.id.WriteMessage);

        back.setOnClickListener((view) -> {
            setContentView(R.layout.activity_main);
            SetupMailList();
        });

        send.setOnClickListener((view)->{
            new Thread(()->{
//                EmailHandler eh = new EmailHandler();
//                // send msg
//                eh.SendMessage(
//                            msg_to.getText().toString(),
//                            msg_text.getText().toString(),
//                            "max-lils2013@yandex.ru",
//                            "ujrqcsyhccflxxqc");

                runOnUiThread(() -> {
                    setContentView(R.layout.activity_main);
                    SetupMailList();
                });
            }).start();
        });
    }


    public void SetupMailList(){
        // initial func

        SharedPreferences shared = getSharedPreferences("config",MODE_PRIVATE);

        String imap_host = shared.getString("imap_host","false");
        String email = shared.getString("email","false");
        String pass = shared.getString("password","false");
        Log.i("SETUP MAIL",imap_host+" "+email);
        if (imap_host.equals("false") || email.equals("false") || pass.equals("false")){
            SettingsHandler();
        } else {
            setContentView(R.layout.activity_main);

            TextView footerText = findViewById(R.id.FooterText);
            footerText.setText(String.format("Pocketmail 9000: %s items recieved", emailList.size() ));

            emailListView = findViewById(R.id.emailListView);
            emailListView.setAdapter(adapter);
            setupClickListener();

            TextView new_msg = findViewById(R.id.new_msg);
            TextView settings = findViewById(R.id.Settings);
            // new msg
            new_msg.setOnClickListener((view)->{
                NewMsgHandler();
            });
            settings.setOnClickListener((view)->{
                SettingsHandler();
            });
            getEmails(); // load emails
        }

    }



    private void SettingsHandler(){
        setContentView(R.layout.settings);
        TextView back = findViewById(R.id.settongs_back);
        TextView save = findViewById(R.id.settongs_save);

        TextView imap_view = findViewById(R.id.imap_host);
        TextView smtp_view = findViewById(R.id.smtp_host);
        TextView email_view = findViewById(R.id.email_login);
        TextView limit_view = findViewById(R.id.email_limit);
        TextView password_view = findViewById(R.id.email_password);

        SharedPreferences shared = getSharedPreferences("config",MODE_PRIVATE);
        SharedPreferences.Editor editor = shared.edit();

        imap_view.setText(shared.getString("imap_host",""));
        email_view.setText(shared.getString("email",""));
        password_view.setText(shared.getString("password",""));

        Log.i("EMAIL settings",shared.getString("imap_host","")+" "+shared.getString("email",""));

        back.setOnClickListener((view)->{
            setContentView(R.layout.activity_main);
            SetupMailList();
        });
        save.setOnClickListener((view)->{
            editor.putString("imap_host",imap_view.getText().toString());
            editor.putString("smtp_host",smtp_view.getText().toString());
            editor.putString("email",email_view.getText().toString());
            editor.putString("password",password_view.getText().toString());
            if (!limit_view.getText().toString().isEmpty()){
                Log.i("EMAIL limit", "limit not empty");
                Log.i("EMAIL limit", String.valueOf(Integer.parseInt(limit_view.getText().toString())));
                editor.putInt("limit",Integer.parseInt(limit_view.getText().toString()));
            } else {
                Log.i("EMAIL limit","use default limit 10");
                editor.putInt("limit",10);
            }

            editor.apply();
            // retrieve new messages
            emailList = new ArrayList<EmailMessage>();
            adapter = new EmailAdapter(this,emailList);

            SetupMailList();
        });
    }


    public void setupClickListener(){
        emailListView.setOnItemClickListener((parent,view,position,id) -> {
            EmailMessage email = emailList.get(position);

            setContentView(R.layout.read_message);
            TextView sender = findViewById(R.id.user_sender);
            TextView time_text = findViewById(R.id.time_sender);
            TextView subject_text = findViewById(R.id.SubjectRead);

            sender.setText(email.sender);
            time_text.setText(email.time);
            subject_text.setText(email.subject);
            TextView backBtn = findViewById(R.id.back_btn);
            WebView webView = findViewById(R.id.ReadMessageWebView);

            if (email.type.contains("html")){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Log.i("EMAIL WEBVIEW","work");
                    String encoded_html = Base64.getEncoder().encodeToString(email.message.getBytes(StandardCharsets.UTF_8));
                    webView.loadData(encoded_html,"text/html","base64");
                }
            }

            backBtn.setOnClickListener((v) -> {
                setContentView(R.layout.activity_main);
                SetupMailList();
            });
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        emailList = new ArrayList<EmailMessage>();
        adapter = new EmailAdapter(this,emailList);
        SetupMailList();


    }
}
