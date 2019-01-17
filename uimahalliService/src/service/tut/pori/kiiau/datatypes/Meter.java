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
import java.util.Iterator;
import java.util.List;

import service.tut.pori.kiiau.datatypes.Meters.ValueValidity;

import com.google.gson.annotations.SerializedName;

/**
 * A single meter.
 * 
 */
@SuppressWarnings("deprecation")
public class Meter {
	transient private Long _meterId = null;
	transient private Long _userId = null;
	@SerializedName(value=Definitions.JSON_NAME_TAG_ID)
	private String _id = null; // the external "tag id"
	@SerializedName(value=Definitions.JSON_NAME_NAME)
	private String _name = null;
	@SerializedName(value=Definitions.JSON_NAME_GAUGES)
	private List<Gauge> _gauges = null;
	@SerializedName(value=Definitions.JSON_NAME_LOCATION_ID)
	private Long _locationId = null;
	@SerializedName(value=Definitions.JSON_NAME_LOCATION_X)
	private Double _locationX = null;
	@SerializedName(value=Definitions.JSON_NAME_LOCATION_Y)
	private Double _locationY = null;

	/**
	 * required for serialization
	 */
	public Meter(){
		// nothing needed
	}
	
	/**
	 * 
	 * @param id the tag id
	 */
	public Meter(String id){
		_id = id;
	}
	
	/**
	 * 
	 * @return the row id
	 */
	public Long getMeterId() {
		return _meterId;
	}

	/**
	 * 
	 * @param meterId the row id
	 */
	public void setMeterId(Long meterId) {
		_meterId = meterId;
	}

	/**
	 * 
	 * @return the tag id
	 */
	public String getId() {
		return _id;
	}

	/**
	 * 
	 * @param id the tag id
	 */
	public void setId(String id) {
		_id = id;
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
	 * 
	 * @return name
	 */
	public String getName() {
		return _name;
	}

	/**
	 * 
	 * @param name
	 */
	public void setName(String name) {
		_name = name;
	}

	/**
	 * 
	 * @return list of gauges for this meter
	 */
	public List<Gauge> getGauges() {
		return _gauges;
	}

	/**
	 * 
	 * @param gauges
	 */
	public void setGauges(List<Gauge> gauges) {
		_gauges = gauges;
	}

	/**
	 * @return the userId
	 */
	public Long getUserId() {
		return _userId;
	}

	/**
	 * @param userId the userId to set
	 */
	public void setUserId(Long userId) {
		_userId = userId;
	}
	
	/**
	 * 
	 * @param gauge
	 */
	public void addGauge(Gauge gauge){
		if(_gauges == null){
			_gauges = new ArrayList<>();
		}
		_gauges.add(gauge);
	}
	
	/**
	 * @return the locationX
	 */
	public Double getLocationX() {
		return _locationX;
	}

	/**
	 * @param locationX the locationX to set
	 */
	public void setLocationX(Double locationX) {
		_locationX = locationX;
	}

	/**
	 * @return the locationY
	 */
	public Double getLocationY() {
		return _locationY;
	}

	/**
	 * @param locationY the locationY to set
	 */
	public void setLocationY(Double locationY) {
		_locationY = locationY;
	}

	/**
	 * 
	 * @return validity status of this meter and its gauges
	 */
	public ValueValidity hasValidValues(){
		if(_gauges == null || _gauges.isEmpty()){
			return ValueValidity.NO_VALUES;
		}else{
			ValueValidity validity = ValueValidity.NO_VALUES;
			for(Iterator<Gauge> iter = _gauges.iterator();iter.hasNext();){
				switch(iter.next().hasValidValues()){
					case VALID:
						validity = ValueValidity.VALID;
						break;
					case INVALID:
						return ValueValidity.INVALID;
					default:
						break;
				}
			}
			return validity;
		}
	}
	
	/**
	 * 
	 * @param one
	 * @param two
	 * @return true if the two meters are the same, the comparison is made by id
	 */
	public static boolean areTheSame(Meter one, Meter two){
		if(one == null || two == null){
			return false;
		}
		if(one == two){
			return true;
		}
		String idOne = one.getId();
		if(idOne == null){
			return false;
		}else{
			return idOne.equals(two.getId());
		}
	}
	
	/**
	 * 
	 * @param meter
	 * @return true if the given meter has no content or if null was passed
	 */
	public static boolean isEmpty(Meter meter){
		return (meter == null || meter._gauges == null || meter._gauges.isEmpty());
	}
}
