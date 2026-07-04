package Backend;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.FlagTerm;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

public class Mail_manager {

    private Store store;


    public void establishConnection(String mail_address, String password) throws MessagingException {

        Properties props = System.getProperties();
        props.setProperty("mail.store.protocol", "imaps");

        Session session = Session.getDefaultInstance(props,null);

        this.store = session.getStore("imaps");
        store.connect("imap.gmail.com",mail_address, password);
    }

    //Count number of emails unread and total

    public int emailCount(){
        try {
            Folder inbox = this.store.getFolder("inbox"); //Get a count of all the emails and unread emails

            inbox.open(Folder.READ_ONLY);

            int num_messages = inbox.getUnreadMessageCount();
            inbox.close(true);
            return num_messages;
        }
        catch (MessagingException e){
            throw new RuntimeException(e);
        }
    }

    //Read an email
    //info stored in an array

    private String getTextFromMessage(MimeMultipart multipart){
        try {

            StringBuilder result = new StringBuilder();

            for (int i = 0; i < multipart.getCount(); i++) {

                BodyPart part = multipart.getBodyPart(i);

                //ignore the attachments and skip to the next part
                if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                    continue;
                }

                Object content = part.getContent();
                if (content instanceof String) {
                    result.append(content);
                } else if (content instanceof MimeMultipart nested) {
                    result.append(getTextFromMessage(nested));
                }
            }

            return result.toString();
        }
        catch (MessagingException| IOException e){
            throw new RuntimeException(e);
        }
    }

    //[messages ID][sender,subject,content]
    //the order is reversed the latest email will be the last in the list
    public String[][] readEmails() {
        try {

            Folder inbox = this.store.getFolder("inbox");
            //array of unread emails size = number of unread emails
            String[][] emails = new String[emailCount()][3];
            //no emails to read
            if (emails[0].length == 0) {
                return emails;
            }


            //Extract only the unread emails into a list
            inbox.open(Folder.READ_ONLY);
            Message[] messages = MailListGetter(inbox, false);

            for (int i = 0; i < messages.length; i++) {
                emails[i][0] = Arrays.toString(messages[i].getFrom());
                emails[i][1] = messages[i].getSubject();

                Object content = messages[i].getContent();
                if (content instanceof String) {
                    //System.out.println("Message: \n" + (String) content);
                    emails[i][2] = (String) content;
                }
                if (content instanceof MimeMultipart multipart) {
                    //System.out.println("Message: \n" + getTextFromMessage(multipart));
                    emails[i][2] = getTextFromMessage(multipart);
                }
            }
            inbox.close(true);

            return emails;
        }catch (MessagingException | IOException e){
        throw new RuntimeException(e);
    }

}

    //Mark an unread email as read based on array position
    public boolean markMailAsRead(int mail_ID){
        try {

            Folder inbox = this.store.getFolder("inbox");
            inbox.open(Folder.READ_WRITE);

            //find unseen messages
            Message[] messages = MailListGetter(inbox, true);
            if (messages.length >= mail_ID - 1) {
                try {
                    Message targetMessage = messages[mail_ID];
                    targetMessage.setFlag(Flags.Flag.SEEN, true);
                    return true; //return true if the change was successful

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }

            inbox.close(true);
            return false;
        }catch (MessagingException e){
            throw new RuntimeException(e);
        }
    }


    private Message[] MailListGetter(Folder inbox, boolean unread) throws MessagingException{
        return inbox.search(new FlagTerm(
                new Flags(Flags.Flag.SEEN),unread));
    }

    //Delete an email
    public boolean deleteEmail (int ID, boolean unread){
        try{
            Folder inbox = this.store.getFolder("inbox");
            inbox.open(Folder.READ_WRITE);

            Message[] message_lst = MailListGetter(inbox, unread);

            Message target_msg = message_lst[ID];

            target_msg.setFlag(Flags.Flag.DELETED, true);
            System.out.println("[+] Message: " + target_msg.getSubject() + " --DELETED--");

            inbox.close(true);

            return true;
        }catch(MessagingException e){
            return false;
        }
    }


    public boolean sendTestEmail(String email, String password) {

        try {

            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");

            Session session = Session.getInstance(
                    props, new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(
                                    email,
                                    password
                            );
                        }
                    });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(email));

            message.setFrom(new InternetAddress(email));
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(email)
            );

            message.setSubject("Test Email for API");
            message.setText("This is a test email to be deleted manually or by the algorithm.");

            Transport.send(message);

            System.out.println("[+]Test Message sent...");
            return true;
        }
        catch (MessagingException e){

            return false;
        }

    }
}
