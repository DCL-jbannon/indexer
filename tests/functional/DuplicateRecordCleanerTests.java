package functional;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.Test;
import org.vufind.ConnectionProvider;
import org.vufind.DuplicateRecordCleaner;

public class DuplicateRecordCleanerTests
{
    @Test
	public void Test() {
        BasicDataSource vufindDataSource = new BasicDataSource();
        vufindDataSource.setDriverClassName("com.mysql.jdbc.Driver");
        vufindDataSource.setUsername("dclvufind");
        vufindDataSource.setPassword("DcLvf!nd");
        vufindDataSource.setUrl("jdbc:mysql://VuFindMySQL04.dcl.lan/dclvufind_prod");
        //vufindDataSource.setUrl("jdbc:mysql://VuFindMySQL02.dcl.lan/dclvufind_prod");
        vufindDataSource.setMaxWaitMillis(5000);
        vufindDataSource.setMaxTotal(50);

        Connection connection = null;
        try {
            connection = vufindDataSource.getConnection();
            DuplicateRecordCleaner.Clean(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }


    }

}