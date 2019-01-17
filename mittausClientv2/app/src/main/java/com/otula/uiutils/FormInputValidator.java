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
package com.otula.uiutils;

import java.util.Date;
import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.otula.datatypes.Gauge;
import com.otula.datatypes.GaugeValue;
import com.otula.datatypes.Meter.ValueValidity;
import com.otula.mittausclient.R;
import com.otula.utils.CommonUtils;
import com.otula.utils.DateUtils;
import com.otula.utils.LogUtils;

/**
 *
 * class for automatically managing the values, this will automatically update the given Gauge
 * with a proper value when user enters such, and manage the highlight of textEdit on invalid input
 *
 */
public class FormInputValidator implements TextWatcher {
	private final static String CLASS_NAME = FormInputValidator.class.toString();
	private Gauge _gauge = null;
	private EditText _field = null;

	/**
	 * 
	 * @param gauge
	 * @param field
	 */
	public FormInputValidator(Gauge gauge, EditText field, Context context){
		_field = field;
		_gauge = gauge;
		setInvalid((_gauge.hasValidValues() == ValueValidity.INVALID ? true : false));
	}

	/**
	 * 
	 * @param invalid if true, the value is automatically cleared from the gauge
	 */
	private void setInvalid(boolean invalid){
		if(invalid){
			Drawable icon = _field.getContext().getResources().getDrawable(android.R.drawable.ic_notification_overlay);
			_field.setCompoundDrawablesWithIntrinsicBounds(null, null, icon, null);
		}else{
			_field.setCompoundDrawables(null, null, null, null);
		}
	}

	@Override
	public void afterTextChanged(Editable s) {
		String value = s.toString().trim();
		List<GaugeValue> values = _gauge.getValues();
		TextView differenceLabel = (TextView)((ViewGroup)_field.getParent()).findViewById(R.id.input_difference_previous_value);
		String difference = null;
		String toDailyAverage = "";
		if(value.isEmpty()){
			_gauge.setValues(null);	// remove all values
		}else{
			Date updatedDate = new Date();
			if(values == null || values.isEmpty()){
				_gauge.addGaugeValue(new GaugeValue(value, updatedDate));	
			}else{
				GaugeValue gv = values.get(0);
				gv.setValue(value);
				gv.setUpdatedTimestamp(updatedDate);
			}
			if(_gauge.getLastValue() != null){
				Double temp = null;
				try{
					temp = Double.parseDouble(value) - CommonUtils.displayStringToDouble(_gauge.getLastValue().getValue());
					switch(_gauge.getDataType()){
						case DOUBLE:
							difference = CommonUtils.doubleValueToDisplayString(temp);
							break;
						case INTEGER:
							difference = String.valueOf(temp.intValue());
							break;
						default: break;
					}
					if(_gauge.isCumulative()){	//add information of calculated differential per day
						Double differential = temp / DateUtils.durationAsDays(_gauge.getLastValue().getUpdatedTimestamp(), updatedDate);
						StringBuffer buf = new StringBuffer();
						buf.append(" (");
						buf.append(CommonUtils.doubleValueToDisplayString(differential));
						buf.append("/");
						buf.append(differenceLabel.getContext().getString(R.string.per_day));
						buf.append(")");
						toDailyAverage = buf.toString();
					}
				}catch(NumberFormatException ex){
					LogUtils.warn(CLASS_NAME, "afterTextChanged", "Number was not valid");
					difference = null;
				}
			}
		}	
		//quite a messy if-else if-else structure to detect whether the user given value was completely invalid, or lower/greater than the defined threshold or was it even Ok
		if(_gauge.hasValidValues() == ValueValidity.INVALID){
			setInvalid(true);
			differenceLabel.setText(R.string.form_label_invalid_value);
		}else if(_gauge.hasValidValues() == ValueValidity.GREATER_THAN_THRESHOLD){
			setInvalid(true);
			differenceLabel.setText(difference + toDailyAverage + "\n" + differenceLabel.getContext().getString(R.string.form_label_greater_than_threshold_value));
		}else if(_gauge.hasValidValues() == ValueValidity.LOWER_THAN_THRESHOLD){
			setInvalid(true);
			differenceLabel.setText(difference + toDailyAverage + "\n" + differenceLabel.getContext().getString(R.string.form_label_lower_than_threshold_value));
		}else{
			setInvalid(false);
			differenceLabel.setText(difference + toDailyAverage);
		}
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		// nothing needed
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// nothing needed
	}
}
