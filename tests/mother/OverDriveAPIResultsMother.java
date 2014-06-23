package mother;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class OverDriveAPIResultsMother 
{
	public static final String overDriveID_one = "864604e2-0146-46ef-a96d-d9977dc15263";
	public static final String overDriveID_two = "76c1b7d0-17f4-4c05-8397-c66c17411584";
	public static final String overDriveID_three = "ee013a9b-53cc-45d2-95da-ec50360b8e80";
	
	public static String accessToken = "aDummyAccessToken";
	
	public JSONObject getLoginResult()
	{
		return this.getLoginResult(this.accessToken);
	}
	
	public JSONObject getLoginResult(String accessToken)
	{
		String jsonResponse = "{\"access_token\":\"" + accessToken + "\",\"token_type\":\"bearer\",\"expires_in\":\"3600\",\"scope\":\"LIB META AVAIL SRCH\"}";
		return (JSONObject) JSONValue.parse(jsonResponse);
	}
	
	public JSONObject getInfoLibrary()
	{
		String jsonResponse = "{\"id\":1344,\"name\":\"Douglas County Libraries (CO)\",\"type\":\"Library\",\"links\":{\"self\":{\"href\":\"https://api.overdrive.com/v1/libraries/1344\",\"type\":\"application/vnd.overdrive.api+json\"},\"products\":{\"href\":\"https://api.overdrive.com/v1/collections/L1BGAEAAA2f/products\",\"type\":\"application/vnd.overdrive.api+json\"},\"advantageAccounts\":{\"href\":\"https://api.overdrive.com/v1/libraries/1344/advantageAccounts\",\"type\":\"application/vnd.overdrive.api+json\"},\"dlrHomepage\":{\"href\":\"www.emedia2go.org\",\"type\":\"text/html\"}},\"formats\":[{\"id\":\"audiobook-wma\",\"name\":\"OverDrive WMA Audiobook\"},{\"id\":\"music-wma\",\"name\":\"OverDrive Music\"},{\"id\":\"video-wmv\",\"name\":\"OverDrive Video\"},{\"id\":\"ebook-pdf-adobe\",\"name\":\"Adobe PDF eBook\"},{\"id\":\"ebook-disney\",\"name\":\"Disney Online Book\"},{\"id\":\"ebook-epub-adobe\",\"name\":\"Adobe EPUB eBook\"},{\"id\":\"ebook-kindle\",\"name\":\"Kindle Book\"},{\"id\":\"audiobook-mp3\",\"name\":\"OverDrive MP3 Audiobook\"},{\"id\":\"ebook-pdf-open\",\"name\":\"Open PDF eBook\"},{\"id\":\"ebook-epub-open\",\"name\":\"Open EPUB eBook\"}]}";
		return (JSONObject) JSONValue.parse(jsonResponse);
	}
	
	
	public JSONObject getDigitalCollection()
	{
		String jsonResponse = "{\"limit\":3,\"offset\":0,\"totalItems\":15005,\"id\":\"L1BGAEAAA2f\",\"products\":[";
		
		jsonResponse += this.getItemFromDigitalCollection(OverDriveAPIResultsMother.overDriveID_one);
		jsonResponse += "," + this.getItemFromDigitalCollection(OverDriveAPIResultsMother.overDriveID_two);
		
		jsonResponse +=	"],\"links\":{\"self\":{\"href\":\"https://api.overdrive.com/v1/collections/L1BGAEAAA2f/products?limit=3&offset=0\",\"type\":\"application/vnd.overdrive.api+json\"},\"first\":{\"href\":\"https://api.overdrive.com/v1/collections/L1BGAEAAA2f/products?limit=3&offset=0\",\"type\":\"application/vnd.overdrive.api+json\"},\"next\":{\"href\":\"https://api.overdrive.com/v1/collections/L1BGAEAAA2f/products?limit=3&offset=3\",\"type\":\"application/vnd.overdrive.api+json\"},\"last\":{\"href\":\"https://api.overdrive.com/v1/collections/L1BGAEAAA2f/products?limit=3&offset=15003\",\"type\":\"application/vnd.overdrive.api+json\"}}}";
		
		return (JSONObject) JSONValue.parse(jsonResponse);
	}
	
	public JSONArray getArrayItemsFromDigitalCollection()
	{
		return (JSONArray) this.getDigitalCollection().get("products");
	}
	
	
	public JSONObject getItemFromDigitalCollection()
	{
		return this.getItemFromDigitalCollection(OverDriveAPIResultsMother.overDriveID_one);
	}
	
	public JSONObject getItemFromDigitalCollection(String OverDriveId)
	{
		String jsonString = "{\"id\":\"" + OverDriveId + "\",\"mediaType\":\"eBook\",\"title\":\"Gone Girl\",\"subtitle\":\"A Novel\",\"primaryCreator\":{\"role\":\"Author\",\"name\":\"Gillian Flynn\"},\"formats\":[{\"id\":\"ebook-epub-adobe\",\"name\":\"Adobe EPUB eBook\"},{\"id\":\"ebook-kindle\",\"name\":\"Kindle Book\"}],\"images\":{\"thumbnail\":{\"href\":\"http://images.contentreserve.com/ImageType-200/0111-1/{" + OverDriveId + "}Img200.jpg\",\"type\":\"image/jpeg\"}},\"contentDetails\":[{\"href\":\"www.emedia2go.org/ContentDetails.htm?ID=" + OverDriveId + "\",\"type\":\"text/html\",\"account\":{\"id\":1344,\"name\":\"Douglas County Libraries (CO)\"}}],\"links\":{\"self\":{\"href\":\"https://api.overdrive.com/v1/collections/L1BGAEAAA2f/products/" + OverDriveId + "\",\"type\":\"application/vnd.overdrive.api+json\"},\"metadata\":{\"href\":\"https://api.overdrive.com/v1/collections/L1BGAEAAA2f/products/" + OverDriveId + "/metadata\",\"type\":\"application/vnd.overdrive.api+json\"},\"availability\":{\"href\":\"https://api.overdrive.com/v1/collections/L1BGAEAAA2f/products/" + OverDriveId + "/availability\",\"type\":\"application/vnd.overdrive.api+json\"}}}";
		return (JSONObject) JSONValue.parse(jsonString);
	}
	
	public JSONObject getItemFromDigitalCollectionNoAuthor(String OverDriveId)
	{
		String jsonString = "{\"id\":\"" + OverDriveId + "\",\"mediaType\":\"eBook\",\"title\":\"Gone Girl\",\"subtitle\":\"A Novel\",\"name\":\"Gillian Flynn\"},\"formats\":[{\"id\":\"ebook-epub-adobe\",\"name\":\"Adobe EPUB eBook\"},{\"id\":\"ebook-kindle\",\"name\":\"Kindle Book\"}],\"images\":{\"thumbnail\":{\"href\":\"http://images.contentreserve.com/ImageType-200/0111-1/{" + OverDriveId + "}Img200.jpg\",\"type\":\"image/jpeg\"}},\"contentDetails\":[{\"href\":\"www.emedia2go.org/ContentDetails.htm?ID=" + OverDriveId + "\",\"type\":\"text/html\",\"account\":{\"id\":1344,\"name\":\"Douglas County Libraries (CO)\"}}],\"links\":{\"self\":{\"href\":\"https://api.overdrive.com/v1/collections/L1BGAEAAA2f/products/" + OverDriveId + "\",\"type\":\"application/vnd.overdrive.api+json\"},\"metadata\":{\"href\":\"https://api.overdrive.com/v1/collections/L1BGAEAAA2f/products/" + OverDriveId + "/metadata\",\"type\":\"application/vnd.overdrive.api+json\"},\"availability\":{\"href\":\"https://api.overdrive.com/v1/collections/L1BGAEAAA2f/products/" + OverDriveId + "/availability\",\"type\":\"application/vnd.overdrive.api+json\"}}}";
		return (JSONObject) JSONValue.parse(jsonString);
	}
	
	public JSONObject getItemMetadata(String OverDriveId, Boolean showAuthor)
	{
		String jsonString = "{\"id\":\"" + OverDriveId + "\",\"mediaType\":\"eBook\",\"title\":\"The Adventures of Sherlock Holmes\",\"sortTitle\":\"Adventures of Sherlock Holmes\",\"series\":\"Sherlock Holmes\",\"publisher\":\"Duke Classics\",\"publishDate\":\"01/01/0001\",";
		
		if(showAuthor)
		{
			jsonString += "\"creators\":[{\"role\":\"Author\",\"name\":\"Sir Arthur Conan Doyle\",\"fileAs\":\"Doyle, Sir Arthur Conan\"}]";
		}
		
		jsonString += ",\"links\":{\"self\":{\"href\":\"https://api.overdrive.com/v1/collections/L1BGAEAAA2f/products/76c1b7d0-17f4-4c05-8397-c66c17411584/metadata\",\"type\":\"application/vnd.overdrive.api+json\"}},\"images\":{\"thumbnail\":{\"href\":\"http://images.contentreserve.com/ImageType-200/2389-1/{76C1B7D0-17F4-4C05-8397-C66C17411584}Img200.jpg\",\"type\":\"image/jpeg\"},\"cover\":{\"href\":\"http://images.contentreserve.com/ImageType-100/2389-1/{76C1B7D0-17F4-4C05-8397-C66C17411584}Img100.jpg\",\"type\":\"image/jpeg\"}},\"languages\":[{\"code\":\"en\",\"name\":\"English\"}],\"isPublicDomain\":false,\"isPublicPerformanceAllowed\":false,\"shortDescription\":\"<p><i>The Adventures of Sherlock Holmes</i> collects Sir Arthur Conan Doyle's first twelve short stories about his famous London detective. It begins with the first meeting of Holmes and his sidekick Watson, who narrates the stories. Doyle was the first to employ the sidekick technique, thereby creating a character in just as much suspense and awe as his readership at the mental escapades of the erratic, terrifyingly intelligent Holmes.\",\"fullDescription\":\"<p><i>The Adventures of Sherlock Holmes</i> collects Sir Arthur Conan Doyle's first twelve short stories about his famous London detective. It begins with the first meeting of Holmes and his sidekick Watson, who narrates the stories. Doyle was the first to employ the sidekick technique, thereby creating a character in just as much suspense and awe as his readership at the mental escapades of the erratic, terrifyingly intelligent Holmes.\",\"starRating\":4.0,\"popularity\":4820,\"subjects\":[{\"value\":\"Classic Literature\"},{\"value\":\"Fiction\"},{\"value\":\"Mystery\"}],\"formats\":[{\"id\":\"ebook-pdf-adobe\",\"name\":\"Adobe PDF eBook\",\"fileName\":\"AdventuresofSherlockHolmes9781620115091\",\"identifiers\":[{\"type\":\"ISBN\",\"value\":\"9781620115091\"}],\"fileSize\":1521697,\"partCount\":0,\"onSaleDate\":\"02/20/2012\",\"rights\":[{\"type\":\"Copying\",\"value\":-1},{\"type\":\"Printing\",\"value\":-1},{\"type\":\"Lending\",\"value\":0},{\"type\":\"ReadAloud\",\"value\":1},{\"type\":\"ExpirationRights\",\"value\":0}]},{\"id\":\"ebook-epub-adobe\",\"name\":\"Adobe EPUB eBook\",\"fileName\":\"AdventuresofSherlockHolmes9781620115091\",\"identifiers\":[{\"type\":\"ISBN\",\"value\":\"9781620115091\"}],\"fileSize\":383229,\"partCount\":0,\"onSaleDate\":\"02/20/2012\",\"rights\":[{\"type\":\"Copying\",\"value\":-1},{\"type\":\"Printing\",\"value\":-1},{\"type\":\"Lending\",\"value\":0},{\"type\":\"ReadAloud\",\"value\":1},{\"type\":\"ExpirationRights\",\"value\":0}],\"samples\":[{\"source\":\"From the book\",\"url\":\"http://excerpts.contentreserve.com/FormatType-410/2389-1/76C/1B7/D0/AdventuresofSherlockHolmes9781620115091.epub\"}]},{\"id\":\"ebook-kindle\",\"name\":\"Kindle Book\",\"fileName\":\"AdventuresofSherlockHolmes9781620115091\",\"identifiers\":[{\"type\":\"ASIN\",\"value\":\"B0031RS42A\"}],\"fileSize\":0,\"partCount\":0,\"onSaleDate\":\"02/20/2012\"},{\"id\":\"ebook-pdf-open\",\"name\":\"Open PDF eBook\",\"fileName\":\"AdventuresofSherlockHolmes9781620115091\",\"identifiers\":[{\"type\":\"ASIN\",\"value\":\"B0031RS42A\"}],\"fileSize\":1520364,\"partCount\":0,\"onSaleDate\":\"02/20/2012\"},{\"id\":\"ebook-epub-open\",\"name\":\"Open EPUB eBook\",\"fileName\":\"AdventuresofSherlockHolmes9781620115091\",\"identifiers\":[{\"type\":\"ASIN\",\"value\":\"B0031RS42A\"}],\"fileSize\":379267,\"partCount\":0,\"onSaleDate\":\"02/20/2012\"}]}";
		return (JSONObject) JSONValue.parse(jsonString);
	}
	
	public JSONObject getItemMetadata()
	{
		return this.getItemMetadata("76c1b7d0-17f4-4c05-8397-c66c17411584", true);
	}
	
	public JSONObject getItemMetadata(String OverDriveId)
	{
		return this.getItemMetadata(OverDriveId, true);
	}
	
	public JSONObject getItemMetadataNoAuthor()
	{
		return this.getItemMetadata("76c1b7d0-17f4-4c05-8397-c66c17411584", false);
	}
}