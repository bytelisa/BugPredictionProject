package org.example.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigurationManager {

    private static ConfigurationManager instance;
    private final Properties properties;

    // Singleton pattern
    private ConfigurationManager() {

        properties = new Properties();

        String configFile = "config.properties";
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(configFile)) {
            if (input == null) {
                Printer.errorPrint("Unable to find " + configFile);
                return;
            }
            properties.load(input);

        } catch (IOException ex) {
            Printer.errorPrint("Unable to open " + configFile);
            ex.printStackTrace();
        }
    }


    public static ConfigurationManager getInstance() {
        if (instance == null) {
            instance = new ConfigurationManager();
        }
        return instance;
    }


    public String getProperty(String key) {
        return properties.getProperty(key);
    }
}