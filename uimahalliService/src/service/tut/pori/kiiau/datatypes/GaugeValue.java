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

import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.annotations.SerializedName;

/**
 * A single value collected from a gauge.
 *
 */
public class GaugeValue {
	transient private Long _rowId = null;
	@SerializedName(value=Definitions.JSON_NAME_VALUE)
	private String _value = null;
	@SerializedName(value=Definitions.JSON_NAME_DATE)
	private Date _updatedTimestamp = null;
	//TODO add "BAD DATA" flag. i.e. do not use these in the calculations or something like that.
	//TODO rowId should be serialized as well, maybe behind a special data group.

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
	 * @param id
	 */
	public GaugeValue(Long id){
		_rowId = id;
	}

	/**
	 * 
	 * @return row id
	 */
	public Long getRowId() {
		return _rowId;
	}

	/**
	 * 
	 * @param rowId
	 */
	public void setRowId(Long rowId) {
		_rowId = rowId;
	}

	/**
	 * 
	 * @return value
	 */
	public String getValue() {
		return _value;
	}

	/**
	 * 
	 * @param value
	 */
	public void setValue(String value) {
		if(StringUtils.isBlank(value)){
			_value = null;
		}else{
			_value = value;
		}		
	}

	/**
	 * 
	 * @return updated timestamp
	 */
	public Date getUpdatedTimestamp() {
		return _updatedTimestamp;
	}

	/**
	 * 
	 * @param updatedTimestamp
	 */
	public void setUpdatedTimestamp(Date updatedTimestamp) {
		_updatedTimestamp = updatedTimestamp;
	}
}
