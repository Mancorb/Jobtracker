package Backend;
import java.sql.*;
import java.util.*;

public class DataBase_connection {
    private Connection connection= null;

    public DataBase_connection(){
        try {
            String database = "src/main/resources/Database.db";
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + database);
            this.connection.setAutoCommit(false);
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    public void CloseConnection(){
        try {
            if (this.connection != null) {
                connection.close();
            }
        }catch(SQLException e){
            throw new RuntimeException(e);
        }
    }

    //Reading and writing info into the DB

    public Dictionary<String,String[]> QuerySQL (String table,String sql){

        try {

            //Get a connection and prepare the query in the appropriate format
            Statement sqlQuery = this.connection.createStatement();

            String countSQL = String.format("SELECT COUNT(*) FROM %s;",table);

            ResultSet result = sqlQuery.executeQuery(countSQL);


            Dictionary<String, String[]> dataDic = new Hashtable<>();//dictionary to store all info

            int rowCounter=0;

            //get the number of rows
            if(result.next()) {
                rowCounter = result.getInt(1);
                //if there are no results just return an empty dictionary
                if (rowCounter ==0) {
                    return dataDic;
                }
            }

            result = sqlQuery.executeQuery(sql);
            return ProcessSQLData(dataDic, result, rowCounter);
        }catch (SQLException e){
            throw new RuntimeException(e);
        }

    }


    private Dictionary<String,String[]> ProcessSQLData(Dictionary<String,String[]> dataDic,
                                                       ResultSet result,
                                                       int rowCount) throws SQLException {

        ResultSetMetaData metadata = result.getMetaData();//extract metadata from result like column info

        //Store column names in the dictionary
        String colName;
        for (int i = 1; i <= metadata.getColumnCount(); i++) {
            colName = metadata.getColumnName(i);
            dataDic.put(colName, new String[]{""});
        }


        //create a list the size of # of rows obtained and cycle through the rows to store them in a list in the dictionary



        int colCount = dataDic.size(); //convert it to a list and obtain the size of the list
        String[][] rows = new String[rowCount][colCount];

        //Make a table of the result and export the columns into the dictionary
        for(int i=0; i<rowCount;i++) {
            Enumeration<String> keys = dataDic.keys();
            result.next();
            for (int j = 0; j < colCount; j++) {
                rows[i][j] = result.getString(keys.nextElement());
            }
        }

        //Store the all the rows corresponding to this column in  a list
        Enumeration<String> keys = dataDic.keys();
        for(int i=0;i<colCount;i++){
            String[] columValsLst = new String[rowCount];
            for(int j=0; j<rowCount;j++){
                columValsLst[j] = rows[j][i];
            }
            //pass the list of the columns values in the corresponding column key of the dictionary
            dataDic.put(keys.nextElement(),columValsLst);
        }

        return dataDic;
    }

    //Insert data
    public boolean SQLCommand(String sql) {
        try{
            Statement statement = this.connection.createStatement();
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
