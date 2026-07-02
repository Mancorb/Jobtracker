import Backend.DataBase_connection;
import Backend.Mail_manager;


import java.util.Dictionary;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class Main {

    public static DataBase_connection connection = new DataBase_connection();
    public static Mail_manager mail = new Mail_manager();

    public static void main(String[] args){
        String appPswd = checkNewUser();

        Notification("Recognized data:"+appPswd);

        connection.CloseConnection();





        //TODO process
        //1.- ask the user for access credentials email can be stored in the DB
        //2.- ask the user for app password credentials
        //3.- get all unread emails



    }

    //check if the user has registered an email address into the database.
    private static String checkNewUser(){
        Dictionary<String,String[]> dicResult = connection.QuerySQL("Auth","SELECT * FROM Auth;");

        if (dicResult.isEmpty()){
            Notification("New User detected, please insert credentials:");
            String address = UserInput();
            Notification("Insert App password to access email:");
            String pswd  = UserInput();

            connection.SQLCommand(String.format("INSERT INTO Auth VALUES(\"%s\",\"%s\")",address, pswd));
            return pswd;
        }
        return dicResult.get("code")[0];
    }

    private static void Notification (String content){
        System.out.print("[+]"+content);
    }

    private static String UserInput (){
        return System.console().readLine();
    }

    private void unreadMailCheck(){

    }



}
