package org.API.Odilo;

import org.API.Odilo.manualModels.*;
import org.eclipse.persistence.jaxb.rs.MOXyJsonProvider;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.*;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Future;

public class OdiloAPI
{
    final static Logger logger = LoggerFactory.getLogger(OdiloAPI.class);

    private Client client = null;
    private String url;
    private String user;
    private String pass;
    private OdiloResponseFilter filter;


	public OdiloAPI(String url, String user, String pass)
	{
        this.url = url;
		this.user = user;
		this.pass = pass;
	}
	
	public boolean login()
	{
        filter = new OdiloResponseFilter();
        client = ClientBuilder.newBuilder()
            .register(filter)
            .register(MOXyJsonProvider.class)
            .build();

        WebTarget loginTarget = client.target(url + "/rest/v1/UserService/login")
            .queryParam("user", user)
            .queryParam("pass", pass);
        //Don't use getBuilder(); we don't want the cookie
        Invocation.Builder invocationBuilder = loginTarget.request(MediaType.APPLICATION_JSON_TYPE);
        Response response = invocationBuilder.get(); //Cookie should now be set through filter, response just for debug

        logger.info("Logged into Odilo", response);

        return true;
	}

    private Invocation.Builder applyCookies(Invocation.Builder builder) {
        for(Cookie cookie: this.filter.getCoookies().values()) {
            builder = builder.cookie(cookie);
        }
        return builder;
    }

	public Set<String> getAllIds()
	{
        return search("*");
	}

    public static enum BoundedSearchType {
        CREATED,
        UPDATED,
        DELETED
    }

    public Set<String> getUpdatesSince(DateTime since, List<BoundedSearchType> searchTypes) {
        return getUpdatesSince(since, searchTypes, 0);
    }

    private DateTimeFormatter getUpdateDateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm'Z'");
    public Set<String> getUpdatesSince(DateTime since, List<BoundedSearchType> searchTypes, int offset)
    {
        String status = null;
        for(BoundedSearchType type :searchTypes) {
            status = (status == null) ? type.name() : status + "|" + type.name();
        }
        WebTarget target = client.target(url + "/rest/v1/SearchService/BoundedSearch")
                .queryParam("ini", getUpdateDateFormatter.print(since))
                .queryParam("end", getUpdateDateFormatter.print(new DateTime()))
                .queryParam("status", status)
                .queryParam("offset", offset)
                .queryParam("limit", 1000);
        Invocation.Builder invocationBuilder = getBuilder(target);
        Response response = invocationBuilder.get();
        Object jResponse = JSONValue.parse(response.readEntity(String.class));
        Set<String> ret = new HashSet<>();
        if(jResponse instanceof JSONObject) {
            JSONObject jObject = (JSONObject) jResponse;
            Object v = jObject.get("recordId");
            if(v instanceof JSONArray) {
                JSONArray recordArr = (JSONArray)v;
                for(Object o: recordArr) {
                    ret.add(o.toString());
                }
            }
        }
        if(ret.size()>=1000) {
            ret.addAll(getUpdatesSince(since, searchTypes, offset+ret.size()));
        }
        return ret;
    }

    public Set<String> search(String query) {
        return search(query, 0);
    }

    /**
     * For tests
     * @return
     */
    public Set<String> search(String query, int offset)
    {
        WebTarget target = client.target(url + "/rest/v1/SearchService/Search")
                .queryParam("Query", query)
                .queryParam("offset", offset)
                .queryParam("limit", 1000);
        Invocation.Builder invocationBuilder = getBuilder(target);
        Response response = invocationBuilder.get();
        Object jResponse = JSONValue.parse(response.readEntity(String.class));
        Set<String> ret = new HashSet<>();
        if(jResponse instanceof JSONObject) {
            JSONObject jObject = (JSONObject) jResponse;
            Object v = jObject.get("recordId");
            if(v instanceof JSONArray) {
                JSONArray recordArr = (JSONArray)v;
                for(Object o: recordArr) {
                    ret.add(o.toString());
                }
            }
        }
        if(ret.size()>=1000) {
            ret.addAll(search(query, offset + ret.size()));
        }
        return ret;
    }
	
