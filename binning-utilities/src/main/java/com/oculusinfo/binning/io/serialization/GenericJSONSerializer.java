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
package com.oculusinfo.binning.io.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.oculusinfo.binning.TileData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.impl.DenseTileData;


public abstract class GenericJSONSerializer<T> implements TileSerializer<T> {
	private static final long serialVersionUID = 2617903534522413550L;

	protected GenericJSONSerializer () {
	}

	abstract protected T getValue (Object bin) throws JSONException;
	abstract protected Object translateToJSON (T value);

	public String getFileExtension(){
		return "json";
	}

	@Override
	public TileData<T> deserialize (TileIndex index, InputStream rawData){

		String jsonString = convertStreamToString(rawData);
		try {
			
			JSONObject json = new JSONObject(jsonString);			
			int level = json.getInt("level");
			int x = json.getInt("xIndex");
			int y = json.getInt("yIndex");
			int xBins = json.getInt("xBinCount");
			int yBins = json.getInt("yBinCount");

			JSONArray bins = json.getJSONArray("bins");

			List<T> values = new ArrayList<T>();
			for (int i = 0; i < bins.length(); i++) {
				values.add(getValue(bins.get(i)));
			}


			TileIndex tileIndex = new TileIndex(level, x, y, xBins, yBins);
			TileData<T> tile = new DenseTileData<T>( tileIndex, values);
			
			if (json.has("meta")) {
				JSONObject metaData = json.getJSONObject("meta");
				String[] keys = JSONObject.getNames(metaData);
				if (null != keys) {
					for (String key: keys) {
						tile.setMetaData(key, metaData.getString(key));
					}
				}
			}

			return tile;
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return null;
	}

	private String convertStreamToString(java.io.InputStream is) {
		try {
			InputStreamReader isr = new InputStreamReader(is, "UTF-8");
			java.util.Scanner s = new java.util.Scanner(isr);
			s.useDelimiter("\\A");
			String result = s.hasNext() ? s.next() : "";
			s.close();
			return result;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;

	}

	@Override
	public void serialize (TileData<T> tile, OutputStream stream) throws IOException {

		TileIndex tileIndex = tile.getDefinition();

		try {
			JSONObject jsonEntry = new JSONObject();
			
			jsonEntry.put("level", tileIndex.getLevel() );
			jsonEntry.put("xIndex", tileIndex.getX() );
			jsonEntry.put("yIndex", tileIndex.getY() );			
			jsonEntry.put("xBinCount", tileIndex.getXBins() );
			jsonEntry.put("yBinCount", tileIndex.getYBins() );
			
			JSONArray bins = new JSONArray();
			int xBins = tile.getDefinition().getXBins();
			int yBins = tile.getDefinition().getYBins();
			for (int y = 0; y < yBins; ++y) {
				for (int x = 0; x < xBins; ++x) {
					T value = tile.getBin(x, y);
					if (value == null) {
						bins.put( new JSONObject() );
					} else {
						bins.put( translateToJSON(value) );
					}
				}				
			}
			
			jsonEntry.put("bins", bins);

			JSONObject metaData = new JSONObject();
			Collection<String> keys = tile.getMetaDataProperties();
			if (null != keys) {
				for (String key: keys) {
					String value = tile.getMetaData(key);
					if (null != value)
						metaData.put(key, value);
				}
			}
			jsonEntry.put("meta", metaData);

			OutputStreamWriter writer = new OutputStreamWriter(stream, "UTF-8");
			writer.write(jsonEntry.toString());
			writer.close();
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
}
