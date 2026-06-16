package Backend;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DataBase_connection {
    private final Connection connection;

    public DataBase_connection() throws SQLException{
        String database="src/main/java/Backend/Database.db";
        connection = DriverManager.getConnection("jdbc:sqlite:"+database);
    }

    public Connection ConnectionGetter(){
        return connection;
    }

    public void close() throws SQLException{
        if (connection != null){
            connection.close();
        }
    }

    //Reading and writing info into the DB
}
