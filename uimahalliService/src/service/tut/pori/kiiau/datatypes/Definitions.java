/**
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
package service.tut.pori.kiiau.datatypes;

/**
 * Definitions for package datatypes.
 */
public final class Definitions {
	/* json object/array names */
	/** JSON name/object declaration */
	public static final String JSON_NAME_DATE = "updated";
	/** JSON name/object declaration */
	public static final String JSON_NAME_CUMULATIVE = "cumulative";
	/** JSON name/object declaration */
	public static final String JSON_NAME_OPTIONS = "option";
	/** JSON name/object declaration */
	public static final String JSON_NAME_MAX = "max";
	/** JSON name/object declaration */
	public static final String JSON_NAME_MIN = "min";
	/** JSON name/object declaration */
	public static final String JSON_NAME_MAX_INCREASE = "maxIncrease";
	/** JSON name/object declaration */
	public static final String JSON_NAME_MIN_INCREASE = "minIncrease";
	/** JSON name/object declaration */
	public static final String JSON_NAME_UNIT = "unit";
	/** JSON name/object declaration */
	public static final String JSON_NAME_DESCRIPTION = "description";
	/** JSON name/object declaration */
	public static final String JSON_NAME_LOCATION_ID = "locationId";
	/** JSON name/object declaration */
	public static final String JSON_NAME_LOCATION_X = "locationX";
	/** JSON name/object declaration */
	public static final String JSON_NAME_LOCATION_Y = "locationY";
	/** JSON name/object declaration */
	public static final String JSON_NAME_NAME = "name";
	/** JSON name/object declaration */
	public static final String JSON_NAME_INDEX = "index";
	/** JSON name/object declaration */
	public static final String JSON_NAME_GAUGES = "gauges";
	/** JSON name/object declaration */
	public static final String JSON_NAME_VALUE = "value";
	/** JSON name/object declaration */
	public static final String JSON_NAME_METERS = "meters";
	/** JSON name/object declaration */
	public static final String JSON_NAME_GAUGE_VALUES = "gaugeValues";
	/** JSON name/object declaration */
	public static final String JSON_NAME_GAUGE_VALUE = "gaugeValue";
	/** JSON name/object declaration */
	public static final String JSON_NAME_DATATYPE = "dataType";
	/** JSON name/object declaration */
	public static final String JSON_NAME_FLOOR_PLAN_URL = "floorPlanUrl";
	/** JSON name/object declaration */
	public static final String JSON_NAME_LOCATIONS = "locations";
	/** JSON name/object declaration */
	public static final String JSON_NAME_ALERTS = "alerts";
	/** JSON name/object declaration */
	public static final String JSON_NAME_ALERT_TYPE = "type";
	/** JSON name/object declaration */
	public static final String JSON_NAME_ALERT_STATUS = "status";
	/** JSON name/object declaration */
	public static final String JSON_NAME_GAUGE_STATISTICS = "gaugeStatistics";
	/** JSON name/object declaration */
	public static final String JSON_NAME_METER_STATISTICS = "meterStatistics";
	/** JSON name/object declaration */
	public static final String JSON_NAME_AVERAGE = "average";
	/** JSON name/object declaration */
	public static final String JSON_NAME_VARIANCE = "variance";
	/** JSON name/object declaration */
	public static final String JSON_NAME_STANDARD_DEVIATION = "standardDeviation";
	/** JSON name/object declaration, the json name/database columns are somewhat ambiguously named, check the implementation for proper meanings */
	@Deprecated
	public static final String JSON_NAME_ALERT_TAG_ID = "tagId";
	/** JSON name/object declaration, the json name/database columns are somewhat ambiguously named, check the implementation for proper meanings */
	@Deprecated
	public static final String JSON_NAME_ALERT_GAUGE_ID = "gaugeId";
	/** JSON name/object declaration, the json name/database columns are somewhat ambiguously named, check the implementation for proper meanings */
	@Deprecated
	public static final String JSON_NAME_GAUGE_ID = "id";
	/** JSON name/object declaration, the json name/database columns are somewhat ambiguously named, check the implementation for proper meanings */
	@Deprecated
	public static final String JSON_NAME_TAG_ID = "id";
	
	/**
	 * 
	 */
	private Definitions(){
		//nothing needed
	}
}
