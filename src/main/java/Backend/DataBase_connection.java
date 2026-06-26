package Backend;
import java.sql.*;

public class DataBase_connection {
    private Connection connection= null;

    public DataBase_connection() throws SQLException{
        String database="src/main/java/Backend/Database.db";
        this.connection = DriverManager.getConnection("jdbc:sqlite:"+database);
        this.connection.setAutoCommit(false);
    }

    public void CloseConnection() throws SQLException{
        if (this.connection != null){
            connection.close();
        }
    }

    //Reading and writing info into the DB

    public String QuerySQL (String sql) throws SQLException{

        Statement sqlQuery = this.connection.createStatement();
        ResultSet result = sqlQuery.executeQuery(sql);
        //Process output
        return result.toString();
    }

    //Insert data
    public boolean InsertSQL(String sql) throws SQLException{

        Statement statement = this.connection.createStatement();

        try{
            statement.executeUpdate(sql);
            statement.close();
            this.connection.commit();
            return true;
        }catch (Exception e){
            System.err.println(e.getClass().getName() + ": " +e.getMessage());
            return false;
        }
    }

}
