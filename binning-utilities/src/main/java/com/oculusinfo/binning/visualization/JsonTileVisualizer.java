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
package com.oculusinfo.binning.visualization;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.json.JSONObject;

import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.TilePyramid;
import com.oculusinfo.binning.impl.AOITilePyramid;
import com.oculusinfo.binning.impl.WebMercatorTilePyramid;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.metadata.PyramidMetaData;
import com.oculusinfo.binning.util.AvroJSONConverter;



public class JsonTileVisualizer extends JFrame {
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = Logger.getLogger(BinVisualizer.class.getName());
	static final String PYRAMID_IO = "pyramidIo";

	public static void main (String[] args) {
		new JsonTileVisualizer().setVisible(true);
	}

	private static enum IOEnum {
		File,
		HBase,
		SQLite,
		ZipStream
	}

	private static  enum PyramidEnum {
		Geographic,
		AreaOfInterest
	}

	private JSONObject                _tile;
	private JTextArea                 _tileVis;
	private PyramidIO                 _pyramidIO;
	private TilePyramid               _pyramid;
	private String                    _pyramidId;



	private GroupLayout               _layout;
	private JPanel                    _tileChooser;
	private JFileChooser              _fileChooser;
	private JComboBox<IOEnum>         _ioField;
	private JPanel                    _ioSelectorContainer;
	private PyramidIOSelector         _ioSelector;
	private JComboBox<PyramidEnum>    _pyramidField;
	private JLabel                    _pyramidDesc;
	private JTextField                _idField;
	private JComboBox<Integer>        _levelField;
	private JComboBox<Integer>        _xField;
	private JComboBox<Integer>        _yField;
	private JButton                   _show;
	private JCheckBox                 _showText;



	public JsonTileVisualizer () {
		setupMenus();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocation(200, 50);
		setSize(1200, 1000);

		_tile = null;
		_tileVis = new JTextArea();

		_pyramidIO = null;
		_pyramid = null;
		_pyramidId = null;
		_fileChooser = new JFileChooser();
		createTileChooser();

		JSplitPane split = new JSplitPane();
		split.setResizeWeight(0.8);
		split.setLeftComponent(new JScrollPane(_tileVis));
		split.setRightComponent(_tileChooser);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(split, BorderLayout.CENTER);
	}

