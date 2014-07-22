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
package com.oculusinfo.twitter.translate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Machine Translation using Google Translate API, which is accessed using REST
 * calls
 * 
 * (NOTE: This class requires a Google Translate API KEY string to be set as an
 * environment variable named GOOGLE_TRANSLATE_KEY.)
 * 
 * @author dgiesbrecht
 */
public class MachineTranslation {
	
	/**
	 * Translate text string from one language to another.
	 * 
	 * @param sourceText
	 *            Original text string to be translated.
	 * 
	 * @param sourceLangID
	 *            Language ID of source text (NOTE: must be iso639-1 language
	 *            code format). If this sourceLangID string is empty then Google
	 *            Translate will try to auto-detect the source language
	 * @param targetLangID
	 *            Desired language ID of translated text (NOTE: must be iso639-1
	 *            language code format).
	 * 
	 * @return Translated text string (NOTE: if any exceptions are caught, the
	 *         original (untranslated) text string will be returned.
	 */
	public String translateText(String sourceText, String sourceLangID, String targetLangID) {

		String translatedText = "";
		String googleTranslateKey = System.getenv("GOOGLE_TRANSLATE_KEY");	//Google Translate API key (set as env variable)
		
		if (sourceText.isEmpty() || sourceLangID == targetLangID) {
			// just return original text string if sourceText is empty or if source and target language IDs are the same
			return sourceText;
		}
		
		String sourceTextNoWS = sourceText.replace(" ", "%20");	//replace whitespace with "%20"
		
		// concatenate full translation message to send to Google Translate API using 'GET' REST call
		String translateMessage;
		if (sourceLangID.isEmpty()) {	
			//if no source language ID is empty then Google Translate will try to auto-detect source language
			translateMessage = "https://www.googleapis.com/language/translate/v2?key=" + googleTranslateKey + 
				"&target=" + targetLangID + "&q=" + sourceTextNoWS;
		}
		else {
			translateMessage = "https://www.googleapis.com/language/translate/v2?key=" + googleTranslateKey + "&source=" +
				sourceLangID + "&target=" + targetLangID + "&q=" + sourceTextNoWS;
		}
		
		try {
			URL url = new URL(translateMessage);			
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();

			if (conn.getResponseCode() != 200) {
				throw new IOException(conn.getResponseMessage());	// REST call NOT OK
			}
			
			// Buffer the result into a string
			BufferedReader rd = new BufferedReader(
				new InputStreamReader(conn.getInputStream(), "UTF-8"));
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = rd.readLine()) != null) {
				sb.append(line);
			}
			rd.close();
			conn.disconnect();
			String resultString = sb.toString();
			
			//System.out.println(resultString);
			
			// Parse JSON results to get translated text
			JSONObject json = new JSONObject(resultString);
			JSONObject dataJson = json.getJSONObject("data");
			JSONArray translationsJson = dataJson.getJSONArray("translations");
			JSONObject translationElement = translationsJson.getJSONObject(0);
			translatedText = translationElement.getString("translatedText");	
			
			//System.out.println(translatedText);
		}
		catch (IOException e) {		// return original (untranslated) sourceText if there are any exceptions
			e.printStackTrace();
			return sourceText;	
		}
		catch (JSONException e) {	
			e.printStackTrace();
			return sourceText;
		}
	
		return translatedText;
	}		
}
