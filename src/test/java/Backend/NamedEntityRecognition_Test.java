package Backend;

import org.apache.commons.lang3.ObjectUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

class NamedEntityRecognition_Test {


    private NamedEntityRecognition NER;

    private String[] texts = {
            "Hello there,\n\n\nThank you for applying to Full Stack Software Engineer - AI-First (Java, Angular).\nYour profile is currently under review.\n\n\nHere's what happens next:We're reviewing your application against the roles requirements. If your profile shows strong alignment, well share it directly with the hiring team and theyll contact you to arrange next steps.",

            "Thank you for your application. We appreciate your interest in working with us at [REDACTED] and your interest in Senior Test Manager IT/Automation.\n\nWe will review your application shortly, and get back to you as quickly as we can.\nWe wish you a wonderful day!\n\nKind regards\n[REDACTED NAME]\nRecruiter, [REDACTED]",

            "Thank you for your interest in [REDACTED]. We appreciate the time you took to explore our opportunities and complete an application.\n\nAt this time, we've decided to not move forward with your candidacy for Senior Backend Engineer - Databases Pyroscope | Sweden | Remote position, but encourage you to keep an eye on our Careers Page and follow us on LinkedIn  for future opportunities as we grow!\n\nThank you,\n\nThe [REDACTED] Recruiting Team",

            "Thank you for applying to [REDACTED]. Unfortunately [REDACTED] Applied AI Developer to a Digital Health Scale-up did not select you for further consideration. We wish you good luck with the other positions you have applied for. You are of course also very welcome to apply for an internship again next semester. The next application period is (16th July-16th August/16 December-16 January).",

            "\"Hi,\nThank you for your application for the [REDACTED] position at [REDACTED].\nWe are very pleased to inform you that we would like to invite you to a first-round case interview. The interview will be conducted with [REDACTED] board members and a few fellow applicants and will take approximately 30 minutes. Each applicant will prepare and present a part of a case individually, followed by a group discussion to develop a final solution.\n\nTIME AND DATE\n\nYour interview will take place on:\n[REDACTED DATE AND TIME]\nLocation: [REDACTED]\nPlease confirm your attendance by replying to this email no later than [REDACTED]. Due to the high number of applicants, changing interview times may be difficult, so we kindly ask you to attend your assigned time slot and be on time.\nRemember to bring your own paper and pencil. If you have any trouble getting into the building, please call [REDACTED PHONE NUMBER] or [REDACTED PHONE NUMBER] no earlier than 10 minutes before your assigned time slot.\n\nPlease feel free to reach out if you have any questions.\nWe look forward to meeting you!\nBest regards,\n\n[REDACTED NAME] and the [REDACTED] board\"",

            "Hello,\n\nYou have applied for a position as a software tester at [REDACTED]. I just wanted to check your availability. I understand that you are in your final semester and will be finishing your master's in [REDACTED]? Are you available to start immediately after that?"
    };
    @BeforeEach
    void setUp() {
        this.NER = new NamedEntityRecognition();
    }

    @Test
    void phraseMatchingTest() {
        int[] expected_results = {0, 0, 1, 1, 2, 2};
        for (int ID = 0; ID < 5; ID++) {
            try {
                int category = expected_results[ID];

                if (ID == 3) {
                    System.out.print("");
                }


                int[] score = NER.phraseMatching(this.texts[ID]);
                assertTrue(score[category] > 13);
                System.out.println("[+]Scoring: " + Arrays.toString(score));
                System.out.println("[+]Correct category should be:" + String.valueOf(category));

                for (int val : score) {
                    assertTrue(val <= score[category]);
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

    }

    @Test
    void jobTitleExtractionTest(){
        String[] titles ={"Full Stack Software Engineer","Senior Test Manager","Senior Backend Engineer","Applied AI Developer"};

        for (int i=0; i<3;i++){
            assertTrue(NER.jobTitleExtraction(texts[i]).contains(titles[i]));
        }

    }
}