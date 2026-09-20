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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NamedEntityRecognition {

    private JSONObject data;


    public int[] phraseMatching(String original_text) throws IOException {
        //Score list
        int[] score = {0, 0, 0, 0};//confirmation, rejection, followup, JobOffer
        String[] cls = {"confirmation", "rejection", "followup", "JobOffer"};

        //Lowercase the text
        //Tokenize the text
        String[] tokens = tokenizer(original_text.toLowerCase());

        //remove punctuation
        //turn to java stream, map (transform each element), filter out if the condition is true using regex, convert a stream back into a string[]
        String[] cleaned_tokens = Arrays.stream(tokens).filter(token -> !token.matches("\\p{Punct}+")).toArray((String[]::new));

        //POST & lemmatization of text lists
        String[] tag_lst = POS_Tagger(cleaned_tokens);
        String[] lem_lst = lemmatizer(cleaned_tokens, tag_lst);

        //generate 2-6 n-grams
        //obtain score for both words and phrases

        //update the list based on engrams
        score = NgramScore(cleaned_tokens,cls, score);

        for (int i = 0; i < score.length; i++) {//go through each classification

            score[i] += wordScore(lem_lst, cls[i]);

        }
        return score;
    }

    public String jobTitleExtraction (String text){
        for (Pattern pattern : PATTERNS){
            Matcher matcher = pattern.matcher(text);

            if (matcher.find()){
                String title = cleanTitle(matcher.group(1));

                if(!title.isEmpty()){
                    return title;
                }
            }
        }
        return null;
    }
    //note that the emails can have a sender of the service like linkedin and the company name may be in the email it self

    private String cleanTitle(String title){
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

    private int[] NgramScore(String[] tokens, String[] classesLst ,int[] score) throws IOException {
        //go through the list of possible combinations and return the total scoring
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
                .filter(token -> token.matches("[a-z]+"))
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

            return posTagger.tag(tokens);///POS TAGS

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

    private static final Pattern[] PATTERNS = {
            // "application for Senior Backend Engineer position"
            Pattern.compile(
                    "(?:application|applying)\\s+(?:for|to)\\s+(?:the\\s+)?(.{2,100}?)(?:\\s+position|\\s+role)\\b",
                    Pattern.CASE_INSENSITIVE
            ),

            // "your application for Senior Backend Engineer has..."
            Pattern.compile(
                    "(?:your\\s+)?application\\s+(?:for|to)\\s+(?:the\\s+)?(.{2,100}?)(?:\\s+(?:has|was|is|will)\\b)",
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
            )
    };

}

