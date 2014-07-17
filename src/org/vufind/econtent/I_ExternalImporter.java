package org.vufind.econtent;

import org.vufind.config.DynamicConfig;

/**
 * Created by jbannon on 7/16/2014.
 */
public interface I_ExternalImporter {
    public void importRecords();
    public boolean init(DynamicConfig config);
}
