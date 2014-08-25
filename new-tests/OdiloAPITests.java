import org.API.Odilo.OdiloAPI;
import org.API.Odilo.manualModels.GetLoanablesResponse;
import org.API.Odilo.manualModels.ReserveResponse;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.util.*;

/**
 * Created by jbannon on 8/12/2014.
 */
public class OdiloAPITests {
    private OdiloAPI api;

    @Before
    public void setUp() throws Exception
    {
        Properties p = new Properties();
        InputStream in = this.getClass().getResourceAsStream("evokeTest.properties");
        p.load(in);
        in.close();
        this.api = new OdiloAPI( p.get("url").toString(), p.get("user").toString(), p.get("pass").toString());
        this.api.login();
    }

    @Test
    public void test_getLoanables() throws Exception {
        List<String> ids = this.api.getIds();
        assert(ids.size()>0);

        Random rand = new Random();
        int len = 4;
        int ran = rand.nextInt(ids.size()-len);

        List<GetLoanablesResponse> list = this.api.getCheckoutOptionsForRecords(ids.subList(ran, ran+len));
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

        Random rand = new Random();
        int len = 4;
        int ran = rand.nextInt(ids.size()-len);

        for(String id : ids.subList(ran, ran+len)) {
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

    @Test
    public void test_makeHold() {
        List<String> ids = this.api.getIds();
        assert(ids.size()>0);

        Random rand = new Random();
        int ran = rand.nextInt(ids.size());

        String recordId = ids.get(ran);
        GetLoanablesResponse loanablesResponse = this.api.getCheckoutOptionsForRecord(recordId);
        assert(loanablesResponse != null);

        ReserveResponse rt = this.api.holdItem(recordId, loanablesResponse.getRecordId());
        assert(rt.getRecordId()!=null);
        assert(rt.getReserveId()!=null);
         int ii = 0;
    }

    @Test
    public void test_releaseAllHolds() {

        List<ReserveResponse> reserveList = this.api.getHolds();
        for(ReserveResponse reserve : reserveList) {

            this.api.releaseHold(reserve);
        }
    }


}
