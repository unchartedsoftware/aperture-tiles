/**
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
package com.oculusinfo.binning.io.serialization.impl;

import com.oculusinfo.binning.io.serialization.GenericJSONSerializer;
import com.oculusinfo.factory.util.Pair;
import com.oculusinfo.binning.util.TypeDescriptor;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

public class StringLongPairArrayMapJsonSerializer extends GenericJSONSerializer<Map<String, List<Pair<String, Long>>>>{
	private static final long serialVersionUID = -7445619308538292627L;
	private static final TypeDescriptor TYPE_DESCRIPTOR = new TypeDescriptor(Map.class,
                                                                                new TypeDescriptor(String.class),
                                                                                new TypeDescriptor(List.class,
                                                                                    new TypeDescriptor(Pair.class,
                                                                                        new TypeDescriptor(String.class),
                                                                                        new TypeDescriptor(Long.class))));

	public StringLongPairArrayMapJsonSerializer() {
		super();
	}

	@Override
	public TypeDescriptor getBinTypeDescription () {
		return TYPE_DESCRIPTOR;
	}

	@Override
	
	/**
	 * 		{
	 * 			string : {  
	 * 				string : long, 
	 * 				string : long,
	 * 			    string : long
	 * 				...
	 * 			},
	 * 			string : {  
	 * 				string : long, 
	 * 				string : long,
	 * 			    string : long
	 * 				...
	 * 			}
	 * 			...
	 * 		}
	 * 
	 */
	
	public Object translateToJSON (Map<String, List<Pair<String, Long>>> value) {

		JSONObject output = new JSONObject();
		try {
			
			for (Map.Entry<String, List<Pair<String, Long>>> entry : value.entrySet() ) {		    	
		    	
		    	String key = entry.getKey();
		    	List<Pair<String, Long>> list = entry.getValue();
		    	
		    	//JSONArray pairMap = new JSONArray();	
		    	JSONObject entryObj = new JSONObject();
		    	for ( Pair<String, Long> pair : list ) {	    		
		    		
					entryObj.put(pair.getFirst(), pair.getSecond());
					//pairMap.put(entryObj);
		    	}
		    	output.put( key, entryObj );		    	
		    }

		} catch (JSONException e) {
			e.printStackTrace();
		}
		return output;
	}

	
	@Override
	protected Map<String, List<Pair<String, Long>>> getValue(Object obj) throws JSONException {
		
		JSONObject entry = (JSONObject) obj;
		
		if (entry.length() == 0) {
			return null;
		}
		
		Map<String, List<Pair<String, Long>>> result = new LinkedHashMap<>(); 

		@SuppressWarnings("unchecked")
		Iterator<String> keys = entry.keys();
		while (keys.hasNext()){
			
			String key = keys.next();
			// for each priority
			List<Pair<String, Long>> list = new ArrayList<>();

			JSONObject jsonMap = entry.getJSONObject(key);
			
			@SuppressWarnings("unchecked")
			Iterator<String> moreKeys = jsonMap.keys();
			while (moreKeys.hasNext()){
		
				String anotherKey = moreKeys.next();				
				Long anotherVal = jsonMap.getLong(anotherKey);
				
				list.add( new Pair<String, Long>(anotherKey, anotherVal) );
			}
			
			result.put( key, list );
		}
		
		return result;
	}
}
