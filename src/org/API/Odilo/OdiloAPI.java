package org.API.Odilo;

import org.eclipse.persistence.jaxb.rs.MOXyJsonProvider;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.*;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

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
        WebTarget target = client.target(url+ "/rest/v1/SearchService/Search")
                .queryParam("Query", "*");
        Invocation.Builder invocationBuilder = target.request(MediaType.APPLICATION_JSON_TYPE);
        applyCookies(invocationBuilder);
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
        Invocation.Builder invocationBuilder = target.request(MediaType.APPLICATION_JSON_TYPE);
        applyCookies(invocationBuilder);
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
        Invocation.Builder invocationBuilder = target.request(MediaType.APPLICATION_JSON_TYPE);
        applyCookies(invocationBuilder);
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
	
}
