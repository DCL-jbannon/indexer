package org.vufind.econtent;

import org.API.Odilo.OdiloAPI;
import org.API.OverDrive.IOverDriveAPIServices;
import org.API.OverDrive.IOverDriveCollectionIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vufind.ConnectionProvider;
import org.vufind.config.DynamicConfig;
import org.vufind.config.sections.OverDriveConfigOptions;
import org.vufind.econtent.db.DBeContentRecordServices;
import org.vufind.econtent.db.IDBeContentRecordServices;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        this.api.login();
        List<String> odiloIds = this.api.getIds();

        PreparedStatement setAsEvoke_PS = ConnectionProvider
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

		}

		logger.info("Finished Odilo");
	}
}