	private void setupMenus () {
		JMenuBar menuBar = new JMenuBar();


		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic('f');

		JMenuItem exit = new JMenuItem("Exit");
		exit.setMnemonic('x');
		exit.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed (ActionEvent e) {
					System.exit(0);
				}
			});
		fileMenu.add(exit);


		menuBar.add(fileMenu);
		setJMenuBar(menuBar);
	}

	private void createTileChooser () {
		JLabel ioLabel = new JLabel("I/O type:");
		ioLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		_ioField = new JComboBox<>(IOEnum.values());
		_ioField.addActionListener(new IOFieldUpdate());
		_ioSelectorContainer = new JPanel();
		_ioSelectorContainer.setMaximumSize(new Dimension(100, 75));
		_ioSelectorContainer.setLayout(new BorderLayout());
		_ioSelectorContainer.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
		_ioSelector = null;

		JLabel pyramidLabel = new JLabel("Pyramid type:");
		pyramidLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		_pyramidField = new JComboBox<>(PyramidEnum.values());
		_pyramidField.setEnabled(false);
		_pyramidDesc = new JLabel();
		_pyramidDesc.setHorizontalAlignment(SwingConstants.RIGHT);

		JLabel idLabel = new JLabel("Pyramid id:");
		idLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		_idField = new JTextField();
		_idField.getDocument().addDocumentListener(new IDUpdate());

		JLabel levelLabel = new JLabel("Zoom level:");
		levelLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		_levelField = new JComboBox<>();
		_levelField.addActionListener(new LevelUpdate());
		JLabel xLabel = new JLabel("Tile x coordinate:");
		xLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		_xField = new JComboBox<>();
		JLabel yLabel = new JLabel("Tile y coordinate:");
		yLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		_yField = new JComboBox<>();

		_show = new JButton("Show tile");
		_show.addActionListener(new ShowTile());

		_showText = new JCheckBox("Bin values");
		_showText.setHorizontalAlignment(SwingConstants.RIGHT);

		JPanel chooser = new JPanel();

		_layout = new GroupLayout(chooser);
		chooser.setLayout(_layout);


		int pref = GroupLayout.PREFERRED_SIZE;
		int max = Short.MAX_VALUE;
		JLabel extraArea = new JLabel();
		_layout.setHorizontalGroup(
		                           _layout.createParallelGroup()
		                           .addGroup(_layout.createSequentialGroup().addComponent(ioLabel, 0, pref, max).addComponent(_ioField))
		                           .addGroup(_layout.createSequentialGroup().addGap(25).addComponent(_ioSelectorContainer, 0, pref, max))
		                           .addGroup(_layout.createSequentialGroup().addComponent(pyramidLabel, 0, pref, max).addComponent(_pyramidField))
		                           .addGroup(_layout.createSequentialGroup().addComponent(_pyramidDesc, 0, pref, max))
		                           .addGroup(_layout.createSequentialGroup().addComponent(idLabel, 0, pref, max).addComponent(_idField))
		                           .addGroup(_layout.createSequentialGroup().addComponent(levelLabel, 0, pref, max).addComponent(_levelField))
		                           .addGroup(_layout.createSequentialGroup().addComponent(xLabel, 0, pref, max).addComponent(_xField))
		                           .addGroup(_layout.createSequentialGroup().addComponent(yLabel, 0, pref, max).addComponent(_yField))
		                           .addComponent(_show, Alignment.TRAILING)
		                           .addComponent(_showText, Alignment.TRAILING)
		                           .addComponent(extraArea)
		                           );
		_layout.setVerticalGroup(
		                         _layout.createSequentialGroup()
		                         .addGroup(_layout.createParallelGroup().addComponent(ioLabel).addComponent(_ioField))
		                         .addComponent(_ioSelectorContainer, 0, pref, 100)
		                         .addGroup(_layout.createParallelGroup().addComponent(pyramidLabel).addComponent(_pyramidField))
		                         .addGroup(_layout.createParallelGroup().addComponent(_pyramidDesc))
		                         .addGroup(_layout.createParallelGroup().addComponent(idLabel).addComponent(_idField))
		                         .addGroup(_layout.createParallelGroup().addComponent(levelLabel).addComponent(_levelField))
		                         .addGroup(_layout.createParallelGroup().addComponent(xLabel).addComponent(_xField))
		                         .addGroup(_layout.createParallelGroup().addComponent(yLabel).addComponent(_yField))
		                         .addComponent(_show)
		                         .addComponent(_showText)
		                         .addComponent(extraArea, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
		                         );
		_layout.linkSize(_ioField, _pyramidField, _idField, _levelField, _xField, _yField);


		_tileChooser = chooser;


		_ioField.setSelectedIndex(0);
		_pyramidField.setSelectedIndex(0);
	}


	private void setIOType (IOEnum type) {
		switch (type) {
		case File:
			if (null != _ioSelector) {
				if (_ioSelector instanceof FileSystemPyramidIOSelector) {
					return;
				}
				_ioSelectorContainer.removeAll();
			}
			_ioSelector = new FileSystemPyramidIOSelector(_fileChooser);
			_ioSelectorContainer.add(_ioSelector.getPanel(), BorderLayout.CENTER);
			break;
		case HBase:
			if (null != _ioSelector) {
				if (_ioSelector instanceof HBasePyramidIOSelector) {
					return;
				}
				_ioSelectorContainer.removeAll();
			}
			_ioSelector = new HBasePyramidIOSelector();
			_ioSelectorContainer.add(_ioSelector.getPanel(), BorderLayout.CENTER);
			break;
		case SQLite:
			if (null != _ioSelector) {
				if (_ioSelector instanceof SQLitePyramidIOSelector) {
					return;
				}
				_ioSelectorContainer.removeAll();
			}
			_ioSelector = new SQLitePyramidIOSelector();
			_ioSelectorContainer.add(_ioSelector.getPanel(), BorderLayout.CENTER);
			break;
		case ZipStream:
			if (null != _ioSelector) {
				if (_ioSelector instanceof ZipFilePyramidIOSelector) {
					return;
				}
				_ioSelectorContainer.removeAll();
			}
			_ioSelector = new ZipFilePyramidIOSelector(_fileChooser);
			_ioSelectorContainer.add(_ioSelector.getPanel(), BorderLayout.CENTER);
			break;
		default:
			if (null != _ioSelector) {
				_ioSelectorContainer.removeAll();
				_ioSelector = null;
			}
		}
		//        _layout.layoutContainer(_tileChooser);
		_ioSelector.getPanel().addPropertyChangeListener(new PropertyChangeListener() {
				@Override
				public void propertyChange (PropertyChangeEvent event) {
					if (PYRAMID_IO.equals(event.getPropertyName())) {
						_pyramidIO = _ioSelector.getPyramidIO();
						updateAvailableLevels();
					}
				}
			});
		_tileChooser.validate();
	}

	private void setPyramidId (String newId) {
		if (!objectsEqual(newId, _pyramidId)) {
			_pyramidId = newId;
			// Notify change functions
			updateAvailableLevels();
			updatePyramidType();
		}
	}

	private void updateAvailableLevels () {
		if (null != _pyramidIO && null != _pyramidId && !_pyramidId.isEmpty()) {
			try {
				String rawMetaData = _pyramidIO.readMetaData(_pyramidId);
				PyramidMetaData metaData = new PyramidMetaData(rawMetaData);

				_levelField.removeAll();
				_xField.removeAll();
				_yField.removeAll();

				List<Integer> levels = metaData.getValidZoomLevels();
				for (Integer level: levels) {
					_levelField.addItem(level);
				}
				_levelField.setSelectedIndex(0);
				return;
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Error getting level metadata for "
				           + _pyramidId);
			}
		}
		_levelField.removeAllItems();
		_xField.removeAll();
		_yField.removeAll();
	}

	private void updatePyramidType () {
		_pyramid = null;
		try {
			String rawMetaData = _pyramidIO.readMetaData(_pyramidId);
			PyramidMetaData metaData = new PyramidMetaData(rawMetaData);
			_pyramid = metaData.getTilePyramid();
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error getting level metadata for "
			           + _pyramidId);
		}

		if (null == _pyramid) {
			_pyramidField.setSelectedIndex(-1);
			_pyramidDesc.setText("");
		} else if (_pyramid instanceof WebMercatorTilePyramid) {
			_pyramidField.setSelectedItem(PyramidEnum.Geographic);
			_pyramidDesc.setText("");
		} else if (_pyramid instanceof AOITilePyramid) {
			_pyramidField.setSelectedItem(PyramidEnum.AreaOfInterest);
			Rectangle2D bounds = _pyramid.getTileBounds(new TileIndex(0, 0, 0));
			_pyramidDesc.setText(String.format("bounds: [%.4f, %.4f] to [%.4f, %.4f]",
			                                   bounds.getMinX(), bounds.getMinY(),
			                                   bounds.getMaxX(), bounds.getMaxY()));
		}
	}
	private void updateAvailableCoordinates () {
		Integer level = (Integer) _levelField.getSelectedItem();

		_xField.removeAllItems();
		_yField.removeAllItems();

		if (null == level) return;
		int pow2 = 1 << level;
		for (int i=0; i<pow2; ++i) {
			_xField.addItem(i);
			_yField.addItem(i);
		}
	}



	private void showCurrentTile () {
		if (null == _pyramidId) return;
		if (null == _pyramidIO) return;
		if (null == _levelField.getSelectedItem()) return;
		if (null == _xField.getSelectedItem()) return;
		if (null == _yField.getSelectedItem()) return;
		TileIndex index = new TileIndex((Integer) _levelField.getSelectedItem(),
		                                (Integer) _xField.getSelectedItem(),
		                                (Integer) _yField.getSelectedItem());

		try {
			InputStream tileStream = _pyramidIO.getTileStream(_pyramidId, null, index);
			_tile = AvroJSONConverter.convert(tileStream);
			if (null == _tile) {
				_tileVis.setText("");
			} else {
				_tileVis.setText(_tile.toString(2));
			}
		} catch (Exception e) {
			_tileVis.setText("");
		}
	}



	private class IOFieldUpdate implements ActionListener {
		@Override
		public void actionPerformed (ActionEvent event) {
			setIOType((IOEnum) _ioField.getSelectedItem());
		}
	}
	private class IDUpdate implements DocumentListener {
		@Override
		public void changedUpdate (DocumentEvent event) {
			setPyramidId(_idField.getText());
		}

		@Override
		public void insertUpdate (DocumentEvent e) {
			setPyramidId(_idField.getText());
		}

		@Override
		public void removeUpdate (DocumentEvent e) {
			setPyramidId(_idField.getText());
		}
	}
	private class LevelUpdate implements ActionListener {
		@Override
		public void actionPerformed (ActionEvent event) {
			updateAvailableCoordinates();
		}
	}
	private class ShowTile implements ActionListener {
		@Override
		public void actionPerformed (ActionEvent e) {
			showCurrentTile();
		}
	}

	private static boolean objectsEqual (Object a, Object b) {
		if (null == a) return null == b;
		return a.equals(b);
	}
}
