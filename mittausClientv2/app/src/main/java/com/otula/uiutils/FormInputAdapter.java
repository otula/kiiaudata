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

import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.otula.datatypes.Gauge;
import com.otula.datatypes.GaugeValue;
import com.otula.datatypes.Meter;
import com.otula.mittausclient.R;
import com.otula.utils.CommonUtils;
import com.otula.utils.DateUtils;
import com.otula.utils.LogUtils;

/**
 * UI adapter to handle generation of input forms.
 * 
 */
public class FormInputAdapter implements OnFocusChangeListener {
	private static final String CLASS_NAME = FormInputAdapter.class.toString();
	private Activity _activity = null;
	private ScrollView _scroller = null;
	private Meter _meter = null;
	private List<Gauge> _gauges = null;
	
	/**
	 * 
	 * @param activity
	 */
	public FormInputAdapter(Activity activity, ScrollView scroller){
		_activity = activity;
		_scroller = scroller;
		addEmptyViewItem();
		Toast.makeText(_activity, _activity.getString(R.string.form_help), Toast.LENGTH_SHORT).show();
	}
	
	private void addEmptyViewItem(){
		TextView emptyView = new TextView(_activity);
		emptyView.setText(R.string.form_empty_list);
		emptyView.setTextAppearance(_activity, R.style.text_medium);
		emptyView.setGravity(Gravity.CENTER_HORIZONTAL);
		_scroller.addView(emptyView);
	}
	
	/**
	 * 
	 * @param meter
	 */
	public void meterChanged(Meter meter){
		_meter = meter;
		if(Meter.isEmpty(meter)){
			_gauges = null;
		}else{
			_gauges = meter.getGauges();
		}
		initialize();	// invalidate the currently active list
	}

	public void initialize() {
		_scroller.removeAllViews();
		if(_gauges == null){
			addEmptyViewItem();
			return;
		}
		
		LayoutInflater inflater = _activity.getLayoutInflater();
		LinearLayout wrapper = new LinearLayout(_activity);
		wrapper.setOrientation(LinearLayout.VERTICAL);
		wrapper.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		for(Iterator<Gauge> iter = _gauges.iterator(); iter.hasNext();){
			Gauge gauge = iter.next();
			View view = inflater.inflate(R.layout.input, null);
			final EditText inputText = (EditText) view.findViewById(R.id.input_edit_text);
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					inputText.requestFocus();
				}
			});
			inputText.setOnFocusChangeListener(this);
			inputText.addTextChangedListener(new FormInputValidator(gauge, inputText, _activity));
			String description = gauge.getDescription();
			if(description == null){
				description = "";
			}
			inputText.setContentDescription(description);
			switch(gauge.getDataType()){
				case DOUBLE:
					inputText.setHint(CommonUtils.createMinMaxString(gauge));
					inputText.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_SIGNED|InputType.TYPE_NUMBER_FLAG_DECIMAL);
					break;
				case INTEGER:
					inputText.setHint(CommonUtils.createMinMaxString(gauge));
					inputText.setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED|InputType.TYPE_CLASS_NUMBER);
					break;
				default:
					LogUtils.warn(CLASS_NAME, "initialize", "Unknown DataType, defaulting to STRING.");
				case STRING:
					inputText.setHint(description);
					inputText.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
					inputText.setTextAppearance(_activity, R.style.text_medium);
					break;
			}
			TextView nameText = (TextView) view.findViewById(R.id.input_name);
			nameText.setTag(gauge);
			String unit = gauge.getUnit();
			if(unit == null){
				nameText.setText(gauge.getName());
			}else{
				nameText.setText(gauge.getName() +" ("+ unit +")");
			}
			
			GaugeValue temp = gauge.getAverage();
			TextView tempView = (TextView)view.findViewById(R.id.input_average);
			if(temp != null){
				tempView.setText(temp.getValue());
				((ViewGroup)tempView.getParent()).setVisibility(View.VISIBLE);
			}else{
				((ViewGroup)tempView.getParent()).setVisibility(View.GONE);
			}
			temp = gauge.getMedian();
			tempView = (TextView)view.findViewById(R.id.input_median);
			if(temp != null){
				tempView.setText(temp.getValue());
				((ViewGroup)tempView.getParent()).setVisibility(View.VISIBLE);
			}else{
				((ViewGroup)tempView.getParent()).setVisibility(View.GONE);
			}
			temp = gauge.getLastValue();
			tempView = (TextView)view.findViewById(R.id.input_previous_value);
			if(temp != null){
				tempView.setText(temp.getValue()+" ("+DateUtils.dateToLocalizedString(temp.getUpdatedTimestamp())+")");
				((ViewGroup)tempView.getParent()).setVisibility(View.VISIBLE);
			}else{
				((ViewGroup)tempView.getParent()).setVisibility(View.GONE);
			}
			tempView = (TextView)view.findViewById(R.id.input_difference_previous_value);
			switch(gauge.getDataType()){
				case DOUBLE:
				case INTEGER:
					((ViewGroup)tempView.getParent()).setVisibility(View.VISIBLE);
					break;
				default:
					((ViewGroup)tempView.getParent()).setVisibility(View.GONE);
					break;
			}
			wrapper.addView(view);
		}		
		_scroller.addView(wrapper);
	}

	/**
	 * 
	 * @return
	 */
	public Meter getValues(){
		return _meter;
	}

	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		ViewGroup parent = (ViewGroup)v.getParent();
		if(hasFocus){
			parent.findViewById(R.id.layout_onfocus_details).setVisibility(View.VISIBLE);
			parent.setBackgroundResource(R.drawable.input_border);
		}else{
			parent.findViewById(R.id.layout_onfocus_details).setVisibility(View.GONE);
			parent.setBackgroundDrawable(null);
		}
	}
}
