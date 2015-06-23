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

import com.google.inject.Singleton;
import com.oculusinfo.tile.rest.layer.LayerServiceImpl;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class ConfigServiceImpl implements ConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LayerServiceImpl.class);

    public static final String CONFIG_ENV_VAR = "TILE_CONFIG_PROPERTIES";
    public static final String DEFAULT_CONFIG_PROPERTIES = "default-config.properties";

    @Override
    public String replaceProperties(File configFile) throws ConfigException {
        try {
            LOGGER.info("Processing configFile {}", configFile);
            Map<String, String> replacements = buildReplacements();
            String configFileContent = new String(Files.readAllBytes(Paths.get(configFile.getPath())), StandardCharsets.UTF_8);
            if (replacements != null) {
                return replaceTokens(configFileContent, replacements);
            } else {
                LOGGER.warn("No config properties found, using file as is {}", configFile.getAbsolutePath());
                return configFileContent;
            }
        } catch (IOException e) {
            throw new ConfigException(String.format("Unable to read config file %s", configFile), e);
        }
    }

    protected Map<String, String> buildReplacements() throws ConfigException {
        String pathToProperties = getPathToProperties();
        if (StringUtils.isEmpty(pathToProperties)) {
            return null;
        }

        try (InputStream input = new FileInputStream(pathToProperties)) {
            Map<String, String> replacements = new HashMap<>();
            Properties properties = new Properties();
            properties.load(input);
            Enumeration e = properties.propertyNames();
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                replacements.put(key, properties.getProperty(key));
            }
            return replacements;
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

    // http://stackoverflow.com/questions/959731/how-to-replace-a-set-of-tokens-in-a-java-string
    protected String replaceTokens(String text, Map<String, String> replacements) {
        Pattern pattern = Pattern.compile("\\[(.+?)\\]");
        Matcher matcher = pattern.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String matchedKey = matcher.group(1);
            String replacement = replacements.get(matchedKey);
            if (replacement != null) {
                LOGGER.info("Replacing {} with {}", matchedKey, replacement);
                matcher.appendReplacement(buffer, "");
                buffer.append(replacement);
            }
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
