package org.strands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;
import org.vufind.*;
import org.vufind.config.ConfigFiller;
import org.vufind.config.DynamicConfig;
import org.vufind.config.sections.BasicConfigOptions;
import org.vufind.config.sections.StrandsConfigOptions;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class StrandsProcessor implements IMarcRecordProcessor, IEContentProcessor, IRecordProcessor {
    final static Logger logger = LoggerFactory.getLogger(StrandsProcessor.class);

    private DynamicConfig config = null;

    private File printCsvTempFile = null;
    private ICsvBeanWriter printCsvWriter = null;
    private File econtentCsvTempFile = null;
    private ICsvBeanWriter econtentCsvWriter = null;

	
	/**
	 * Build a csv file to import into strands
	 */
	public boolean init(DynamicConfig config) {
        this.config = config;

        ConfigFiller.fill(config, Arrays.asList(StrandsConfigOptions.values()), new File(config.getString(BasicConfigOptions.CONFIG_FOLDER)));
		logger.info("Creating Catalog File for Strands");

		try {
            if(config.getBool(BasicConfigOptions.DO_FULL_REINDEX)) {
                //printCsvWriter and econtentCsvWriter are the same because we're just producing one giant file that
                //will overwrite everything
                printCsvTempFile = File.createTempFile("strandsFull", ".csv");
                printCsvWriter = new CsvBeanWriter(new FileWriter(printCsvTempFile), CsvPreference.STANDARD_PREFERENCE);
                econtentCsvTempFile = printCsvTempFile;
                econtentCsvWriter = printCsvWriter;
                writeHeader(this.printCsvWriter);
            } else {
                printCsvTempFile = File.createTempFile("strandsPrint", ".csv");
                printCsvWriter = new CsvBeanWriter(new FileWriter(printCsvTempFile), CsvPreference.STANDARD_PREFERENCE);
                econtentCsvTempFile = File.createTempFile("strandsEcontent", ".csv");
                econtentCsvWriter = new CsvBeanWriter(new FileWriter(econtentCsvTempFile), CsvPreference.STANDARD_PREFERENCE);
                writeHeader(this.printCsvWriter);
                writeHeader(this.econtentCsvWriter);
            }

		} catch (Exception e) {
			logger.error("Error generating Strands catalog " + e.toString());
			e.printStackTrace();
			return false;
		}
		return true;
	}

    private String[] headers = new String[]{
            "id",
            "link",
            "title",
            "author",
            "image_link",
            "publisher",
            "description",
            "genre",
            "format",
            "subject",
            "audience",
            "collection",
    };

    private void writeHeader(ICsvBeanWriter writer) throws IOException {
        writer.writeHeader(headers);
    }

    private final Collection<String> emptyCollection = new ArrayList(Arrays.asList(new String[]{null, ""}));

	@Override
	public boolean processEContentRecord(String indexName, ResultSet eContentRecord) {
        PreparedStatement getFormatsForRecord = null;
        Connection econtentConn = ConnectionProvider.getConnection(config, ConnectionProvider.PrintOrEContent.E_CONTENT);
        try {
            getFormatsForRecord = econtentConn.prepareStatement("SELECT distinct item_type from econtent_item where recordId = ?");

        } catch (Exception ex) {
            // handle any errors
            logger.error("Error processing eContent ", ex);
            return false;
        }

		try {
			// Write the id
            ContentBean content = new ContentBean();

			String id = eContentRecord.getString("id");
			logger.info("Processing eContentRecord " + id);
            content.setId("'econtentRecord" + id + "'");
            content.setLink(config.getString(BasicConfigOptions.VUFIND_URL) + "/EContentRecord/" + id);
            content.setTitle(eContentRecord.getString("title"));

            List<String> authors = new ArrayList();
            authors.add(eContentRecord.getString("author"));
            authors.add(eContentRecord.getString("author2"));
            authors.removeAll(emptyCollection);
            content.setAuthor(getSemiColonSeparatedString(authors, true));

            String isbn = eContentRecord.getString("isbn");
            String upc = eContentRecord.getString("upc");
            content.setImage_link(config.getString(BasicConfigOptions.BOOK_COVER_URL)+ "?isn=" + isbn + "&upc=" + upc + "&id=econtentRecord" + id + "&size=small&econtent=true");

            content.setPublisher(eContentRecord.getString("publisher"));

            content.setDescription(prepForCsv(eContentRecord.getString("description")));

            content.setGenre(prepForCsv(eContentRecord.getString("genre")));

            List<String> formats = new ArrayList();
            getFormatsForRecord.setString(1, id);
            ResultSet formatsRs = getFormatsForRecord.executeQuery();
            while (formatsRs.next()) {
                formats.add(formatsRs.getString(1));
            }
            content.setFormat(getSemiColonSeparatedString(formats, true));

            content.setSubject(eContentRecord.getString("subject"));

            content.setAudience(eContentRecord.getString("target_audience"));

            content.setCollection("EMedia");

            this.econtentCsvWriter.write(content, headers);
			
			return true;
		} catch (Exception e) {
			logger.error("Error processing eContent record", e);
			return false;
		}
	}


	public boolean processMarcRecord(MarcRecordDetails recordInfo) {
		try {

            if(recordInfo.getRecordStatus() == MarcProcessor.RecordStatus.RECORD_DELETED) {
                return true;
            }

            // Write the id
            ContentBean content = new ContentBean();

            logger.info("Processing record: "+recordInfo.getId());
            content.setId(recordInfo.getId());

            content.setLink(config.getString(BasicConfigOptions.VUFIND_URL) + "/Record/" + recordInfo.getId());
            content.setTitle(recordInfo.getTitle());

            Set<String> authors = recordInfo.getAuthors();
            authors.removeAll(emptyCollection);
            content.setAuthor(getSemiColonSeparatedString(authors, true));

            content.setImage_link(config.getString(BasicConfigOptions.BOOK_COVER_URL)
                    + "?isn=" + recordInfo.getIsbn()
                    + "&upc=" + recordInfo.getFirstFieldValueInSet("upc")
                    + "&id=" + recordInfo.getId()
                    + "&size=small");

            content.setPublisher(recordInfo.getFirstFieldValueInSet("publisher"));

            content.setDescription(recordInfo.getDescription());

            content.setGenre(getSemiColonSeparatedString(recordInfo.getMappedField("genre"), true));

            content.setFormat(getSemiColonSeparatedString(recordInfo.getMappedField("format"), true));

            content.setSubject(getSemiColonSeparatedString(recordInfo.getMappedField("topic"), true));

            content.setAudience(getSemiColonSeparatedString(recordInfo.getMappedField("target_audience"), true));

            content.setCollection(getSemiColonSeparatedString(recordInfo.getMappedField("format_category"), true));

            this.printCsvWriter.write(content, headers);

			return true;
		} catch (IOException e) {
			logger.error("Error writing to catalog file, " + e.toString());
			return false;
		}
	}

    private void copyToOutput(File outputFile, File tempFile) {
        if (outputFile.exists()) {
            outputFile.delete();
        }

        if (!tempFile.renameTo(outputFile)) {
            logger.error("Could not copy the temp file to the final output file.");
        } else {
            logger.info("Strands output file has been created as " + outputFile.getAbsolutePath());
        }
    }

    @Override
	public void finish() {
        if(this.printCsvWriter!=null) {
            try {
                this.printCsvWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(this.econtentCsvWriter!=null) {
            try {
                this.econtentCsvWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Copy the temp file to the correct location so it can be picked up by
        // strands
        if(config.getBool(BasicConfigOptions.DO_FULL_REINDEX)) {
            String loc = config.getString(StrandsConfigOptions.FULL_RECATALOG_CSV_LOC);
            copyToOutput(new File(loc), this.printCsvTempFile);

        } else {
            String loc = config.getString(StrandsConfigOptions.PARTIAL_RECATALOG_PRINT_CSV_LOC);
            copyToOutput(new File(loc), this.printCsvTempFile);

            loc = config.getString(StrandsConfigOptions.PARTIAL_RECATALOG_ECONTENT_CSV_LOC);
            copyToOutput(new File(loc), this.econtentCsvTempFile);
        }
    }

    private static String prepForCsv(String value) {
        String ret = value.trim();
        if(value.endsWith(".")) {
            ret = ret.substring(0, ret.length()-1);
        }
        //Strands uses semicolon to designate and array
        ret.replace(";", " ");
        return ret;
    }

    private static String getSemiColonSeparatedString(Object values, boolean prepForCsv) {
        StringBuffer crSeparatedString = new StringBuffer();
        if (values instanceof String){
            if (prepForCsv){
                crSeparatedString.append(prepForCsv((String)values));
            }else{
                crSeparatedString.append((String)values);
            }
        }else if (values instanceof Iterable){
            @SuppressWarnings("unchecked")
            Iterable<String> valuesIterable = (Iterable<String>)values;
            for (String curValue : valuesIterable) {
                if (crSeparatedString.length() > 0) {
                    crSeparatedString.append(";");
                }
                if (prepForCsv){
                    crSeparatedString.append(prepForCsv(curValue));
                }else{
                    crSeparatedString.append(curValue);
                }
            }
        }
        return crSeparatedString.toString();
    }
}
