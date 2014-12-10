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

import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.impl.PyramidSource;
import com.oculusinfo.binning.io.impl.FileBasedPyramidIO;
import com.oculusinfo.binning.io.impl.ZipResourcePyramidSource;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;



/**
 * Class to allow a user to specify parameters for a Zip based FileBasedPyramidIO, and to
 * create one.
 * 
 * @author nkronenfeld
 * 
 */
public class ZipFilePyramidIOSelector extends JPanel implements PyramidIOSelector {
	private static final long serialVersionUID = 1L;



	private String                          _zipPath;
	private String                          _extension;
	private FileBasedPyramidIO 				_io;
	private JTextField                      _zipPathField;
	private JFileChooser                    _fileChooser;
	private JTextField                      _extensionField;



	public ZipFilePyramidIOSelector (JFileChooser chooser) {
		_zipPath = null;
		_extension = "avro";
		_io = null;
		_fileChooser = chooser;

		JLabel zipPathLabel = new JLabel("Zipfile path:");
		_zipPathField = new JTextField();
		JButton button = new JButton("...");

		JLabel extensionLabel = new JLabel("Tile extension:");
		_extensionField = new JTextField(_extension);

		setLayout(new GridBagLayout());
		add(zipPathLabel,    new GridBagConstraints(0, 0, 1, 1, 0.5, 0.0, GridBagConstraints.EAST,   GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		add(_zipPathField,   new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		add(button,          new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		add(extensionLabel,  new GridBagConstraints(0, 1, 1, 1, 0.5, 0.0, GridBagConstraints.EAST,   GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		add(_extensionField, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

		_zipPathField.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void removeUpdate (DocumentEvent event) {
					setRootPath(_zipPathField.getText());
				}

				@Override
				public void insertUpdate (DocumentEvent event) {
					setRootPath(_zipPathField.getText());
				}

				@Override
				public void changedUpdate (DocumentEvent event) {
					setRootPath(_zipPathField.getText());
				}
			});
		button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed (ActionEvent event) {
					_fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
					_fileChooser.setFileFilter(new FileNameExtensionFilter("zip files", "zip"));
					_fileChooser.addChoosableFileFilter(_fileChooser.getAcceptAllFileFilter());
					int returnVal = _fileChooser.showOpenDialog(ZipFilePyramidIOSelector.this);

					if (returnVal == JFileChooser.APPROVE_OPTION) {
						File file = _fileChooser.getSelectedFile();
						_zipPathField.setText(file.getAbsolutePath());
					}
				}
			});
		_extensionField.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void removeUpdate (DocumentEvent event) {
					setExtension(_extensionField.getText());
				}

				@Override
				public void insertUpdate (DocumentEvent event) {
					setExtension(_extensionField.getText());
				}

				@Override
				public void changedUpdate (DocumentEvent event) {
					setExtension(_extensionField.getText());
				}
			});
	}

	private void setRootPath (String zipPath) {
		if (!objectsEqual(_zipPath, zipPath)) {
			_zipPath = zipPath;
			updatePyramid();
		}
	}

	private void setExtension (String extension) {
		if (!objectsEqual(_extension, extension)) {
			_extension = extension;
			updatePyramid();
		}
	}

	private void updatePyramid () {
		FileBasedPyramidIO oldIO = _io;
		if (null != _zipPath && null != _extension) {
			PyramidSource newSource = new ZipResourcePyramidSource(_zipPath, _extension);
			_io = new FileBasedPyramidIO(newSource);
			firePropertyChange(BinVisualizer.PYRAMID_IO, oldIO, _io);
		} else if (null != oldIO) {
			_io = null;
			firePropertyChange(BinVisualizer.PYRAMID_IO, oldIO, _io);
		}
	}

	@Override
	public PyramidIO getPyramidIO () {
		return _io;
	}

	@Override
	public JPanel getPanel () {
		return this;
	}

	private static boolean objectsEqual (Object a, Object b) {
		if (null == a) return null == b;
		return a.equals(b);
	}
}
