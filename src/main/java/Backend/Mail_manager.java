package Backend;

import jakarta.mail.*;
import jakarta.mail.search.FlagTerm;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

public class Mail_manager {

    static Store establishConnection(String mail_address, String provider, String password) throws MessagingException {

        if (provider.isEmpty()){
            provider="GMAIL";
        }

        Properties props = System.getProperties();
        props.setProperty("mail.store.protocol", "imaps");

        Session session = Session.getDefaultInstance(props,null);

        Store store = session.getStore("imaps");
        store.connect(mail_address, provider, password);

        return store ;
    }

    //Count number of emails unread and total

    static void emailCount(Store store) throws MessagingException{
        Folder inbox = store.getFolder("inbox"); //Get a count of all the emails and unread emails

        inbox.open(Folder.READ_ONLY);

        System.out.println("[+] # of Messages: "+ inbox.getMessageCount());
        System.out.println("[+] # of unread Messages: "+ inbox.getUnreadMessageCount());
        inbox.close(true);
    }

    //Read an email
    //info stored in ana array
    static void readEmails(Store store) throws MessagingException{
        Folder inbox =store.getFolder("inbox");
        inbox.open(Folder.READ_ONLY);
        Message[] messages =inbox.getMessages();

        if(messages.length > 0){
           Message message = messages[0];
           System.out.println("Subject: "+message.getSubject());
           System.out.println("From: "+ Arrays.toString(message.getFrom()));
            try {
                System.out.println("Message"+message.getContent());
            } catch (IOException e) {
                System.out.println("No message found");
                throw new RuntimeException(e);
            }
        }

        inbox.close(true);
    }

    //Mark an unread email as read

    static void markLatestUnreadAsRead(Store store) throws MessagingException{
        Folder inbox = store.getFolder("inbox");
        inbox.open(Folder.READ_WRITE);

        Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
        if (messages.length> 0){
            Message latestUnreadMessage = messages [messages.length - 1];
            latestUnreadMessage.setFlag(Flags.Flag.SEEN, true);
        }

        inbox.close(true);

    }

    //Delete an email

    static void deleteEmail (Message targetmessage) throws MessagingException {
         targetmessage.setFlag(Flags.Flag.DELETED, true);
         System.out.println("[+] Message: "+targetmessage.getSubject() +" --DELETED--");
    }

}
