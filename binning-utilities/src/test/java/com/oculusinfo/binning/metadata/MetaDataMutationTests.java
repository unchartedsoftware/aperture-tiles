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
package com.oculusinfo.binning.metadata;

import com.oculusinfo.binning.util.Pair;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class MetaDataMutationTests {
	@Test
	public void testVersionMutationPath () {
		PyramidMetaDataVersionMutator.ALL_MUTATORS.clear();
		PyramidMetaDataVersionMutator.ALL_MUTATORS.add(new PyramidMetaDataVersionMutator("v0", "v1"));
		PyramidMetaDataVersionMutator.ALL_MUTATORS.add(new PyramidMetaDataVersionMutator("v1", "v2"));
		PyramidMetaDataVersionMutator.ALL_MUTATORS.add(new PyramidMetaDataVersionMutator("v2", "v2a"));
		PyramidMetaDataVersionMutator.ALL_MUTATORS.add(new PyramidMetaDataVersionMutator("v2", "v2b"));
		PyramidMetaDataVersionMutator.ALL_MUTATORS.add(new PyramidMetaDataVersionMutator("v2", "v2c"));
		PyramidMetaDataVersionMutator.ALL_MUTATORS.add(new PyramidMetaDataVersionMutator("v2a", "v3a"));
		PyramidMetaDataVersionMutator.ALL_MUTATORS.add(new PyramidMetaDataVersionMutator("v2b", "v3a"));
		PyramidMetaDataVersionMutator.ALL_MUTATORS.add(new PyramidMetaDataVersionMutator("v2c", "v3b"));
		PyramidMetaDataVersionMutator.ALL_MUTATORS.add(new PyramidMetaDataVersionMutator("v3a", "v4"));
		PyramidMetaDataVersionMutator.ALL_MUTATORS.add(new PyramidMetaDataVersionMutator("v4", "v5"));

		List<PyramidMetaDataVersionMutator> path = PyramidMetaDataVersionMutator.getMutators("v2", "v5");
		Assert.assertEquals(4, path.size());
		Assert.assertTrue(new Pair<String, String>("v2", "v2a").equals(path.get(0).getVersionBounds()) ||
		                  new Pair<String, String>("v2", "v2b").equals(path.get(0).getVersionBounds()));
		Assert.assertTrue(new Pair<String, String>("v2a", "v3a").equals(path.get(1).getVersionBounds()) ||
		                  new Pair<String, String>("v2b", "v3a").equals(path.get(1).getVersionBounds()));
		Assert.assertEquals(new Pair<String, String>("v3a", "v4"), path.get(2).getVersionBounds());
		Assert.assertEquals(new Pair<String, String>("v4", "v5"), path.get(3).getVersionBounds());

		Assert.assertNull(PyramidMetaDataVersionMutator.getMutators("v2c", "v5"));
	}
}
