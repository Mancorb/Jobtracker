package Backend;
import opennlp.tools.lemmatizer.LemmatizerME;
import opennlp.tools.lemmatizer.LemmatizerModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.SimpleTokenizer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import org.json.JSONObject;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NamedEntityRecognition {

    private JSONObject data;

    private String extractVisibleText(String html) {
        // jsoup drops <style>/<script>/<head> content, decodes entities correctly,
        // and returns only the rendered text.
        return org.jsoup.Jsoup.parse(html).text();
    }

    private String stripHistory(String text) {
        int earliest = -1;
        for (Pattern p : HISTORY_MARKERS) {
            Matcher m = p.matcher(text);
            if (m.find()) {
                if (earliest == -1 || m.start() < earliest) earliest = m.start();
            }
        }
        return (earliest == -1 ? text : text.substring(0, earliest)).stripTrailing();
    }

    public String URL_Purger(String text) {
        text = extractVisibleText(text);              // was: HTML_BLOCKS/COMMENTS/TAGS/ENTITIES/CSS chain
        text = URL_PATTERN.matcher(text).replaceAll(" ");
        text = EMAIL_ADDR_PATTERN.matcher(text).replaceAll(" ");
        text = PHONE_PATTERN.matcher(text).replaceAll(" ");
        return text;
    }

    public int[] phraseMatching(String original_text) throws IOException {
        //Score list
        int[] score = {0, 0, 0, 0};//confirmation, rejection, followup, JobOffer
        String[] cls = {"confirmation", "rejection", "followup", "JobOffer"};


        //delete any urls
        //Lowercase the text
        //Tokenize the text
        String[] tokens = tokenizer(URL_Purger(stripHistory(original_text)).toLowerCase());

        //remove URLS to reduce the number of tokens


        //remove punctuation
        //turn to java stream, map (transform each element), filter out if the condition is true using regex, convert a stream back into a string[]
        String[] cleaned_tokens = Arrays.stream(tokens).filter(token -> !token.matches("\\p{Punct}+")).toArray((String[]::new));

        //POST & lemmatization of text lists
        String[] tag_lst = POS_Tagger(cleaned_tokens);
        String[] lem_lst = lemmatizer(cleaned_tokens, tag_lst);

        //generate 2-6 n-grams
        //obtain score for both words and phrases
        for (int i = 0; i < score.length; i++) {//go through each classification

            score[i] += wordScore(lem_lst, cls[i]);

        }
        if (Arrays.stream(score).anyMatch(x -> x > 13)) {
            //update the list based on engrams
            score = NgramScore(cleaned_tokens, cls, score);
        }
        return score;
    }

    public String jobTitleExtraction(String text) {
        for (Pattern pattern : PATTERNS) {
            Matcher matcher = pattern.matcher(text);

            if (matcher.find()) {
                String title = cleanTitle(matcher.group(1));

                if (!title.isEmpty()) {
                    return title;
                }
            }
        }
        return null;
    }
    //note that the emails can have a sender of the service like linkedin and the company name may be in the email it self

    private String cleanTitle(String title) {
        title = title.replaceAll("\\s+", " ").trim();//white spaces
        title = title.replaceFirst("(?i)^(the|a|an)\\s+", "");//remove the first article like "the, a & an"
        title = title.replaceAll("[\\s:;,.!?-]+$", "");//remove punctuation
        return title.trim();
    }

    private int wordScore(String[] lemTokens, String cls) throws IOException {
        String json = Files.readString(Path.of("src/main/resources/vocabulary.json"));
        this.data = new JSONObject(json);
        JSONObject classification = data.getJSONObject(cls);
        JSONObject words = classification.getJSONObject("words");
        int score = 0;
        //check each word
        for (String lemToken : lemTokens) {
            score += words.optInt(lemToken, 0);
        }

        return score;
    }

    private void showngrams(String[] tokens) {
        for (int i = 0; i < tokens.length; i++) {
            System.out.println(tokens[i]);
        }
    }

    private int[] NgramScore(String[] tokens, String[] classesLst, int[] score) throws IOException {
        //go through the list of possible combinations and return the total scoring

        //showngrams(tokens);

        for (int size = 2; size <= 6; size++) { //make ngrams from 2 - 6 in size
            for (int i = 0; i <= tokens.length - size; i++) {

                String phrase = String.join(" ", Arrays.copyOfRange(tokens, i, i + size));

                for (int j = 0; j < score.length; j++) {//go through each classification for every ngram

                    int tempScore = phraseScore(phrase, classesLst[j]);

                    if (tempScore > 0) {
                        score[j] += tempScore;
                    }
                }
            }
        }
        return score;
    }

    private int phraseScore(String phrase, String cls) throws IOException {
        String json = Files.readString(Path.of("src/main/resources/vocabulary.json"));
        this.data = new JSONObject(json);
        JSONObject classification = data.getJSONObject(cls);//look up the category
        JSONObject phrases = classification.getJSONObject("phrases");
        return phrases.optInt(phrase, 0);
    }

    private String[] tokenizer(String Text) {
        SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
        String[] tokens = tokenizer.tokenize(Text);
        return Arrays.stream(tokens).map(token -> token.toLowerCase(Locale.ROOT))
                .filter(token -> token.matches("[a-z]{2,}")&& !NON_WORDS.contains(token))
                .toArray(String[]::new);
    }

    //Create a list of tokens followed by their respective POS tag
    private String[] POS_Tagger(String[] tokens) {
        //Returns String array of Position of speech tags from the input text
        //@param tokens tokenized text to do POS on
        //@return String[]


        try {

            InputStream inputStreamPOSTagger = getClass().getResourceAsStream("/Models/en-pos-maxent.bin");
            assert inputStreamPOSTagger != null;

            POSModel posModel = new POSModel(inputStreamPOSTagger);
            POSTaggerME posTagger = new POSTaggerME(posModel);

            return posTagger.tag(tokens);//POS TAGS

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private String[] lemmatizer(String[] tokens, String[] tags) {
        try {
            InputStream modelInput = getClass().getResourceAsStream("/Models/opennlp-en-ud-ewt-lemmas-1.3-2.5.4.bin");
            if (modelInput == null) {
                throw new RuntimeException(
                        "Could not find lemmatizer model"
                );
            }
            LemmatizerModel model = new LemmatizerModel(modelInput);
            LemmatizerME lemmatizer = new LemmatizerME(model);

            return lemmatizer.lemmatize(tokens, tags);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    // --- Seniority / level modifiers ---
    private static final String SENIORITY =
            "(?:Senior|Sr\\.?|Staff|Principal|Lead|Junior|Jr\\.?|Associate|Entry[- ]Level|Mid[- ]Level|Chief)";

    // --- Tech/domain qualifiers that commonly prefix a generic role suffix ---
    private static final String TECH_STACK =
            "(?:Python|Java(?:Script)?|TypeScript|SQL|C\\+\\+|C#|PHP|Go(?:lang)?|Kotlin|Swift|Ruby|Rust|" +
                    "React|Angular|Node\\.js|AWS|Azure|AI|ML|DevOps|Full[- ]Stack|Front[- ]End|Back[- ]End|Cloud|Cybersecurity)";

    // --- Generic role-noun suffixes, so "<qualifier> + suffix" catches combos not in the fixed phrase list ---
    private static final String GENERIC_ROLE_SUFFIX =
            "(?:Developer|Engineer|Programmer|Architect|Consultant|Specialist|Manager|Tester|Analyst)";

    // --- Fixed multi-word title phrases, spanning software and non-software fields ---
    private static final String ROLE_NOUNS =
            "(?:Software\\s+Engineer|Software\\s+Developer|Software\\s+Tester|Backend\\s+Engineer|Frontend\\s+Engineer|" +
                    "Full[- ]Stack\\s+(?:Software\\s+)?(?:Engineer|Developer)|DevOps\\s+Engineer|Site\\s+Reliability\\s+Engineer|" +
                    "Data\\s+Scientist|Data\\s+Analyst|Data\\s+Engineer|Machine\\s+Learning\\s+Engineer|" +
                    "Cybersecurity\\s+Analyst|Cloud\\s+(?:Solutions\\s+)?Architect|Network\\s+Engineer|" +
                    "QA\\s+Engineer|Quality\\s+Assurance\\s+Engineer|Test\\s+Manager|" +
                    "Product\\s+Manager|Project\\s+Manager|Program\\s+Manager|Business\\s+Analyst|" +
                    "Operations\\s+Manager|Marketing\\s+Manager|Digital\\s+Marketing\\s+Specialist|" +
                    "Sales\\s+Representative|Account\\s+Executive|Customer\\s+Success\\s+Manager|" +
                    "Financial\\s+Analyst|Accountant|Human\\s+Resources\\s+Manager|HR\\s+Specialist|" +
                    "Recruiter|Talent\\s+Acquisition\\s+Specialist|" +
                    "Registered\\s+Nurse|Nurse\\s+Practitioner|Physical\\s+Therapist|Occupational\\s+Therapist|" +
                    "Mechanical\\s+Engineer|Electrical\\s+Engineer|Civil\\s+Engineer|" +
                    "Graphic\\s+Designer|UX\\s+Designer|UI\\s+Designer|UX/UI\\s+Designer)";


    private static final Pattern[] PATTERNS = {


            // "application for Senior Backend Engineer position"
            Pattern.compile("(?:application|applying)\\s+(?:for|to)\\s+(?:the\\s+)?(.{2,100}?)(?:\\s+position\\b|\\s+role\\b|[.!?\\n]|$)",
                    Pattern.CASE_INSENSITIVE
            ),

            // "your application for Senior Backend Engineer has..."
            Pattern.compile("(?:your\\s+)?application\\s+(?:for|to)\\s+(?:the\\s+)?(.{2,100}?)(?:\\s+(?:has|was|is|will)\\b)",
                    Pattern.CASE_INSENSITIVE
            ),

            // "Senior Backend Engineer position"
            Pattern.compile(
                    "([A-Z][^.!?\\n]{2,100}?)\\s+(?:position|role)\\b",
                    Pattern.CASE_INSENSITIVE
            ),

            // "position: Senior Backend Engineer"
            Pattern.compile(
                    "(?:job\\s+)?position\\s*[:\\-]\\s*([^\\n.!?]{2,100})",
                    Pattern.CASE_INSENSITIVE
            ),

            // "role: Senior Backend Engineer"
            Pattern.compile(
                    "(?:job\\s+)?role\\s*[:\\-]\\s*([^\\n.!?]{2,100})",
                    Pattern.CASE_INSENSITIVE
            ),

            // "Application received: Senior Backend Engineer"
            Pattern.compile(
                    "application\\s+(?:received|submitted)\\s*[:\\-]\\s*([^\\n.!?]{2,100})",
                    Pattern.CASE_INSENSITIVE
            ),
            Pattern.compile(
                    "\\b((?:" + SENIORITY + "\\s+)?(?:" + TECH_STACK + "\\s+" + GENERIC_ROLE_SUFFIX +
                            "|(?:" + TECH_STACK + "\\s+)?" + ROLE_NOUNS + "))\\b",
                    Pattern.CASE_INSENSITIVE
            )
    };

    private static final Pattern[] HISTORY_MARKERS = {
            // "On Mon, 22 Sep 2026 at 14:03, John Smith <j@x.com> wrote:"
            Pattern.compile("^On .{0,120}wrote:\\s*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE),
            // "-----Original Message-----"
            Pattern.compile("^-{2,}\\s*Original Message\\s*-{2,}\\s*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE),
            // Outlook-style header block: From/Sent/To/(Cc)/Subject
            Pattern.compile("^From:\\s.+\\nSent:\\s.+\\nTo:\\s.+\\n(?:Cc:\\s.+\\n)?Subject:\\s.+", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE),
            // Outlook separator line, a long run of underscores
            Pattern.compile("^_{10,}\\s*$", Pattern.MULTILINE),
            // first '>' quoted line
            Pattern.compile("^>{1,}.*$", Pattern.MULTILINE),
            // mobile signature that often sits right before quoted text
            Pattern.compile("^Sent from my (?:iPhone|iPad|Android|Samsung).*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE),
    };

    private static final Pattern URL_PATTERN = Pattern.compile("(?i)\\b(?:https?://|ftp://|www\\.)\\S+");
    private static final Pattern EMAIL_ADDR_PATTERN = Pattern.compile("\\S+@\\S+\\.\\S+");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\w)(?:\\+?\\d{1,3}[-.\\s]?)?(?:\\(?\\d{2,4}\\)?[-.\\s]?)?\\d{3,4}[-.\\s]?\\d{3,4}(?!\\w)");

    // Tokens that are markup/CSS/font names rather than language.
    private static final Set<String> NON_WORDS = Set.of(
            "div","span","td","tr","th","tbody","thead","table","br","hr","img","src","href","alt","nbsp","amp","quot",
            "px","pt","rem","rgb","rgba","css","html","xml","doctype","meta","charset","utf","viewport","xmlns","mso",
            "webkit","moz","https","http","www","cid","mailto",
            "font","fonts","family","serif","sans","arial","helvetica","verdana","tahoma","calibri","georgia","times",
            "roman","courier","monospace","segoe","roboto","geneva","lucida","trebuchet",
            "cellpadding","cellspacing","colspan","rowspan","valign","align","bgcolor","nowrap","border",
            "padding","margin","width","height","important","inherit","solid","transparent","opacity",
            "nofollow","noreferrer","ltr","rtl");

}


