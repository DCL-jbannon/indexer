import org.API.Odilo.OdiloAPI;
import org.API.Odilo.models.LoanablesType;
import org.API.OverDrive.IOverDriveAPIWrapper;
import org.API.OverDrive.OverDriveAPI;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by jbannon on 8/12/2014.
 */
public class OdiloAPITests {
    private OdiloAPI api;

    @Before
    public void setUp() throws Exception
    {
        this.api = new OdiloAPI("http://www.evokecolorado.org", "Monique1", "Monique1");
        this.api.login();
    }

    @Test
    public void test_getLoanables() throws Exception {
        List<String> ids = this.api.getIds();
        assert(ids.size()>0);

        Random rand = new Random();
        int len = 4;
        int ran = rand.nextInt(ids.size()-len);

        List<LoanablesType> list = this.api.getCheckoutOptionsForRecords(ids.subList(ran, ran+len));
        int ii = 0;
        assert(list.size() == len);

        ii++;
    }

    @Test
    public void test_getRecord() throws Exception {

        List<String> ids = this.api.getIds();
        assert(ids.size()>0);

        Random rand = new Random();
        int ran = rand.nextInt(ids.size());

        JSONObject res = this.api.getItemMetadata(ids.get(ran));
        assert(res != null);
        assert(res.containsKey("leader"));
    }

    @Test
    public void test_getISBNs() throws Exception {

        List<String> ids = this.api.getIds();
        assert(ids.size()>0);


        Map<String, String> map = new HashMap();

        for(String id : ids) {
            String isbn = this.api.getISBN(id);
            if(isbn != null) {
                map.put(id, isbn);
            }
        }

        int i =0;
        i++;
        //assert(res != null);
        //assert(res.containsKey("leader"));
    }



}
