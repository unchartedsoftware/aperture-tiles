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
package com.oculusinfo.tile.spi;

import java.awt.image.BufferedImage;
import java.util.UUID;

import org.json.JSONObject;

public interface ImageTileService {
	
	/**
	 * Given options (colour ramp to use, scale), return an ID
	 * to use as the TMS 'layer name' for constructing TMS requests. 
	 * This ID will be used by getTile to retrieve the options used
	 * supplied here so it can render the image with them.
	 * @param hostUrl
	 * @param options
	 * @return A JSON object containing (at least) a field called 'id', to use as the TMS layer identifier.
	 */
	public JSONObject getLayer(String hostUrl, JSONObject options);

	/**
	 * TMS tile request.
	 * 
	 * @param id - 'default' is ok - means use server defaults. Use getLayer (/layer) to obtain an id.
	 * @param layer
	 * @param zoomLevel
	 * @param x
	 * @param y
	 * @return rendered image.
	 */
	public BufferedImage getTileImage (UUID id, String layer, int zoomLevel, double x, double y);

	public JSONObject getTileObject (UUID fromString, String layer, int zoomLevel, double x, double y);
	
}
