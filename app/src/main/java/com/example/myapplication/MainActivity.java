package com.example.myapplication;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;


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

        // add messages from database
        emailList.clear();
//        emailList.addAll(DatabaseHelper.getInstance(this).getRecords());

        if (NetworkUtil.isNetworkAvailable(this)){
            new Thread(()->{
                TextView footerText = findViewById(R.id.FooterText);
                List<EmailMessage> emails = EmailHandler.getInstance()
                        .getEmails(email,pass,imap_host,limit,this,footerText);
                Log.i("EMAIL HANDLER M",String.valueOf(emails.size()));


                runOnUiThread(() -> {
                    emailList.addAll(emails);
                    adapter.notifyDataSetChanged();


                    footerText.setText(String.format("Pocketmail 9000: %s items recieved", emailList.size() ));
                });
            }).start();
        } else {
            Log.i("EMAIL","no network connection");
        }


    }

    private void NewMsgHandler(Optional<String> predef_to){
        setContentView(R.layout.write_message);

        TextView back = findViewById(R.id.back_btn);
        TextView send = findViewById(R.id.send_message);
        EditText msg_to = findViewById(R.id.msg_to);
        EditText msg_subject = findViewById(R.id.msg_subject);
        EditText msg_text = findViewById(R.id.WriteMessage);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (predef_to.isPresent()){
                msg_to.setText(predef_to.get());
            }
        }

        back.setOnClickListener((view) -> {
            setContentView(R.layout.activity_main);
            SetupMailList();
        });

        send.setOnClickListener((view)->{
            if (!msg_subject.getText().toString().isBlank() && !msg_text.getText().toString().isBlank() && !msg_to.getText().toString().isBlank()){
                new Thread(()->{
                    // send msg
                    EmailHandler.getInstance().SendMessage(
                            msg_to.getText().toString(),
                            msg_text.getText().toString(),
                            msg_subject.getText().toString(),
                            this);

                    runOnUiThread(() -> {
                        setContentView(R.layout.activity_main);
                        SetupMailList();
                    });
                }).start();
            }
        });
    }


    public void SetupMailList(){
        // initial func

        SharedPreferences shared = getSharedPreferences("config",MODE_PRIVATE);

        String imap_host = shared.getString("imap_host","false");
        String imap_port = shared.getString("imap_port","false");
        String smtp_host = shared.getString("smtp_host","false");
        String smtp_port = shared.getString("smtp_port","false");
        String email = shared.getString("email","false");
        String pass = shared.getString("password","false");
        Log.i("SETUP MAIL",imap_host+" "+email);
        if (imap_host.equals("false") || email.equals("false") || pass.equals("false") || smtp_host.equals("false")
            || imap_port.equals("false") || smtp_port.equals("false")
        ){
            SettingsHandler();
        } else {
            // check db

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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    NewMsgHandler(Optional.empty());
                }
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

        TextView imap_view      = findViewById(R.id.imap_host);
        TextView imap_port_view = findViewById(R.id.imap_port);
        TextView smtp_view      = findViewById(R.id.smtp_host);
        TextView smtp_port_view = findViewById(R.id.smtp_port);

        TextView email_view     = findViewById(R.id.email_login);
        TextView limit_view     = findViewById(R.id.email_limit);
        TextView password_view  = findViewById(R.id.email_password);

        SharedPreferences shared = getSharedPreferences("config",MODE_PRIVATE);
        SharedPreferences.Editor editor = shared.edit();

        imap_view.setText(shared.getString("imap_host",""));
        imap_port_view.setText(shared.getString("imap_port",""));

        smtp_view.setText(shared.getString("smtp_host",""));
        smtp_port_view.setText(shared.getString("smtp_port",""));

        email_view.setText(shared.getString("email",""));
        password_view.setText(shared.getString("password",""));

        Log.i("EMAIL settings",shared.getString("imap_host","")+" "+shared.getString("email",""));

        back.setOnClickListener((view)->{
            setContentView(R.layout.activity_main);
            SetupMailList();
        });
        save.setOnClickListener((view)->{
            editor.putString("imap_host",imap_view.getText().toString());
            editor.putString("imap_port",imap_port_view.getText().toString());
            editor.putString("smtp_host",smtp_view.getText().toString());
            editor.putString("smtp_port",smtp_port_view.getText().toString());
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
            TextView answerBtn = findViewById(R.id.answer_btn);
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

            answerBtn.setOnClickListener((v) ->{
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    NewMsgHandler(Optional.of(email.sender));
                }
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
