package com.oculusinfo.tile.rest.config;

import java.io.File;

/**
 * The ConfigService is responsible for replacing property keys in templated configuration files with values from a properties file.
 *
 * @author danielabar
 */
public interface ConfigService {

    String replaceProperties(File configFile) throws ConfigException;

}
