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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.oculusinfo.tile.rest.layer.LayerServiceImpl;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
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

	public static final String PROPERTIES_FILE_REPLACEMENT_REGEX = "\\$\\{(.+?)\\}";
	public static final String ENV_VAR_REPLACEMENT_REGEX = "\\%\\{(.+?)\\}";

    private ConfigPropertiesService _service;

	@Inject
	public ConfigServiceImpl( ConfigPropertiesService service ) {
		this._service = service;
	}

    @Override
    public String replaceProperties(File configFile) throws ConfigException {
        try {
            LOGGER.info("Processing configFile {}", configFile);
            Map<String, String> replacements = buildReplacements();
            String configFileContent = new String(Files.readAllBytes(Paths.get(configFile.getPath())), StandardCharsets.UTF_8);

			// Do configuration file replacements
			String replacedContent = configFileContent;
			if (replacements != null) {
                replacedContent = replaceTokens(configFileContent, replacements, PROPERTIES_FILE_REPLACEMENT_REGEX, false);
            } else {
                LOGGER.warn("No config properties found, using file as is {}", configFile.getAbsolutePath());
            }

			// Do environment variable replacements
			// Replace undefined environment variables with empty string
			// (Windows doesn't allow you to set empty environment variables - it just deletes them)
			return replaceTokens(replacedContent, System.getenv(), ENV_VAR_REPLACEMENT_REGEX, true);

        } catch (IOException e) {
            throw new ConfigException(String.format("Unable to read config file %s", configFile), e);
        }
    }

    @Override
    public File findResourceConfig(String name) throws URISyntaxException {
        String path = ConfigResource.class.getResource("/config").toURI().getPath();
        File matchingFile = null;
        File configRoot = new File(path);
        File[] files = configRoot.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().equals(name)) {
                    matchingFile = file;
                }
            }
        }
        return matchingFile;
    }

    protected Map<String, String> buildReplacements() throws ConfigException {
		Map<String, String> replacements = new HashMap<>();
    	Properties properties = _service.getConfigProperties();
    	if ( properties != null ) {
	        Enumeration e = properties.propertyNames();
	        while (e.hasMoreElements()) {
	            String key = (String) e.nextElement();
	            replacements.put(key, properties.getProperty(key));
	        }
    	}
        return replacements;
    }

    // http://stackoverflow.com/questions/959731/how-to-replace-a-set-of-tokens-in-a-java-string
    protected String replaceTokens(String text,
								   Map<String, String> replacements,
								   String regex,
								   boolean replaceIfNotDefined) {
        Pattern pattern = Pattern.compile(regex);
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
			else if (replaceIfNotDefined) {
				LOGGER.info("Replacing {} with empty string", matchedKey);
				matcher.appendReplacement(buffer, "");
			}
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
