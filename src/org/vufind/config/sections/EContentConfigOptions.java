package org.vufind.config.sections;

import org.vufind.config.ConfigMethods;
import org.vufind.config.I_ConfigOption;

import java.util.function.Function;

/**
 * Created by jbannon on 7/7/14.
 */
public enum EContentConfigOptions implements I_ConfigOption {
    PROCESSORS(ConfigMethods::fillClass, true),
    COLLECTION_GROUP_MAP_PATH(ConfigMethods::fillSimpleString, false),
    ITEM_TYPE_FORMAT_MAP_PATH(ConfigMethods::fillSimpleString, false),
    DEVICE_COMPATIBILTY_MAP_PATH(ConfigMethods::fillSimpleString, false),
    ECONTENT_RECORD_CLASSES(ConfigMethods::fillSimpleString, true)
    ;

    final private Function fillFunction;
    final private boolean isList;

    EContentConfigOptions(Function fillFunction, boolean isList) {
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
