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
package service.tut.pori.kiiau;

/**
 * common definitions for kiiau 
 *
 */
public final class Definitions {
	/**
	 * 
	 */
	private Definitions(){
		// nothing needed
	}
	
	/**  service name */
	protected static final String SERVICE_UIMAHALLI = "uimahalli";
	
	/* methods */
	/** service method declaration */
	protected static final String METHOD_ADD_GAUGE = "AddGauge";
	/** service method declaration */
	protected static final String METHOD_ADD_METER = "AddMeter";
	/** service method declaration */
	protected static final String METHOD_CHECK_CREDENTIALS = "CredentialInterface";
	/** service method declaration */
	protected static final String METHOD_DATA_PARSER = "DataParser";
	/** service method declaration */
	protected static final String METHOD_DATA_STATISTICS = "DataStatistics";
	/** service method declaration */
	protected static final String METHOD_GET_LOCATIONS = "LocationInterface";
	/** service method declaration */
	protected static final String METHOD_GET_MEASUREMENTS = "MeasurementInterface";
	/** service method declaration */
	protected static final String METHOD_MODIFY_METER = "ModifyMeter";
	/** service method declaration */
	protected static final String METHOD_POST_MEASUREMENTS = METHOD_GET_MEASUREMENTS;
	/** service method declaration */
	protected static final String METHOD_GET_ALERTS = "AlertInterface";
	
	/* parameters */
	/** service method parameter declaration */
	protected static final String PARAMETER_VALUE_CONTENT = "data";
	/** service method parameter declaration */
	protected static final String PARAMETER_END_DATE = "end_date";
	/** service method parameter declaration */
	protected static final String PARAMETER_LOCATION_ID = "location_id";
	/** service method parameter declaration */
	protected static final String PARAMETER_START_DATE = "start_date";
	/** service method parameter declaration */
	protected static final String PARAMETER_TAG_ID = "tag_id";
	/** service method parameter declaration */
	protected static final String PARAMETER_USER_ID = "user_id";
	/** service method parameter declaration */
	protected static final String PARAMETER_ALERT_STATUS = "alert_status";
	/** service method parameter declaration */
	protected static final String PARAMETER_SORT = "sort";
		
	/* DATA GROUPS */
	/** data group that contains gauge details */
	public static final String DATA_GROUP_GAUGES = "gauges";
	/** data group that contains gauge values */
	public static final String DATA_GROUP_GAUGE_VALUES = "gauge_values";
}
