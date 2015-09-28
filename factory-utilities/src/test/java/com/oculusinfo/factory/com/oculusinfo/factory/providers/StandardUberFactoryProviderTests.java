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
package com.oculusinfo.factory.com.oculusinfo.factory.providers;

import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.UberFactory;
import com.oculusinfo.factory.properties.IntegerProperty;
import com.oculusinfo.factory.providers.FactoryProvider;
import com.oculusinfo.factory.providers.AbstractFactoryProvider;
import com.oculusinfo.factory.providers.StandardUberFactoryProvider;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by nkronenfeld on 1/22/2015.
 */
public class StandardUberFactoryProviderTests {
    // Test whether or not path merging works properly
    @Test
    public void testPathMergingWithParent () throws JSONException, ConfigurationException {
        Set<FactoryProvider<TestConstruct>> subProviders = new HashSet<>();
        subProviders.add(new ConstructFactoryProvider("A", "a"));
        subProviders.add(new ConstructFactoryProvider("B", "b"));
        subProviders.add(new ConstructFactoryProvider("C", "c"));
        FactoryProvider<TestConstruct> provider = new StandardUberFactoryProvider<TestConstruct>(subProviders) {
            @Override
            public ConfigurableFactory<TestConstruct> createFactory(String name, ConfigurableFactory<?> parent, List<String> path) {
                return new UberFactory<TestConstruct>(name, TestConstruct.class, parent, path, createChildren(parent, path), "A");
            }
        };

        ConfigurableFactory<String> parent = new TestParent(Arrays.asList("root"));
        ConfigurableFactory<? extends TestConstruct> factoryA = provider.createFactory(parent, Arrays.asList("branch"));
        factoryA.readConfiguration(new JSONObject("{root: {branch: {type: \"A\", a: {value: 1}}}}"));
        TestConstruct a = factoryA.produce(TestConstruct.class);
        Assert.assertEquals("A", a._name);
        assertPathsEqual(Arrays.asList("root", "branch", "a"), a._path);
        Assert.assertEquals(1, a._value);

        ConfigurableFactory<? extends TestConstruct> factoryB = provider.createFactory(parent, Arrays.asList("branch"));
        factoryB.readConfiguration(new JSONObject("{root: {branch: {type: \"B\", b: {value: 2}}}}"));
        TestConstruct b = factoryB.produce(TestConstruct.class);
        Assert.assertEquals("B", b._name);
        assertPathsEqual(Arrays.asList("root", "branch", "b"), b._path);
        Assert.assertEquals(2, b._value);

        ConfigurableFactory<? extends TestConstruct> factoryC = provider.createFactory(parent, Arrays.asList("branch"));
        factoryC.readConfiguration(new JSONObject("{root: {branch: {type: \"C\", c: {value: 3}}}}"));
        TestConstruct c = factoryC.produce(TestConstruct.class);
        Assert.assertEquals("C", c._name);
        assertPathsEqual(Arrays.asList("root", "branch", "c"), c._path);
        Assert.assertEquals(3, c._value);
    }

    private void assertPathsEqual (List<String> expected, List<String> actual) {
        Assert.assertEquals(expected.size(), actual.size());
        for (int i=0; i<expected.size(); ++i) {
            Assert.assertEquals(expected.get(i), actual.get(i));
        }
    }



    public static class TestParent extends ConfigurableFactory<String> {
        public TestParent (List<String> path) {
            super(String.class, null, path);
        }
        @Override
        protected String create() {
            return "parent";
        }
    }
    public static class TestConstruct {
        String _name;
        List<String> _path;
        int _value;
        public TestConstruct (String name, List<String> path, int value) {
            _name = name;
            _path = path;
            _value = value;
        }
    }

    public static class ConstructFactory extends ConfigurableFactory<TestConstruct> {
        public static IntegerProperty VALUE = new IntegerProperty("value", "", 0);
        public ConstructFactory (String name, List<String> pathAdditions, ConfigurableFactory<?> parent, List<String> path) {
            super(name, TestConstruct.class, parent, mergePaths(path, pathAdditions));
            addProperty(VALUE);
        }

        @Override
        protected TestConstruct create() throws ConfigurationException {
            int value = getPropertyValue(VALUE);
            return new TestConstruct(getName(), getRootPath(), value);
        }
    }

    public static class ConstructFactoryProvider extends AbstractFactoryProvider<TestConstruct> {
        private String _name;
        private List<String> _pathAdditions;
        public ConstructFactoryProvider (String name, String... pathAdditions) {
            _name = name;
            _pathAdditions = Arrays.asList(pathAdditions);
        }

        @Override
        public ConfigurableFactory<? extends TestConstruct> createFactory(String name, ConfigurableFactory<?> parent, List<String> path) {
            // Ignore passed-in name, we have a specific name to use here.
            return new ConstructFactory(_name, _pathAdditions, parent, path);
        }
    }
}
