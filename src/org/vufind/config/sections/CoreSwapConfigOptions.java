package org.vufind.config.sections;

import org.vufind.config.ConfigMethods;
import org.vufind.config.I_ConfigOption;

import java.util.function.Function;

/**
 * Created by jbannon on 7/7/14.
 */
public enum CoreSwapConfigOptions implements I_ConfigOption {
    PRINT_OLD_CORE(ConfigMethods::fillSimpleString, false),
    PRINT_NEW_CORE(ConfigMethods::fillSimpleString, false),
    ECONTENT_OLD_CORE(ConfigMethods::fillSimpleString, false),
    ECONTENT_NEW_CORE(ConfigMethods::fillSimpleString, false),

    ;

    final private Function fillFunction;
    final private boolean isList;

    CoreSwapConfigOptions(Function fillFunction, boolean isList) {
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
