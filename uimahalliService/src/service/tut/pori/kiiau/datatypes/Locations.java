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

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

import core.tut.pori.http.JSONResponseData;

/**
 * A list of locations.
 * 
 */
public class Locations extends JSONResponseData {
	@SerializedName(value=Definitions.JSON_NAME_LOCATIONS)
	private List<Location> _locations = null;

	/**
	 * @return the locations
	 */
	public List<Location> getLocations() {
		return _locations;
	}

	/**
	 * @param locations the locations to set
	 */
	public void setLocations(List<Location> locations) {
		_locations = locations;
	}
	
	/**
	 * 
	 * @param location
	 */
	public void addLocation(Location location){
		if(_locations == null){
			_locations = new ArrayList<>();
		}
		_locations.add(location);
	}
}
