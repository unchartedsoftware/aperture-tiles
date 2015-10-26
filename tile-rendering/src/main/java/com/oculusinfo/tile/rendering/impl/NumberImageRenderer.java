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
package com.oculusinfo.tile.rendering.impl;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.metadata.PyramidMetaData;
import com.oculusinfo.binning.util.TypeDescriptor;
import com.oculusinfo.factory.util.Pair;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.properties.StringProperty;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import com.oculusinfo.tile.rendering.TileDataImageRenderer;
import com.oculusinfo.tile.rendering.color.ColorRamp;
import com.oculusinfo.tile.rendering.transformations.value.ValueTransformer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.*;

/**
 * @author dgray
 */
public class NumberImageRenderer implements TileDataImageRenderer<Number> {

	private static final Logger LOGGER = LoggerFactory.getLogger( NumberImageRenderer.class );
	private static final Color COLOR_BLANK = new Color( 255, 255, 255, 0 );

	private static final double pow2( double x ) {
		return x * x;
	}    //in-line func for squaring a number (instead of calling Math.pow(x, 2.0)

	@Override
	public Class<Number> getAcceptedBinClass() {
		return Number.class;
	}

	@Override
	public TypeDescriptor getAcceptedTypeDescriptor() {
		return new TypeDescriptor( getAcceptedBinClass() );
	}

	/* (non-Javadoc)
	 * @see TileDataImageRenderer#render(LayerConfiguration)
	 */
	@Override
	public BufferedImage render( TileData<Number> data, TileData<Number> alphaData, LayerConfiguration config ) {
		BufferedImage bi;
		try {
			int outputWidth = config.getPropertyValue( LayerConfiguration.OUTPUT_WIDTH );
			int outputHeight = config.getPropertyValue( LayerConfiguration.OUTPUT_HEIGHT );
			int rangeMax = config.getPropertyValue( LayerConfiguration.RANGE_MAX );
			int rangeMin = config.getPropertyValue( LayerConfiguration.RANGE_MIN );
			String rangeMode = config.getPropertyValue( LayerConfiguration.RANGE_MODE );
			String pixelShape = config.getPropertyValue( LayerConfiguration.PIXEL_SHAPE );

			bi = new BufferedImage( outputWidth, outputHeight, BufferedImage.TYPE_INT_ARGB );

			@SuppressWarnings( "unchecked" )
			ValueTransformer<Number> t = config.produce( "valueTransformer", ValueTransformer.class );
			double scaledMax = ( double ) rangeMax / 100;
			double scaledMin = ( double ) rangeMin / 100;

			ColorRamp colorRamp = config.produce( ColorRamp.class );

			bi = renderImage( data, t, scaledMin, scaledMax, rangeMode, colorRamp, bi, pixelShape );
		} catch ( Exception e ) {
			LOGGER.warn( "Configuration error: ", e );
			return null;
		}
		return bi;
	}

	protected BufferedImage renderImage( TileData<Number> data,
										 ValueTransformer<Number> t, double valueMin, double valueMax,
										 String mode, ColorRamp colorRamp, BufferedImage bi, String pixelShape ) {

		int outWidth = bi.getWidth();
		int outHeight = bi.getHeight();
		int xBins = data.getDefinition().getXBins();
		int yBins = data.getDefinition().getYBins();

		float xScale = outWidth / xBins;
		float yScale = outHeight / yBins;
		double radius2 = pow2( Math.min( xScale, yScale ) * 0.5 );    // min squared 'radius' of final scaled bin

		double oneOverScaledRange = 1.0 / ( valueMax - valueMin );
		boolean bCoarseCircles = pixelShape.equals( "circle" );    // render 'coarse' bins as circles or squares?

		int[] rgbArray = ( ( DataBufferInt ) bi.getRaster().getDataBuffer() ).getData();

		if ( ( xScale == 1.0 ) && ( yScale == 1.0 ) ) {
			// no bin scaling needed

			for ( int ty = 0; ty < yBins; ty++ ) {
				for ( int tx = 0; tx < xBins; tx++ ) {
					// get bin count
					double binCount = data.getBin( tx, ty ).doubleValue();
					// transform value
					double transformedValue = t.transform( binCount ).doubleValue();
					// set pixel value
					int rgb;
					if ( ( mode.equals( "dropZero" ) && binCount != 0 ) || binCount > 0 ) {
						if ( mode.equals( "cull" ) ) {
							if ( transformedValue >= valueMin && transformedValue <= valueMax ) {
								rgb = colorRamp.getRGB( ( transformedValue - valueMin ) * oneOverScaledRange );
							} else {
								rgb = COLOR_BLANK.getRGB();
							}
						} else {
							rgb = colorRamp.getRGB( ( transformedValue - valueMin ) * oneOverScaledRange );
						}
					} else {
						rgb = COLOR_BLANK.getRGB();
					}

					// set pixel
					int i = ty * outWidth + tx;
					rgbArray[i] = rgb;
				}
			}
		} else {
			// perform bin scaling (i.e. if bin coarseness != 1.0)

			for ( int ty = 0; ty < yBins; ty++ ) {
				for ( int tx = 0; tx < xBins; tx++ ) {
					//calculate the scaled dimensions of this 'pixel' within the image
					int minX = Math.round( tx * xScale );
					int maxX = Math.round( ( tx + 1 ) * xScale );
					int minY = Math.round( ty * yScale );
					int maxY = Math.round( ( ty + 1 ) * yScale );
					double centreX = ( maxX + minX ) * 0.5;
					double centreY = ( maxY + minY ) * 0.5;
					// get bin count
					double binCount = data.getBin( tx, ty ).doubleValue();
					// transform value
					double transformedValue = t.transform( binCount ).doubleValue();
					// set pixel value
					int rgb;
					if ( ( mode.equals( "dropZero" ) && binCount != 0 ) || binCount > 0 ) {
						if ( mode.equals( "cull" ) ) {
							if ( transformedValue >= valueMin && transformedValue <= valueMax ) {
								rgb = colorRamp.getRGB( ( transformedValue - valueMin ) * oneOverScaledRange );
							} else {
								rgb = COLOR_BLANK.getRGB();
							}
						} else {
							rgb = colorRamp.getRGB( ( transformedValue - valueMin ) * oneOverScaledRange );
						}
					} else {
						rgb = COLOR_BLANK.getRGB();
					}

					//'draw' out the scaled 'pixel'
					if ( bCoarseCircles && radius2 > 1.0 ) {
						// draw scaled (coarse) bin as a circle (Note: need radius to be > 1.0 pixels in order to render a circle)
						for ( int ix = minX; ix < maxX; ++ix ) {
							for ( int iy = minY; iy < maxY; ++iy ) {
								int i = iy * outWidth + ix;
								double dist = ( pow2( ix + 0.5 - centreX ) + pow2( iy + 0.5 - centreY ) );
								if ( dist <= radius2 ) {
									rgbArray[i] = rgb;        // scaled bin is within bin's valid radius, so render normally
								} else {
									rgbArray[i] = COLOR_BLANK.getRGB();    // scaled bin is outside bin's valid radius, so force to be blank
								}
							}
						}
					} else {
						// draw scaled bin simply as a square
						for ( int ix = minX; ix < maxX; ++ix ) {
							for ( int iy = minY; iy < maxY; ++iy ) {
								int i = iy * outWidth + ix;
								rgbArray[i] = rgb;
							}
						}
					}
				}
			}
		}
		return bi;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getNumberOfImagesPerTile( PyramidMetaData metadata ) {
		// Number tile rendering always produces a single image.
		return 1;
	}

}
