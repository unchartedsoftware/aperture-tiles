package com.oculusinfo.tile.rest.config;

import java.io.File;
import java.io.IOException;

/**
 * The ConfigService is responsible for converting templated configuration files...
 *
 * @author danielabar
 */
public interface ConfigService {

    String replaceProperties(File configFile) throws IOException;

}
