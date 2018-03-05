package jaggles.util;

import jaggles.Jaggles;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

/*
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
*/

public class SendEmail {

    private static final String propertiesFileName = "smtpmail.properties";

    public static void sendLog() {

        Properties props = new Properties();

        // first check to see if there is a smtpmail.properties file in the working jar directory to overwrite the defaults in resources
        File overwritePropertiesFile = new File("./" + propertiesFileName);
        if( overwritePropertiesFile.exists() ) {
            try {
                FileInputStream fileInputStream = new FileInputStream("./" + propertiesFileName);
                props.load(fileInputStream);
                fileInputStream.close();
                ProcessLog.add("Found overriding " + propertiesFileName + " in working directory, using it.");
            } catch(Exception e) {
                ProcessLog.add("Error loading overriding " + propertiesFileName, true, e);
            }
        } else {
            try {
                InputStream resourcesPropertiesFile = Jaggles.class.getResourceAsStream("/" + propertiesFileName);
                props.load( resourcesPropertiesFile );
                resourcesPropertiesFile.close();
                ProcessLog.add("Using included resources file: " + propertiesFileName + ".  This can be overridden by including it in the jar dir.");
            } catch (Exception e) {
                ProcessLog.add("Error loading included resources file: " + propertiesFileName, true, e);
                return;
            }
        }


        /*
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        */

        Session session = Session.getInstance(props,
                new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(props.getProperty("username"), props.getProperty("password"));
                    }
                });

        try {

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress( props.getProperty("username") ));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(props.getProperty("mailto")));
            message.setSubject(props.getProperty("mailsubject") + " " + new Date());
            message.setText( String.join("\\r\\n", ProcessLog.getLog()) );

            Transport.send(message);

            System.out.println("Mail Sent");

        } catch (Exception e) {
            System.out.println("Error sending mail");
            e.printStackTrace();
        }
    }

}
