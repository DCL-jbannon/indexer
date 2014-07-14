package org.vufind.config.sections;

import org.vufind.config.ConfigMethods;
import org.vufind.config.I_ConfigOption;

import java.util.function.Function;

/**
 * Created by jbannon on 7/7/14.
 */
public enum StrandsConfigOptions implements I_ConfigOption {
    FULL_RECATALOG_CSV_LOC(ConfigMethods::fillSimpleString, false),
    PARTIAL_RECATALOG_PRINT_CSV_LOC(ConfigMethods::fillSimpleString, false),
    PARTIAL_RECATALOG_ECONTENT_CSV_LOC(ConfigMethods::fillSimpleString, false),
    ;

    final private Function fillFunction;
    final private boolean isList;

    StrandsConfigOptions(Function fillFunction, boolean isList) {
        this.isList = isList;
        this.fillFunction = fillFunction;
    }

    public Function getFillFunction() {
        return fillFunction;
    }

    @Override
    public boolean isList() {
        return this.isList;
    }
}
