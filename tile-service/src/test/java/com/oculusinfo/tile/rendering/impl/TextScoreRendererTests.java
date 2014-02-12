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
package com.oculusinfo.tile.rendering.impl;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.TilePyramid;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.util.Pair;
import com.oculusinfo.tile.rendering.RenderParameter;
import com.oculusinfo.tile.rendering.color.ColorRampParameter;

/**
 * Manual, visual test of text score tile rendering.  This is not an automated test.
 */
public class TextScoreRendererTests {
	private static class TestPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		private BufferedImage _image;
		
		public TestPanel () {
			_image = null;
		}

		public void setImage (BufferedImage image) {
			_image = image;
			repaint();
		}
		@Override
		public void paint(Graphics g) {
			int w = getWidth();
			int h = getHeight();
			g.setColor(new Color(32, 32, 32));
			g.fillRect(0, 0, w, h);
			g.drawImage(_image, 0, 0, null);
		}
	}
	
	public static void main (String[] args) {
		JFrame frame = new JFrame();
		frame.setLocation(100, 100);
		frame.setSize(300, 400);
		final TestPanel panel = new TestPanel();
		frame.getContentPane().add(panel);
		TestPyramidIO pio = new TestPyramidIO();
		final TopTextScoresImageRenderer renderer = new TopTextScoresImageRenderer(pio);
		JMenuBar menuBar = new JMenuBar();
		JMenu menu = new JMenu("actions");
		menu.add(new JMenuItem(new AbstractAction("reload") {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				RenderParameter param = new RenderParameter();
				param.setString("layer", "foobar");
				param.setObject("rampType", new ColorRampParameter("br"));
				param.setOutputWidth(256);
				param.setOutputHeight(256);
				param.setObject("tileCoordinate", new TileIndex(4, 3, 2, 1, 1));

				panel.setImage(renderer.render(param));
			}
		}));
		menu.add(new JMenuItem(new AbstractAction("quit") {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		}));
		menuBar.add(menu);
		frame.setJMenuBar(menuBar);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}

	static class TestPyramidIO implements PyramidIO {
		@Override
		public void initializeForWrite(String pyramidId) throws IOException {
		}

		@Override
		public <T> void writeTiles(String pyramidId, TilePyramid tilePyramid,
				TileSerializer<T> serializer, Iterable<TileData<T>> data)
				throws IOException {
		}

		@Override
		public void writeMetaData(String pyramidId, String metaData)
				throws IOException {
		}

	    @Override
	    public void initializeForRead(String pyramidId, int tileSize,
	    		Properties dataDescription) {
	    }

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public <T> List<TileData<T>> readTiles(String pyramidId,
				TileSerializer<T> serializer, Iterable<TileIndex> tiles)
				throws IOException {
			TileData<List<Pair<String, Double>>> tile = new TileData<List<Pair<String,Double>>>(new TileIndex(4, 3, 2, 1, 1));

			List<Pair<String, Double>> bin = new ArrayList<Pair<String,Double>>();
			bin.add(new Pair<String, Double>("abc", 5.0));
			bin.add(new Pair<String, Double>("def", 4.0));
			bin.add(new Pair<String, Double>("ghi", 3.0));
			bin.add(new Pair<String, Double>("jkl", 2.0));
			bin.add(new Pair<String, Double>("mno", 1.0));
			bin.add(new Pair<String, Double>("pqr", -1.0));
			bin.add(new Pair<String, Double>("stu", -2.0));
			bin.add(new Pair<String, Double>("vwx", -3.0));
			bin.add(new Pair<String, Double>("yyy", -4.0));
			bin.add(new Pair<String, Double>("zzz", -5.0));
			tile.setBin(0, 0, bin);

			return (List) Collections.singletonList(tile);
		}

		@Override
		public InputStream getTileStream (String pyramidId, TileIndex tile) throws IOException {
		    return null;
		}

		@Override
		public String readMetaData(String pyramidId) throws IOException {
			return null;
		}
	}
}
