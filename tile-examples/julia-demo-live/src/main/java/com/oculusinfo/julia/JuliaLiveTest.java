package com.oculusinfo.julia;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import org.apache.avro.file.CodecFactory;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaSparkContext;
import org.json.JSONException;
import org.json.JSONObject;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.io.serialization.impl.DoubleAvroSerializer;
import com.oculusinfo.tilegen.binning.LiveStaticTilePyramidIO;

public class JuliaLiveTest extends JFrame {
	private static final long serialVersionUID = 1L;



	public static void main (String[] args) throws IOException, JSONException {
		JuliaLiveTest frame = new JuliaLiveTest();
		frame.setSize(500, 500);
		frame.setLocation(200, 100);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}



	private SparkContext            _sc;
	private LiveStaticTilePyramidIO _pyramidIO;
	private TileSerializer<Double>  _serializer;



	public JuliaLiveTest () throws IOException, JSONException {
		setupSparkContext();
		setupPyramidIO();
		setupTileSerializer();

		setupUI();
	}

	private void setupSparkContext () {
		SparkConf conf = new SparkConf();
		conf.setMaster("local[1]");
		conf.setAppName("Julia live tile testing");
		JavaSparkContext jsc = new JavaSparkContext(conf);
		_sc = JavaSparkContext.toSparkContext(jsc);
		_sc.cancelAllJobs();
	}

	private void setupPyramidIO () throws IOException, JSONException {
		String rawConfig = "";
		String line;
		InputStream configStream = JuliaLiveTest.class.getResourceAsStream("/layers/julia-layer.json");
		BufferedReader configReader = new BufferedReader(new InputStreamReader(configStream));
		while (null != (line = configReader.readLine()))
			rawConfig += line;
		configReader.close();

		JSONObject jsonConfig = new JSONObject(rawConfig)
			.getJSONArray("layers")
			.getJSONObject(0)
			.getJSONObject("data")
			.getJSONObject("pyramidio")
			.getJSONObject("data");

		Properties config = new Properties();
		for (String key: JSONObject.getNames(jsonConfig))
			config.setProperty(key, jsonConfig.getString(key));

		_pyramidIO = new LiveStaticTilePyramidIO(_sc);
		_pyramidIO.initializeForRead("julia", 256, 256, config);
	}

	private void setupTileSerializer () {
		_serializer = new DoubleAvroSerializer(CodecFactory.bzip2Codec());
	}

	private void setupUI () {
		setLayout(new GridBagLayout());
		final JTextArea tiles = new JTextArea("Enter tile list here");
		tiles.setEditable(true);
		tiles.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Tiles to Retrieve"));

		JButton doTiling = new JButton("Retrieve tiles");

		final JTextField partitions = new JTextField();
		partitions.setEditable(true);
		if (_pyramidIO.getConsolidationPartitions().isDefined()) {
			partitions.setText(_pyramidIO.getConsolidationPartitions().get().toString());
		} else {
			partitions.setText("");
		}
		add(tiles,                      new GridBagConstraints(0, 0, 4, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		add(new JLabel(""),             new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		add(new JLabel("Partitions: "), new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		add(partitions,                 new GridBagConstraints(2, 1, 1, 1, 0.5, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		add(doTiling,                   new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

		doTiling.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed (ActionEvent event) {
					Integer conParts = null;
					try {
						conParts = Integer.parseInt(partitions.getText().trim());
					} catch (NumberFormatException e) {}
					if (null == conParts) {
						_pyramidIO.eliminateConsolidationPartitions();
					} else {
						_pyramidIO.setConsolidationPartitions(conParts);
					}
					List<TileIndex> indices = parseIndices(tiles.getText().replace("\n", ""));
					testTileRetrieval(indices);
				}
			});
	}

	private List<TileIndex> parseIndices (String text) {
		List<TileIndex> tileList = new ArrayList<>();

		// Parse tile list
		for (String part: text.split(";")) {
			String[] subParts = part.split(",");
			if (3 <= subParts.length) {
				List<Integer> xs = parseIndex(subParts[0].trim());
				List<Integer> ys = parseIndex(subParts[1].trim());
				List<Integer> zs = parseIndex(subParts[2].trim());
				for (int x: xs)
					for (int y: ys)
						for (int z: zs)
							tileList.add(new TileIndex(z, x, y));
			}
		}
		return tileList;
	}

	private List<Integer> parseIndex (String index) {
		List<Integer> result = new ArrayList<>();
		String[] subIndices;
		if (index.contains(" ")) {
			subIndices = index.split(" ");
		} else {
			subIndices = new String[] {index};
		}

		for (String subIndex: subIndices) {
			if (subIndex.contains("-")) {
				String[] bounds = subIndex.split("-");
				if (bounds.length >= 2) {
					int min = Integer.parseInt(bounds[0]);
					int max = Integer.parseInt(bounds[1]);
					for (int i=min; i<=max; ++i) result.add(i);
				}
			} else {
				result.add(Integer.parseInt(subIndex));
			}
		}
		return result;
	}


	public void testTileRetrieval (List<TileIndex> indices) {
		// Do a GC to begin with, so we're just timing tiling
		System.gc();
		System.gc();
		System.gc();
		long startTime = System.currentTimeMillis();
		List<TileData<Double>> tiles = _pyramidIO.readTiles("julia", _serializer, indices);
		long endTime = System.currentTimeMillis();
		Set<TileIndex> input = new HashSet<>(indices);
		Set<TileIndex> output = new HashSet<>();
		for (TileData<Double> tile: tiles) output.add(tile.getDefinition());
		System.out.println("Retrieved "+tiles.size()+" tiles ("+indices.size()+" expected) in "+
		                   ((endTime-startTime)/1000.0)+" seconds");
		System.out.println("Input: "+input);
		System.out.println("Output: "+output);
		Set<TileIndex> missing = new HashSet<>(input);
		missing.removeAll(output);
		Set<TileIndex> extra = new HashSet<>(output);
		extra.removeAll(input);
		System.out.println(missing.size()+" missing, "+extra.size()+" extra");
		System.out.println("Missing: "+missing);
		System.out.println("Extra: "+extra);
	}
}
