import Backend.DataBase_connection;
import Backend.Mail_manager;
import Backend.NamedEntityRecognition;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


public class Main {

    public static DataBase_connection DB = new DataBase_connection();
    public static Mail_manager mail = new Mail_manager();
    public static NamedEntityRecognition NER = new NamedEntityRecognition();
    private static final Pattern EMAIL_EXTRACT_PATTERN = Pattern.compile("[\\w.+-]+@[\\w-]+(?:\\.[\\w-]+)+");

    public static void main(String[] args) {
        String[] credentials = checkNewUser();

        Notification("[+] Recognized data:" + credentials[0]);
        //Connect with user's email service provider
        List<String[]> emails = unreadMailCheck();

        if (emails.isEmpty()){
            DB.CloseConnection();
            Notification("[+] No new emails, come back later.");
            System.exit(0);
        }
        categorizeMail(emails);

        //Convert processed emails to 'read'
        Iterator<String[]> iterator = emails.iterator();
        String[] senders = new String[emails.size()];
        String[] subjects = new String[emails.size()];
        int counter = 0;
        while (iterator.hasNext()){
             String[] temp= iterator.next();
             senders[counter] = temp[0];
             subjects[counter] = temp[1];
             counter = counter + 1;

        }
        Main.mail.markMailAsRead(senders,subjects);

        DB.CloseConnection();


    }

    //Classify the filtered emails
    private static void categorizeMail(List<String[]>emails){
        //REMEMBER STRUCTURE VALUES [sender, subject, content]
        //Add the score of the subject and the text for each one
        Iterator<String[]> iterator = emails.iterator();



        while (iterator.hasNext()){
            String[] mail = iterator.next();
            //clean the email
            mail[2] = Main.NER.URL_Purger(mail[2]);

            try {
                int[] Score = Main.NER.phraseMatching(mail[2]);
                int max = 0;
                int maxID = 0;
                for (int i=0; i<4;i++){
                    if (max<Score[i]){
                        max = Score[i];
                        maxID = i;
                    }
                    else if (max == Score[i]){
                        max = Score[i];
                        maxID = 2;//default to followup classification
                    }
                }
                // act according to the classification results
                String classification;
                if (maxID == 0){
                    classification = "confirmation";
                }else if (maxID == 1){
                    classification = "rejection";
                }else if (maxID==2){
                    classification = "followup";
                }else{
                    classification = "jobOffer";
                }

                /*
                //Extract location from email ... possibly increases run time
                String[] extractedVals = Main.NER.main_text_NER(mail[3],"location");
                */
                String Location = "";

                //update date of modification
                Matcher emailMatcher = EMAIL_EXTRACT_PATTERN.matcher(mail[0]);
                String sender = emailMatcher.find() ? emailMatcher.group() : "";

                String query = "";
                String job_title = Main.NER.jobTitleExtraction(mail[2]);
                String ID = sender+"-"+job_title;
                query = String.format("SELECT ID FROM JobApplications WHERE 'ID'='%s'",sender);
                //check if the application already is saved in the DB
                Dictionary<String,String[]> ID_query = Main.DB.QuerySQL("JobApplications",query);


                //update EXISTING job application registry
                if (ID_query.size()!=0 && ID_query.get("ID")[0]!=null && maxID!=0){
                    query = String.format("UPDATE JobApplications SET 'State'='%s',WHERE 'ID'='%s'",
                                            classification,ID);

                }
                else{//new job application registry
                    query  = String.format("SELECT ID FROM Companies WHERE Email LIKE '%%%s%%';",sender);

                    ID_query = Main.DB.QuerySQL("Companies",query);//company name + job position

                    //ID,title,date,state,link,company ID, Description, Location ID
                    query = String.format("INSERT INTO JobApplications (ID,Title,ApplicationDate,State,Company,Description) VALUES ('\"%s\"','\"%s\"','\"%s\"','\"%s\"',%s,'\"%s\"');",
                            ID, job_title,LocalDate.now().toString(),classification,ID_query.get("ID")[0],mail[2]);
                }
                Main.DB.SQLCommand(query);



            }catch(Exception e){
                Notification("[!] Error could not process email from:"+mail[1]+"\n[!]Reason: "+e);
            }

        }
    }

