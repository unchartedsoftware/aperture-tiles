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
package com.oculusinfo.tile.rest;

import com.oculusinfo.tile.rest.tile.TileResource.ExtensionType;
import com.oculusinfo.tile.rest.tile.TileResource.ResponseType;
import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.MediaType;

public class TileResourceTests {
	@Test
	public void testExtensionTypes () {
		ExtensionType ext;

		ext = ExtensionType.valueOf("jpg");
		Assert.assertEquals(ResponseType.Image, ext.getResponseType());
		Assert.assertEquals(MediaType.IMAGE_JPEG, ext.getMediaType());

		ext = ExtensionType.valueOf("jpeg");
		Assert.assertEquals(ResponseType.Image, ext.getResponseType());
		Assert.assertEquals(MediaType.IMAGE_JPEG, ext.getMediaType());

		ext = ExtensionType.valueOf("png");
		Assert.assertEquals(ResponseType.Image, ext.getResponseType());
		Assert.assertEquals(MediaType.IMAGE_PNG, ext.getMediaType());

		ext = ExtensionType.valueOf("json");
		Assert.assertEquals(ResponseType.Tile, ext.getResponseType());
		Assert.assertEquals(MediaType.APPLICATION_JSON, ext.getMediaType());
	}
}
