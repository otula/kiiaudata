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
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import com.google.gson.annotations.SerializedName;
import com.otula.datatypes.Meter.ValueValidity;
import com.otula.utils.CommonUtils;
import com.otula.utils.DateUtils;
import com.otula.utils.LogUtils;

/**
 * Class to store information of gauge. This is a child of a {@link Meter}.
 *
 */
public class Gauge {
	private static final String CLASS_NAME = Gauge.class.toString();
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
	transient private GaugeValue _median = null;
	transient private GaugeValue _average = null;
	transient private GaugeValue _lastValue = null;
	@SerializedName(value=Definitions.JSON_NAME_MIN_INCREASE)
	private Double _minIncrease = null;
	@SerializedName(value=Definitions.JSON_NAME_MAX_INCREASE)
	private Double _maxIncrease = null;
	@SerializedName(value=Definitions.JSON_NAME_CUMULATIVE)
	private Boolean _cumulative = null;
	
	/**
	 * 
	 *
	 */
	public enum DataType{
		STRING,
		INTEGER,
		DOUBLE;
		public static final DataType DEFAULT_DATATYPE = DataType.DOUBLE;
		
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
		
		public String toDataTypeString(){
			return name();
		}
	} //enum DataType
	
	/**
	 * 
	 *
	 */
	public enum Option{
		REQUIRED,
		OPTIONAL;
		
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
		
