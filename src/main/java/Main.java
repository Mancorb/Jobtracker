import Backend.DataBase_connection;
import Backend.Mail_manager;
import jakarta.mail.MessagingException;

import java.util.Arrays;
import java.util.Scanner;


import java.util.Dictionary;


public class Main {

    public static DataBase_connection DB = new DataBase_connection();
    public static Mail_manager mail = new Mail_manager();

    public static void main(String[] args){
        String[] credentials = checkNewUser();

        if (credentials.length==0 || credentials[0]==null){
            System.out.println("[!] ERROR no user credentials found!!!\nSHUTTING DOWN...");
            System.exit(0);
        }
        Notification("Recognized data:" + credentials[0]);
        //Connect with user's email service provider
        try {
            mail.establishConnection(credentials[0],credentials[1]);
            unreadMailCheck();
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }

        DB.CloseConnection();





        //TODO process
        //DONE 1.- ask the user for access credentials email can be stored in the DB
        //DONE 2.- ask the user for app password credentials
        //DONE 3.- check for unread emails
        //4.- filter company names and add them to the DB



    }

    //check if the user has registered an email address into the database.
    private static String[] checkNewUser(){
        Dictionary<String,String[]> dicResult = DB.QuerySQL("Auth","SELECT * FROM Auth;");
        String[] DB_result = new String[2];

        //no user found in DB register a new one
        if (dicResult.isEmpty()){
            Notification("New User detected, please insert email address:");
            DB_result[0] = UserInput();
            Notification("Insert App password to access email:");
            DB_result[1]  = UserInput();

            DB.SQLCommand(String.format("INSERT INTO Auth VALUES(\"%s\",\"%s\")",
                    DB_result[0], DB_result[1]));
            return DB_result;

        }
        return new String[] {
                dicResult.get("address")[0],
                dicResult.get("code")[0]
        };
    }

    private static void Notification (String content){
        System.out.println("[+]"+content);
    }

    private static String UserInput (){
        Scanner s = new Scanner(System.in);
        return s.nextLine();
    }

    private static void unreadMailCheck(){
        int num_mails = mail.emailCount();
        if (num_mails<0){
            return;
        }
        //extract content of the emails that are unread
        String [][] inbox = mail.readEmails();
        System.out.println("[+]Senders:");

        for (int i=0; i< inbox.length;i++){
            System.out.println(
                    String.format("[%d] %s\n%s\n-----------------",
                            i,inbox[i][0],inbox[i][1])
            );
        }
    }



}
