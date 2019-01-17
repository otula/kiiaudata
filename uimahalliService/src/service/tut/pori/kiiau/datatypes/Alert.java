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

import java.util.Collection;
import java.util.EnumSet;

import com.google.gson.annotations.SerializedName;

/**
 * Alert related to a single gauge value.
 */
@SuppressWarnings("deprecation")
public class Alert {
	@SerializedName(Definitions.JSON_NAME_GAUGE_VALUE)
	private GaugeValue _value = null;
	@SerializedName(Definitions.JSON_NAME_ALERT_STATUS)
	private AlertStatus _status = null;
	@SerializedName(Definitions.JSON_NAME_ALERT_TYPE)
	private AlertType _type = null;
	@SerializedName(value=Definitions.JSON_NAME_ALERT_TAG_ID)
	private String _tagId = null; // the external "tag id"
	@SerializedName(value=Definitions.JSON_NAME_ALERT_GAUGE_ID)
	private String _gaugeId = null;
	
	/**
	 * Confirmation status of the alert.
	 */
	public enum AlertStatus {
		/** the alert is a new one */
		NEW(0),
		/** the alert has been checked/confirmed by the user */
		CHECKED(1);
		
		private int _value;
		
		/**
		 * 
		 * @param value
		 */
		private AlertStatus(int value){
			_value = value;
		}
		
		/**
		 * 
		 * @param values
		 * @return the enumerations contained in the value list or null if empty or null collection was passed
		 * @throws IllegalArgumentException on invalid string value
		 */
		public static EnumSet<AlertStatus> fromString(Collection<String> values) throws IllegalArgumentException{
			if(values == null || values.isEmpty()){
				return null;
			}
			EnumSet<AlertStatus> retval = EnumSet.noneOf(AlertStatus.class);
			AlertStatus[] statuses = values();
			for(String value : values){
				AlertStatus match = null;
				for(AlertStatus s : statuses){
					if(s.name().equalsIgnoreCase(value)){
						match = s;
						break;
					}
				}
				if(match == null){
					throw new IllegalArgumentException("Invalid value: "+value);
				}
				retval.add(match);
			}
			return retval;
		}
		
		/**
		 * 
		 * @param statuses a valid non-null, non-empty status list
		 * @return the list of statuses as integer array
		 */
		public static int[] toInt(EnumSet<AlertStatus> statuses){
			int[] values = new int[statuses.size()];
			int index = -1;
			for(AlertStatus status : statuses){
				values[++index] = status.toInt();
			}
			return values;
		}
		
		/**
		 * 
		 * @return alert as integer
		 */
		public int toInt(){
			return _value;
		}
		
		/**
		 * 
		 * @param value
		 * @return value converted to status
		 * @throws IllegalArgumentException
		 */
		public static AlertStatus fromInt(int value) throws IllegalArgumentException{
			for(AlertStatus s : values()){
				if(s._value == value){
					return s;
				}
			}
			throw new IllegalArgumentException("Invalid value: "+value);
		}
	} // enum AlertStatus
	
	/**
	 * The type of the alert.
	 */
	public enum AlertType {
		/** too high temperature value has been detected */
		HIGH_TEMPERATURE(0),
		/** too low temperature value has been detected */
		LOW_TEMPERATURE(1),
		/** too high humidity value has been detected */
		HIGH_HUMIDITY(2),
		/** too low humidity value has been detected */
		LOW_HUMIDITY(3);
		
		private int _value;
		
		/**
		 * 
		 * @param value
		 */
		private AlertType(int value){
			_value = value;
		}
		
		/**
		 * 
		 * @return alert as integer
		 */
		public int toInt(){
			return _value;
		}
		
		/**
		 * 
		 * @param value
		 * @return value converted to alert type
		 * @throws IllegalArgumentException
		 */
		public static AlertType fromInt(int value) throws IllegalArgumentException{
			for(AlertType t : values()){
				if(t._value == value){
					return t;
				}
			}
			throw new IllegalArgumentException("Invalid value: "+value);
		}
	} // enum AlertType

	/**
	 * @return the value
	 */
	public GaugeValue getValue() {
		return _value;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(GaugeValue value) {
		_value = value;
	}

	/**
	 * @return the status
	 */
	public AlertStatus getStatus() {
		return _status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(AlertStatus status) {
		_status = status;
	}

	/**
	 * @return the type
	 */
	public AlertType getType() {
		return _type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(AlertType type) {
		_type = type;
	}

	/**
	 * @return the tagId
	 */
	public String getTagId() {
		return _tagId;
	}

	/**
	 * @param tagId the tagId to set
	 */
	public void setTagId(String tagId) {
		_tagId = tagId;
	}

	/**
	 * @return the gaugeId
	 */
	public String getGaugeId() {
		return _gaugeId;
	}

	/**
	 * @param gaugeId the gaugeId to set
	 */
	public void setGaugeId(String gaugeId) {
		_gaugeId = gaugeId;
	}
}
