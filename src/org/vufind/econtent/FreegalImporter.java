package org.vufind.econtent;

import org.API.Freegal.FreegalAPI;
import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vufind.ConnectionProvider;
import org.vufind.config.ConfigFiller;
import org.vufind.config.DynamicConfig;
import org.vufind.config.sections.BasicConfigOptions;
import org.vufind.config.sections.FreegalConfigOptions;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

public class FreegalImporter implements I_ExternalImporter {
    final static Logger logger = LoggerFactory.getLogger(FreegalImporter.class);

	private DynamicConfig config = null;
    private EContentRecordDAO eContentRecordDAO = null;
	private FreegalAPI freegalAPI;

	public void importRecords() {
        logger.info("Importing Freegal API Items");

		try {
			EContentRecordDAO dao = this.eContentRecordDAO;

            try {
                dao.flagAllFreegalRecordsAsDeleted();
            } catch (SQLException e) {
                logger.error("Error flagging all Freegal records as \"deleted\".", e);
                return;
            }


			Collection<String> genres = new HashSet<String>();
            genres.addAll(freegalAPI.getAllGenres());
            if(genres.size()<1){
                logger.error("Number of Freegal Genres found: " + genres.size());
            } else {
                logger.info("Number of Freegal Genres found: " + genres.size());
            }

            int songsAdded = 0;

            // Remove all existing songs for the album from the
            // database since freegal doesn't keep unique ids
            dao.deleteFreegalItems();
			for (String genre : genres) {
                ArrayList<Album> albums = new ArrayList(freegalAPI.getAlbums(genre));

                int partitionSize = 100;
                for(int i = 0; albums.size() > i*partitionSize; i++) {
                    Collection<Album> partition = albums.subList(i*partitionSize,
                            (i+1)*partitionSize < albums.size() -1 ? (i+1)*partitionSize : albums.size() -1);
                    processAlbumPartition(partition);
                }
			}
		} catch (ParserConfigurationException | SAXException | IOException e ) {
            logger.error("Error downloading Freegal contents.", e);
		} catch (SQLException e) {
            logger.error("Error adding/updating Freegal records.", e);
		}
	}

    private void syncEContentRecordsInDB(Collection<Album> albums) throws SQLException{
        for (Album album : albums) {

            logger.debug("Processing album " + album.getTitle()
                    + " the album has " + album.getSongs().size()
                    + " songs");
            EContentRecord record = this.eContentRecordDAO.findByTitleAndAuthor(
                    album.getTitle(), album.getAuthor());
            boolean added = false;
            if (record == null) {
                // create new record
                record = new EContentRecord(this.eContentRecordDAO, config);
                record.set("date_added",
                        (int) (new Date().getTime() / 100));
                record.set("accessType", "free");
                record.set("source", "Freegal");
                record.set("availableCopies", 1);
                added = true;
            } else {

                // set date updated
                record.set("date_updated",
                        (int) (new Date().getTime() / 100));
            }
            record.set("status", "active");
            record.set("title", album.getTitle());
            record.set("author", album.getAuthor());
            record.set("author2", album.getAuthor2());
            record.set("contents", album.getContents());
            record.set("language", album.getLanguage());
            record.set("genre", album.getGenre());
            record.set("collection", album.getCollection());
            record.set("cover", album.getCoverUrl());

            record.set("addedBy", -1);

            try {
                this.eContentRecordDAO.save(record);
            } catch( Exception e) {
                e.printStackTrace();
            }

        }
    }
    private void processAlbumPartition(Collection<Album> albums) throws SQLException {
        int songsAdded = 0;
        syncEContentRecordsInDB(albums);
        for (Album album : albums) {
            EContentRecord record = null;
            String recordId = null;
            try {
                record = this.eContentRecordDAO.findByTitleAndAuthor(
                        album.getTitle(), album.getAuthor());
                if(record != null && record.get("id") != null)   {
                    recordId = record.get("id").toString();
                } else {
                    int ii = 0;
                    ii++;
                    continue;
                }
            } catch(Exception e) {
                e.printStackTrace();
                continue;
            }

            // Add songs to the database
            for (Song song : album.getSongs()) {
                String notes = song.getTitle();
                String songArtist = song.getArtist();
                if (songArtist != null && !songArtist.equals(album.getAuthor())) {
                    notes += " -- " + song.getArtist();
                }

                EContentItem item = new EContentItem(
                        null,
                        recordId,
                        song.getDownloadUrl(),
                        "externalMP3",
                        notes,
                        null,
                        (int) (new Date().getTime() / 100),
                        (int) (new Date().getTime() / 100));
                this.eContentRecordDAO.addEContentItem(item);
            }
        }
        this.eContentRecordDAO.flushEContentItems(false);
    }

	public FreegalImporter() {
	}

    public boolean init(DynamicConfig config) {
        ConfigFiller.fill(config, FreegalConfigOptions.values(), new File(config.getString(BasicConfigOptions.CONFIG_FOLDER)));
        this.config = config;
        BasicDataSource econtentDataSource = ConnectionProvider.getDataSource(config, ConnectionProvider.PrintOrEContent.E_CONTENT);
        this.eContentRecordDAO = new EContentRecordDAO(econtentDataSource, config);
        this.freegalAPI = new FreegalAPI(
                config.getString(FreegalConfigOptions.URL),
                config.getString(FreegalConfigOptions.USER),
                config.getString(FreegalConfigOptions.PIN),
                config.getString(FreegalConfigOptions.API_KEY),
                config.getString(FreegalConfigOptions.LIBRARY_ID));

        return true;
    }

    @Override
	public void finish() {
		try {
			int deleted = this.eContentRecordDAO.deleteFreegalRecordsFlaggedAsDeleted();
		} catch (SQLException e) {
			logger.error("Error deleting records flagged as deleted", e);
		}
	}
}
