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

package com.oculusinfo.tile.rendering;

import org.json.JSONException;
import org.json.JSONObject;

import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.tile.rendering.impl.DoublesImageRenderer;
import com.oculusinfo.tile.rendering.impl.DoublesSeriesImageRenderer;
import com.oculusinfo.tile.rendering.impl.DoublesStatisticImageRenderer;
import com.oculusinfo.tile.rendering.impl.LegacyDoublesImageRenderer;
import com.oculusinfo.tile.rendering.impl.TopAndBottomTextScoresImageRenderer;
import com.oculusinfo.tile.rendering.impl.TopTextScoresImageRenderer;

public class ImageRendererFactory {
	public static TileDataImageRenderer getRenderer (JSONObject parameterObject, PyramidIO pyramidIo) {
	    String rendererType = "default";
	    try {
	        if (null != parameterObject) {
	            rendererType = parameterObject.getString("renderer");
	        }
	    } catch (JSONException e) {
		}
		
		rendererType = rendererType.toLowerCase();
		if ("toptextscores".equals(rendererType)) {
			return new TopTextScoresImageRenderer(pyramidIo);
		} else if ("textscores".equals(rendererType) || "textscore".equals(rendererType)) {
			return new TopAndBottomTextScoresImageRenderer(pyramidIo);
		} else if ("legacy".equals(rendererType)) {
			return new LegacyDoublesImageRenderer(pyramidIo);
		} else if ("doubleseries".equals(rendererType)) {
			return new DoublesSeriesImageRenderer(pyramidIo);
		} else if ("doublestatistics".equals(rendererType)){
			return new DoublesStatisticImageRenderer(pyramidIo);
		} else {
			return new DoublesImageRenderer(pyramidIo);
		}
	}
}
