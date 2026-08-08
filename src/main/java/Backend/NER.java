package Backend;
import opennlp.tools.lemmatizer.DictionaryLemmatizer;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.json.JSONObject;

//TODO: test the scoring methods in the main class.
public class NER {

    private JSONObject data;

    //pretrained model to find people, and locations
    public String[] main_text_NER(String text, String type) {
        String model_loc = "src/main/resources/Models/";
        String model_name;

        if (type.contains("person")) {
            model_name = model_loc + "en-ner-person.bin";
        } else if (type.contains("location")) {
            model_name = model_loc + "en-ner-location.bin";
        } else {
            throw new RuntimeException("Invalid input for 'type' variable obtained: " + type);
        }


        try {
            //loading the tokenizer model
            InputStream tokenModelIn = new FileInputStream(model_loc + "en-token.bin");
            TokenizerModel tokenModel = new TokenizerModel(tokenModelIn);
            TokenizerME tokenizer = new TokenizerME(tokenModel);

            //tokenize the text
            String[] tokens = tokenizer.tokenize(text);

            //Load the NER model
            InputStream nerModelIn = new FileInputStream(model_name);
            TokenNameFinderModel nerModel = new TokenNameFinderModel(nerModelIn);
            NameFinderME nameFinder = new NameFinderME(nerModel);

            //Obtain results
            Span[] spans = nameFinder.find(tokens);

            //save results in array
            String[] results = new String[spans.length];
            int counter = 0;
            for (Span span : spans) {
                String entity = String.join(" ",
                        Arrays.copyOfRange(tokens, span.getStart(), span.getEnd())
                );
                results[counter] = entity;
                counter++;
            }
            tokenModelIn.close();
            nerModelIn.close();

            return results;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

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

        for (int i = 0; i < score.length; i++) {//go through each classification
            score = NgramScore(cleaned_tokens, cls[i], score);
            score[i] += wordScore(lem_lst, cls[i]);

        }
        return score;
    }
    
    //note that the emails can have a sender of the service like linkedin and the company name may be in the email it self

    private int wordScore(String[] lemTokens, String cls) throws IOException {
        String json = Files.readString(Path.of("src/main/resources/vocabulary.json"));
        this.data = new JSONObject(json);
        JSONObject classification = data.getJSONObject(cls);
        JSONObject words = classification.getJSONObject("words");
        int score = 0;
        //check each word
        for (int i = 0; i < lemTokens.length; i++) {
            score += words.optInt(lemTokens[i], 0);
        }

        return score;
    }

    private int[] NgramScore(String[] tokens, String classification, int[] score) throws IOException {
        //go through the list of possible combinations and return the total scoring
        for (int size = 2; size <= 6; size++) { //make ngrams from 2 - 6 in size
            for (int i = 0; i <= tokens.length - size; i++) {

                String phrase = String.join(" ", Arrays.copyOfRange(tokens, i, i + size));

                for (int cls = 0; cls < score.length; cls++) {//go through each classification for every ngram

                    int tempScore = phraseScore(phrase, classification);

                    if (tempScore > 0) {
                        score[cls] += tempScore;
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
        ///Returns String array of Position of speech tags from the input text
        ///@param tokens tokenized text to do POS on
        /// @return String[]


        try {

            InputStream inputStreamPOSTagger = getClass().getResourceAsStream("src/main/resources/Models/en-pos-maxent.bin");
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
            InputStream dictLemmatizer = getClass().getResourceAsStream("src/main/resources/Models/opennlp-en-ud-ewt-lemmas-1.3-2.5.4.bin");
            assert dictLemmatizer != null;
            DictionaryLemmatizer lemmatizer = new DictionaryLemmatizer(dictLemmatizer);

            return lemmatizer.lemmatize(tokens, tags);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

