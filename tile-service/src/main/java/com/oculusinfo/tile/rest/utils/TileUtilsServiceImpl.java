/*
 * Copyright (c) 2014 Oculus Info Inc.
 * http://www.oculusinfo.com/
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
package com.oculusinfo.tile.rest.utils;


import com.google.inject.Singleton;



import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * 
 * 
 *
 */
@Singleton
public class TileUtilsServiceImpl implements TileUtilsService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TileUtilsServiceImpl.class);

    /* (non-Javadoc)
	 * @see TileUtilsServiceImpl#getTranslationGoogle(JSONObject query)
	 */
	public JSONObject getTranslationGoogle( JSONObject query ) {
		JSONObject result = null;
		
		//Translator translator = new Translator(args[0]);
		
        try {
    		String sourceText = query.getString("text");
    		String targetLang = query.getString("targetLang");

			//Translation translation = translator.translate( sourceText, null, targetLang);
			
		} catch (/*TranslatorException*/JSONException e) {
			LOGGER.error("Google Translate API returned an error " + e.getMessage());
			e.printStackTrace();
		} catch ( Exception e) {
			LOGGER.error("Google Translate API returned an error " + e.getMessage());
			e.printStackTrace();
		} finally {
			//translator.close();
		}
		
		return result;
    }
}
