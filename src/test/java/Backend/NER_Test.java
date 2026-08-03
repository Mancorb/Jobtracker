package Backend;

import org.junit.jupiter.api.Test;

class NER_Test {


    @Test
    void main_text_NER() {
        NER ner_obj = new NER();
        String phrase = "Dear John Doe,\n" +
                "\n" +
                "Thank you for your interest in joining our team in our offices at Vancouver and for taking the time to submit your application for App Developer at Compileit.\n" +
                "\n" +
                "We have successfully received your application and appreciate the effort you put into sharing your experience and qualifications with us. Our recruitment team will review your application carefully as part of our selection process.\n" +
                "\n" +
                "If your qualifications match our current requirements, we will contact you regarding the next steps. In the meantime, we appreciate your patience while we review all applications.\n" +
                "\n" +
                "Thank you again for considering a career with us. We wish you the best of luck and appreciate your interest in our organization.\n" +
                "\n" +
                "Kind regards,\n" +
                "\n" +
                "Jannet Copperson\n" +
                "Compileit\n" +
                "Compileit@provider.org";

        String[] person_result = ner_obj.main_text_NER(phrase,"person");
        String[] organization_result = ner_obj.main_text_NER(phrase,"location");

        System.out.println(person_result[0]);
        System.out.println(organization_result[0]);


    }

    @Test
    void job_Extraction() {
    }
}