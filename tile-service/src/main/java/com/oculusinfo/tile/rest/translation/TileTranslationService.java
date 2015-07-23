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

import org.json.JSONObject;


public interface TileTranslationService {

    /**
     * Calls the translation service to perform translation given a query object such as:
     * {
     *  	"service": "some-service",   		// currently only supports the google translation service
     *  	"target": "en",				 		// target language to translate to, can be any that google translation service supports
     *  	"text": "client%20supplied%20text"	// text to translate. Should be uri encoded before calling translation service (client side)
     * }
     *
     * Note: make sure to add two properties to the client app config .properties file as follows:
     * 
     * # for translation services
	 * translation.api.key=client-supplied-api-key
	 * translation.api.endpoint=https://www.googleapis.com/language/translate/v2 
     *
     * Then this method will return, if successful):
     * {
     *  	"data": {
     *   		"translations": [
     *      		{
     *           		"translatedText": "translated text",
     *           		"detectedSourceLanguage": "ar"
     *       		}
     *  		]
     *		}
	 * }
	 * 
	 * if unsuccessful (from google translate service): 
	 * {
	 * 		"error": {
	 * 			"message":"Bad Request",
	 * 			"errors": [
	 * 				{
	 * 					"message":"Bad Request",
	 * 					"reason":"keyInvalid",
	 * 					"domain":"usageLimits"
	 * 				}
	 * 			],
	 * 			"code":400
	 * 		}
	 *  }
	 * 
	 * if unsuccessful (if tile-service is incorrectly configured):
	 * {
	 * 		"error": {
	 * 			"message":"Incorrect Translation Service Configuration",
	 * 		}
	 *  } 
     *
     * 
     * @param query The query parameters JSON object to configure translation service.
     * @return JSONObject - the results of the call to the translation service
     */
	public JSONObject getTranslation( JSONObject query );
}
