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
package com.oculusinfo.tile.rest.translation;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Properties;

import javax.net.ssl.HttpsURLConnection;

import com.google.inject.Singleton;
import com.oculusinfo.tile.rest.config.ConfigException;
import com.oculusinfo.tile.rest.config.ConfigPropertiesUtil;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * A service that will translate given text to a target language based on the parameters 
 * 	passed in.  Currently only supports the Google Translate Service. 
 *
 */
@Singleton
public class TileTranslationServiceImpl implements TileTranslationService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TileTranslationServiceImpl.class);
	
	public static final String TRANSLATE_API_KEY = "translation.api.key";

    /* (non-Javadoc)
	 * @see TileUtilsServiceImpl#getTranslationGoogle(JSONObject query)
	 */
	public JSONObject getTranslation( JSONObject query ) {
    	// get the translation arguments from the query
		JSONObject result = null;
    	try {
    		// as we integrate more translation service we can add a more sophisticated selection mechanism
    		String service = query.getString("service");
    		if ( service.equals( "google" ) ) {
    			// get google translate params
    			String text = query.getString("text");
        		String target = query.getString("target");
        		
        		ConfigPropertiesUtil configUtil = new ConfigPropertiesUtil();
            	Properties properties = configUtil.getConfigProperties();
        		String translationApiKey = properties.getProperty(TRANSLATE_API_KEY);
        		
            	result = translateGoogle( text, target, translationApiKey );
            }	
    	} catch ( JSONException e ) {
    		LOGGER.error( "Incorrect Configuration for Translation API", e );
		} catch ( ConfigException e ) {
			LOGGER.error( "Error with internal configuration properties", e );
		} 
		return result;
	}
       
	/*
	 * Translates the given text using the Google Translate API
	 */
    private JSONObject translateGoogle( String text, String target, String key ) {
		JSONObject result = null;
		try {
			// assumes that the text has already been uri encoded
            String urlStr = "https://www.googleapis.com/language/translate/v2?key=" + key + "&q=" + text + "&target=" + target;
            URL url = new URL( urlStr );
 
            HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
            StringBuilder reply = new StringBuilder();
            InputStream stream;
            if ( connection.getResponseCode() == 200 ) //success
            {	
                stream = connection.getInputStream();
            } else {
                stream = connection.getErrorStream();
            }
 
            BufferedReader reader = new BufferedReader( new InputStreamReader( stream ) );
            String line;
            while (( line = reader.readLine() ) != null ) {
            	reply.append(line);
            }
            result = new JSONObject( reply.toString() );
        } catch ( IOException e ) {
        	LOGGER.error( "Error reading response from Google Translation Service", e );
        } catch (JSONException e) {
        	LOGGER.error( "Error creating JSON Objects from Google Translation Service response", e );
		}
 
        return result;
    }
}
