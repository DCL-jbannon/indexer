package org.vufind;

import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vufind.config.Config;

public class MarcIndexer implements IMarcRecordProcessor, IRecordProcessor {
    final static Logger logger = LoggerFactory.getLogger(MarcIndexer.class);

    private Config config = null;
    private String coreToIndex;

    @Override
    public boolean init(Config config) {
        return true;
    }

    @Override
    public void finish() {

    }

    @Override
	public boolean processMarcRecord(MarcProcessor processor, MarcRecordDetails recordInfo, MarcProcessor.RecordStatus recordStatus, Logger logger) {
		try {
			if (recordStatus == MarcProcessor.RecordStatus.RECORD_UNCHANGED && !config.shouldReindexUnchangedRecords()){
				logger.debug("Skipping record["+recordInfo.getId()+"] because it hasn't changed");
				return true;
			} else if(recordStatus == MarcProcessor.RecordStatus.RECORD_DELETED) {
                config.getSolrUpdateServer(coreToIndex).deleteById(recordInfo.getId());
                logger.debug("Deleted record["+recordInfo.getId()+"]");
                return true;
            }
			
			
			if (!recordInfo.isEContent()){
				//Create the XML document for the record
				try {
					//String xmlDoc = recordInfo.createXmlDoc();
					SolrInputDocument doc = recordInfo.getSolrDocument();
					if (doc != null){
						//Post to the Solr instance
                        config.getSolrUpdateServer(coreToIndex).add(doc);
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