    //Check if the user has registered an email address into the database.
    private static String[] checkNewUser(){
        Dictionary<String,String[]> dicResult = Main.DB.QuerySQL("Auth","SELECT * FROM Auth;");

        //no user found in DB register a new one
        String username,password;

        if (dicResult.isEmpty()) {//avoid user miss input
            while(true) {//ask for login until login successfull.
                try {
                    Notification("[+] New User detected, please insert email address:");
                    username = UserInput();
                    Notification("[+] Insert App password to access email:");
                    password = UserInput();

                    Main.mail.establishConnection(username,password);
                    break;

                } catch (Exception e) {
                    Notification("[!] ERROR Something failed:\n" + e + "\n[+]Please try again...\n");
                }
            }
            //Save data only if login was successfull
            Main.DB.SQLCommand(String.format("INSERT INTO Auth VALUES(\"%s\",\"%s\")",username, password));
            return new String[]{username, password};
        }
        else{
            try{//checked saved data if it fails delete saved credentials and ask for new ones to register
                Dictionary<String, String[]> rawOutput = Main.DB.QuerySQL("Auth","SELECT * FROM Auth;");
                username = rawOutput.get("address")[0];
                password = rawOutput.get("code")[0];
                Main.mail.establishConnection(username,password);
            } catch (Exception e) {
                Notification("[!] Warning saved credentials in Database are no longer valid please reinsert credentials.");
                Main.DB.SQLCommand("DELETE FROM auth;");
                checkNewUser();
            }
        }


        return new String[] {
                dicResult.get("address")[0],
                dicResult.get("code")[0]
        };
    }

    private static List<String[]> unreadMailCheck(){
        //Gets number of unread emails, extract text data

        Notification("[+] Checking for unread emails");
        int num_mails = Main.mail.emailCount();
        if (num_mails==0){
            return new ArrayList<>();
        }
        //extract content of the emails that are unread
        String [][] inbox = Main.mail.readEmails();
        List<String[]> inboxLst = new ArrayList<>(Arrays.asList(inbox));
        //Filter out other emails
        return senderFilter(inboxLst);
        //extract sender

    }

    private static List<String[]> senderFilter (List<String[]> emails) {

        //REMEMBER STRUCTURE VALUES [sender, subject, content]

        Iterator<String[]> iterator = emails.iterator();

        while (iterator.hasNext()){//go through all emails
            //remove all special characters from the extracted text except '@'
            String[] mail = iterator.next();
            Matcher emailMatcher = EMAIL_EXTRACT_PATTERN.matcher(mail[0]);
            String sender = emailMatcher.find() ? emailMatcher.group() : "";

            //ONLY REGISTER IN DB VALID EMIAL RESULTS or if they don't contain keywords like '@newsletter' ro customer service
            if (sender.toLowerCase().contains("newsletter") || sender.toLowerCase().contains("support")) {
                iterator.remove();
                continue;
            }
            //New company???
            String query = String.format("SELECT Email FROM Companies WHERE Email= '%s'", sender);
            Dictionary<String,String[]> temp = Main.DB.QuerySQL("Companies", query);

            if (temp.size()==0 || temp.get("Email")[0]==null) {
                //check if the email is relevant
                //Obtain a score for the subject

                try {
                    //first scan evaluates only the email's subject not the main text
                    int[] scoreLst = Main.NER.phraseMatching(mail[1]);
                    //System.out.println("[]-"+mail[1]+"\nScore:"+ Arrays.toString(scoreLst));

                    int max_scr = 0;

                    for (int x=0;x<4;x++){
                        max_scr = max_scr + scoreLst[x];
                    }

                    if (max_scr >=5) {
                        RegisterCompanyName(mail[0]);
                    }else{
                        iterator.remove();//remove irrelevant emails from the list
                    }
                } catch (Exception e) {
                    Notification("[!] Error:\t" + e + "\n[!]Sender: "+sender);
                }
            }
        }
        return emails;
    }

    private static void RegisterCompanyName(String raw_name){
            raw_name = raw_name.replaceAll("[\\[\\]]", "");//delete format

            Matcher emailMatcher = EMAIL_EXTRACT_PATTERN.matcher(raw_name); // extract email address
            String email = emailMatcher.find() ? emailMatcher.group() : "";

            String name = EMAIL_EXTRACT_PATTERN.matcher(raw_name)//extract a name (if possible)
                .replaceAll("")
                .replaceAll("[<> ,]", "")
                .trim();

            if (name.isEmpty()){
                name = "-";
            }else if (name.contains("-")){
                name = name.substring(name.indexOf("-") + 1); //save all after the '-'
            }
            String query = String.format("SELECT \"Name\" FROM Companies WHERE NAME = '\"%s\"' AND NAME !='-';",name);
            Dictionary<String,String[]> temp = Main.DB.QuerySQL("Companies",query);

            if (temp.size()==0 || temp.get("Name")[0]==null){
                query = String.format("INSERT INTO Companies (Name, \"Last Update\", Email) VALUES ('\"%s\"','\"%s\"','\"%s\"')",name, LocalDate.now().toString(),email);
                Main.DB.SQLCommand(query);
            }


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
