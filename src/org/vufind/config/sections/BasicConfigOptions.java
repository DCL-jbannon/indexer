package org.vufind.config.sections;

import org.vufind.config.ConfigMethods;
import org.vufind.config.I_ConfigOption;

import java.util.function.Function;

/**
 * Created by jbannon on 7/7/14.
 */
public enum BasicConfigOptions implements I_ConfigOption {
    CONFIG_FOLDER(ConfigMethods::fillSimpleString, false),
    BASE_SOLR_URL(ConfigMethods::fillSimpleString, false),
    PRINT_CORE(ConfigMethods::fillSimpleString, false),
    ECONTENT_CORE(ConfigMethods::fillSimpleString, false),
    VUFINDDB_DRIVER(ConfigMethods::fillSimpleString, false),
    VUFINDDB_URL(ConfigMethods::fillSimpleString, false),
    VUFINDDB_USER(ConfigMethods::fillSimpleString, false),
    VUFINDDB_PASS(ConfigMethods::fillSimpleString, false),
    ECONTENTDB_DRIVER(ConfigMethods::fillSimpleString, false),
    ECONTENTDB_URL(ConfigMethods::fillSimpleString, false),
    ECONTENTDB_USER(ConfigMethods::fillSimpleString, false),
    ECONTENTDB_PASS(ConfigMethods::fillSimpleString, false),
    TRANSLATION_MAPS_FOLDER(ConfigMethods::fillSimpleString, false),
    SCRIPTS_FOLDER(ConfigMethods::fillSimpleString, false),
    DO_FULL_REINDEX(ConfigMethods::fillBool, false),
    BOOK_COVER_URL(ConfigMethods::fillSimpleString, false),
    VUFIND_URL(ConfigMethods::fillSimpleString, false),
    ;

    final private Function fillFunction;
    final private boolean isList;

    BasicConfigOptions(Function fillFunction, boolean isList) {
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
