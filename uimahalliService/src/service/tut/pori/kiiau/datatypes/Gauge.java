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
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import service.tut.pori.kiiau.datatypes.Meters.ValueValidity;

import com.google.gson.annotations.SerializedName;

/**
 * A gauge.
 */
@SuppressWarnings("deprecation")
public class Gauge {
	private static final Logger LOGGER = Logger.getLogger(Gauge.class);
	@SerializedName(value=Definitions.JSON_NAME_INDEX)
	private Integer _index = null;
	@SerializedName(value=Definitions.JSON_NAME_NAME)
	private String _name = null;
	@SerializedName(value=Definitions.JSON_NAME_DESCRIPTION)
	private String _description = null;
	@SerializedName(value=Definitions.JSON_NAME_UNIT)
	private String _unit = null;
	@SerializedName(value=Definitions.JSON_NAME_MIN)
	private Double _min = null;
	@SerializedName(value=Definitions.JSON_NAME_MAX)
	private Double _max = null;
	@SerializedName(value=Definitions.JSON_NAME_OPTIONS)
	private String _options = null;	// for serialization
	@SerializedName(value=Definitions.JSON_NAME_GAUGE_VALUES)
	private List<GaugeValue> _values = null;
	@SerializedName(value=Definitions.JSON_NAME_GAUGE_ID)
	private String _id = null;
	transient private Date _updatedTimestamp = null;
	@SerializedName(value=Definitions.JSON_NAME_DATATYPE)
	private DataType _dataType = null;
	@SerializedName(value=Definitions.JSON_NAME_MIN_INCREASE)
	private Double _minIncrease = null;
	@SerializedName(value=Definitions.JSON_NAME_MAX_INCREASE)
	private Double _maxIncrease = null;
	@SerializedName(value=Definitions.JSON_NAME_CUMULATIVE)
	private Boolean _cumulative = true;
	
	/**
	 * Gauge value data type.
	 *
	 */
	public enum DataType{
		/** data is text content */
		STRING,
		/** data is a signed integer value */
		INTEGER,
		/** data is a double/decimal value */
		DOUBLE;
		/** Default data type */
		public static final DataType DEFAULT_DATATYPE = DataType.DOUBLE;
		
		/**
		 * 
		 * @param s
		 * @return data type converted from the given string or null on bad value
		 */
		public static DataType fromDataTypeString(String s){
			if(s != null){
				for(DataType t : DataType.values()){
					if(t.toDataTypeString().equalsIgnoreCase(s)){
						return t;
					}
				}
			}
			return null;
		}
		
		/**
		 * 
		 * @return this type as string
		 */
		public String toDataTypeString(){
			return name();
		}
	} //enum DataType
	
	/**
	 * Gauge option.
	 *
	 */
	public enum Option{
		/** The values for this gauge must be given */
		REQUIRED,
		/** The values for this gauge are optional */
		OPTIONAL;
		
		/**
		 * 
		 * @return this option as a string
		 */
		public String toOptionString(){
			switch(this){
				case OPTIONAL:
					return "O";
				case REQUIRED:
					return "M";
				default:
					throw new IllegalArgumentException("Unknown type: "+this.name());
			}
		}
		
		/**
		 * 
		 * @param s
		 * @return the string converted to an option or null on invalid value
		 */
		public static EnumSet<Option> fromOptionString(String s){
			if(StringUtils.isBlank(s)){
				return null;
			}
			EnumSet<Option> options = EnumSet.noneOf(Option.class);
			for(Option o : Option.values()){
				if(s.contains(o.toOptionString())){
					options.add(o);
				}
			}
			if(options.isEmpty()){
				return null;
			}else{
				return options;
			}
		}
		
		/**
		 * 
		 * @param options
		 * @return the given options as a string or null if null or empty set was given
		 */
		public static String toOptionString(EnumSet<Option> options){
			if(options == null || options.isEmpty()){
				return null;
			}
			StringBuilder s = new StringBuilder();
			for(Iterator<Option> iter = options.iterator();iter.hasNext();){
				s.append(iter.next().toOptionString());
			}
			return s.toString();
		}
	}	// enum Option
	
	/**
	 * required for serialization
	 */
	public Gauge(){
		// nothing needed
	}
	
	/**
	 * 
	 * @param id
	 */
	public Gauge(String id){
		_id = id;
	}

	/**
	 * 
	 * @return id
	 */
	public String getId() {
		return _id;
	}

