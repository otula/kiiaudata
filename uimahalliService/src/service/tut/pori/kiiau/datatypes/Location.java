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

import com.google.gson.annotations.SerializedName;

/**
 * Details of meter location.
 * 
 */
public class Location {
	@SerializedName(value=Definitions.JSON_NAME_FLOOR_PLAN_URL)
	private String _floorPlanUrl = null;
	@SerializedName(value=Definitions.JSON_NAME_LOCATION_ID)
	private Long _locationId = null;
	@SerializedName(value=Definitions.JSON_NAME_NAME)
	private String _name = null;
	
	/**
	 * @return the floorPlanUrl
	 */
	public String getFloorPlanUrl() {
		return _floorPlanUrl;
	}
	
	/**
	 * @param floorPlanUrl the floorPlanUrl to set
	 */
	public void setFloorPlanUrl(String floorPlanUrl) {
		_floorPlanUrl = floorPlanUrl;
	}
	
	/**
	 * @return the locationId
	 */
	public Long getLocationId() {
		return _locationId;
	}
	
	/**
	 * @param locationId the locationId to set
	 */
	public void setLocationId(Long locationId) {
		_locationId = locationId;
	}
	
	/**
	 * @return the name
	 */
	public String getName() {
		return _name;
	}
	
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		_name = name;
	}
}
