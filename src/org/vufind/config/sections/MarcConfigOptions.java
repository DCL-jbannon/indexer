package org.vufind.config.sections;

import org.vufind.config.ConfigMethods;
import org.vufind.config.I_ConfigOption;

import java.util.function.Function;

/**
 * Created by jbannon on 7/7/14.
 */
public enum MarcConfigOptions implements I_ConfigOption {
    MARC_RECORD_PROCESSOR(ConfigMethods::fillClass, true),
    SHOULD_REINDEX_UNCHANGED_RECORDS(ConfigMethods::fillBool, false),
    MARC_FOLDER(ConfigMethods::fillSimpleString, false),
    MARC_PROPERTIES(ConfigMethods::fillSimpleString, true)
    ;

    final private Function fillFunction;
    final private boolean isList;

    MarcConfigOptions(Function fillFunction, boolean isList) {
        this.isList = isList;
        this.fillFunction = fillFunction;
    }

    public Function getFillFunction() {
        return fillFunction;
    }

    public boolean isList() {
        return this.isList;
    }
}
