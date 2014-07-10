package org.econtent;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import javax.xml.parsers.ParserConfigurationException;

import org.API.Freegal.FreegalAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vufind.config.Config;
import org.vufind.IRecordProcessor;
import org.vufind.config.DynamicConfig;
import org.xml.sax.SAXException;

public class FreegalImporter implements IRecordProcessor {
    final static Logger logger = LoggerFactory.getLogger(FreegalImporter.class);

	private Config config = null;

	private FreegalAPI freegalAPI;
	private EContentRecordDAO dao;

	public void importRecords() {
        logger.info("Importing Freegal API Items");

        try {
            dao.flagAllFreegalRecordsAsDeleted();
        } catch (SQLException e) {
            logger.error("Error flagging all Freegal records as \"deleted\".", e);
            return;
        }

		try {
			EContentRecordDAO dao = config.getEContentRecordDAO();
			Collection<String> genres = freegalAPI.getAllGenres();
            logger.info("Freegal Gender Number: " + genres.size());
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
		} catch (ParserConfigurationException e) {
            logger.error("Error downloading Freegal contents.", e);
		} catch (SAXException e) {
            logger.error("Error downloading Freegal contents.", e);
		} catch (IOException e) {
            logger.error("Error downloading Freegal contents.", e);
		} catch (SQLException e) {
            logger.error("Error adding/updating Freegal records.", e);
		}
	}

    private void syncEContentRecordsInDB(Collection<Album> albums) throws SQLException{
        for (Album album : albums) {

            logger.info("Processing album " + album.getTitle()
                    + " the album has " + album.getSongs().size()
                    + " songs");
            EContentRecord record = dao.findByTitleAndAuthor(
                    album.getTitle(), album.getAuthor());
            boolean added = false;
            if (record == null) {
                // create new record
                record = new EContentRecord(config.getEContentRecordDAO());
                record.set("date_added",
                        (int) (new Date().getTime() / 100));
                record.set("addedBy", -1);
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
            dao.save(record);
        }
    }
    private void processAlbumPartition(Collection<Album> albums) throws SQLException {
        int songsAdded = 0;
        for (Album album : albums) {
            syncEContentRecordsInDB(albums);
            EContentRecord record = dao.findByTitleAndAuthor(
                    album.getTitle(), album.getAuthor());
            String recordId = record.get("id").toString();

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
                dao.addEContentItem(item);
            }
        }
        dao.flushEContentItems(false);
    }

	public FreegalImporter() {
	}


    public boolean init(Config config) {
        this.config = config;

        this.freegalAPI = new FreegalAPI(config.getFreegalUrl(), config.getFreegalUser(), config.getFreegalPIN(),
                config.getFreegalAPIkey(), config.getFreegalLibraryId());
        return true;
    }

    @Override
    public boolean init(DynamicConfig config) {
        return false;
    }

    @Override
	public void finish() {
		try {
			int deleted = dao.deleteFreegalRecordsFlaggedAsDeleted();
		} catch (SQLException e) {
			logger.error("Error deleting records flagged as deleted", e);
		}
	}

    @Override
    public void accept(Object o) {

    }
}
