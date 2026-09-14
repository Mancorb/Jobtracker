package Backend;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.junit.jupiter.api.Test;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;

class NamedEntityRecognition_Test {


    @Test
    //average time for processing a 2000 line csv 25 min
    void scoring_general (){
        NamedEntityRecognition namedEntityRecognition = new NamedEntityRecognition();

        //read general_email.csv
        int [][] totalScores = new int[2000][];

        try{
            String file = "src/main/resources/job_search_email_dataset_2000.csv";
            FileReader filereader = new FileReader(file);

            CSVReader csvReader = new CSVReaderBuilder(filereader).withSkipLines(1).build();

            List<String[]>allData = csvReader.readAll();


            int counter = 0;

            for (String[] row: allData){
                int[] tempScores_subject = namedEntityRecognition.phraseMatching(row[4]);
                int[] tempScores_content = namedEntityRecognition.phraseMatching(row[5]);
                int [] tempScoreLst = new int[4];

                if ((counter)%100 == 0){
                    System.out.println("Processing row: "+(counter)+ "  ");
                }

                for(int i=0; i<tempScores_content.length;i++){
                    tempScoreLst[i] = tempScores_subject[i]+tempScores_content[i];
                }

                totalScores[counter] = tempScoreLst;
                counter +=1;

            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try(FileWriter writer = new FileWriter("src/main/resources/results.csv")){

            for (int[] row : totalScores){
                for (int i=0; i<3; i++){
                    writer.write(Integer.toString(row[i]));

                    if (i<2){
                        writer.write(",");
                    }
                }
                writer.write("\n");

            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }



//
//    @Test
//    void main_text_NER() {
//        NER ner_obj = new NER();
//        String phrase = "Dear Linkedin,\n" +
//                "\n" +
//                "Thank you for your interest in joining our team in our offices at Vancouver and for taking the time to submit your application for App Developer at Compileit.\n" +
//                "\n" +
//                "We have successfully received your application and appreciate the effort you put into sharing your experience and qualifications with us. Our recruitment team will review your application carefully as part of our selection process.\n" +
//                "\n" +
//                "If your qualifications match our current requirements, we will contact you regarding the next steps. In the meantime, we appreciate your patience while we review all applications.\n" +
//                "\n" +
//                "Thank you again for considering a career with us. We wish you the best of luck and appreciate your interest in our organization.\n" +
//                "\n" +
//                "Kind regards,\n" +
//                "\n" +
//                "Jannet Copperson\n" +
//                "Compileit\n" +
//                "Compileit@provider.org";
//
//        String[] person_result = ner_obj.main_text_NER(phrase,"person");
//        String[] organization_result = ner_obj.main_text_NER(phrase,"location");
//
//        System.out.println(person_result[0]);
//        System.out.println(organization_result[0]);
//
//
//    }
//
//    @Test
//    void job_Extraction() {
//    }
}