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
package com.otula.datatypes;

public final class Definitions {
	/* json object/array names */
	protected static final String JSON_NAME_CUMULATIVE = "cumulative";
	protected static final String JSON_NAME_OPTIONS = "option";
	protected static final String JSON_NAME_MAX = "max";
	protected static final String JSON_NAME_MIN = "min";
	protected static final String JSON_NAME_MAX_INCREASE = "maxIncrease";
	protected static final String JSON_NAME_MIN_INCREASE = "minIncrease";
	protected static final String JSON_NAME_UNIT = "unit";
	protected static final String JSON_NAME_DESCRIPTION = "description";
	protected static final String JSON_NAME_NAME = "name";
	protected static final String JSON_NAME_INDEX = "index";
	protected static final String JSON_NAME_METER_ID = "id";
	protected static final String JSON_NAME_GAUGES = "gauges";
	protected static final String JSON_NAME_GAUGE_ID = "id";
	protected static final String JSON_NAME_VALUE = "value";
	protected static final String JSON_NAME_DATE = "updated";
	protected static final String JSON_NAME_METERS = "meters";
	protected static final String JSON_NAME_GAUGE_VALUES = "gaugeValues";
	protected static final String JSON_NAME_GAUGE_VALUE = "gaugeValue";
	protected static final String JSON_NAME_DATATYPE = "dataType";
	
	/* common */
	public static final String DEFAULT_CHARSET = "UTF-8";
	public static final String DEFAULT_CONTENT_TYPE = "application/x-www-form-urlencoded; charset=utf-8";
	
	/* touch event thresholds */
	public static final long THRESHOLD_CLICK_MAX_DURATION = 300;	// threshold for click, in ms
	public static final float THRESHOLD_CLICK_MAX_MOVEMENT = 10; // in pixels?
	public static final float THRESHOLD_SWIPE_MIN_MOVEMENT = 100; // in pixels?
	public static final long THRESHOLD_SWIPE_MAX_DURATION = 1000;	// in ms
	public static final long THRESHOLD_LONG_CLICK_MIN_DURATION = 2000; // in ms
	public static final long THRESHOLD_LONG_CLICK_MAX_DURATION = 30000; // in ms
	
	private Definitions(){
		//nothing needed
	}
}
