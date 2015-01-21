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
import com.oculusinfo.binning.io.impl.HBasePyramidIO;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;



/**
 * Class to allow a user to specify parameters for a HBasePyramidIO, and to
 * create one.
 * 
 * @author nkronenfeld
 * 
 */
public class HBasePyramidIOSelector extends JPanel implements PyramidIOSelector {
	private static final long serialVersionUID = 1L;



	private HBasePyramidIO    _io;

	private String _zookeeperQuorum;
	private String _zookeeperPort;
	private String _hbaseMaster;

	private JTextField        _zookeeperQuorumField;
	private JTextField        _zookeeperPortField;
	private JTextField        _hbaseMasterField;
	private JButton           _connect;



	public HBasePyramidIOSelector () {
		_zookeeperQuorum = null;
		_zookeeperPort = null;
		_hbaseMaster = null;
		_io = null;

		JLabel zookeeperQuorumLabel = new JLabel("Zookeeper quorum:");
		_zookeeperQuorumField = new JTextField();
		_zookeeperQuorumField.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void changedUpdate (DocumentEvent event) {
					setZookeeperQuorum(_zookeeperQuorumField.getText());
				}

				@Override
				public void insertUpdate (DocumentEvent event) {
					setZookeeperQuorum(_zookeeperQuorumField.getText());
				}

				@Override
				public void removeUpdate (DocumentEvent event) {
					setZookeeperQuorum(_zookeeperQuorumField.getText());
				}
			});

		JLabel zookeeperPortLabel = new JLabel("Zookeeper port:");
		_zookeeperPortField = new JTextField();
		_zookeeperPortField.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void removeUpdate (DocumentEvent event) {
					setZookeeperPort(_zookeeperPortField.getText());
				}

				@Override
				public void insertUpdate (DocumentEvent event) {
					setZookeeperPort(_zookeeperPortField.getText());
				}

				@Override
				public void changedUpdate (DocumentEvent event) {
					setZookeeperPort(_zookeeperPortField.getText());
				}
			});

		JLabel hbaseMasterLabel = new JLabel("HBase master:");
		_hbaseMasterField = new JTextField();
		_hbaseMasterField.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void changedUpdate (DocumentEvent event) {
					setHBaseMaster(_hbaseMasterField.getText());
				}

				@Override
				public void insertUpdate (DocumentEvent event) {
					setHBaseMaster(_hbaseMasterField.getText());
				}

				@Override
				public void removeUpdate (DocumentEvent event) {
					setHBaseMaster(_hbaseMasterField.getText());
				}
			});
		_connect = new JButton(new AbstractAction("Connect") {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed (ActionEvent event) {
					updateIO();
				}
			});

		setLayout(new GridBagLayout());
		add(zookeeperQuorumLabel,  new GridBagConstraints(0, 0, 1, 1, 0.5, 0.0, GridBagConstraints.EAST,   GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		add(_zookeeperQuorumField, new GridBagConstraints(1, 0, 2, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		add(zookeeperPortLabel,    new GridBagConstraints(0, 1, 1, 1, 0.5, 0.0, GridBagConstraints.EAST,   GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		add(_zookeeperPortField,   new GridBagConstraints(1, 1, 2, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		add(hbaseMasterLabel,      new GridBagConstraints(0, 2, 1, 1, 0.5, 0.0, GridBagConstraints.EAST,   GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		add(_hbaseMasterField,     new GridBagConstraints(1, 2, 2, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		add(_connect,              new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,   GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
	}

	private void setZookeeperQuorum (String text) {
		if (!objectsEqual(_zookeeperQuorum, text)) {
			_zookeeperQuorum = text;
		}
	}

	private void setZookeeperPort (String text) {
		if (!objectsEqual(_zookeeperPort, text)) {
			_zookeeperPort = text;
		}
	}

	private void setHBaseMaster (String text) {
		if (!objectsEqual(_hbaseMaster, text)) {
			_hbaseMaster = text;
		}
	}

	private void updateIO () {
		if (null != _zookeeperQuorum && null != _zookeeperPort && null != _hbaseMaster) {
			HBasePyramidIO oldIO = _io;
			try {
				_io = new HBasePyramidIO(_zookeeperQuorum, _zookeeperPort, _hbaseMaster);
			} catch (IOException e) {
				_io = null;
			}
			if (null != _io || null != oldIO) {
				firePropertyChange(BinVisualizer.PYRAMID_IO, oldIO, _io);
			}
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
