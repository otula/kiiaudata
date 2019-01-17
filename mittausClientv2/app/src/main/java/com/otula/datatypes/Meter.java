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

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Class to store information of a Meter.
 *       
 */
public class Meter {
	transient private Date _updatedTimestamp = null;
	@SerializedName(value=Definitions.JSON_NAME_METER_ID)
	private String _id = null;
	@SerializedName(value=Definitions.JSON_NAME_NAME)
	private String _name = null;
	@SerializedName(value=Definitions.JSON_NAME_GAUGES)
	private List<Gauge> _gauges = null;
	
	/**
	 * 
	 *
	 */
	public enum ValueValidity{
		VALID,
		INVALID,
		NO_VALUES,
		GREATER_THAN_THRESHOLD,
		LOWER_THAN_THRESHOLD
	}

	/**
	 * required for serialization
	 */
	public Meter(){
		// nothing needed
	}
	
	/**
	 * 
	 * @param id
	 */
	public Meter(String id){
		_id = id;
	}

	public String getId() {
		return _id;
	}

	public void setId(String id) {
		_id = id;
	}

	public String getName() {
		return _name;
	}

	public void setName(String name) {
		_name = name;
	}

	public List<Gauge> getGauges() {
		return _gauges;
	}

	public void setGauges(List<Gauge> gauges) {
		_gauges = gauges;
	}

	public Date getUpdatedTimestamp() {
		return _updatedTimestamp;
	}

	public void setUpdatedTimestamp(Date updatedTimestamp) {
		_updatedTimestamp = updatedTimestamp;
	}
	
	public void addGauge(Gauge gauge){
		if(_gauges == null){
			_gauges = new ArrayList<Gauge>();
		}
		_gauges.add(gauge);
	}
	
	/**
	 * 
	 * @return
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
					case GREATER_THAN_THRESHOLD:
						return ValueValidity.GREATER_THAN_THRESHOLD;
					case LOWER_THAN_THRESHOLD:
						return ValueValidity.LOWER_THAN_THRESHOLD;
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
	 * @return
	 */
	public static boolean isEmpty(Meter meter){
		return (meter == null || meter._gauges == null || meter._gauges.isEmpty());
	}
	
	/**
	 * 
	 * @param isSent
	 */
	public void setSent(boolean isSent) {
		if(!Meter.isEmpty(this)){
			for (Gauge gauge : _gauges) {
				gauge.setSent(isSent);
			}
		}
	}
}