		public static EnumSet<Option> fromOptionString(String s){
			if(s == null || s.isEmpty()){
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

	public String getName() {
		return _name;
	}

	public void setName(String name) {
		_name = name;
	}

	public String getDescription() {
		return _description;
	}

	public void setDescription(String description) {
		_description = description;
	}

	public String getUnit() {
		return _unit;
	}

	public void setUnit(String unit) {
		_unit = unit;
	}

	public Double getMin() {
		return _min;
	}

	public void setMin(Double min) {
		_min = min;
	}

	public Double getMax() {
		return _max;
	}

	public void setMax(Double max) {
		_max = max;
	}
	
	/**
	 * Returns the maximum limit value if available, this is either the value returned by @see getMax(), or the value
	 * generated based on the previous value (if available) and @see getMaxIncrease(). If both are given, this will
	 * return the lower value.
	 * 
	 * @return 
	 */
	public Double getMaxLimitValue(){
		if(!isCumulative()){
			return getMax();
		}else if(_maxIncrease == null){	
			try{
				if(_lastValue != null && _average != null){	// if no max increase is available, but lastValue and average is available, calculate that
					double increase = CommonUtils.displayStringToDouble(_average.getValue()) * DateUtils.durationAsDays(_lastValue.getUpdatedTimestamp(), new Date());
					double max = CommonUtils.displayStringToDouble(_lastValue.getValue())+increase*2;
					if(_max == null || max < _max){	// if no max limit or it is higher
						return max;
					}else{
						return _max;
					}
				}else{	// otherwise, return whatever is the maximum value
					return _max;
				}
			}catch(NumberFormatException ex){
				LogUtils.error(CLASS_NAME, "getMaxLimitValue", ex.toString());
				return _max;
			}
		}else if(_lastValue == null){	// last value is not known, use the max
			LogUtils.debug(CLASS_NAME, "getMaxLimitValue", "Previous value is not known, returning max().");
			return _max;
		}else{
			try{
				double increase = _maxIncrease * DateUtils.durationAsDays(_lastValue.getUpdatedTimestamp(), new Date());
				double max = CommonUtils.displayStringToDouble(_lastValue.getValue())+increase;
				if(_max == null || max < _max){	// if no max limit or it is higher
					return max;
				}else{
					return _max;
				}
			}catch(NumberFormatException ex){
				LogUtils.error(CLASS_NAME, "getMaxLimitValue", ex.toString());
				return _max;
			}
		}
	}
	
	/**
	 * Returns the minimum limit value if available, this is either the value returned by @see getMin(), or the value
	 * generated based on the previous value (if available) and @see getMinIncrease(). If both are given, this will
	 * return the higher value.
	 * 
	 * @return 
	 */
	public Double getMinLimitValue(){
		if(_lastValue != null && _average != null && (_dataType == DataType.INTEGER || _dataType == DataType.DOUBLE) && isCumulative()){	//if the gauge is cumulative and last value is available, then the value cannot be lower than last value
			//also add 1/4th of the average per day to the latest given value
			double increase = CommonUtils.displayStringToDouble(_average.getValue())/4 * DateUtils.durationAsDays(_lastValue.getUpdatedTimestamp(), new Date());
			double min = CommonUtils.displayStringToDouble(_lastValue.getValue())+increase;
			return min;
		}else if(_minIncrease == null){	// if no increase, return whatever is the minimum value
			return _min;
		}else if(_lastValue == null){	// last value is not known, use the min
			LogUtils.debug(CLASS_NAME, "getMaxLimitValue", "Previous value is not known, returning min().");
			return _min;
		}else{
			try{
				double min = CommonUtils.displayStringToDouble(_lastValue.getValue())+_minIncrease;
				if(_min == null || min > _min){	// if no min limit or it is lower
					return min;
				}else{
					return _min;
				}
			}catch(NumberFormatException ex){
				LogUtils.error(CLASS_NAME, "getMinLimitValue", ex.toString());
				return _min;
			}
		}
	}

	/**
	 * 
	 * @return the options for this meter value, note that editing the returned set has no effect on the options, if you want to modify settings set new settings with setOptions()
	 */
	public EnumSet<Option> getOptions() {
		return Option.fromOptionString(_options);
	}

	public void setOptions(EnumSet<Option> options) {
		_options = Option.toOptionString(options);
	}
	
	/**
	 * 
	 * @param o
	 * @return
	 */
	public boolean hasOption(Option o){
		EnumSet<Option> options = getOptions();
		if(options == null){
			return false;
		}else{
			return options.contains(o);
		}
	}

	public Integer getIndex() {
		return _index;
	}

	public void setIndex(Integer index) {
		_index = index;
	}

	public Date getUpdatedTimestamp() {
		return _updatedTimestamp;
	}

	public void setUpdatedTimestamp(Date updatedTimestamp) {
		_updatedTimestamp = updatedTimestamp;
	}

	public List<GaugeValue> getValues() {
		return _values;
	}

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
	 * @return the median
	 */
	public GaugeValue getMedian() {
		return _median;
	}

	/**
	 * @param median the median to set
	 */
	public void setMedian(GaugeValue median) {
		_median = median;
	}

	/**
	 * @return the average
	 */
	public GaugeValue getAverage() {
		return _average;
	}

	/**
	 * @param average the average to set
	 */
	public void setAverage(GaugeValue average) {
		_average = average;
	}

	/**
	 * @return the lastValue
	 */
	public GaugeValue getLastValue() {
		return _lastValue;
	}

	/**
	 * @param lastValue the lastValue to set
	 */
	public void setLastValue(GaugeValue lastValue) {
		_lastValue = lastValue;
	}

	public void addGaugeValue(GaugeValue v){
		if(_values == null){
			_values = new ArrayList<GaugeValue>();
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
		if(_cumulative == null){
			return false;
		}else{
			return _cumulative;
		}
	}

	/**
	 * @param cumulative the cumulative to set
	 */
	public void setCumulative(Boolean cumulative) {
		_cumulative = cumulative;
	}

	/**
	 * 
	 * @return
	 */
	public ValueValidity hasValidValues(){
		if(hasOption(Option.REQUIRED)){	// if required
			if(_values == null || _values.isEmpty()){	// and does not have any values
				return ValueValidity.INVALID;
			}	
		}else if(_values == null || _values.isEmpty()){	// not required, and has no values
			return ValueValidity.NO_VALUES;
		}
		try{
			Double min = getMinLimitValue();
			Double max = getMaxLimitValue();
			for(Iterator<GaugeValue> iter = _values.iterator(); iter.hasNext();){	// check the values for validity
				GaugeValue gv = iter.next();
				if(gv.getUpdatedTimestamp() == null){
					return ValueValidity.INVALID;
				}
				String value = gv.getValue();
				if(value == null || value.isEmpty()){
					return ValueValidity.INVALID;
				}
				switch(_dataType){
					case DOUBLE:
						Double dValue = null;
						if(min != null){
							dValue = Double.valueOf(value);
							if(dValue < min){
								return ValueValidity.LOWER_THAN_THRESHOLD;
							}
						}
						if(max != null){
							if(dValue == null){
								dValue = Double.valueOf(value);
							}
							if(dValue > max){
								return ValueValidity.GREATER_THAN_THRESHOLD;
							}
						}
						break;
					case INTEGER:
						Integer iValue = null;
						if(min != null){
							iValue = Integer.valueOf(value);
							if(iValue < min){
								return ValueValidity.LOWER_THAN_THRESHOLD;
							}
						}
						if(max != null){
							if(iValue == null){
								iValue = Integer.valueOf(value);
							}
							if(iValue > max){
								return ValueValidity.GREATER_THAN_THRESHOLD;
							}
						}
						break;
					case STRING:
						break;
					default:
						LogUtils.warn(CLASS_NAME, "hasValidValues", "Unhandled data type: "+_dataType.toDataTypeString());
						return ValueValidity.INVALID;
				}
			}
		}catch(NumberFormatException ex){
			LogUtils.error(CLASS_NAME, "hasValidValues", ex.toString());
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
	 * @return
	 */
	public static boolean isValuesEmpty(Gauge gauge){
		if(gauge == null || gauge._values == null || gauge._values.isEmpty()){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * 
	 * @param isSent
	 */
	public void setSent(boolean isSent) {
		if(!Gauge.isValuesEmpty(this)){
			for (GaugeValue value : _values) {
				value.setSent(isSent);
			}
		}
	}
}
