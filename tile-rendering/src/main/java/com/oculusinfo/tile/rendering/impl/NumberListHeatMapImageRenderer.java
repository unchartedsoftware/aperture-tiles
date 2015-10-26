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

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.List;

import com.oculusinfo.tile.rendering.LayerConfiguration;
import com.oculusinfo.tile.rendering.TileDataImageRenderer;
import com.oculusinfo.tile.rendering.color.ColorRamp;
import com.oculusinfo.tile.rendering.transformations.value.LinearValueTransformer;
import com.oculusinfo.tile.rendering.transformations.value.Log10ValueTransformer;
import com.oculusinfo.tile.rendering.transformations.value.ValueTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.metadata.PyramidMetaData;
import com.oculusinfo.factory.util.Pair;
import com.oculusinfo.binning.util.TypeDescriptor;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.properties.StringProperty;

/**
 * A server side to render List<Pair<String, Int>> tiles.
 * <p/>
 * This renderer by default renders the sum of all key values.
 *
 * @author mkielo
 */

public class NumberListHeatMapImageRenderer implements TileDataImageRenderer<List<Number>> {

	private final Logger LOGGER = LoggerFactory.getLogger( getClass() );
	private static final Color COLOR_BLANK = new Color( 255, 255, 255, 0 );

	private static final double pow2( double x ) {
		return x * x;
	}    //in-line func for squaring a number (instead of calling Math.pow(x, 2.0)

	// This is the only way to get a generified class; because of type erasure,
	// it is definitionally accurate.
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public Class<List<Number>> getAcceptedBinClass() {
		return ( Class ) List.class;
	}

	public TypeDescriptor getAcceptedTypeDescriptor() {
		return new TypeDescriptor( List.class, new TypeDescriptor( Number.class ) );
	}

	/* (non-Javadoc)
	 * @see TileDataImageRenderer#render(LayerConfiguration)
	 */
	public BufferedImage render( TileData<List<Number>> data, TileData<List<Number>> alphaData, LayerConfiguration config ) {
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
			@SuppressWarnings( "unchecked" )
			ValueTransformer<Number> alphaTransformer = config.produce( "alphaValueTransformer", ValueTransformer.class );
			double scaledMax = ( double ) rangeMax / 100;
			double scaledMin = ( double ) rangeMin / 100;

			ColorRamp colorRamp = config.produce( ColorRamp.class );

			bi = renderImage( data, alphaData, t, alphaTransformer, scaledMin, scaledMax, rangeMode, colorRamp, bi, pixelShape );
		} catch ( Exception e ) {
			LOGGER.warn( "Configuration error: ", e );
			return null;
		}
		return bi;
	}

	private double sumBinContents(List<Number> binContents) {
		double binCount = 0;
		for ( int i = 0; i < binContents.size(); i++ ) {
			if ( binContents.get( i ) != null ) {
				binCount += binContents.get( i ).doubleValue();
			}
		}
		return binCount;
	}

	private int[] scaleBin(int tx, int ty, float xScale, float yScale, int[] rgbArray, int outWidth, String pixelShape, int rgb) {
		boolean bCoarseCircles = pixelShape.equals( "circle" );    // render 'coarse' bins as circles or squares?
		double radius2 = pow2( Math.min( xScale, yScale ) * 0.5 );    // min squared 'radius' of final scaled bin
		//calculate the scaled dimensions of this 'pixel' within the image
		int minX = Math.round( tx * xScale );
		int maxX = Math.round( ( tx + 1 ) * xScale );
		int minY = Math.round( ty * yScale );
		int maxY = Math.round( ( ty + 1 ) * yScale );
		double centreX = ( maxX + minX ) * 0.5;
		double centreY = ( maxY + minY ) * 0.5;
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

		return rgbArray;
	}

	protected BufferedImage renderImage( TileData<List<Number>> data,
										 TileData<List<Number>> alphaData,
										 ValueTransformer<Number> t,
										 ValueTransformer<Number> alphaTransformer,
										 double valueMin, double valueMax,
										 String mode, ColorRamp colorRamp, BufferedImage bi, String pixelShape ) {
		int outWidth = bi.getWidth();
		int outHeight = bi.getHeight();
		int xBins = data.getDefinition().getXBins();
		int yBins = data.getDefinition().getYBins();
		float xScale = outWidth / xBins;
		float yScale = outHeight / yBins;
		double oneOverScaledRange = 1.0 / ( valueMax - valueMin );

		int[] rgbArray = ( ( DataBufferInt ) bi.getRaster().getDataBuffer() ).getData();

		for ( int ty = 0; ty < yBins; ty++ ) {
			for ( int tx = 0; tx < xBins; tx++ ) {
				List<Number> binContents = data.getBin( tx, ty ); // Colour ramp bin contents
				// sum buckets for bin count
				double binCount = sumBinContents(binContents);
				double alpha = 1;
				// transform value
				double transformedValue = t.transform( binCount ).doubleValue();
				// Get alpha value if any
				if (alphaData != null) {
					List<Number> alphaBinContents = alphaData.getBin(tx, ty);
					alpha = alphaTransformer.transform(sumBinContents(alphaBinContents)).doubleValue();
				}
				// set pixel value
				int rgb;
				if ( ( mode.contains( "dropZero" ) && binCount != 0 ) || binCount > 0 ) {
					if ( mode.contains("cull" ) ) {
						if ( transformedValue >= valueMin && transformedValue <= valueMax ) {
							rgb = colorRamp.getRGBA((transformedValue - valueMin) * oneOverScaledRange, alpha);
						} else {
							rgb = COLOR_BLANK.getRGB();
						}
					} else {
						rgb = colorRamp.getRGBA((transformedValue - valueMin) * oneOverScaledRange, alpha);
					}
				} else {
					rgb = COLOR_BLANK.getRGB();
				}

				// set the pixel
				if ( ( xScale == 1.0 ) && ( yScale == 1.0 ) ) {
					int i = ty * outWidth + tx;
					rgbArray[i] = rgb;
				} else {
					rgbArray = scaleBin(tx, ty, xScale, yScale, rgbArray, outWidth, pixelShape, rgb);
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
		// Text score rendering always produces a single image.
		return 1;
	}
}