	public Record getRecord(String odiloId)
	{
        WebTarget target = client.target(url+ "/rest/v1/RecordService/Get_Record")
            .queryParam("recordId", odiloId);
        Invocation.Builder invocationBuilder = getBuilder(target, MediaType.APPLICATION_XML_TYPE);
        Response response = invocationBuilder.get();

        //Object jResponse = response.readEntity(String.class);

        Record record = getRecordFromString(response.readEntity(String.class));
        record.setExternalId(odiloId);

        return record;
	}

    private XPathFactory xPathfactory = XPathFactory.newInstance();
    private XPath xpath = xPathfactory.newXPath();

    synchronized private Record getRecordFromString(String xmlString) {
        Document doc = getXMLDocumentFromString(new InputSource(new StringReader(xmlString)));
        XPathExpression isbnX = null;
        XPathExpression titleX = null;
        XPathExpression subtitleX = null;
        XPathExpression descriptionX = null;
        XPathExpression authorX = null;
        XPathExpression subjectX = null;
        XPathExpression publisherX = null;
        XPathExpression editionX = null;
        XPathExpression targetAudienceX = null;
        XPathExpression publishDateX = null;

        try {
            isbnX = xpath.compile("/record/datafield[@tag='020']/subfield[@code='a']");
            titleX = xpath.compile("/record/datafield[@tag='245']/subfield[@code='a']");
            subtitleX = xpath.compile("/record/datafield[@tag='245']/subfield[@code='b']");
            descriptionX = xpath.compile("/record/datafield[@tag='520']/subfield[@code='a']");
            authorX = xpath.compile("/record/datafield[@tag='245']/subfield[@code='c']");
            subjectX = xpath.compile("(/record/datafield[@tag='650']/subfield[@code='a'])[1]");
            publisherX = xpath.compile("(/record/datafield[@tag='260']/subfield[@code='b'])[1]");
            editionX = xpath.compile("(/record/datafield[@tag='250']/subfield[@code='a'])[1]");
            publishDateX = xpath.compile("(/record/datafield[@tag='260']/subfield[@code='c'])[1]");
            //TargetAudience hasn't been tested because none of the Odilo mrc seems to have this field yet,
            //but it should work if available
            targetAudienceX = xpath.compile("(/record/datafield[@tag='521']/subfield)[1]");
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
        try {
            String isbn = cleanupMrcString((String)isbnX.evaluate(doc, XPathConstants.STRING));
            String description = (String)descriptionX.evaluate(doc, XPathConstants.STRING);
            String author = cleanupMrcString((String) authorX.evaluate(doc, XPathConstants.STRING));
            String subject = cleanupMrcString((String) subjectX.evaluate(doc, XPathConstants.STRING));
            String title = cleanupMrcString((String) titleX.evaluate(doc, XPathConstants.STRING));
            String subtitle = cleanupMrcString((String) subtitleX.evaluate(doc, XPathConstants.STRING));
            String language = "English"; //WTH? It seems like Marc doesn't track language
            String publisher = cleanupMrcString((String) publisherX.evaluate(doc, XPathConstants.STRING));
            String edition = cleanupMrcString((String) editionX.evaluate(doc, XPathConstants.STRING));
            String targetAudience = cleanupMrcString((String) targetAudienceX.evaluate(doc, XPathConstants.STRING));
            String publishDate = cleanupMrcString((String) publishDateX.evaluate(doc, XPathConstants.STRING));
            if(publishDate.startsWith("c")) {
                publishDate = publishDate.substring(1);
            }
            //String externalId = recordId;//TODO let's set externalId outside of this function

            return new Record(isbn, description, author, subject, title, subtitle, language, publisher, edition,
                    targetAudience, publishDate);

        } catch (XPathExpressionException e) {
            logger.error("Could not extract from Odilo Marc", e);
        }

        return null;
    }

    private String cleanupMrcString(String in) {
        in = in.trim();
        if(in.length()<1) {
            return "";
        }

        char last = in.charAt(in.length() - 1);
        switch(last) {
            case '.':
            case ',':
            case '/':
                return cleanupMrcString(in.substring(0, in.length()-1));
        }
        return in;
    }

    private DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private DocumentBuilder builder = null;

    synchronized private Document getXMLDocumentFromString(InputSource xmlSource) {
        if(builder == null) {
            try {
                builder = factory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                logger.error("Could not make DocumentBuilder", e);
            }
        }

        try {
            return builder.parse(xmlSource);
        } catch (SAXException | IOException e) {
            logger.error("Could not parse XMLDocument", e);
        }

        return null;
    }

    public String getISBN(String odiloId)
    {
        WebTarget target = client.target(url+ "/rest/v1/RecordService/Get_Record")
                .queryParam("recordId", odiloId);
        Invocation.Builder invocationBuilder = getBuilder(target);

        Response response = invocationBuilder.get();
        Object jResponse = JSONValue.parse(response.readEntity(String.class));
        if(jResponse instanceof  JSONObject) {
            Object data = ((JSONObject)jResponse).get("datafield");
            if(data instanceof JSONArray) {
                JSONArray jsonData = (JSONArray) data;
                if(jsonData != null) {
                    for(Object o : jsonData.toArray()) {
                        if(o instanceof JSONObject) {
                            JSONObject field = (JSONObject)o;
                            if(field.get("@tag").toString().equals("020")) {
                                o = field.get("subfield");
                                if(o instanceof JSONObject) {
                                    o = ((JSONObject)o).get("$");
                                    String s = o.toString();
                                    if(s.length()==13) {
                                        try {
                                            Long.parseLong(s);
                                            return s;
                                        } catch(NumberFormatException e) {
                                            return null;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public HoldResponse holdItem(String recordId, String loanableId) {
        WebTarget target = client.target(url+ "/rest/v1/LoanService/New_Hold")
                .queryParam("recordId", recordId);
                //.queryParam("loanableId", loanableId);

        Invocation.Builder invocationBuilder = getBuilder(target, MediaType.APPLICATION_JSON_TYPE);

        //ResponseType response = invocationBuilder.get(String.class);
        String response = invocationBuilder.get(String.class);
        try {
            Object o = JSONValue.parse(response);
            if(o instanceof JSONObject) {
                JSONObject jsonResp = (JSONObject) o;
                jsonResp = (JSONObject)jsonResp.get("response");
                return new HoldResponse(
                        jsonResp.get("recordId").toString(),
                        jsonResp.get("holdId").toString(),
                        jsonResp.get("status").toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<HoldResponse> getHolds() {
        WebTarget target = client.target(url + "/rest/v1/LoanService/Get_Holds");
        //.queryParam("loanableId", loanableId);

        Invocation.Builder invocationBuilder = getBuilder(target, MediaType.APPLICATION_JSON_TYPE);

        //ResponseType response = invocationBuilder.get(String.class);
        String response = invocationBuilder.get(String.class);
        try {
            Object o = JSONValue.parse(response);
            if(o instanceof JSONObject) {
                JSONObject jsonResp = (JSONObject) o;
                jsonResp = (JSONObject)jsonResp.get("response");
                List<HoldResponse> l = new ArrayList();
                Object reservesO = jsonResp.get("holds");
                if(reservesO instanceof JSONArray){
                    JSONArray jsonArray = (JSONArray) reservesO;
                    for(Object oo : jsonArray) {
                        if(oo instanceof JSONObject) {
                            JSONObject reserveJO = (JSONObject) oo;
                            l.add( new HoldResponse(
                                    reserveJO.get("recordId").toString(),
                                    reserveJO.get("holdId").toString(),
                                    reserveJO.get("status").toString()) );
                        }
                    }
                }

                return l;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public void releaseHold(HoldResponse reserve) {
        WebTarget target = client.target(url + "/rest/v1/LoanService/Remove_Hold")
            .queryParam("holdId", reserve.getHoldId());

        Invocation.Builder invocationBuilder = getBuilder(target, MediaType.APPLICATION_JSON_TYPE);

        //ResponseType response = invocationBuilder.get(String.class);
        String response = invocationBuilder.get(String.class);
        int i = 0;
        i++;
    }

    public LoanResponse checkout(String recordId) {
        WebTarget target = client.target(url + "/rest/v1/LoanService/New_Loan")
            .queryParam("recordId", recordId);

        Invocation.Builder invocationBuilder = getBuilder(target, MediaType.APPLICATION_JSON_TYPE);

        //ResponseType response = invocationBuilder.get(String.class);
        String response = invocationBuilder.get(String.class);
        try {
            Object o = JSONValue.parse(response);
            if(o instanceof JSONObject) {
                JSONObject jsonResp = (JSONObject) o;
                jsonResp = (JSONObject)jsonResp.get("response");
                List<URL> l = new ArrayList();
                Object urlsO = jsonResp.get("urlsDownload");
                if(urlsO instanceof JSONArray){
                    JSONArray urlsJA = (JSONArray) urlsO;
                    for(Object urlO : urlsJA) {
                        if(urlO instanceof String) {
                            URL url = new URL((String) urlO);
                            l.add(url);
                        }
                    }
                }

                return new LoanResponse(recordId, l);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<CheckoutInformation> getCheckedOut() {
        WebTarget target = client.target(url + "/rest/v1/LoanService/Get_Active_Loans");

        Invocation.Builder invocationBuilder = getBuilder(target, MediaType.APPLICATION_JSON_TYPE);

        //ResponseType response = invocationBuilder.get(String.class);
        String response = invocationBuilder.get(String.class);
        List<CheckoutInformation> checkouts = new ArrayList<>();
        try {
            Object o = JSONValue.parse(response);
            if(o instanceof JSONObject) {
                JSONObject jsonResp = (JSONObject) o;
                jsonResp = (JSONObject)jsonResp.get("response");
                List<URL> l = new ArrayList();
                Object loanO = jsonResp.get("loan");
                if(loanO instanceof JSONObject){
                    JSONObject loanJO = (JSONObject) loanO;
                    checkouts.add(new CheckoutInformation(
                            loanJO.get("recordId").toString(),
                            loanJO.get("loanId").toString(),
                            new DateTime(loanJO.get("startDate")),
                            new DateTime(loanJO.get("endDate"))
                            ));
                }

                //return new LoanResponse(recordId, l);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return checkouts;
    }

    public void returnCheckout(CheckoutInformation checkoutInformation) {
        WebTarget target = client.target(url + "/rest/v1/LoanService/Return_Loan")
                .queryParam("loanId", checkoutInformation.getLoanId());

        Invocation.Builder invocationBuilder = getBuilder(target, MediaType.APPLICATION_JSON_TYPE);

        //ResponseType response = invocationBuilder.get(String.class);
        String response = invocationBuilder.get(String.class);

        int i = 0;
        i++;
    }

    public List<CheckoutInformation> getCheckoutHistory() {
        WebTarget target = client.target(url + "/rest/v1/LoanService/Get_Historical_Loans");

        Invocation.Builder invocationBuilder = getBuilder(target, MediaType.APPLICATION_JSON_TYPE);

        //ResponseType response = invocationBuilder.get(String.class);
        String response = invocationBuilder.get(String.class);
        List<CheckoutInformation> checkouts = new ArrayList<>();
        //TODO

        return checkouts;
    }

    public GetLoanablesResponse getCheckoutOptionsForRecord(String recordId)
    {
        List<GetLoanablesResponse> l = getCheckoutOptionsForRecords(Arrays.asList(recordId));
        if(l.size()>0) {
            return l.get(0);
        } else {
            return null;
        }

    }

    public List<GetLoanablesResponse> getCheckoutOptionsForRecords(List<String> recordIds)
    {
        final List<GetLoanablesResponse> ret = Collections.synchronizedList(new ArrayList<>());

        final List<String> responseString = Collections.synchronizedList(new ArrayList<>());
        final List<Integer> counter = Collections.synchronizedList(new ArrayList<>());
        counter.add(0);

        /*InvocationCallback callback = new InvocationCallback<String>() {
            @Override
            public void completed(String response) {
                counter.set(0, counter.get(0)+1);
                responseString.add(response);
                try {
                    JSONObject retJO = (JSONObject) ((JSONObject) JSONValue.parse(response)).get("loanable");
                    responseString.add("Got retJO");

                    List<String> types = new ArrayList();
                    Object typesO = retJO.get("types");
                    if(typesO instanceof JSONArray) {
                        JSONArray typesJA = (JSONArray) typesO;
                        for(Object o : typesJA ) {
                            types.add(o.toString());
                        }
                    } else {
                        types.add(typesO.toString());
                    }

                    GetLoanablesResponse glResponse = new GetLoanablesResponse(
                            retJO.get("recordId").toString(),
                            types);
                    ret.add(glResponse);
                } catch (Exception e) {
                    responseString.add(e.getMessage());
                    e.printStackTrace();
                }
            }

            @Override
            public void failed(Throwable throwable) {
                System.out.println("Invocation failed.");
                throwable.printStackTrace();
            }
        }; */

        List<Future> futures = new ArrayList<Future>();

        for(String recordId : recordIds) {
            WebTarget target = client.target(url+ "/rest/v1/RecordService/Get_Loanables")
                    .queryParam("recordId", recordId);

            Invocation.Builder invocationBuilder = getBuilder(target, MediaType.APPLICATION_JSON_TYPE);
            if(false) {
                //ARGHGHGH! freakin frustrating. Something is messing up the async calls, but we can't seem to catch
                //the exception.
                invocationBuilder.get(String.class);
                //ret.add(lt);
            } else {
                futures.add(callFromBuilder(invocationBuilder, HttpMethod.GET));
            }
        }

        //finish them up
        for(Future f : futures) {
            try {
                Object resO = f.get();
                if(resO instanceof Response) {
                    Response resp = (Response) resO;
                    String respS = resp.readEntity(String.class);
                    JSONObject retJO = (JSONObject) ((JSONObject) JSONValue.parse(respS)).get("loanable");
                    responseString.add("Got retJO");

                    List<String> types = new ArrayList();
                    Object typesO = retJO.get("types");
                    if(typesO instanceof JSONArray) {
                        JSONArray typesJA = (JSONArray) typesO;
                        for(Object o : typesJA ) {
                            types.add(o.toString());
                        }
                    } else {
                        types.add(typesO.toString());
                    }

                    GetLoanablesResponse glResponse = new GetLoanablesResponse(
                            retJO.get("recordId").toString(),
                            types,
                            Integer.parseInt(retJO.get("available").toString())
                    );
                    ret.add(glResponse);
                }

            } catch(Exception e) {
                logger.error("Error while getting loanable", e);
            }
        }

        return ret;
    }

    private int getAvailable(String recordId) {
        WebTarget target = client.target(url + "/rest/v1/LoanService/Get_Available")
                .queryParam("recordId", recordId);

        Invocation.Builder invocationBuilder = getBuilder(target, MediaType.APPLICATION_JSON_TYPE);

        String response = invocationBuilder.get(String.class);
        List<CheckoutInformation> checkouts = new ArrayList<>();
        try {
            Object o = JSONValue.parse(response);
            if(o instanceof JSONObject) {
                JSONObject jsonResp = (JSONObject) o;
                jsonResp = (JSONObject)jsonResp.get("response");
                return Integer.parseInt(jsonResp.get("$").toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    private Invocation.Builder getBuilder(WebTarget target) {
        return getBuilder(target, MediaType.APPLICATION_JSON_TYPE);
    }

    private Invocation.Builder getBuilder(WebTarget target, MediaType mediaType) {
        Invocation.Builder invocationBuilder = target.request(mediaType);
        return applyCookies(invocationBuilder);
    }

    private Future<Response> callFromBuilder(Invocation.Builder builder, InvocationCallback<Response> callback, String method) {
        if(method.equals(HttpMethod.GET)) {
            return builder.async().get(callback);
        }//TODO we may need more methods in the future, but right now Odilo is only using GET

        return null;
    }

    private Future<Response> callFromBuilder(Invocation.Builder builder, String method) {
        if(method.equals(HttpMethod.GET)) {
            return builder.async().get();
        }

        return null;
    }
	
}
