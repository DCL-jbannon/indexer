package org.API.Odilo;

import org.API.Odilo.manualModels.GetLoanablesResponse;
import org.API.Odilo.manualModels.ReserveResponse;
import org.eclipse.persistence.jaxb.rs.MOXyJsonProvider;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.*;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

	public List<String> getIds()
	{
        //TODO we'll have to refine this when we have a significant number of items in Odilo
        WebTarget target = client.target(url + "/rest/v1/SearchService/Search")
                .queryParam("Query", "*");
        Invocation.Builder invocationBuilder = getBuilder(target);
        Response response = invocationBuilder.get();
        Object jResponse = JSONValue.parse(response.readEntity(String.class));
        List<String> ret = new ArrayList();
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
        return ret;
	}
	
	public JSONObject getItemMetadata(String odiloId)
	{
        WebTarget target = client.target(url+ "/rest/v1/RecordService/Get_Record")
            .queryParam("recordId", odiloId);
        Invocation.Builder invocationBuilder = getBuilder(target);
        Response response = invocationBuilder.get();
        Object jResponse = JSONValue.parse(response.readEntity(String.class));
        if(jResponse instanceof  JSONObject) {
            return (JSONObject)jResponse;
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

    public ReserveResponse holdItem(String recordId, String loanableId) {
        WebTarget target = client.target(url+ "/rest/v1/LoanService/New_Reserve")
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
                return new ReserveResponse(
                        jsonResp.get("recordId").toString(),
                        jsonResp.get("reserveId").toString(),
                        jsonResp.get("status").toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<ReserveResponse> getHolds() {
        WebTarget target = client.target(url + "/rest/v1/LoanService/Get_Reserves");
        //.queryParam("loanableId", loanableId);

        Invocation.Builder invocationBuilder = getBuilder(target, MediaType.APPLICATION_JSON_TYPE);

        //ResponseType response = invocationBuilder.get(String.class);
        String response = invocationBuilder.get(String.class);
        try {
            Object o = JSONValue.parse(response);
            if(o instanceof JSONObject) {
                JSONObject jsonResp = (JSONObject) o;
                jsonResp = (JSONObject)jsonResp.get("response");
                List<ReserveResponse> l = new ArrayList();
                Object reservesO = jsonResp.get("reserves");
                if(reservesO instanceof JSONArray){
                    JSONArray jsonArray = (JSONArray) reservesO;
                    for(Object oo : jsonArray) {
                        if(oo instanceof JSONObject) {
                            JSONObject reserveJO = (JSONObject) oo;
                            l.add( new ReserveResponse(
                                    reserveJO.get("recordId").toString(),
                                    reserveJO.get("reserveId").toString(),
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

    public void releaseHold(ReserveResponse reserve) {
        WebTarget target = client.target(url + "/rest/v1/LoanService/Remove_Reserve")
            .queryParam("reserveId", reserve.getReserveId());

        Invocation.Builder invocationBuilder = getBuilder(target, MediaType.APPLICATION_JSON_TYPE);

        //ResponseType response = invocationBuilder.get(String.class);
        String response = invocationBuilder.get(String.class);
        int i = 0;
        i++;
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
                            types);
                    ret.add(glResponse);
                }

            } catch(Exception e) {
                logger.error("Error while getting loanable", e);
            }
        }

        return ret;
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
