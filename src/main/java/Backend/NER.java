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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

//TODO make a NER make a class for email scraping
public class NER {

    //pretrained model to find people, and locations
    public String[] main_text_NER(String text, String type){
        String model_loc = "src/main/resources/Models/";
        String model_name;

        if (type.contains("person")){
            model_name = model_loc+"en-ner-person.bin";
        }else if (type.contains("location")){
            model_name = model_loc+"en-ner-location.bin";
        }else{
            throw new RuntimeException("Invalid input for 'type' variable obtained: "+type);
        }


        try {
            //loading the tokenizer model
            InputStream tokenModelIn = new FileInputStream(model_loc+"en-token.bin");
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
            for (Span span:spans){
                String entity = String.join(" ",
                        Arrays.copyOfRange(tokens,span.getStart(),span.getEnd())
                );
                results[counter]=entity;
                counter++;
            }
            tokenModelIn.close();
            nerModelIn.close();

            return results;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    public void phraseMatching (String text){

    }

    //Add more methods depending on characteristics that seem more important later on
    //create POS tags for every token in the text and return a list of these tags in the same position as in the list of
    //note that the emails can have a sender of the service like linkedin and the company name may be in the email it self


    private String[] tokenizer (String Text){
        SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
        String[] tokens = tokenizer.tokenize(Text);
        return Arrays.stream(tokens).map(token-> token.toLowerCase(Locale.ROOT))
                .filter(token -> token.matches("[a-z]+"))
                .toArray(String[]::new);
    }

    private String[][] JoinArrays(String[] arr1, String[] arr2){
        String[][] result = new String[2][arr1.length];

        for (int i=0;i<arr1.length;i++){
            result[0][i]=arr1[i];
        }
        for (int j=0;j<arr2.length;j++){
            result[0][j]=arr2[j];
        }
        return result;
    }

    //Create a list of tokens followed by their respective POS tag
    private String[][] POS_Tagger(String Text){
         ///Returns String array of Position of speech tags from the input text
         ///@param Text original text to identify POS taggs.
         /// @return String[]


        try{
            String[] tokens = this.tokenizer(Text);///tokenize, lower text & remove punctuation

            InputStream inputStreamPOSTagger= getClass().getResourceAsStream("src/main/resources/Models/en-pos-maxent.bin");
            assert inputStreamPOSTagger != null;

            POSModel posModel = new POSModel(inputStreamPOSTagger);
            POSTaggerME posTagger = new POSTaggerME(posModel);

            String[] tags = posTagger.tag(tokens);///POS TAGS

            String[] lemText = lemmatizer(tokens, tags);///Lemmatize

            return JoinArrays(tags,lemText);

        }catch (Exception e){
            throw new RuntimeException(e);
        }

    }

    private String[] lemmatizer (String[] tokens, String[] tags){
        try {
            InputStream dictLemmatizer = getClass().getResourceAsStream("src/main/resources/Models/opennlp-en-ud-ewt-lemmas-1.3-2.5.4.bin");
            assert dictLemmatizer != null;
            DictionaryLemmatizer lemmatizer = new DictionaryLemmatizer(dictLemmatizer);

            return lemmatizer.lemmatize(tokens, tags);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> ngramsCreator (String[] tokens, int min, int max){
        List <String> ngrams = new ArrayList<>();

        for (int size=min;size<=max;size++){
            for(int i=0;i<=tokens.length;i++){

                StringBuilder phrase = new StringBuilder();

                ///Add words to the phrase up to the specified limit
                for (int j=i; j<i+size;j++){
                    if(j>i){
                        phrase.append(" ");
                    }
                    phrase.append(tokens[j]);
                }
                ///add word to the list
                ngrams.add(phrase.toString());
            }

        }
    return ngrams;
}
