package config;

import java.io.*;
import java.util.Properties;

public class ConfigManager {
    private static final String CONFIG_FILE = "config.properties";
    private static Properties properties = new Properties();

    static {
        loadConfig();
    }

    private static void loadConfig() {
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            properties.load(input);
        } catch (IOException ex) {
            properties.setProperty("petType", "blue");
            properties.setProperty("allowWander", "true");
            saveConfig();
        }
    }

    public static void saveConfig() {
        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            properties.store(output, null);
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    public static String getPetType() {
        return properties.getProperty("petType", "blue");
    }

    public static void setPetType(String type) {
        properties.setProperty("petType", type);
        saveConfig();
    }

    public static boolean isWanderAllowed() {
        return Boolean.parseBoolean(properties.getProperty("allowWander", "true"));
    }

    public static void setWanderAllowed(boolean allowed) {
        properties.setProperty("allowWander", String.valueOf(allowed));
        saveConfig();
    }
}