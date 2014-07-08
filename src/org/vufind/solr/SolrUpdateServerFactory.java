package org.vufind.solr;

import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer;

import java.util.HashMap;

/**
 * Created by jbannon on 7/7/14.
 */
public class SolrUpdateServerFactory {
    static private HashMap<String,ConcurrentUpdateSolrServer> solrServers = new HashMap<String,ConcurrentUpdateSolrServer> ();
    static public ConcurrentUpdateSolrServer getSolrUpdateServer(final String solrURL) {
        ConcurrentUpdateSolrServer ret = solrServers.get(solrURL);
        if(ret == null) {
            ret = new ConcurrentUpdateSolrServer(solrURL, 1024, 5);
            solrServers.put(solrURL, ret);
        }
        return ret;
    }
}
