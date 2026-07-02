import Backend.DataBase_connection;
import Backend.Mail_manager;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Dictionary;

public class Main {

    public static DataBase_connection connection = new DataBase_connection();
    public static Mail_manager mail = new Mail_manager();

    public static void main(String[] args){
        checkNewUser();

        connection.CloseConnection();



        //TODO process
        //1.- ask the user for access credentials email can be stored in the DB
        //2.- ask the user for app password credentials
        //3.- get all unread emails



    }

    //check if the user has registered an email address into the database.
    private static void checkNewUser(){
        Dictionary<String,String[]> dicResult = connection.QuerySQL("Auth","SELECT * FROM Auth;");
        System.out.println(Arrays.toString(dicResult.get("code")));

    }

    private void unreadMailCheck(){

    }



}
