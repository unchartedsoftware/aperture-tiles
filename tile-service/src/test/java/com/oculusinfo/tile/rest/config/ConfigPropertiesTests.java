package com.oculusinfo.tile.rest.config;

import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.oculusinfo.tile.rest.config.ConfigPropertiesServiceImpl.CONFIG_ENV_VAR;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class ConfigPropertiesTests {

    protected ConfigPropertiesServiceImpl _service;

    @Before
    public void setUp() throws Exception {
    	_service = new ConfigPropertiesServiceImpl();
    }

    @After
    public void tearDown() throws Exception {
        HashMap<String, String> newenv = new HashMap<>();
        newenv.put(CONFIG_ENV_VAR, "");
        setEnv(newenv);
    }

    @Test
    public void testGetProperties() throws Exception {
        String pathToProperties = this.getClass().getClassLoader().getResource("config-service-unit-test.properties").toURI().getPath();
        Map<String,String> newEnv = new HashMap<>();
        newEnv.put(CONFIG_ENV_VAR, pathToProperties);
        setEnv(newEnv);
        
        Properties properties = _service.getConfigProperties();

        assertTrue(StringUtils.equals( properties.getProperty("foo.zookeeper.quorum"), "bar.test.local") );
        assertTrue(StringUtils.equals( properties.getProperty("foo.zookeeper.port"), "2222") );
        assertTrue(StringUtils.equals( properties.getProperty("foo.master"), "bar.test.local:33333") );
        assertTrue(StringUtils.equals( properties.getProperty("drilldown.endpoint"), "http://some.host/some.data/") );
    }

    @Test(expected = ConfigException.class)
    public void testGetProperties_invalidPathToPropertiesInEnvVar() throws Exception {
        Map<String,String> newEnv = new HashMap<>();
        newEnv.put(CONFIG_ENV_VAR, "invalid/path/to.properties");
        setEnv(newEnv);
        
    	Properties properties = _service.getConfigProperties();
    	assertNull(properties.get( "foo.zookeeper.quorum" ));
    }

    @Test
    public void testReplaceProperties_envVarIsNull_usesDefaultReplacements() throws Exception {
    	HashMap<String, String> newenv = new HashMap<>();
        newenv.put(CONFIG_ENV_VAR, "");
        setEnv(newenv);
        
        Properties properties = _service.getConfigProperties();

        assertTrue(StringUtils.equals( properties.getProperty("hbase.zookeeper.quorum"), "some.host") );
        assertTrue(StringUtils.equals( properties.getProperty("hbase.zookeeper.port"), "12345") );
        assertTrue(StringUtils.equals( properties.getProperty("hbase.master"), "foo") );
        assertTrue(StringUtils.equals( properties.getProperty("drilldown.endpoint"), "http://some.endpoint/somedata") );     
    }

    // ONLY FOR UNIT TESTING
    // http://stackoverflow.com/questions/318239/how-do-i-set-environment-variables-from-java
    @SuppressWarnings({ "unchecked", "rawtypes" })
	private void setEnv(Map<String, String> newenv) {
        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.putAll(newenv);
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>)     theCaseInsensitiveEnvironmentField.get(null);
            cienv.putAll(newenv);
        } catch (NoSuchFieldException e) {
            try {
                Class[] classes = Collections.class.getDeclaredClasses();
                Map<String, String> env = System.getenv();
                for(Class cl : classes) {
                    if("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                        Field field = cl.getDeclaredField("m");
                        field.setAccessible(true);
                        Object obj = field.get(env);
                        Map<String, String> map = (Map<String, String>) obj;
                        map.clear();
                        map.putAll(newenv);
                    }
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }
}