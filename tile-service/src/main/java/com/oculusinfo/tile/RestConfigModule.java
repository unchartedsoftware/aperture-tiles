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
package com.oculusinfo.tile;

import oculus.aperture.common.rest.ResourceDefinition;
import com.oculusinfo.tile.rest.JsonTileResource;
import com.oculusinfo.tile.rest.ImageTileLegendResource;
import com.oculusinfo.tile.rest.ImageTileResource;
import com.oculusinfo.tile.rest.PreRenderedTileResource;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

public class RestConfigModule extends AbstractModule {


	@Override
	protected void configure() {

		// Bind REST endpoints for clients.
		MapBinder<String, ResourceDefinition> resourceBinder =
			MapBinder.newMapBinder(binder(), String.class, ResourceDefinition.class);
				
		resourceBinder.addBinding("/layer").toInstance(new ResourceDefinition(ImageTileResource.class));
		resourceBinder.addBinding("/tile/{id}/{version}/{layer}/{level}/{x}/{y}").toInstance(new ResourceDefinition(ImageTileResource.class));
		resourceBinder.addBinding("/legend").toInstance(new ResourceDefinition(ImageTileLegendResource.class));
		
		resourceBinder.addBinding("/jsonTile").toInstance(new ResourceDefinition(JsonTileResource.class));
		
		resourceBinder.addBinding("/preRenderedTile/{version}/{layer}/{level}/{x}/{y}").toInstance(new ResourceDefinition(PreRenderedTileResource.class));
	}


}
