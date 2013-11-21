/**
 * Copyright (c) 2013 Oculus Info Inc.
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
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.tile.spi.impl.pyramidio.image.renderer;

import java.util.HashMap;
import java.util.Map;

import com.oculusinfo.binning.TileIndex;

/**
 * A general property bag that guarantees certain methods
 * will be available. This does not necessarily mean that the property bag
 * will contain all the properties retrieved in each method.
 * 
 * @author  dgray
 * @author cregnier
 * 
 */
public class RenderParameter {
	
	private Map<String, Object> data;
	
	/**
	 * Creates a RenderParameter based on the given values. 
	 */
	public RenderParameter(String layer, String rampType, String transformId,
			int rangeMin, int rangeMax, int dimension, String levelMaximums,
			TileIndex tileCoordinate, int currentImage) {
		data = new HashMap<>();
		setLayer(layer);
		setRampType(rampType);
		setTransformId(transformId);
		setRangeMin(rangeMin);
		setRangeMax(rangeMax);
		setOutputWidth(dimension);
		setOutputHeight(dimension);
		setLevelMaximums(levelMaximums);
		setTileCoordinate(tileCoordinate);
		setCurrentImage(currentImage);
	}

	public RenderParameter(Map<String, Object> params) {
		data = new HashMap<String, Object>(params);
	}
	
	public Map<String, Object> getRawData() {
		return data;
	}
	
	
	public String getLayer() {
		return getString("layer");
	}



	public void setLayer(String layer) {
		setString("layer", layer);
	}



	public String getRampType() {
		return getString("rampType");
	}



	public void setRampType(String rampType) {
		setString("rampType", rampType);
	}



	public String getTransformId() {
		return getString("transformId");
	}



	public void setTransformId(String transformId) {
		setString("transformId", transformId);
	}



	public int getRangeMin() {
		return getInt("rangeMin");
	}



	public void setRangeMin(int rangeMin) {
		setInt("rangeMin", rangeMin);
	}



	public int getRangeMax() {
		return getInt("rangeMax");
	}



	public void setRangeMax(int rangeMax) {
		setInt("rangeMax", rangeMax);
	}



	public int getOutputWidth() {
		return getInt("outputWidth");
	}



	public void setOutputWidth(int outputWidth) {
		setInt("outputWidth", outputWidth);
	}



	public int getOutputHeight() {
		return getInt("outputHeight");
	}



	public void setOutputHeight(int outputHeight) {
		setInt("outputHeight", outputHeight);
	}



	public String getLevelMaximums() {
		return getString("levelMaximums");
	}



	public void setLevelMaximums(String levelMaximums) {
		setString("levelMaximums", levelMaximums);
	}


	public TileIndex getTileCoordinate() {
		return typedGet("tileCoordinate", TileIndex.class);
	}



	public void setTileCoordinate(TileIndex tileCoordinate) {
		data.put("tileCoordinate", tileCoordinate);
	}



	public int getCurrentImage() {
		return getInt("currentImage");
	}



	public void setCurrentImage(int currentImage) {
		setInt("currentImage", currentImage);
	}


	//-------------------------------------------------------
	// Helpers
	//-------------------------------------------------------
	
	protected String getString(String key) {
		return typedGet(key, String.class);
	}
	
	protected void setString(String key, String value) {
		data.put(key, value);
	}
	
	protected Integer getInt(String key) {
		return typedGet(key, Integer.class);
	}
	
	protected void setInt(String key, Integer value) {
		data.put(key, value);
	}
	
	protected <T> T typedGet(String key, Class<T> clazz) {
		return clazz.cast(data.get(key));
	}
	
}