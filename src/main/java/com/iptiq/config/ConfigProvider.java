package com.iptiq.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * com.iptiq.Application configuration loader.
 * In normal times a framework IOC approach would be used (spring boot or other..).
 * Here, for the sake of using no external framework, let's define our config singleton and load config from the yaml file
 * application_config.yaml.
 */
public class ConfigProvider {

    static Logger logger = LoggerFactory.getLogger(ConfigProvider.class);

    private ApplicationConfig config;

    private ConfigProvider() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();

        try {
            config = mapper.readValue(new File("src/main/resources/application_config.yaml"), ApplicationConfig.class);
        } catch (IOException e) {
            logger.error(String.format("error while loading config : %s", e.toString()));
        }
    }

    private static class ApplicationConfigHelper {
        private static final ConfigProvider INSTANCE = new ConfigProvider();
    }

    public static ApplicationConfig getConfig() {
        return ApplicationConfigHelper.INSTANCE.config;
    }

}