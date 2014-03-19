package com.oculusinfo.factory.properties;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.ConfigurationProperty;
import com.oculusinfo.factory.JSONNode;

public class EnumProperty<T extends Enum<T>> implements ConfigurationProperty<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnumProperty.class);



    private String   _name;
    private String   _description;
    private Class<T> _type;
    private T _defaultValue;
    private Method _valueOf;



    public EnumProperty (String name, String description, Class<T> type, T defaultValue) {
        _name = name;
        _description = description;
        _type = type;
        _defaultValue = defaultValue;
        try {
            _valueOf = type.getDeclaredMethod("valueOf", String.class);
        } catch (NoSuchMethodException e) {
            LOGGER.warn("Attempt to initialize an EnumProperty with a non-enum", e);
        } catch (SecurityException e) {
            LOGGER.warn("Attempt to initialize an EnumProperty with an inaccessible enum", e);
        }
    }

    @Override
    public String getName () {
        return _name;
    }

    @Override
    public String getDescription () {
        return _description;
    }

    @Override
    public Class<T> getType () {
        return _type;
    }

    @Override
    public T[] getPossibleValues () {
        return _type.getEnumConstants();
    }

    @Override
    public T getDefaultValue () {
        return _defaultValue;
    }

    @Override
    public String encode (T value) {
        return value.toString();
    }

    @Override
    public T unencode (String value) throws ConfigurationException {
        try {
            return _type.cast(_valueOf.invoke(null, value));
        } catch (IllegalAccessException e) {
            throw new ConfigurationException("Attempt to get inaccessible value "+value, e);
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException("enum.valueOf somehow takes a non-string", e);
        } catch (InvocationTargetException e) {
            throw new ConfigurationException("enum.valueOf is impossibly non-static!", e);
        }
    }

    @Override
    public void encodeJSON (JSONNode propertyNode, T value) throws JSONException {
        propertyNode.setAsString(encode(value));
    }

    @Override
    public T unencodeJSON (JSONNode propertyNode) throws JSONException, ConfigurationException {
        return unencode(propertyNode.getAsString());
    }
}
