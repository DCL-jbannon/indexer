package org.API.Freegal;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.noggit.JSONWriter;
import org.vufind.econtent.Album;
import org.vufind.econtent.Song;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vufind.Base64Coder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class FreegalAPI {
    final static Logger logger = LoggerFactory.getLogger(FreegalAPI.class);
	private String freegalUrl;
	private String freegalUser;
	private String freegalPIN;
	private String freegalAPIkey;
	private String freegalLibrary;
	private DocumentBuilderFactory documentBuilderFactory;

	public FreegalAPI(String freegalUrl, String freegalUser, String freegalPIN,
			String freegalAPIkey, String freegalLibrary) {
		this.freegalUrl = freegalUrl;
		this.freegalUser = freegalUser;
		this.freegalPIN = freegalPIN;
		this.freegalAPIkey = freegalAPIkey;
		this.freegalLibrary = freegalLibrary;
		documentBuilderFactory = DocumentBuilderFactory.newInstance();

	}

	public Collection<String> getAllGenres()
			throws ParserConfigurationException, SAXException, IOException {
		List<String> allGenres = new ArrayList<String>();

		// Get a list of all genres in the freegal site
		String genreUrl = freegalUrl + "/services/genre/" + freegalAPIkey + "/"
				+ freegalLibrary + "/" + freegalUser;
		logger.debug("Genre url: " + genreUrl);
		DocumentBuilder db = documentBuilderFactory.newDocumentBuilder();
		Document genreDoc = db.parse(genreUrl);
		NodeList genres = genreDoc.getElementsByTagName("Genre");
		for (int i = 0; i < genres.getLength(); i++) {
			Node genreNode = genres.item(i);
			String genre = genreNode.getTextContent();
			genre = genre.trim();
			if (genre.length() == 0) {
				continue;
			}
			allGenres.add(genre);
		}
		return allGenres;
	}

	public Collection<Album> getAlbums(String genre)
			throws ParserConfigurationException, SAXException, IOException {
		HashMap<String, Album> albums = new HashMap<String, Album>();
		String base64Genre = Base64Coder.encodeString(genre);
		String songUrl = freegalUrl + "/services/genre/" + freegalAPIkey + "/"
				+ freegalLibrary + "/" + freegalUser + "/" + freegalPIN + "/"
				+ base64Genre;
		logger.debug("Get songs from genre url: " + songUrl);
		Document songsDoc = null;
        DocumentBuilder songsDB = documentBuilderFactory.newDocumentBuilder();
        ByteArrayOutputStream baos = null;
        //File tempFile = File.createTempFile("Freegal_"+((int)Math.random()*1000), ".tmp");
        try {
			songsDoc = songsDB.parse(songUrl);
            /*URL url = new URL(songUrl);
            ReadableByteChannel rbc = Channels.newChannel(url.openStream());
            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.getChannel().transferFrom(rbc, 0, Integer.MAX_VALUE);
            fos.close();
            rbc.close();
            //TODO this is temporary for debugging purposes; we should just read the URL stream straight via SAX
            songsDoc = songsDB.parse(tempFile);     */
		} catch (SAXException | IOException e) {
			logger.error("Error while reading Freegal URL", e);
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            try {

                songsDoc = songsDB.parse(songUrl);
            } catch (SAXException | IOException ee) {
                logger.error("Failed twice to read Freegal URL", ee);
            }
			return albums.values();
		}
		NodeList songs = songsDoc.getElementsByTagName("Song");
        if(songs.getLength()<1){
            logger.info("Number of Freegal Genre songs found: 0");
        } else {
            logger.debug("Number of Freegal Genre songs found: " + songs.getLength());
        }

		// Group the songs by album
		for (int j = 0; j < songs.getLength(); j++) {
			Element songNode = (Element) songs.item(j);
			NodeList tags = songNode.getElementsByTagName("Title");
			String text = tags.getLength() > 0 ? tags.item(0).getTextContent() : null; 
			if (text != null) {
				Album album = new Album();
				album.setTitle(text);
				
				tags = songNode.getElementsByTagName("ArtistText");
				text = tags.getLength() > 0 ? tags.item(0).getTextContent() : null; 
				album.setAuthor(text);
				if (!albums.containsKey(album.toString())) {
					logger.debug("Found new album " + album.toString());
                    String realGenre = "MUSIC";
                    try{
                        realGenre = songNode.getElementsByTagName("Genre").item(0).getTextContent().trim();
                        if(realGenre.endsWith(",")) {
                            realGenre = realGenre.substring(0, realGenre.length() - 1);
                        }
                    } catch (Exception e){}
					album.setGenre(realGenre);
					tags = songNode.getElementsByTagName("Album_Artwork");
					text = tags.getLength() > 0 ? tags.item(0).getTextContent() : null; 
					album.setCoverUrl(text);
				} else {
					album = albums.get(album.toString());
				}
				albums.put(album.toString(), album);
				// Add the song to the album
				Song song = new Song();
				tags = songNode.getElementsByTagName("SongTitle");
				text = tags.getLength() > 0 ? tags.item(0).getTextContent() : null; 
				song.setTitle(text);
				
				tags = songNode.getElementsByTagName("Artist");
				text = tags.getLength() > 0 ? tags.item(0).getTextContent() : null;
				song.setArtist(text);
				
				tags = songNode.getElementsByTagName("Composer");
				text = tags.getLength() > 0 ? tags.item(0).getTextContent() : null;
				song.setComposer(text);
				
				tags = songNode.getElementsByTagName("freegal_url");
				text = tags.getLength() > 0 ? tags.item(0).getTextContent() : null;
				if (text != null) {
					String freegalUrl = text;
					freegalUrl = freegalUrl.replaceAll("/" + freegalUser + "/",
							"/{patronBarcode}/");
					freegalUrl = freegalUrl.replaceAll("/" + freegalPIN + "/",
							"/{patronPin}/");
					song.setDownloadUrl(freegalUrl);
					album.getSongs().add(song);
				}
			}
		}
		return albums.values();
	}
}
