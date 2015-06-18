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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigServiceImpl.class);

    @Override
    public String replaceProperties(File configFile) throws IOException {
        String configFileContent = new String(Files.readAllBytes(Paths.get(configFile.getPath())), StandardCharsets.UTF_8);
        Properties properties = new Properties();
        String pathToProperties = System.getenv("TILE_CONFIG_PROPERTIES");
        if (pathToProperties == null) {
            LOGGER.warn("TILE_CONFIG_PROPERTIES environment variable not set, returning original config file content");
            return configFileContent;
        }
        Map<String, String> replacements = new HashMap<>();

        try (InputStream input = new FileInputStream(pathToProperties)) {
            properties.load(input);
            Enumeration e = properties.propertyNames();
            LOGGER.info("Loaded tile config properties file: {}", pathToProperties);

            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                replacements.put(key, properties.getProperty(key));
            }
        } catch (IOException e) {
            LOGGER.error("Unable to read tile config properties file {}.", pathToProperties, e);
        }

        return replaceTokens(configFileContent, replacements);
    }

    // http://stackoverflow.com/questions/959731/how-to-replace-a-set-of-tokens-in-a-java-string
    protected String replaceTokens(String text, Map<String, String> replacements) {
        Pattern pattern = Pattern.compile("\\[(.+?)\\]");
        Matcher matcher = pattern.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String replacement = replacements.get(matcher.group(1));
            if (replacement != null) {
                matcher.appendReplacement(buffer, "");
                buffer.append(replacement);
            }
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
