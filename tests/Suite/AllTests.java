package Suite;

/**
 * http://www.mkyong.com/unittest/junit-4-tutorial-5-suite-test/
 */

import org.vufind.econtent.PopulateSolrOverDriveAPIItemsTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import dbTests.DBeContentRecordServicesTests;
import dcl.utils.ActiveEcontentUtilsTests;
import solr.SolrUtilsTests;
import solr.SolrWrapperTests;
import api.OverDrive.OverDriveAPIServicesTests;
import api.OverDrive.OverDriveAPITests;
import api.OverDrive.OverDriveAPIUtilsTests;
import api.OverDrive.OverDriveAPIWrapperTests;
import api.OverDrive.OverDriveCollectionIteratorTests;

@RunWith(Suite.class)
@Suite.SuiteClasses({
		OverDriveAPIWrapperTests.class,
		OverDriveAPIServicesTests.class,
		OverDriveAPITests.class,
		OverDriveCollectionIteratorTests.class,
		OverDriveAPIUtilsTests.class,
		SolrWrapperTests.class,
		DBeContentRecordServicesTests.class,
		SolrUtilsTests.class,
		PopulateSolrOverDriveAPIItemsTests.class,
		ActiveEcontentUtilsTests.class
})

public class AllTests 
{
}