package com.oculusinfo.factory.properties;

import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.ConfigurationProperty;
import com.oculusinfo.factory.JSONNode;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

public class EnumProperty<T extends Enum<T>> implements ConfigurationProperty<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnumProperty.class);
	private String _name;
	private String _description;
	private Class<T> _type;
	private T _defaultValue;
	private Method _valueOf;
	private String _uuid;

	public EnumProperty (String name, String description, Class<T> type, T defaultValue) {
		_name = name;
		_description = description;
		_type = type;
		_defaultValue = defaultValue;
		_uuid = UUID.randomUUID().toString();
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

	@Override
	public int hashCode () {
		return _uuid.hashCode();
	}

	@Override
	public boolean equals (Object that) {
		if (this == that) return true;
		if (null == that) return false;
		if (!(that instanceof EnumProperty)) return false;
        
		EnumProperty<?> thatP = (EnumProperty<?>) that;
		return thatP._uuid.equals(this._uuid);
	}

	@Override
	public String toString () {
		return String.format("<property name=\"%s\" type=\"enum[%s]\"/>", _name, _type.getSimpleName());
	}
}
