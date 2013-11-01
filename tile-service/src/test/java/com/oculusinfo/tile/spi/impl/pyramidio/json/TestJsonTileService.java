/**
 * Copyright (C) 2013 Oculus Info Inc. 
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
package com.oculusinfo.tile.spi.impl.pyramidio.json;

import java.io.IOException;

import com.oculusinfo.tile.spi.JsonTileService;
import com.oculusinfo.tile.spi.impl.pyramidio.json.JsonTileServiceImpl;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import com.oculusinfo.binning.io.PyramidIO;

import junit.framework.TestCase;
import static org.mockito.Mockito.*;

/**
 * @author dgray
 *
 */
public class TestJsonTileService extends TestCase {

	@Test
	public void testGetTilesByBounds() throws IOException, JSONException{
		String dummyLayer = "dummyLayer";
		PyramidIO mockPio = mock(PyramidIO.class);
		when(mockPio.readMetaData(dummyLayer)).thenReturn("{\"projection\": \"EPSG:900913\"}");
		
		JsonTileService service = new JsonTileServiceImpl(mockPio, null);
		
		String request = "{ "
						+ "\"layer\": \"dummyLayer\","
						+ "\"level\": 0," 
						+ "\"bounds\": {"
								+ "\"xmin\": 0,"
								+ "\"ymin\": 0,"
								+ "\"xmax\": 0,"
								+ "\"ymax\": 0"
						+ "}"
				+ "}";
		JSONObject jsonRequest = new JSONObject(request);
		JSONObject jsonResult = service.getTiles(jsonRequest);
		
		assertNotNull(jsonResult);
	}
	
}
