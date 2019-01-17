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

import java.util.Date;

import com.google.gson.annotations.SerializedName;
import com.otula.utils.CommonUtils;

/**
 * Class to store a meter value. This is a child of {@link Gauge}.
 *
 */
public class GaugeValue {
	transient private Long _rowId = null;
	@SerializedName(value=Definitions.JSON_NAME_VALUE)
	private String _value = null;
	transient private boolean _sent = false;
	@SerializedName(value=Definitions.JSON_NAME_DATE)
	private Date _updatedTimestamp = null;

	/**
	 * required for serialization
	 */
	public GaugeValue(){
		// nothing needed
	}
	
	/**
	 * 
	 * @param value
	 * @param updated
	 */
	public GaugeValue(String value, Date updated){
		_value = value;
		_updatedTimestamp = updated;
	}
	
	/**
	 * 
	 * @param value
	 * @param updated
	 */
	public GaugeValue(double value, Date updated){
		this(CommonUtils.doubleValueToDisplayString(value), updated);
	}
	
	/**
	 * 
	 * @param id
	 */
	public GaugeValue(Long id){
		_rowId = id;
	}

	public Long getRowId() {
		return _rowId;
	}

	public void setRowId(Long rowId) {
		_rowId = rowId;
	}

	public String getValue() {
		return _value;
	}

	public void setValue(String value) {
		if(value == null || value.isEmpty()){
			_value = null;
		}else{
			_value = value;
		}		
	}

	public boolean isSent() {
		return _sent;
	}

	public void setSent(boolean sent) {
		_sent = sent;
	}

	public Date getUpdatedTimestamp() {
		return _updatedTimestamp;
	}

	public void setUpdatedTimestamp(Date updatedTimestamp) {
		_updatedTimestamp = updatedTimestamp;
	}
}
