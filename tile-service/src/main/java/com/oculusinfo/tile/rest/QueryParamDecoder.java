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
package com.oculusinfo.tile.rest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;

public class QueryParamDecoder {

    /**
     * Decodes a query parameter string representing a JSONObject using dot notation.
     * @param params query parameter string
     * @return the JSON object the query parameter string represents. Returns null if
     * there is no parameters, or if there is invalid input.
     *
     * Example:
     * <pre>
     * <code>
     *
     *     String query = "renderer.ramp=spectral&renderer.coarseness=2&valueTransform.type=log10";
     *     JSONObject queryObject = QueryParamDecoder.decode( query );
     *     System.out.println( queryObject.toString( 4 ) );
     *
     *     Output:
     *
     *     {
     *         renderer: {
     *             ramp: "spectral",
     *             coarseness: 2
     *         },
     *         valueTransform: {
     *             type: "log10"
     *         }
     *     }
     *  </code>
     *  </pre>
     */
    static public JSONObject decode( String params ) {
		if ( params == null ) {
            return null;
        }
        JSONObject query = new JSONObject();
        try {
			params = URLDecoder.decode(params, "UTF-8");
			List<String> paramArray = Arrays.asList( params.split( "&" ) );
			for ( String param : paramArray ) {
                // break param into key value pair
                List<String> keyValue = Arrays.asList( param.split( "=" ) );
                String key = keyValue.get( 0 );
				try {
					String paramValue = keyValue.get( 1 );
					List<String> value =  Arrays.asList( paramValue.split(",") );
					if ( value.size() == 0 ) {
						value.add( keyValue.get( 1 ) );
					}
					// split key into array of sub paths
					List<String> paramPath = Arrays.asList( key.split( "\\." ) );
					JSONObject node = query;
					for ( int i=0; i<paramPath.size(); i++ ) {
						String subpath = paramPath.get( i );
						if ( i != paramPath.size()-1 ) {
							if ( !node.has( subpath ) ) {
								node.put( subpath, new JSONObject() );
							}
							node = node.getJSONObject( subpath );
						} else {
							if ( value.size() == 1 ) {
								// single value
								node.put( subpath, value.get( 0 ) );
							} else {
								// array value
								JSONArray valueArray = new JSONArray();
								for ( String val : value ) {
									valueArray.put( val );
								}
								node.put( subpath, valueArray );
							}
						}
					}
				} catch ( Exception e ) {
					// ignore and move onto next
				}
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            return null;
        }
        return query;
    }
}
