/*
 * Copyright (c) 2015 Uncharted Software
 * https://uncharted.software/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.tile.rest.config;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class ConfigPropertiesUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigPropertiesUtil.class);

    public static final String CONFIG_ENV_VAR = "TILE_CONFIG_PROPERTIES";
    public static final String DEFAULT_CONFIG_PROPERTIES = "default-config.properties";
    
	public Properties getConfigProperties() throws ConfigException {
		String pathToProperties = getPathToProperties();
	    if (StringUtils.isEmpty(pathToProperties)) {
	        return null;
	    }
	
	    try (InputStream input = new FileInputStream(pathToProperties)) {
	        Properties properties = new Properties();
	        properties.load(input);
	        return properties;
	    } catch (IOException e) {
            throw new ConfigException(String.format("Unable to read properties file %s", pathToProperties), e);
        }
	}
	
	protected String getPathToProperties() throws ConfigException {
        String pathToProperties;
        String configEnvVar = System.getenv(CONFIG_ENV_VAR);
        if (StringUtils.isNotEmpty(configEnvVar)) {
            pathToProperties = configEnvVar;
            LOGGER.warn("{} environment variable is set, using properties file {} ", CONFIG_ENV_VAR, pathToProperties);
        } else {
            pathToProperties = getPathToPropertiesFromClasspath();
            LOGGER.warn("{} environment variable NOT set, using default file from classpath {}", CONFIG_ENV_VAR, pathToProperties);
        }
        return pathToProperties;
    }
	
	protected String getPathToPropertiesFromClasspath() {
        String path = null;
        URL resource = this.getClass().getClassLoader().getResource(DEFAULT_CONFIG_PROPERTIES);
        if (resource != null) {
            path = resource.getPath();
        }
        return path;
    }

}
