import Backend.DataBase_connection;
import Backend.Encriptier;
import Backend.Mail_manager;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Objects;

public class Interpreter {

    private DataBase_connection connection;
    private Encriptier encriptier;
    private Mail_manager mail;
    private SecretKey cypherKey;

    public void Main(){
        //Establish objects
        try {
            this.connection = new DataBase_connection();
            this.encriptier = new Encriptier();
            this.mail = new Mail_manager();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        //Check if there is a verification key stored in the Database
        verifyKey();

    }

    private void unreadMailCheck(){

    }

    private void verifyKey(){
        /// Checks if a cipher key needs to be generated or not
        /// If there is one it will store it locally for future use if not it will
        /// generate a new one and store it in the DB
        try {
            String output = this.connection.QuerySQL("SELECT COUNT key FROM Auth;");

            if (Objects.equals(output, "0")){
                this.cypherKey = this.encriptier.GenerateKey();
                String keyString = Base64.getEncoder().encodeToString(cypherKey.getEncoded());
                this.connection.QuerySQL("INSERT INTO Auth VALUES ("+keyString+")");
            }
            else{
                String keyString =this.connection.QuerySQL("SELECT key FROM Auth;");
                byte[] decodedKey = Base64.getDecoder().decode(keyString);
                this.cypherKey = new SecretKeySpec(decodedKey, "DES");
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
