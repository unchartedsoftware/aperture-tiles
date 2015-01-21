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
import com.oculusinfo.binning.io.impl.SQLitePyramidIO;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;



/**
 * Class to allow a user to specify parameters for a SQLitePyramidIO, and to
 * create one.
 * 
 * @author nkronenfeld
 * 
 */
public class SQLitePyramidIOSelector extends JPanel implements PyramidIOSelector {
	private static final long serialVersionUID = 1L;



	private String            _dbPath;
	private SQLitePyramidIO   _io;
	private JTextField        _dbPathField;



	public SQLitePyramidIOSelector () {
		_dbPath = null;
		_io = null;

		JLabel dbPathLabel = new JLabel("Database path:");
		_dbPathField = new JTextField();
		setLayout(new GridBagLayout());
		add(dbPathLabel,  new GridBagConstraints(0, 0, 1, 1, 0.5, 0.0, GridBagConstraints.EAST,   GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		add(_dbPathField, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

		_dbPathField.addPropertyChangeListener(new PropertyChangeListener() {
				@Override
				public void propertyChange (PropertyChangeEvent arg0) {
					setDBPath(_dbPathField.getText());
				}
			});
	}

	private void setDBPath (String dbPath) {
		if (!objectsEqual(_dbPath, dbPath)) {
			_dbPath = dbPath;
			SQLitePyramidIO oldIO = _io;
			try {
				_io = new SQLitePyramidIO(_dbPath);
			} catch (Exception e) {
				_io = null;
				if (null == oldIO)
					return;
			}
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
