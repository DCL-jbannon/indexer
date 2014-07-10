package org.vufind.config;

import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;
import org.ini4j.Profile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Created by jbannon on 7/3/14.
 */
public class ConfigFiller {


    public static void main(String[] args) {
        //just for testing
        String confileFileLoc = ""; //args[0];

        DynamicConfig config = new DynamicConfig();

        fill(config, Arrays.asList(ConfigOption.values()), new File("./"));
        int i = 0;
    }

    //static void fill(DynamicConfig config, Class<? extends I_ConfigOption> optionsClass, File configFolder) {
    public static void fill(DynamicConfig config, List<I_ConfigOption> options, File configFolder) {
        I_ConfigOption firstOption = options.get(0);
        if(config.isFilledFor(firstOption.getClass())) {
            return;
        }
        String iniPath = firstOption.getClass().getName()+".ini";
        File iniFile = null;
        try {
            iniFile = new File(configFolder.getCanonicalPath()+"/"+iniPath);
            fillFromIni(config, options, iniFile);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private static void fillFromIni(DynamicConfig config, List<I_ConfigOption> options, File configFile) {

        Ini ini = loadIni(configFile);
        String extendsINI = ini.get("INI", "extends");
        if(extendsINI != null && extendsINI.length() > 0) {
            File extendsFile = new File(configFile.getParentFile().getAbsoluteFile()+"/"+extendsINI);
            if(extendsFile.exists() && !extendsFile.isDirectory()) {
                fillFromIni(config, options, extendsFile);
            }
        }

        for(I_ConfigOption option : options) {
            Function f = option.getFillFunction();
            if(!option.isList()) {
                config.put(option, option.getFillFunction().apply(ini.get("DATA", option.name())));

            } else {
                Ini.Section section = ini.get("DATA");
                List<String> vals = section.getAll(option.name());
                Stream<String> stream = vals.stream();

                config.put(option, Arrays.asList(stream.map(option.getFillFunction()).toArray()));
            }
        }
    }

    static private Ini loadIni(File file) {
        Ini ini = new Ini();
        try {
            ini.load(new FileReader(file));
        } catch (InvalidFileFormatException e) {
            System.exit(-1);
        } catch (FileNotFoundException e) {
            System.exit(-1);
        } catch (IOException e) {
            System.exit(-1);
        }

        return ini;
    }
}
