package Backend;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;

public class Encriptier {

    private  KeyGenerator keygenerator;
    private Cipher desCipher;

    public void Main(){
        try {
            this.keygenerator = KeyGenerator.getInstance("DES");

            this.desCipher = Cipher.getInstance("DES");//Creating object of Cipher

        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
    }


    public SecretKey GenerateKey(){
        return this.keygenerator.generateKey();
    }

    public String Encrypt(String data, SecretKey key){
        try{

            //Creating byte array to store string
            byte[] text = data.getBytes("UTF8");
            //Encrypt the text
            this.desCipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] textEncrypted = this.desCipher.doFinal(text);

            //Converting encrypted byte array to string
            return new String(textEncrypted);

        }catch (Exception  e){
            throw new RuntimeException(e);
        }
    }
    public String Decipher(String data, SecretKey key){

        try{
            //Decrypting text
            this.desCipher.init(Cipher.DECRYPT_MODE, key);
            byte[] textDecrypted = this.desCipher.doFinal(data.getBytes());
            //Converting decrypted byte array to a string
            return new String(textDecrypted);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
