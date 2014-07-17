package org.vufind.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jbannon on 7/7/14.
 */
public class ConfigMethods {
    final static Logger logger = LoggerFactory.getLogger(ConfigMethods.class);

    public static Object fillSimpleString(Object o) {
        return o;
    }

    public static Object fillInteger(Object o) {
        return Integer.parseInt(o.toString());
    }

    public static Object fillClass(Object o) {
        try {
            if(o instanceof String) {
                return Class.forName((String)o);
            }
        }catch (Exception e) {
            logger.error("Unable to load configured class", e);
        }

        return null;
    }

    public static Boolean fillBool(Object o) {
        return Boolean.parseBoolean(o.toString());
    }
}
