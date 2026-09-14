import Backend.DataBase_connection;
import Backend.Mail_manager;
import Backend.NamedEntityRecognition;
import jakarta.mail.MessagingException;
import java.time.LocalDate;
import java.util.*;


public class Main {

    public static DataBase_connection DB = new DataBase_connection();
    public static Mail_manager mail = new Mail_manager();
    public static NamedEntityRecognition NER = new NamedEntityRecognition();

    public static void main(String[] args) throws MessagingException {
        String[] credentials = checkNewUser();

        Notification("[+] Recognized data:" + credentials[0]);
        //Connect with user's email service provider
        List<String[]> emails = unreadMailCheck();

        if (emails.isEmpty()){
            categorizeMail(emails);
        }

        DB.CloseConnection();





        //TODO process
        //DONE 1.- ask the user for access credentials email can be stored in the DB
        //DONE 2.- ask the user for app password credentials
        //DONE 3.- check for unread emails

        //unread emails to analyze, can be filtered out by looking for specific words in the sender and in the subject section

        //DONE extract only unread emails if they contain specific words from the wordlist
        //DONE-NEEDS TESTING 4.- filter company names and add them to the DB
        //sometimes the email will come from a hiring site instead of the actual company site




    }
    //Classify the filtered emails
    private static void categorizeMail(List<String[]>emails){

    }

    //Check if the user has registered an email address into the database.
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

    private static List<String[]> unreadMailCheck(){
        ///Gets number of unread emails, extract text data
        Notification("[+] Checking for unread emails");
        int num_mails = mail.emailCount();
        if (num_mails<0){
            return new ArrayList<>();
        }
        //extract content of the emails that are unread
        String [][] inbox = mail.readEmails();
        List<String[]> inboxLst = new ArrayList<>(Arrays.asList(inbox));
        //TODO apply filters to look for corresponding job application emails
        //Filter out other emails
        return senderFilter(inboxLst);
        //extract sender

    }

    private static List<String[]> senderFilter (List<String[]> emails) {

        //REMEMBER STRUCTURE VALUES [messageId, sender, subject, content]

        Iterator<String[]> iterator = emails.iterator();

        while (iterator.hasNext()){//go through all emails
            //remove all special characters from the extracted text except '@'
            String[] mail = iterator.next();
            String sender = mail[1].replaceAll("[^\\p{L}\\p{N} @]", "");

            //ONLY REGISTER IN DB VALID EMIAL RESULTS or if they don't contain keywords like '@newsletter'
            if (sender.toLowerCase().contains(".*newsletter*.")){
                iterator.remove();
                continue;
            }

            //New company???
            String query = String.format("SELECT 'Name' FROM Companies WHERE 'NAME'= %s", sender);

            if (DB.QuerySQL("Companies", query).isEmpty()) {
                //check if the email is relevant
                //Obtain a score for the subject

                try {
                    boolean save = false;
                    int[] scoreLst = NER.phraseMatching(mail[2]);
                    for (int x=0;x<=4;x++){
                        if (scoreLst[x]>=13){
                            RegisterCompanyName(mail[1]);
                            save=true;
                            break;
                        }
                    }

                    //remove irrelevant emails from the list
                    if (!save){
                        iterator.remove();
                    }

                } catch (Exception e) {
                    Notification("[!] Error:\t" + e + "\n[!]Sender: "+sender);
                }
            }
        }
        return emails;
    }


    private static void RegisterCompanyName(String name){
            String query = String.format("INSERT INTO 'Companies' VALUES 'Name'=%s, 'Last Update'=%s",
                    name.replaceAll(".*@", ""), LocalDate.now().toString());
            DB.SQLCommand(query);
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
