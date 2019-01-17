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

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Container object for array of {@link Meter} objects
 *
 */
public class Meters {
	@SerializedName(value=Definitions.JSON_NAME_METERS)
	private List<Meter> _meters = null;
	
	/**
	 * required for serialization
	 */
	public Meters(){
		// nothing needed
	}

	/**
	 * 
	 * @return
	 */
	public List<Meter> getMeters() {
		return _meters;
	}

	/**
	 * 
	 * @param meters
	 */
	public void setMeters(List<Meter> meters) {
		_meters = meters;
	}
	
	/**
	 * 
	 * @param meters
	 * @return
	 */
	public static boolean isEmpty(Meters meters){
		if(meters == null || meters._meters == null || meters._meters.isEmpty()){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * 
	 * @param meterList
	 * @return meters object, or null if the given list is null or empty
	 */
	public static Meters getMeters(List<Meter> meterList){
		if(meterList == null || meterList.isEmpty()){
			return null;
		}else{
			Meters meters = new Meters();
			meters.setMeters(meterList);
			return meters;
		}
	}

	/**
	 * Helper method to set all values of a meter/gauge to either true (has been sent to the server) or false (has not been sent to server).
	 * @param isSent
	 */
	public void setSent(boolean isSent) {
		if(!Meters.isEmpty(this)){
			for (Meter meter : _meters) {
				meter.setSent(isSent);
			}
		}
	}
}
