package com.oculusinfo.tile.spi.impl;

import com.oculusinfo.tile.util.TransformParameter;

public class ValueTransformerFactory {

	public static IValueTransformer create(TransformParameter transform, double levelMin, double levelMax) {
		IValueTransformer t;
		String name = transform.getName();
		
		if(name.equalsIgnoreCase("log10")){ 
			t = new Log10ValueTransformer(levelMax);
		}else if (name.equalsIgnoreCase("minmax")) {
			double min = transform.getDoubleOrElse("min", levelMin);
			double max = transform.getDoubleOrElse("max", levelMax);
			t = new LinearCappedValueTransformer(min, max);
		}else { //if 'linear'
			t = new LinearCappedValueTransformer(levelMin, levelMax);
		}
		return t;
	}

}
