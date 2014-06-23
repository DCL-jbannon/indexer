package mother;

import org.apache.solr.common.SolrInputDocument;
import org.solr.SolrConstantsEContentFields;

public class SolrInputDocumentMother 
{
	
	public static final String id = "aDummyId";
	public static final String recordType = "aDummyRecordType";
	public static final String title = "aDummyTitle";
	public static final String titleSort = "aDummyTitleSort";
	public static final String available = "aDummyAvailableAt";
	public static final String author = "aDummyAuthor";

	
	public SolrInputDocument getEContentOPverDriveAPIItemSolrInputDocument(String id)
	{
		SolrInputDocument document = new SolrInputDocument();
		document.addField(SolrConstantsEContentFields.id, id);
		document.addField(SolrConstantsEContentFields.recordType, SolrInputDocumentMother.recordType);
		document.addField(SolrConstantsEContentFields.title, SolrInputDocumentMother.title);
		document.addField(SolrConstantsEContentFields.titleSort, SolrInputDocumentMother.titleSort);
		document.addField(SolrConstantsEContentFields.available, SolrInputDocumentMother.available);
		document.addField(SolrConstantsEContentFields.author, SolrInputDocumentMother.author);
		document.addField(SolrConstantsEContentFields.keywords, SolrInputDocumentMother.title + "\n" +  SolrInputDocumentMother.author + "\n" + SolrInputDocumentMother.available);
		return document;
	}
	
	public SolrInputDocument getEContentOPverDriveAPIItemSolrInputDocument()
	{
		return this.getEContentOPverDriveAPIItemSolrInputDocument(SolrInputDocumentMother.id);
	}
	
}