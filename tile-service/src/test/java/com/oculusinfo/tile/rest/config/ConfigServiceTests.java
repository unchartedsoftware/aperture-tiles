package com.oculusinfo.tile.rest.config;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;

public class ConfigServiceTests {

    protected ConfigServiceImpl _configService;

    @Before
    public void setUp() throws Exception {
        _configService = new ConfigServiceImpl();
        String pathToProperties = this.getClass().getClassLoader().getResource("config-service-unit-test.properties").toURI().getPath();
        Map<String,String> newEnv = new HashMap<>();
        newEnv.put("TILE_CONFIG_PROPERTIES", pathToProperties);
        setEnv(newEnv);
    }

    @Test
    public void testReplaceProperties() throws Exception {
        File configFile = new File(this.getClass().getClassLoader().getResource("config-service-unit-test.json").toURI());
        String result = _configService.replaceProperties(configFile);
        System.out.println(result);
        assertNotNull(result);
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