package com.oculusinfo.tile.rest.config;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.oculusinfo.tile.rest.config.ConfigPropertiesServiceImpl.CONFIG_ENV_VAR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ConfigServiceTests {

    protected ConfigServiceImpl _configService;

    @Before
    public void setUp() throws Exception {
    	ConfigPropertiesService configPropsService = new ConfigPropertiesServiceImpl();
    	_configService = new ConfigServiceImpl( configPropsService );
    }

    @After
    public void tearDown() throws Exception {
        HashMap<String, String> newenv = new HashMap<>();
        newenv.put(CONFIG_ENV_VAR, "");
        setEnv(newenv);
    }

    @Test
    public void testReplaceProperties() throws Exception {
        String pathToProperties = this.getClass().getClassLoader().getResource("config-service-unit-test.properties").getPath();
        Map<String,String> newEnv = new HashMap<>();
        newEnv.put(CONFIG_ENV_VAR, pathToProperties);
        setEnv(newEnv);

        File configFile = new File(this.getClass().getClassLoader().getResource("config-service-unit-test.json").toURI().getPath());
        String replaced = _configService.replaceProperties(configFile);

        assertTrue(replaced.contains("\"foo.zookeeper.quorum\": \"bar.test.local\""));
        assertTrue(replaced.contains("\"foo.zookeeper.port\": \"2222\""));
        assertTrue(replaced.contains("\"foo.master\": \"bar.test.local:33333\""));
        assertTrue(replaced.contains("\"endpoint\": \"http://some.host/some.data/\""));
    }

    @Test(expected = ConfigException.class)
    public void testReplaceProperties_invalidConfigFile() throws Exception {
        File foo = new File("foo");
        _configService.replaceProperties(foo);
    }

    @Test(expected = ConfigException.class)
    public void testReplaceProperties_invalidPathToPropertiesInEnvVar() throws Exception {
        Map<String,String> newEnv = new HashMap<>();
        newEnv.put(CONFIG_ENV_VAR, "invalid/path/to.properties");
        setEnv(newEnv);
        File configFile = new File(this.getClass().getClassLoader().getResource("config-service-unit-test.json").toURI().getPath());
        _configService.replaceProperties(configFile);
    }

    @Test
    public void testReplaceProperties_envVarIsNull_usesDefaultReplacements() throws Exception {
        File configFile = new File(this.getClass().getClassLoader().getResource("config-service-unit-test.json").toURI().getPath());
        String replaced = _configService.replaceProperties(configFile);

        // The default properties file only has one replacement that matches a key in the test json file
        assertTrue(replaced.contains("\"endpoint\": \"http://some.endpoint/somedata\""));

        // The other keys remain templated
        assertTrue(replaced.contains("\"foo.zookeeper.quorum\": \"${foo.zookeeper.quorum}\""));
        assertTrue(replaced.contains("\"foo.zookeeper.port\": \"${foo.zookeeper.port}\""));
        assertTrue(replaced.contains("\"foo.master\": \"${foo.master}\""));
    }

    @Test
    public void testReplaceTokens() {
        String text = "The ${some.animal} jumped over the ${some.object}";
        Map<String, String> replacements = new HashMap<>();
        replacements.put("some.animal", "cow");
        replacements.put("some.object", "moon");

        String replaced = _configService.replaceTokens(text, replacements, ConfigServiceImpl.PROPERTIES_FILE_REPLACEMENT_REGEX, false);
        assertEquals("The cow jumped over the moon", replaced);
    }

    @Test
    public void testFindResourceConfig_found() throws Exception {
        File resourceConfig = _configService.findResourceConfig("test1.json");
        assertNotNull(resourceConfig);
    }

    @Test
    public void testFindResourceConfig_notFound() throws Exception {
        File resourceConfig = _configService.findResourceConfig("test2.txt");
        assertNull(resourceConfig);
    }

    // ONLY FOR UNIT TESTING
    // http://stackoverflow.com/questions/318239/how-do-i-set-environment-variables-from-java
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
