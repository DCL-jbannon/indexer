import org.API.Odilo.OdiloAPI;
import org.API.Odilo.manualModels.*;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

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
        Set<String> ids = this.api.search("*");
        assert(ids.size()>0);

        Random rand = new Random();
        int len = 4;
        int ran = rand.nextInt(ids.size()-len);

        List<GetLoanablesResponse> list = this.api.getCheckoutOptionsForRecords(new ArrayList(ids).subList(ran, ran+len));
        int ii = 0;
        assert(list.size() == len);

        ii++;
    }

    @Test
    public void test_getUpdatesSince() {
        Set<String> updatedRecords = this.api.getUpdatesSince(new DateTime().minusDays(90),
                Arrays.asList(new OdiloAPI.BoundedSearchType[]{
                        OdiloAPI.BoundedSearchType.CREATED,
                        OdiloAPI.BoundedSearchType.UPDATED}));

        Set<String> deleted = this.api.getUpdatesSince(new DateTime().minusDays(90),
                Arrays.asList(new OdiloAPI.BoundedSearchType[]{
                        OdiloAPI.BoundedSearchType.DELETED}));

        assert(updatedRecords.size()>0);
    }

    @Test
    public void test_getRecord() throws Exception {

        List<String> ids = new ArrayList(this.api.search("*"));
        assert(ids.size()>0);

        Random rand = new Random();
        int ran = rand.nextInt(ids.size());

        Record record = this.api.getRecord(ids.get(ran));


        int i = 0;
        i++;
    }

    @Test
    public void test_getISBNs() throws Exception {

        List<String> ids = new ArrayList(this.api.search("*"));
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
        List<String> ids = new ArrayList(this.api.search("*"));
        assert(ids.size()>0);

        Random rand = new Random();
        int ran = rand.nextInt(ids.size());

        String recordId = ids.get(ran);
        GetLoanablesResponse loanablesResponse = this.api.getCheckoutOptionsForRecord(recordId);
        assert(loanablesResponse != null);

        HoldResponse rt = this.api.holdItem(recordId, loanablesResponse.getRecordId());
        assert(rt.getRecordId()!=null);
         int ii = 0;
    }

    @Test
    public void test_releaseAllHolds() {

        List<HoldResponse> reserveList = this.api.getHolds();
        for(HoldResponse reserve : reserveList) {

            this.api.releaseHold(reserve);
        }
    }

    @Test
    public void test_makeCheckout() {

        List<String> ids = new ArrayList(this.api.search("*"));
        assert(ids.size()>0);

        Random rand = new Random();
        int ran = rand.nextInt(ids.size());

        String recordId = ids.get(ran);
        LoanResponse loanResponse = this.api.checkout(recordId);

        assert(loanResponse!=null);
    }

    @Test
    public void test_returnCheckouts() {

        List<String> ids = new ArrayList(this.api.search("*"));
        assert(ids.size()>0);

        Random rand = new Random();
        int ran = rand.nextInt(ids.size());

        String recordId = ids.get(ran);
        List<CheckoutInformation> checkouts = this.api.getCheckedOut();

        for(CheckoutInformation checkout : checkouts) {
            this.api.returnCheckout(checkout);
        }

        assert(checkouts!=null);
    }

    @Test
    public void test_getCheckoutHistory() {

        Object o = this.api.getCheckoutHistory();


        assert(o!=null);
    }

    @Test
    public void test_releaseHolds() {

        List<HoldResponse> reserveList = this.api.getHolds();
        for(HoldResponse reserve : reserveList) {

            this.api.releaseHold(reserve);
        }
    }
}
