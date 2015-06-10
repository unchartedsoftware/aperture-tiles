package com.oculusinfo.binning.util;

import com.oculusinfo.binning.util.Statistics;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test statistics functions
 */
public class StatisticsTest {
	@Test
	public void testMilliTimer () throws Exception {
		Object foo = new Object();
		synchronized (foo) {
			Statistics.clearAll();
			Statistics.checkpointTimeMillis("a");
			foo.wait(50);
			Statistics.checkpointTimeMillis("b");
			foo.wait(75);
			Statistics.checkpointTimeMillis("c");
			foo.wait(100);
			Statistics.checkpointTimeMillis("d");
			Statistics.addCheckpointDifference("time", "a", "b");
			Statistics.addCheckpointDifference("time", "b", "c");
			Statistics.addCheckpointDifference("time", "c", "d");
			Statistics.Statistic stat = Statistics.statistic("time");
			double mean = stat.mean();
			double sigma = stat.standardDeviation();
			Assert.assertEquals(3, stat.count());
			Assert.assertTrue(stat.toString(), 74 < mean && mean < 76);
			// Standard deviation should, ideally, be 20.4ish
			Assert.assertTrue(stat.toString(), 20 < sigma && sigma < 21);
			// Lower bound should be absolute
			Assert.assertTrue(stat.toString(), 50 <= stat.min());
			Assert.assertTrue(stat.toString(), stat.max() <= 101);
		}
	}

	@Test
	public void testNanoTimer () throws Exception {
		Object foo = new Object();
		synchronized (foo) {
			Statistics.clearAll();
			Statistics.checkpointTimeNano("a");
			foo.wait(50, 500000);
			Statistics.checkpointTimeNano("b");
			foo.wait(75, 500000);
			Statistics.checkpointTimeNano("c");
			foo.wait(100, 500000);
			Statistics.checkpointTimeNano("d");
			Statistics.addCheckpointDifference("time", "a", "b");
			Statistics.addCheckpointDifference("time", "b", "c");
			Statistics.addCheckpointDifference("time", "c", "d");
			Statistics.Statistic stat = Statistics.statistic("time");
			double mean = stat.mean();
			double sigma = stat.standardDeviation();
			Assert.assertEquals(3, stat.count());
			Assert.assertTrue(stat.toString(), 74.5 < mean && mean < 76.5);
			// Standard deviation should, ideally, be 20.4ish
			Assert.assertTrue(stat.toString(), 20 < sigma && sigma < 21);
			// Lower bound should be absolute
			Assert.assertTrue(stat.toString(), 50.5 <= stat.min());
			Assert.assertTrue(stat.toString(), stat.max() <= 101.5);
		}
	}

	@Test
	public void testClearing () {
		double epsilon = 1E-12;
		Statistics.clearAll();
		Statistics.addStatistic("abc", 10L);
		Statistics.Statistic stat1 = Statistics.statistic("abc");
		Assert.assertEquals(1,    stat1.count());
		Assert.assertEquals(10.0, stat1.mean(), epsilon);
		Assert.assertEquals(0.0,  stat1.standardDeviation(), epsilon);
		Assert.assertEquals(10.0, stat1.min(), epsilon);
		Assert.assertEquals(10.0, stat1.max(), epsilon);

		Statistics.addStatistic("abc", 20L);
		Statistics.Statistic stat2 = Statistics.statistic("abc");
		Assert.assertEquals(2,    stat2.count());
		Assert.assertEquals(15.0, stat2.mean(), epsilon);
		Assert.assertEquals(5.0,  stat2.standardDeviation(), epsilon);
		Assert.assertEquals(10.0, stat2.min(), epsilon);
		Assert.assertEquals(20.0, stat2.max(), epsilon);

		Statistics.clearStatistics("abc");

		Statistics.addStatistic("abc", 30L);
		Statistics.Statistic stat3 = Statistics.statistic("abc");
		Assert.assertEquals(1,    stat3.count());
		Assert.assertEquals(30.0, stat3.mean(), epsilon);
		Assert.assertEquals(0.0,  stat3.standardDeviation(), epsilon);
		Assert.assertEquals(30.0, stat3.min(), epsilon);
		Assert.assertEquals(30.0, stat3.max(), epsilon);

		Assert.assertEquals(stat1, stat2);
		Assert.assertNotEquals(stat1, stat3);
	}

	private class TestListener implements Statistics.StatisticListener {
		boolean notified = false;
		Statistics.Statistic last  = null;
		@Override
		public void onStatisticUpdated(Statistics.Statistic statistic) {
			last = statistic;
			notified = true;
		}
	}
	@Test
	public void testListenerNotification () {
		Statistics.clearAll();;

		TestListener listener = new TestListener();
		Statistics.addStatisticListener("notification", listener);
		Assert.assertTrue(listener.notified);
		Assert.assertEquals(null, listener.last);


		Statistics.addStatistic("notification", 1L);
		Assert.assertEquals(1, listener.last.count());
		Statistics.addStatistic("no notification", 1L);
		Assert.assertEquals(1, listener.last.count());
		Statistics.addStatistic("notification", 1L);
		Assert.assertEquals(2, listener.last.count());

		Statistics.clearStatistics("notification");
		Assert.assertEquals(null, listener.last);
		Statistics.addStatistic("no notification", 1L);
		Assert.assertEquals(null, listener.last);
		Statistics.addStatistic("notification", 1L);
		Assert.assertEquals(1, listener.last.count());

		Statistics.clearAll();;
		Assert.assertEquals(null, listener.last);
		Statistics.addStatistic("no notification", 1L);
		Assert.assertEquals(null, listener.last);
		Statistics.addStatistic("notification", 1L);
		Assert.assertEquals(1, listener.last.count());
	}
}
