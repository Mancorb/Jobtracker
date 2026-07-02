package Backend;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Dictionary;

import static org.junit.jupiter.api.Assertions.*;

class DataBase_connectionTest {

    private DataBase_connection db;

    @BeforeEach
    void setUp() {
        this.db = new DataBase_connection();
    }

    @AfterEach
    void cleanup(){
        this.db.CloseConnection();
    }


    @Test
    void querySQL_SimpleTest() {
        String query = "SELECT * FROM Companies";

        assertNotNull(this.db.QuerySQL(query));
    }

    @Test
    void insertSQL() {
        //delete the line just in case
        db.InsertSQL("DELETE FROM Locations WHERE ID=9999");

        String query = "INSERT INTO Locations VALUES(9999,'Terra','Golden Throne');";

        assertTrue(this.db.InsertSQL(query));
        query = "DELETE FROM Locations WHERE ID=9999";
        assertTrue(db.InsertSQL(query));

    }

    @Test
    void dataInsertionLookup() {

        String Table = "Locations";
        String Values = "9999,\"Terra\",\"Golden Throne\"";

        String[] customQuery = {"INSERT INTO ",Table," Values (",Values,");"};

        String SelectQuery = String.format("SELECT * FROM %s",Table);

        Dictionary<String,String[]> prevVal, newVal;

        prevVal = this.db.QuerySQL(SelectQuery);

        assertTrue(this.db.InsertSQL(String.join("",customQuery)));

        newVal = this.db.QuerySQL(SelectQuery);

        assertNotEquals(prevVal,newVal);


        String query = "DELETE FROM Locations WHERE ID=9999";

        assertTrue(db.InsertSQL(query));

    }

}