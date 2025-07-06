package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.TextView;

import com.sun.mail.imap.IMAPFolder;

import jakarta.mail.Address;
import jakarta.mail.Authenticator;
import jakarta.mail.BodyPart;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.Transport;
import jakarta.mail.UIDFolder;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class EmailHandler {
    // smtp port yandex 587
    // smtp port yandex 587

    // imap port google
    // smtp port google

    private Session defaultSession;

    public  static EmailHandler instance;

    public static  synchronized EmailHandler getInstance(){
        if (instance == null){
            instance = new EmailHandler();
        }
        return  instance;
    }

    private String extractEmail(String text){
        String emailRegex = "<([^<>]+@[^<>]+)>";
        Pattern pattern = Pattern.compile(emailRegex);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()){
            String res = matcher.group(1);
            return  res;
        } else {
            return  text;
        }
    }

    private static String getHtmlContent(Part part) throws Exception {
        if (part.isMimeType("text/html")) {
            return (String) part.getContent(); // Return HTML content
        } else if (part.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) part.getContent();
            for (int i = 0; i < mimeMultipart.getCount(); i++) {
                BodyPart bodyPart = mimeMultipart.getBodyPart(i);
                String content = getHtmlContent(bodyPart); // Recursive call
                if (content != null) {
                    return content; // Return the first HTML content found
                }
            }
        }
        return null; // No HTML content found
    }

    public void SendMessage(String to, String text,String subject,Context ctx)  {
        SharedPreferences sharedPreferences = ctx.getSharedPreferences("config",Context.MODE_PRIVATE);
        Properties props = new Properties();
        props.put("mail.smtp.auth","true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",sharedPreferences.getString("smtp_host",""));
        props.put("mail.smtp.port",sharedPreferences.getString("smtp_port",""));


        String email = sharedPreferences.getString("email","");
        String password = sharedPreferences.getString("password","");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(email, password);
            }
        });
        Log.i("EMAIL SEND","session created "+email);

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(email));
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(to));
            message.setSubject(subject);

            message.setText(text);
            Log.i("EMAIL","message "+message.getSubject());

            Transport.send(message);
            Log.i("EMAIL SEND","message sended (maybe) "+text);
        } catch (Exception e) {
            Log.e("EMAIL ERROR",e.toString());
        }

    }


    private void AddMessageToDB(
            Context ctx,
            String sender,
            String data,
            String subject,
            Date timestamp,
            Long uid){
        DatabaseHelper.getInstance(ctx).AddMessage(
                sender,
                data,
                uid.toString(),
                timestamp,
                subject
        );
    }

    private void AddMessageToScreen(
            List<EmailMessage> emails,
            String sender,
            String data,
            String subject,
            Date timestamp,
            String type,
            Long uid){

        emails.add(new EmailMessage(
                sender,
                data,
                subject,
                new SimpleDateFormat("EEE MMM dd HH:mm yyyy", Locale.ENGLISH).format(timestamp),
                type,
                uid
        ));
    }


    // Mode
    // Update load while uid not in DB
    // Load load all
    public List<EmailMessage> getEmails(
            String email,
            String password,
            String imap_host,
            int limit,
            Context ctx,
            TextView status
            ){

        SharedPreferences sharedPreferences = ctx.getSharedPreferences("config",Context.MODE_PRIVATE);
        Properties props = new Properties();
        props.put("mail.store.protocol","imaps");
        props.put("mail.imaps.host",imap_host);
        props.put("mail.imaps.port",sharedPreferences.getString("imap_port",""));
        props.put("mail.imaps.ssl.enable",true);
        props.put("mail.debug",true);

        List<EmailMessage> emails = new ArrayList<>();
        DatabaseHelper databaseHelper = DatabaseHelper.getInstance(ctx);
        String last_uid = databaseHelper.getLastRecord();
        List<String> existing_uids = databaseHelper.getAllUids();
        Log.i("EMAIL HANDLER","last uid "+last_uid);


        try {
            Session session = Session.getInstance(props,null);
            Log.i("EMAIL HANDLER","session created");
            Store store = session.getStore("imaps");
            store.connect(imap_host,email,password);
            Log.i("EMAIL HANDLER","connected");

            IMAPFolder inbox = (IMAPFolder) store.getFolder("INBOX");

            inbox.open(Folder.READ_ONLY);
            Log.i("EMAIL HANDLER","always ok");



            int messageCount = inbox.getMessageCount();
            int start = Math.max(1,messageCount - limit +1);
//            Message[] messages = inbox.getMessages();
            // get last n
            Message[] messages = inbox.getMessages(start, messageCount);
            Log.i("EMAIL HANDLER","message count "+messages.length);
            status.setText("Retrieve messages");
            int counter = 1;
            for (int i = messages.length-1; i >= 0; i--){
                    // work only with html now !!
                    Message m  = messages[i];
                    String subject = m.getSubject(); // msg Subject
                    String sender = extractEmail(InternetAddress.toString(m.getFrom()));
                    String data = m.getContent().toString();
                    Date date = m.getSentDate();
                    String type = m.getContentType();
                    Long uid = inbox.getUID(m);

//                    Log.i("EMAIL HANDLER","from "+sender+" "+type);

                    status.setText(String.format("Retrieve messages %s:%s",counter,limit));
                    counter++;
                    if (existing_uids.contains(uid.toString())){
                        Log.i("EMAIL HANDLER","last uid is "+last_uid);
                        this.AddMessageToScreen(emails,sender,data,subject,date,type,uid);

                        continue;
                    }


                    if (type.contains("text/html")){
//                        Log.i("DB helper","uid "+uid.toString());
                        this.AddMessageToDB(ctx,sender,data,subject,date,uid);
                        this.AddMessageToScreen(emails,sender,data,subject,date,type,uid);

                    } else if (type.contains("multipart/")) {
                        String content = getHtmlContent(m);
                        if (content != null){
                            this.AddMessageToDB(ctx,sender,data,subject,date,uid);
                            this.AddMessageToScreen(emails,sender,data,subject,date,type,uid);
                        }
                    }
            }
            inbox.close();
            store.close();
            Log.i("DB helper","uids end "+databaseHelper.getAllUids().toString());

        } catch (Exception e){
            Log.e("EMAIL HANDLER",e.toString());
        }
        Log.i("EMAIL HANDLER","messages len "+emails.size());
        return emails;
    }

}
