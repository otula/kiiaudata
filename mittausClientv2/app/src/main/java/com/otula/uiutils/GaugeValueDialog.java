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

import com.otula.datatypes.Definitions;
import com.otula.datatypes.Gauge;
import com.otula.datatypes.Gauge.Option;
import com.otula.datatypes.GaugeValue;
import com.otula.mittausclient.R;
import com.otula.utils.CommonUtils;
import com.otula.utils.DateUtils;
import com.otula.utils.LogUtils;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

/**
 * displays gauge values in a dialog, the calling activity MUST implement the provided DialogListener interface
 *
 */
public class GaugeValueDialog extends DialogFragment {
	public static final String TAG = "GaugeValueDialogTAG";
	private static final String CLASS_NAME = GaugeValueDialog.class.toString();
	private DialogListener _listener = null;
	private TextView _valueUpdated = null;
	private TextView _value = null;
	private TextView _valueLimits = null;
	private TextView _valueRequired = null;
	private TextView _valueIndex = null;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		_listener = (DialogListener)activity;	// let it throw an exception on failure
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.dialog_value_details, container);
		_valueUpdated = (TextView) view.findViewById(R.id.valuedialog_value_updated);
		_value = (TextView) view.findViewById(R.id.valuedialog_value_value);
		_valueLimits = (TextView) view.findViewById(R.id.valuedialog_value_limits);
		_valueRequired = (TextView) view.findViewById(R.id.valuedialog_value_required);
		_valueIndex = (TextView) view.findViewById(R.id.valuedialog_value_index);
		view.setOnTouchListener(new DialogTouchListener());
		return view;
	}

	@Override
	public void onResume() {
		showValue(_listener.getCurrentGaugeValue());
		super.onResume();
	}
	
	/**
	 * 
	 * @param gauge
	 */
	private void showValue(GaugeValue value){
		Gauge gauge = _listener.getCurrentGauge();
		if(gauge == null){
			LogUtils.debug(CLASS_NAME, "showValue", "No current gauge.");
			getDialog().setTitle(R.string.not_available);
			_valueRequired.setText(R.string.not_available);
			_value.setText(R.string.not_available);
			_valueLimits.setText(R.string.not_available);
			_valueUpdated.setText(R.string.not_available);
			_valueIndex.setText(R.string.not_available);
		}else{
			LogUtils.debug(CLASS_NAME, "showValue", "Showing gauge with id: "+gauge.getId());
			getDialog().setTitle(gauge.getName());
			if(gauge.hasOption(Option.REQUIRED)){
				_valueRequired.setText(R.string.valuedialog_text_required);
			}else{
				_valueRequired.setText(R.string.valuedialog_text_not_required);
			}
			
			String unit = gauge.getUnit();
			if(value == null){
				LogUtils.debug(CLASS_NAME, "showValue", "No current value.");
				_valueUpdated.setText(R.string.not_available);
				_value.setText(R.string.not_available);
				_valueIndex.setText(R.string.not_available);
			}else{
				LogUtils.debug(CLASS_NAME, "showValue", "Showing gauge value with row id: "+value.getRowId());
				_valueUpdated.setText(DateUtils.dateToString(value.getUpdatedTimestamp()));
				_value.setText((unit == null ? value.getValue() : value.getValue() + " (" + unit +")"));	
				_valueIndex.setText(String.valueOf(value.getRowId()));
			}
			
			Double min = gauge.getMin();
			Double max = gauge.getMax();
			if(min == null && max == null){
				_valueLimits.setText(R.string.valuedialog_text_no_limits);
			}else{
				_valueLimits.setText(CommonUtils.createMinMaxString(gauge));
			}
		}
	}
	
	/**
	 * 
	 *
	 */
	private class DialogTouchListener implements View.OnTouchListener{
		private long _startTime = 0;
		private float _startX = 0;

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			switch(event.getAction()){
				case MotionEvent.ACTION_DOWN:
					_startTime = System.currentTimeMillis();
					_startX = event.getX();
					break;
				case MotionEvent.ACTION_UP:
					if(_startTime > 0 && System.currentTimeMillis()-_startTime < Definitions.THRESHOLD_SWIPE_MAX_DURATION){
						float endX = event.getX();
						if(Math.abs(endX-_startX) < Definitions.THRESHOLD_SWIPE_MIN_MOVEMENT){
							break;
						}else if(endX > _startX){	// swipe rightwards
							LogUtils.debug(CLASS_NAME, "onTouch", "Swipe right: checking for previous value.");
							GaugeValue previous = _listener.getPreviousGaugeValue();
							if(previous == null){
								Toast.makeText(getActivity(), R.string.valuedialog_notification_no_previous_value, Toast.LENGTH_SHORT).show();
							}else{
								showValue(previous);
							}
						}else{	// endX < _startX, swipe leftwards
							LogUtils.debug(CLASS_NAME, "onTouch", "Swipe right: checking for next value.");
							GaugeValue next = _listener.getNextGaugeValue();
							if(next == null){
								Toast.makeText(getActivity(), R.string.valuedialog_notification_no_next_value, Toast.LENGTH_SHORT).show();
							}else{
								showValue(next);
							}
						}
					}
					break;
				default:
					break;
			}
			return false;
		}
		
	}

	/**
	 * 
	 * 
	 */
	public interface DialogListener{
		
		/**
		 * 
		 * @return currently active gauge
		 */
		public Gauge getCurrentGauge();
		
		/**
		 * 
		 * @return currently active gauge values
		 */
		public GaugeValue getCurrentGaugeValue();
		
		/**
		 * 
		 * @return the values located before the current values or null if not available
		 */
		public GaugeValue getPreviousGaugeValue();
		
		/**
		 * 
		 * @return the values located after the current values or null if not available
		 */
		public GaugeValue getNextGaugeValue();
	}
}
