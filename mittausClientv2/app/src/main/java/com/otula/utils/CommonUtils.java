/*
 * Copyright 2014 Tampere University of Technology, Pori Department
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.otula.utils;

import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Date;

import android.webkit.URLUtil;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.otula.datatypes.Gauge;

/**
 * 
 * Common utility methods for MittausClient
 *
 */
public final class CommonUtils {
	private static final String CLASS_NAME = CommonUtils.class.toString(); 
	public static final int DEFAULT_DECIMAL_SCALE = 2; 
	
	private static final NumberFormat DECIMAL_FORMAT;
	
	static {
		DECIMAL_FORMAT = DecimalFormat.getInstance();
		DECIMAL_FORMAT.setMaximumFractionDigits(DEFAULT_DECIMAL_SCALE);
	}
	
	/**
	 * 
	 */
	private CommonUtils(){
		// nothing needed
	}
	
	/**
	 * 
	 * @return
	 */
	public static Gson createGsonSerializer(){
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(Date.class, new JsonSerializer<Date>() {

			@Override
			public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
				return (src == null ? null : new JsonPrimitive(DateUtils.dateToString(src)));
			}
		});
		builder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {

			@Override
			public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
				return (json == null ? null : DateUtils.stringToDate(json.getAsString()));
			}
		});
		return builder.create();
	}
	
	/**
	 * 
	 * @param b
	 * @return
	 */
	public static int booleanToInt(Boolean b){
		if(b == null || !b){
			return 0;
		}else{
			return 1;
		}
	}
	
	/**
	 * 
	 * @param i
	 * @return
	 */
	public static boolean intToBoolean(Integer i){
		if(i == null || i == 0){
			return false;
		}else{
			return true;
		}
	}
	
	/**
	 * 
	 * @param url
	 * @return true if url was valid
	 */
	public static boolean isValidUrl(String url){
		if(url != null && !url.isEmpty() && URLUtil.isValidUrl(url)){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * This method returns char sequence instead of string for a reason.
	 * @param gauge
	 * @return
	 */
	public static CharSequence createMinMaxString(Gauge gauge){
		Double min = gauge.getMinLimitValue();
		Double max = gauge.getMaxLimitValue();
		if(min == null && max == null){
			return "";
		}
		
		String unit = gauge.getUnit();
		StringBuilder sb = new StringBuilder();
		if(min != null){
			sb.append(doubleValueToDisplayString(min));
		}
		sb.append(" \u2013 "); //" -- "
		if(max != null){
			sb.append(doubleValueToDisplayString(max));
		}
		if(unit != null){
			sb.append(" (");
			sb.append(unit);
			sb.append(")");
		}
		return sb;
	}
	
	public static String doubleValueToDisplayString(double value){
		synchronized (DECIMAL_FORMAT) {
			return DECIMAL_FORMAT.format(value);
		}		
	}
	
	public static Double displayStringToDouble(String value){
		synchronized (DECIMAL_FORMAT) {
			try {
				return DECIMAL_FORMAT.parse(value).doubleValue();
			} catch (ParseException ex) {
				 LogUtils.error(CLASS_NAME, "displayStringToDouble", ex.toString());
				 return null;
			}
		}		
	}
}