	/**
	 * 
	 * @param id
	 */
	public void setId(String id) {
		_id = id;
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
	 * @return description
	 */
	public String getDescription() {
		return _description;
	}

	/**
	 * 
	 * @param description
	 */
	public void setDescription(String description) {
		_description = description;
	}

	/**
	 * 
	 * @return unit
	 */
	public String getUnit() {
		return _unit;
	}

	/**
	 * 
	 * @param unit
	 */
	public void setUnit(String unit) {
		_unit = unit;
	}

	/**
	 * 
	 * @return min 
	 */
	public Double getMin() {
		return _min;
	}
	
	/**
	 * 
	 * @param min
	 */
	public void setMin(Double min) {
		_min = min;
	}

	/**
	 * 
	 * @return max
	 */
	public Double getMax() {
		return _max;
	}

	/**
	 * 
	 * @param max
	 */
	public void setMax(Double max) {
		_max = max;
	}

	/**
	 * 
	 * @return the options for this meter value, note that editing the returned set has no effect on the options, if you want to modify settings set new settings with setOptions()
	 */
	public EnumSet<Option> getOptions() {
		return Option.fromOptionString(_options);
	}

	/**
	 * 
	 * @param options
	 */
	public void setOptions(EnumSet<Option> options) {
		_options = Option.toOptionString(options);
	}
	
	/**
	 * 
	 * @param options
	 */
	public void setOptionsString(String options){
		_options = options;
	}
	
	/**
	 * 
	 * @return option string
	 */
	public String getOptionsString(){
		return _options;
	}
	
	/**
	 * 
	 * @param o
	 * @return true if the gauge has the given option
	 */
	public boolean hasOption(Option o){
		EnumSet<Option> options = getOptions();
		if(options == null){
			return false;
		}else{
			return options.contains(o);
		}
	}

	/**
	 * 
	 * @return index
	 */
	public Integer getIndex() {
		return _index;
	}

	/**
	 * 
	 * @param index
	 */
	public void setIndex(Integer index) {
		_index = index;
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

	/**
	 * 
	 * @return list of values for this gauge
	 */
	public List<GaugeValue> getValues() {
		return _values;
	}

	/**
	 * 
	 * @param values
	 */
	public void setValues(List<GaugeValue> values) {
		_values = values;
	}
	
	/**
	 * @return the dataType
	 */
	public DataType getDataType() {
		return _dataType;
	}

	/**
	 * @param dataType the dataType to set
	 */
	public void setDataType(DataType dataType) {
		if(dataType == null){
			_dataType = DataType.DEFAULT_DATATYPE;
		}else{
			_dataType = dataType;
		}
	}

	/**
	 * 
	 * @param v
	 */
	public void addGaugeValue(GaugeValue v){
		if(_values == null){
			_values = new ArrayList<>();
		}
		_values.add(v);
	}
	
	/**
	 * @return the minIncrease
	 */
	public Double getMinIncrease() {
		return _minIncrease;
	}

	/**
	 * @param minIncrease the minIncrease to set
	 */
	public void setMinIncrease(Double minIncrease) {
		_minIncrease = minIncrease;
	}

	/**
	 * Get the maximum daily increase
	 * @return the maxIncrease
	 */
	public Double getMaxIncrease() {
		return _maxIncrease;
	}

	/**
	 * @param maxIncrease the maxIncrease to set
	 */
	public void setMaxIncrease(Double maxIncrease) {
		_maxIncrease = maxIncrease;
	}

	/**
	 * @return the cumulative
	 */
	public Boolean isCumulative() {
		return _cumulative;
	}

	/**
	 * @param cumulative the cumulative to set
	 */
	public void setCumulative(Boolean cumulative) {
		_cumulative = cumulative;
	}

	/**
	 * 
	 * @return validity status for this gauge and its values
	 */
	public ValueValidity hasValidValues(){
		if(hasOption(Option.REQUIRED)){	// if required
			if(_values == null || _values.isEmpty()){	// and does not have any values
				return ValueValidity.INVALID;
			}	
		}else if(_values == null || _values.isEmpty()){	// not required, and has no values
			return ValueValidity.NO_VALUES;
		}else if(StringUtils.isBlank(_id)){
			LOGGER.warn("Id was missing...");
			return ValueValidity.INVALID;
		}
		try{
			for(Iterator<GaugeValue> iter = _values.iterator(); iter.hasNext();){	// check the values for validity
				GaugeValue gv = iter.next();
				if(gv.getUpdatedTimestamp() == null){
					return ValueValidity.INVALID;
				}
				String value = gv.getValue();
				if(StringUtils.isBlank(value)){
					return ValueValidity.INVALID;
				}
				switch(_dataType){
					case DOUBLE:
						Double.valueOf(value);
						break;
					case INTEGER:
						Integer.valueOf(value);
						break;
					case STRING:
						break;
					default:
						LOGGER.warn("Unhandled data type: "+_dataType.toDataTypeString());
						return ValueValidity.INVALID;
				}
			}
		}catch(NumberFormatException ex){
			LOGGER.error(ex, ex);
			return ValueValidity.INVALID;
		}
		return ValueValidity.VALID;
	}
	
	/**
	 * 
	 * @param one
	 * @param two
	 * @return true if the two gauges are the same, the comparison is made by id
	 */
	public static boolean areTheSame(Gauge one, Gauge two){
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
	 * @param gauge
	 * @return true if the given gauge has no value or null was passed
	 */
	public static boolean isValuesEmpty(Gauge gauge){
		if(gauge == null || gauge._values == null || gauge._values.isEmpty()){
			return true;
		}else{
			return false;
		}
	}
}
