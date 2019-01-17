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

import java.util.List;

import com.google.gson.annotations.SerializedName;

import core.tut.pori.http.JSONResponseData;

/**
 * A list of meters.
 *
 */
public class Meters extends JSONResponseData {
	@SerializedName(value=Definitions.JSON_NAME_METERS)
	private List<Meter> _meters = null;
	
	/**
	 * Status of a value validation.
	 *
	 */
	public enum ValueValidity{
		/** The value is valid */
		VALID,
		/** the value is invalid */
		INVALID,
		/** the value is null or empty */
		NO_VALUES
	}
	
	/**
	 * required for serialization
	 */
	public Meters(){
		// nothing needed
	}

	/**
	 * 
	 * @return list of meters
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
	 * @return true if the meters have no content or if null was passed
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
	 * 
	 * @param meters
	 * @return validity status for the given object and its content
	 */
	public static ValueValidity hasValidValues(Meters meters){
		if(isEmpty(meters)){
			return ValueValidity.NO_VALUES;
		}
		for(Meter meter : meters.getMeters()){
			if(meter.hasValidValues() != ValueValidity.VALID){
				return ValueValidity.INVALID;
			}
		}
		return ValueValidity.VALID;
	}
}
