package com.oculusinfo.binning.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by nkronenfeld on 6/9/2015.
 */
public class Statistics {
	private static Set<String> _nanoCheckpoints = new HashSet<>();
	private static Set<String> _milliCheckpoints = new HashSet<>();
	private static Map<String, Double> _checkpoints = new HashMap<>();
	private static Map<String, Statistic> _statistics = new HashMap<>();
	private static Map<String, List<StatisticListener>> _listeners = new HashMap<>();

	public static void checkpoint (String name, double value) {
		_checkpoints.put(name, value);
	}

	public static void checkpointTimeMillis (String name) {
		_milliCheckpoints.add(name);
		checkpoint(name, System.currentTimeMillis());
	}

	public static void checkpointTimeNano (String name) {
		long time = System.nanoTime();
		_nanoCheckpoints.add(name);
		checkpoint(name, time);
	}

	public static void clearCheckpoints (String... names) {
		for (String name: names) {
			_checkpoints.remove(name);
			_nanoCheckpoints.remove(name);
			_milliCheckpoints.remove(name);
		}
	}

	public static void addStatistic (String name, double value) {
		if (!_statistics.containsKey(name))
			_statistics.put(name, new Statistic());

		Statistic stat = _statistics.get(name);
		stat.addDatum(value);

		if (_listeners.containsKey(name)) {
			for (StatisticListener listener: _listeners.get(name)) {
				listener.onStatisticUpdated(stat);
			}
		}
	}

	public static void addCheckpointDifference (String statisticName, String startCheckpointName, String endCheckpointName) {
		if (!_checkpoints.containsKey(startCheckpointName))
			throw new MissingCheckpointException(startCheckpointName);
		if (!_checkpoints.containsKey(endCheckpointName))
			throw new MissingCheckpointException(endCheckpointName);
		double difference = _checkpoints.get(endCheckpointName) - _checkpoints.get(startCheckpointName);

		// Set units and scales for times
		if (_nanoCheckpoints.contains(startCheckpointName) && _nanoCheckpoints.contains(endCheckpointName)) {
			if (!_statistics.containsKey(statisticName))
				_statistics.put(statisticName, new Statistic("ms", 1.0/1000000.0));
		} else if (_milliCheckpoints.contains(startCheckpointName) && _milliCheckpoints.contains(endCheckpointName)) {
			if (!_statistics.containsKey(statisticName))
				_statistics.put(statisticName, new Statistic("ms", 1.0));
		}

		addStatistic(statisticName, difference);
	}

	public static void clearStatistics (String... names) {
		for (String name: names) {
			_statistics.remove(name);
			if (_listeners.containsKey(name)) {
				for (StatisticListener listener: _listeners.get(name)) {
					listener.onStatisticUpdated(null);
				}
			}
		}
	}

	public static void clearAll () {
		List<String> statistics = new ArrayList<>(_statistics.keySet());
		for (String name: statistics) clearStatistics(name);
		_checkpoints.clear();
		_nanoCheckpoints.clear();
		_milliCheckpoints.clear();
	}

	public static void addStatisticListener (String name, StatisticListener listener) {
		if (!_listeners.containsKey(name)) {
			_listeners.put(name, new ArrayList<StatisticListener>());
		}
		_listeners.get(name).add(listener);
		listener.onStatisticUpdated(_statistics.get(name));
	}

	public static void removeStatisticListener (String name, StatisticListener listener) {
		if (_listeners.containsKey(name)) {
			List<StatisticListener> nameListeners = _listeners.get(name);
			nameListeners.remove(listener);
			if (nameListeners.isEmpty()) _listeners.remove(name);
		}
	}

	public static Statistic statistic (String name) {
		return _statistics.get(name);
	}
	public static String statisticReport (String name) {
		if (!_statistics.containsKey(name)) return "";
		return name+"\t"+_statistics.get(name).toString();
	}

	public static String fullReport () {
		List<String> statistics = new ArrayList<>(_statistics.keySet());
		Collections.sort(statistics);
		String result = "";
		for (String stat: statistics) {
			result += stat+"\t"+_statistics.get(stat).toString()+"\n";
		}
		return result;
	}

	public static class Statistic {
		private int _count;
		private double _total;
		private double _totalSquare;
		private double _min;
		private double _max;
		private String _unit;
		private double _scale;
		Statistic () {
			_count = 0;
			_total = 0L;
			_totalSquare = 0L;
			_scale = 1.0;
			_min = Double.POSITIVE_INFINITY;
			_max = Double.NEGATIVE_INFINITY;
			_unit = "";
		}
		Statistic (String unit, double scale) {
			this();
			_scale = scale;
			_unit = unit;
		}

		public void addDatum (double value) {
			_count++;
			_total += value;
			_totalSquare += value*value;
			_min = Math.min(_min, value);
			_max = Math.max(_max, value);
		}

		public int count () {return _count;}
		public double mean () {
			return (_total*_scale)/_count;
		}
		public double standardDeviation () {
			double mean = mean();
			return Math.sqrt((_totalSquare*_scale*_scale)/_count - mean*mean);
		}
		public double min () {return _min*_scale;}
		public double max () {return _max*_scale;}

		public String toString () {
			return String.format("count\t%d\tmean\t%.6f%s\tsigma\t%.6f%s\tmin\t%.6f%s\tmax\t%.6f%s",
			                     count(),
			                     mean(), _unit,
			                     standardDeviation(), _unit,
			                     min(), _unit,
			                     max(), _unit
				);
		}
	}

	private static class MissingCheckpointException extends RuntimeException {
		public MissingCheckpointException (String checkpointName) {
			super("Checkpoint "+checkpointName+" is missing.");
		}
	}
	public static interface StatisticListener {
		public void onStatisticUpdated (Statistic statistic);
	}
}
