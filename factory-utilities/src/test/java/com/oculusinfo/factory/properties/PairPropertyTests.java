/*
 * Copyright (c) 2014 Oculus Info Inc. http://www.oculusinfo.com/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.factory.properties;

import com.oculusinfo.factory.util.Pair;
import com.oculusinfo.factory.ConfigurationException;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by nkronenfeld on 1/7/2015.
 */
public class PairPropertyTests {
	private static IntegerProperty INT = new IntegerProperty("", "", 0);
	private static StringProperty STRING = new StringProperty("", "", "");
	private static PairProperty<Integer, String> PAIR1 =
		new PairProperty<>(INT, STRING, "", "", new Pair<Integer, String>(0, ""));

	private static PairProperty<String, Integer> PAIR2 =
		new PairProperty<>(STRING, INT, "", "", new Pair<String, Integer>("", 0));

	private static PairProperty<String, String> PAIR3 =
		new PairProperty<>(STRING, STRING, "", "", new Pair<String, String>("", ""));

	private static PairProperty<List<String>, List<Integer>> PAIR4 =
		new PairProperty<>(new ListProperty<String>(STRING, "", ""), new ListProperty<Integer>(INT, "", ""),
		                   "", "", new Pair<List<String>, List<Integer>>(new ArrayList<String>(), new ArrayList<Integer>()));

	@Test
	public void testStringEncoding () throws ConfigurationException {
		Pair<Integer, String> a = new Pair<Integer, String>(4, "four");
		String aEncoded = PAIR1.encode(a);
		Pair<Integer, String> aRedux = PAIR1.unencode(aEncoded);
		Assert.assertEquals(a, aRedux);

		Pair<String, Integer> b = new Pair<String, Integer>("negative Eight, or less", -9);
		String bEncoded = PAIR2.encode(b);
		Pair<String, Integer> bRedux = PAIR2.unencode(bEncoded);
		Assert.assertEquals(b, bRedux);

		Pair<String, String> c = new Pair<String, String>("abc,,,\\,\\\\", "\\,\\,\\,\\,\\,");
		String cEncoded = PAIR3.encode(c);
		Pair<String, String> cRedux = PAIR3.unencode(cEncoded);
		Assert.assertEquals(c, cRedux);

		Pair<String, String> d = new Pair<String, String>("\\,\\,\\,\\,\\,", "abc,,,\\,\\\\");
		String dEncoded = PAIR3.encode(d);
		Pair<String, String> dRedux = PAIR3.unencode(dEncoded);
		Assert.assertEquals(d, dRedux);

		Pair<List<String>, List<Integer>> e = new Pair<List<String>, List<Integer>>(
			      Arrays.asList("abc", "def", "ghi", "jkl\\,\\,\\"),
			      Arrays.asList(1, 2, 3, 4, 5, 10, 11, 12, 13, 14));
		String eEncoded = PAIR4.encode(e);
		Pair<List<String>, List<Integer>> eRedux = PAIR4.unencode(eEncoded);
		Assert.assertEquals(e.getFirst().size(), eRedux.getFirst().size());
		for (int i=0; i<e.getFirst().size(); ++i)
			Assert.assertEquals(e.getFirst().get(i), eRedux.getFirst().get(i));
		Assert.assertEquals(e.getSecond().size(), eRedux.getSecond().size());
		for (int i=0; i<e.getSecond().size(); ++i)
			Assert.assertEquals(e.getSecond().get(i), eRedux.getSecond().get(i));
	}
}
