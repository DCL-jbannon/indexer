package org.vufind.econtent;

import org.API.Odilo.OdiloAPI;
import org.API.Odilo.OdiloEContentRecord;
import org.API.Odilo.manualModels.Record;
import org.joda.time.DateTime;
import org.json.simple.JSONArray;
import org.marc4j.MarcXmlHandler;
import org.marc4j.MarcXmlParser;
import org.marc4j.RecordStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vufind.ConnectionProvider;
import org.vufind.config.DynamicConfig;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

public class PopulateOdiloItems
{
    final static Logger logger = LoggerFactory.getLogger(PopulateOdiloItems.class);

    private OdiloAPI api;
    private DynamicConfig config;

	public PopulateOdiloItems(OdiloAPI api, DynamicConfig config)
	{
		this.api = api;
        this.config = config;
	}

	public void execute() throws SQLException 
	{
		logger.info("Started getting OverDrive API Collection");
		int j = 0;
		long totalItems = new Long("0");
		JSONArray items = new JSONArray();
        //this.api.login();
        //Set<String> odiloIds = this.api.getAllIds();

       /* PreparedStatement setAsEvoke_PS = ConnectionProvider
                .getConnection(config, ConnectionProvider.PrintOrEContent.E_CONTENT)
                .prepareStatement(
                        "UPDATE econtent_record " +
                        "SET source = 'Odilo', external_id = ? " +
                        "WHERE " +
                            "accessType = 'acs' AND isbn like ?");

        for(String odiloId : odiloIds) {
            String isbn = api.getISBN(odiloId);
            logger.debug("Setting odiloId["+odiloId+"] isbn["+isbn+"]");

            setAsEvoke_PS.setString(1, odiloId);
            setAsEvoke_PS.setString(2, isbn+"%");
            int affectedRows = setAsEvoke_PS.executeUpdate();
            if(affectedRows != 1) {
                logger.info("Couldn't find a match for Odilo item");
            } else {
                int ii = 0;
                ii++;
            }

		}*/

        this.syncSince(new DateTime().minusDays(90));
		logger.info("Finished Odilo");
	}

    private RecordStack recordStack = new RecordStack();
    private MarcXmlParser parser = new MarcXmlParser(new MarcXmlHandler(recordStack));
    private void syncSince(DateTime since) {
        api.login();
        Set<String> recordIds = this.api.getUpdatesSince(since,
                Arrays.asList(new OdiloAPI.BoundedSearchType[]{
                        OdiloAPI.BoundedSearchType.CREATED,
                        OdiloAPI.BoundedSearchType.UPDATED}));

        ForkJoinPool forkJoinPool = new ForkJoinPool(10);

        ForkJoinTask addTask = forkJoinPool.submit(()->
                recordIds.parallelStream().forEach((recordId) -> this.syncRecord(recordId)));
        addTask.join();

        Set<String> deletedIds = this.api.getUpdatesSince(since,
                Arrays.asList(new OdiloAPI.BoundedSearchType[]{OdiloAPI.BoundedSearchType.DELETED}));

        ForkJoinTask deleteTask = forkJoinPool.submit(()->
                deletedIds.parallelStream().forEach((recordId) -> this.deleteRecord(recordId)));
        deleteTask.join();
        int i = 0;
        i++;
    }

    private ThreadLocal<EContentRecordDAO> dao = new ThreadLocal<EContentRecordDAO>(){
        @Override protected EContentRecordDAO initialValue() {
            EContentRecordDAO dao = new EContentRecordDAO(
                    ConnectionProvider.getDataSource(config, ConnectionProvider.PrintOrEContent.E_CONTENT),
                    config);

            return dao;
        }
    };

    private void deleteRecord(String recordId) {
        EContentRecordDAO dao = this.dao.get();
        try {
            EContentRecord econtentRecord = dao.findByExternalId(recordId);
            econtentRecord.set("status", "deleted");
            dao.save(econtentRecord);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void syncRecord(String recordId) {
        Record apiRecord = api.getRecord(recordId);
        EContentRecordDAO dao = this.dao.get();

        try {
            EContentRecord econtentRecord = dao.findByExternalId(recordId);
            if(econtentRecord == null) {
                econtentRecord = new OdiloEContentRecord(dao, config);
                //yes this is very stupid, but I don't have time to refactor all the evil out of econtent_records right now
                econtentRecord.set("date_added", (int)(new Date().getTime()/1000));
            }
            //then update
            econtentRecord.set("author", apiRecord.getAuthor());
            econtentRecord.set("description", apiRecord.getDescription());
            econtentRecord.set("edition", apiRecord.getEdition());
            econtentRecord.set("external_id", apiRecord.getExternalId());
            econtentRecord.set("isbn", apiRecord.getIsbn());
            econtentRecord.set("language", apiRecord.getLanguage());
            econtentRecord.set("publishDate", apiRecord.getPublishDate());
            econtentRecord.set("publisher", apiRecord.getPublisher());
            econtentRecord.set("subject", apiRecord.getSubject());
            econtentRecord.set("subtitle", apiRecord.getSubtitle());
            econtentRecord.set("target_audience", apiRecord.getTargetAudience());
            econtentRecord.set("title", apiRecord.getTitle());
            econtentRecord.set("source", "Odilo");

            dao.save(econtentRecord);
        } catch (SQLException e) {
            logger.error("DAO error in PopulateOdiloItems", e);
        }
    }
}