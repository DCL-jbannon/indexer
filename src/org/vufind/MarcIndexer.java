package org.vufind;

import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vufind.config.DynamicConfig;
import org.vufind.config.sections.BasicConfigOptions;
import org.vufind.config.sections.MarcConfigOptions;
import org.vufind.solr.SolrUpdateServerFactory;

public class MarcIndexer implements IMarcRecordProcessor, IRecordProcessor {
    final static Logger logger = LoggerFactory.getLogger(MarcIndexer.class);

    private DynamicConfig config = null;

    @Override
    public boolean init(DynamicConfig config) {
        this.config = config;
        return true;
    }

    @Override
    public void finish() {

    }

    private ConcurrentUpdateSolrServer solr = null;
    private ConcurrentUpdateSolrServer getSolrUpdator() {
        if(solr==null) {
            solr = SolrUpdateServerFactory.getSolrUpdateServer(
                    config.get(BasicConfigOptions.BASE_SOLR_URL).toString()
                            + config.get(BasicConfigOptions.PRINT_CORE).toString());
        }
        return solr;
    }

    @Override
	public boolean processMarcRecord(MarcRecordDetails recordInfo) {
		try {
            MarcProcessor.RecordStatus recordStatus = recordInfo.getRecordStatus();
			if (recordStatus == MarcProcessor.RecordStatus.RECORD_UNCHANGED
                    && !config.getBool(MarcConfigOptions.SHOULD_REINDEX_UNCHANGED_RECORDS)){
				logger.debug("Skipping record["+recordInfo.getId()+"] because it hasn't changed");
				return true;
			} else if(recordStatus == MarcProcessor.RecordStatus.RECORD_DELETED) {
                getSolrUpdator().deleteById(recordInfo.getId());
                logger.debug("Deleted record["+recordInfo.getId()+"]");
                return true;
            }
			
			if (!recordInfo.isEContent()){
				//Create the XML document for the record
				try {
					SolrInputDocument doc = recordInfo.getSolrDocument();
					if (doc != null){
                        getSolrUpdator().add(doc);
                        logger.debug("Added record[" + recordInfo.getId() + "]");
						return true;
					}else{
                        logger.error("Got a null doc back from getSolrDocuemnt() for record["+recordInfo.getId()+"]");
						return false;
					}
				} catch (Exception e) {
                    logger.error("Error creating xml doc for record[" + recordInfo.getId() + "]", e);
					return false;
				}
			}else{
				logger.debug("Skipping record because it is eContent");
				return false;
			}
		} catch (Exception ex) {
			// handle any errors
			logger.error("Error indexing marc record[" + recordInfo.getId() + "]", ex);
			return false;
		}
	}
}
