package com.example.myapplication;

import android.util.Log;

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
import java.util.Properties;



public class EmailHandler {
//    private static String IMAP = "imap.yandex.ru";
    private static String SMTP = "smtp.yandex.ru";
    private static String IMAP_PORT = "993";
    private static String SMTP_PORT = "465";




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

    public void SendMessage(String to, String text,String email,String password)  {
        Properties props = new Properties();
        props.put("mail.smtp.auth","true");
        props.put("mail.smtp.starttls","true");
        props.put("mail.smtp.host",SMTP);
        props.put("mail.smtp.port",SMTP_PORT);

        Session session = Session.getDefaultInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(email,password);
            }
        });
        Log.i("EMAIL SEND","session created "+email);

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(email));
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse("sashafurancev@gmail.com"));
            message.setSubject("Test mod");

            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setContent("tet text", "text/html; charset=utf-8");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(mimeBodyPart);

            Transport.send(message);
            Log.i("EMAIL SEND","message sended (maybe) "+text);
        } catch (Exception e) {
            Log.e("EMAIL ERROR",e.toString());
        }



    }

    public List<EmailMessage> getEmails(String email,String password,String imap_host,int limit){

        Properties props = new Properties();
        props.put("mail.store.protocol","imaps");
        props.put("mail.imaps.host",imap_host);
        props.put("mail.imaps.port",IMAP_PORT);
        props.put("mail.imaps.ssl.enable",true);
        props.put("mail.debug",true);

        List<EmailMessage> emails = new ArrayList<>();
        try {
            Session session = Session.getDefaultInstance(props,null);
            Log.i("EMAIL HANDLER","session created");
            Store store = session.getStore("imaps");
            store.connect(imap_host,email,password);
            Log.i("EMAIL HANDLER","connected");

            IMAPFolder inbox = (IMAPFolder) store.getFolder("INBOX");

            inbox.open(Folder.READ_ONLY);
            Log.i("EMAIL HANDLER","always ok");



            // get last 10 messages
            int messageCount = inbox.getMessageCount();
            int start = Math.max(1,messageCount - limit +1);
            Message[] messages = inbox.getMessages(start,messageCount);

            for (int i = messages.length-1; i >= 0; i--){
                    // work only with html now !!
                    Message m  = messages[i];
                    String subject = m.getSubject(); // msg Subject
                    String sender = InternetAddress.toString(m.getFrom());
                    String data = m.getContent().toString();
                    Date date = m.getSentDate();
                    String type = m.getContentType();
                    Long uid = inbox.getUID(m);

                    Log.i("EMAIL HANDLER","from "+sender+" "+type);

                    if (type.contains("text/html")){
                        emails.add(new EmailMessage(
                                sender,
                                data,
                                subject,
                                new SimpleDateFormat("HH:mm").format(date),
                                type,
                                uid
                        ));
                    } else if (type.contains("multipart/")) {
                        String content = getHtmlContent(m);
                        if (content != null){
                            emails.add(new EmailMessage(
                                    sender,
                                    content,
                                    subject,
                                    new SimpleDateFormat("HH:mm").format(date),
                                    "text/html",
                                    uid
                            ));
                        }
                    }
            }
            inbox.close();
            store.close();

        } catch (Exception e){
            Log.e("EMAIL HANDLER",e.toString());
        }
        Log.i("EMAIL HANDLER","messages len "+emails.size());
        return emails;
    }

}
