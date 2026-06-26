package Backend;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class DataBase_connectionTest {

    private DataBase_connection db;

    @BeforeEach
    void setUp() {
        try {
            this.db = new DataBase_connection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void cleanup(){
        try {
            this.db.CloseConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    void querySQL_SimpleTest() {
        String query = "SELECT * FROM Companies";
        try {

            assertNotNull(this.db.QuerySQL(query));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void insertSQL() {
        String Table = "Locations";
        String Values = "9999,\"Terra\",\"Golden Throne\"";

        String query = "INSERT INTO "+Table+" VALUES("+Values+"));";
        try {
            assertTrue(this.db.InsertSQL(query));

            query = "DELETE FROM Locations WHERE ID=9999";

            assertTrue(db.InsertSQL(query));

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void dataInsertionLookup() {

        String Table = "Locations";
        String Values = "9999,\"Terra\",\"Golden Throne\"";

        String[] customQuery = {"INSERT INTO ",Table," Values (",Values,");"};

        String ID = "ID";
        String SelectQuery = String.format("SELECT COUNT(%s) FROM %s",ID,Table);

        try {
            String prevVal = this.db.QuerySQL(SelectQuery);

            assertTrue(this.db.InsertSQL(String.join("",customQuery)));

            String newVal = this.db.QuerySQL(SelectQuery);

            assertNotEquals(prevVal,newVal);


            String query = "DELETE FROM Locations WHERE ID=9999";

            assertTrue(db.InsertSQL(query));

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}