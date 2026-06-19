package Backend;

import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class MailManagerTest {

    private String address;
    private String password;
    private String provider;


    @BeforeEach
    void setup() {
        this.address = System.getenv("GMAIL_USER");
        this.password = System.getenv("GMAIL_APP_PASS");
        this.provider = "imap.gmail.com";
    }

    @Test
    void establishConnection() {
        Mail_manager manager = new Mail_manager();

        try {
            manager.establishConnection(this.address, this.provider, this.password);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void sendTestEmail(){
        Mail_manager manager = new Mail_manager();
        try{
            manager.establishConnection(this.address,this.provider,this.password);
            assertTrue(manager.sendTestEmail(this.address,this.password));

        }catch (MessagingException e){
            throw new RuntimeException(e);
        }
    }

    @Test
    void emailCount(){
        Mail_manager manager = new Mail_manager();
        int result =-1;
        try {
            manager.establishConnection(this.address,this.provider, this.password);
            result = manager.emailCount();
        }
        catch (MessagingException e){
            throw new RuntimeException(e);
        }
        Assertions.assertTrue(result>=0);
    }

    @Test
    void readEmails() {
        int unexpected = -1;
        int actual = 0;

        Mail_manager manager = new Mail_manager();
        try {
            manager.establishConnection(this.address,this.provider,this.password);
            String [][] emails = manager.readEmails();

            actual = emails[0].length;

            assertNotEquals(unexpected, actual);
            System.out.print("Number of emails:"+actual);

            //check the content of the emails is not empty
            for(int i=0; i<actual; i++){
                for(int j=0;j<3;j++) {
                    assertNotEquals(0, emails[i][j].length());
                }
            }


        } catch (MessagingException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void ReadEmails_latest(){
        Mail_manager manager = new Mail_manager();

        try{
            manager.establishConnection(this.address,this.provider,this.password);
            String[][]messages = manager.readEmails();

            assertNotNull(messages[0][0]);
            System.out.println("Latest email:\n");
            int id = messages.length-1;

            for(int i=0; i<messages[0].length; i++){
                System.out.println(messages[id][i]);
            }


        }catch (MessagingException | IOException e){
            throw new RuntimeException(e);
        }
    }

    @Test
    void markLastMailAsRead() {
        Mail_manager manager = new Mail_manager();

        try {
            manager.establishConnection(this.address, this.provider, this.password);
            int id = manager.emailCount()-1;
            boolean response = manager.markMailAsRead(id);
            assertTrue(response);

        }catch (MessagingException e){
            throw new RuntimeException(e);
        }
    }

    @Test
    void deleteEmail() {
        Mail_manager manager = new Mail_manager();

        try{
            manager.establishConnection(this.address,this.provider,this.password);

            //send test email to delete only if one has not been sent before
            manager.sendTestEmail(this.address, this.password);

            int init_num_msg = manager.emailCount();

            assertTrue(manager.deleteEmail(init_num_msg-1,false));

            int fin_num_msg = manager.emailCount();

            assertNotEquals(init_num_msg,fin_num_msg);


        }catch(MessagingException e){
            throw new RuntimeException(e);
        }
    }
}