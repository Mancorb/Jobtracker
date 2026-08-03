import Backend.DataBase_connection;
import Backend.Mail_manager;
import Backend.NER;
import jakarta.mail.MessagingException;

import java.util.Scanner;


import java.util.Dictionary;


public class Main {

    public static DataBase_connection DB = new DataBase_connection();
    public static Mail_manager mail = new Mail_manager();

    public static void main(String[] args) throws MessagingException {
        String[] credentials = checkNewUser();

        Notification("[+] Recognized data:" + credentials[0]);
        //Connect with user's email service provider

        unreadMailCheck();

        DB.CloseConnection();





        //TODO process
        //DONE 1.- ask the user for access credentials email can be stored in the DB
        //DONE 2.- ask the user for app password credentials
        //DONE 3.- check for unread emails
        //extract only unread emails if they contain specific words from the wordlist
        //4.- filter company names and add them to the DB
        //sometimes the email will come from a hiring site instead of the actual company site




    }

    //check if the user has registered an email address into the database.
    private static String[] checkNewUser(){
        Dictionary<String,String[]> dicResult = DB.QuerySQL("Auth","SELECT * FROM Auth;");

        //no user found in DB register a new one
        String username,password;

        if (dicResult.isEmpty()) {//avoid user miss input
            while(true) {//ask for login until login successfull.
                try {
                    Notification("[+] New User detected, please insert email address:");
                    username = UserInput();
                    Notification("[+] Insert App password to access email:");
                    password = UserInput();

                    mail.establishConnection(username,password);
                    break;

                } catch (Exception e) {
                    Notification("[!] ERROR Something failed:\n" + e + "\n[+]Please try again...\n");
                }
            }
            //Save data only if login was successfull
            DB.SQLCommand(String.format("INSERT INTO Auth VALUES(\"%s\",\"%s\")",username, password));
            return new String[]{username, password};
        }
        else{
            try{//checked saved data if it fails delete saved credentials and ask for new ones to register
                Dictionary<String, String[]> rawOutput = DB.QuerySQL("Auth","SELECT * FROM Auth;");
                username = rawOutput.get("address")[0];
                password = rawOutput.get("code")[0];
                mail.establishConnection(username,password);
            } catch (Exception e) {
                Notification("[!] Warning saved credentials in Database are no longer valid please reinsert credentials.");
                DB.SQLCommand("DELETE FROM auth;");
                checkNewUser();
            }
        }


        return new String[] {
                dicResult.get("address")[0],
                dicResult.get("code")[0]
        };
    }

    private static void unreadMailCheck(){
        Notification("[+] Checking for unread emails");
        int num_mails = mail.emailCount();
        if (num_mails<0){
            return;
        }
        //extract content of the emails that are unread
        String [][] inbox = mail.readEmails();
        //Filter out other emails
        System.out.println("pass");
    }

    //NLP processing if it gets too big make it into a separate class

    private static String[][]workWordfilter(String[][] inbox){



        return inbox;
    }

    //Handle UI
    //----------------------------------
    private static void Notification (String content){
        System.out.println(content);
    }

    private static String UserInput (){
        String result;
        while (true){
            Scanner s = new Scanner(System.in);
            result = s.nextLine();
            if (!result.isEmpty()){
                return result;
            }
            else {
                Notification("[!] WARNING Invalid input please try again.");
            }
        }


    }
    //----------------------------------

}
