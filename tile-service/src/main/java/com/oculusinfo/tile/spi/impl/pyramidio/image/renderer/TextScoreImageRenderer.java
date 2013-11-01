package com.oculusinfo.tile.spi.impl.pyramidio.image.renderer;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.oculusinfo.tile.spi.impl.pyramidio.image.ColorRampFactory;
import com.oculusinfo.tile.util.AbstractColorRamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.io.Pair;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.TileSerializer;
import com.oculusinfo.binning.io.impl.StringDoublePairArrayAvroSerializer;
import com.oculusinfo.binning.util.PyramidMetaData;

/**
 * A renderer to render on the server side tiles of Map<String, Double> tiles
 * @author nkronenfeld
 *
 */
public class TextScoreImageRenderer implements TileDataImageRenderer {
	private final Logger _logger = LoggerFactory.getLogger(getClass());



	private PyramidIO _pyramidIo;
	private TileSerializer<List<Pair<String, Double>>> _serializer;
	public TextScoreImageRenderer (PyramidIO pyramidIo) {
		_pyramidIo = pyramidIo;
		_serializer = new StringDoublePairArrayAvroSerializer();
	}

	private void drawScoredText (Graphics2D g, Pair<String, Double> textScore, int indexFromCenter,
								 int minX, int maxX, int minY, int maxY,
								 int rowHeight, int barHeight, int padding,
								 AbstractColorRamp ramp, double scale) {
		int centerX = (minX + maxX) / 2;
		int centerY = (minY + maxY) / 2;
		int baseline = centerY + indexFromCenter * rowHeight - padding;

		int barBaseline = baseline - (rowHeight - 2*padding - barHeight)/2;
		// For bar purposes, value should be between -1 and 1
		double value = textScore.getSecond()/scale;
        // For color purposes, value should be between 0 and 1
		double colorValue = (value+1.0)/2.0;
		int barWidth = (int)Math.round((maxX-centerX)*0.8*value);

		String text = textScore.getFirst();
		FontMetrics metrics = g.getFontMetrics();
		int textBaseline = baseline;

		g.setColor(new Color(ramp.getRGB(colorValue)));
		if (barWidth > 0) {
			g.fillRect(centerX+padding, barBaseline, barWidth, barHeight);
		} else {
			g.fillRect(centerX+barWidth-padding, barBaseline, -barWidth, barHeight);
		}

		g.setColor(new Color(255, 255, 128, 192));
		if (barWidth < 0) {
			g.drawString(text, centerX+padding, textBaseline);
		} else {
			int textWidth = metrics.stringWidth(text);
			g.drawString(text, centerX-padding-textWidth, textBaseline);
		}
	}
			
	/**
	 * {@inheritDoc}
	 */
	@Override
	public BufferedImage render(RenderParameter parameter) {
		BufferedImage bi;
		try {
			int width = parameter.outputWidth;
			int height = parameter.outputHeight;
			bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			AbstractColorRamp ramp = ColorRampFactory.create(parameter.rampType, 64);

			List<TileData<List<Pair<String, Double>>>> tileDatas = _pyramidIo.readTiles(parameter.layer, _serializer, Collections.singleton(parameter.tileCoordinate));
			if (tileDatas.isEmpty()) {
			    _logger.debug("Layer {} is missing tile ().", parameter.layer, parameter.tileCoordinate);
			    return null;
			}
			TileData<List<Pair<String, Double>>> data = tileDatas.get(0);
			int xBins = data.getDefinition().getXBins();
			int yBins = data.getDefinition().getYBins();

			Graphics2D g = bi.createGraphics();
			// Transparent background
			g.setColor(new Color(0, 0, 0, 0));
			g.fillRect(0, 0, width, height);

			int rowHeight = 16;
			int barHeight = 3;
			int padding = 2;

			for (int x=0; x<xBins; ++x) {
				for (int y=0; y<yBins; ++y) {
					int xMin = x*width/xBins;
					int xMax = (x+1)*width/xBins;
					int yMin = y*height/yBins;
					int yMax = (y+1)*height/yBins;

					int cellHeight = yMax-yMin;

					List<Pair<String, Double>> cellData = new ArrayList<Pair<String, Double>>(data.getBin(x, y));
					if (cellData.size()>0) {
						Collections.sort(cellData, new Comparator<Pair<String, Double>>() {
							@Override
							public int compare(Pair<String, Double> p1,
									           Pair<String, Double> p2) {
								if (p1.getSecond() < p2.getSecond()) return -1;
								else if (p1.getSecond() > p2.getSecond()) return 1;
								else return 0;
							}
						});
						double minVal = cellData.get(0).getSecond();
						double maxVal = cellData.get(cellData.size()-1).getSecond();
						double scaleVal = Math.max(Math.abs(minVal), Math.abs(maxVal));
	
						int numStrings = Math.min(cellData.size(), cellHeight/rowHeight);
						int numTopStrings = numStrings/2;
	
						g.setClip(null);
						g.clipRect(xMin, yMin, xMax-xMin, yMax-yMin);
						int n = cellData.size();
						for (int i=0; i<numTopStrings; ++i) {
							drawScoredText(g, cellData.get(n-1-i), i+1-numTopStrings,
										   xMin, xMax, yMin, yMax, rowHeight, barHeight, padding, ramp, scaleVal);
							drawScoredText(g, cellData.get(i), numTopStrings-i,
										   xMin, xMax, yMin, yMax, rowHeight, barHeight, padding, ramp, scaleVal);
						}
					}
				}
			}
		} catch (Exception e) {
			_logger.debug("Tile is corrupt: " + parameter.layer+":"+parameter.tileCoordinate);
			_logger.debug("Tile error: ", e);
			bi = null;
		}
		return bi;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getNumberOfImagesPerTile (PyramidMetaData metadata) {
		// Text score rendering always produces a single image.
		return 1;
	}
}
