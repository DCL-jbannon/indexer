package org.API.Odilo;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.Cookie;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jbannon on 8/12/2014.
 */
public class OdiloResponseFilter implements ClientResponseFilter {
    Map<? extends String, ? extends Cookie> coookies = new HashMap<>();

    public Map<? extends String, ? extends Cookie> getCoookies() {
        return coookies;
    }

    @Override
    public void filter(ClientRequestContext clientRequestContext, ClientResponseContext clientResponseContext) throws IOException {
        this.coookies = clientResponseContext.getCookies();
    }
}
