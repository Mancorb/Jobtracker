import Backend.DataBase_connection;
import Backend.Mail_manager;
import Backend.NamedEntityRecognition;
import java.time.LocalDate;
import java.util.*;


public class Main {

    public static DataBase_connection DB = new DataBase_connection();
    public static Mail_manager mail = new Mail_manager();
    public static NamedEntityRecognition NER = new NamedEntityRecognition();

    public static void main(String[] args) {
        String[] credentials = checkNewUser();

        Notification("[+] Recognized data:" + credentials[0]);
        //Connect with user's email service provider
        List<String[]> emails = unreadMailCheck();

        if (emails.isEmpty()){
            categorizeMail(emails);
        }

        //Convert processed emails to 'read'
        Iterator<String[]> iterator = emails.iterator();
        while (iterator.hasNext()){
            String[] message = iterator.next();
            Main.mail.markMailAsRead(Integer.parseInt(message[0]));
        }

        DB.CloseConnection();


    }
    //Classify the filtered emails
    private static void categorizeMail(List<String[]>emails){
        //REMEMBER STRUCTURE VALUES [messageId, sender, subject, content]
        //Add the score of the subject and the text for each one
        Iterator<String[]> iterator = emails.iterator();

        while (iterator.hasNext()){
            String[] mail = iterator.next();
            try {
                int[] Score = Main.NER.phraseMatching(mail[3]);
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
                String query = String.format("UPDATE 'Companies' SET 'Last Update'=%s WHERE 'Name'=%s",
                        LocalDate.now().toString(), mail[1]);
                Main.DB.SQLCommand(query);


                String job_title = Main.NER.jobTitleExtraction(mail[3]);
                String ID = mail[1].split("\\s+")[0]+job_title;

                //update job application registry
                if (maxID != 0){
                    query = String.format("UPDATE JobApplications SET 'State'='%s',WHERE 'ID'=%s",
                                            classification,ID);

                }
                else{//new job application registry

                    Dictionary<String,String[]> ID_query = Main.DB.QuerySQL("Companies","SELECT ID WHERE Name="+mail[1]);//company name + job position

                    //ID,title,date,state,link,company ID, Description, Location ID
                    query = String.format("INSERT INTO 'JobApplications' VALUES '%s','%s','%s','Sent','','%s','%s',''",
                                            ID, job_title,LocalDate.now().toString(),ID_query.get("ID")[0],Location);
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
        if (num_mails<0){
            return new ArrayList<>();
        }
        //extract content of the emails that are unread
        String [][] inbox = Main.mail.readEmails();
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

            if (Main.DB.QuerySQL("Companies", query).isEmpty()) {
                //check if the email is relevant
                //Obtain a score for the subject

                try {
                    boolean save = false;
                    int[] scoreLst = Main.NER.phraseMatching(mail[2]);
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
            Main.DB.SQLCommand(query);
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
