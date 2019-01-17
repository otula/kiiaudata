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

import java.util.List;

import com.otula.datatypes.GaugeValue;
import com.otula.mittausclient.R;
import com.otula.utils.DateUtils;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * UI adapter to handle list of text based history.
 *
 */
public class TextHistoryAdapter extends BaseAdapter{
	private List<GaugeValue> _values = null;
	private Activity _context = null;
	private int _valueCount = 0;
	
	/**
	 * 
	 * @param context
	 */
	public TextHistoryAdapter(Activity context){
		_context = context;
	}
	
	/**
	 * 
	 * @param values
	 */
	public void setGaugeValues(List<GaugeValue> values){
		_values = values;
		_valueCount = (_values == null ? 0 : _values.size());
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return _valueCount;
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if(convertView == null){
			convertView = _context.getLayoutInflater().inflate(R.layout.text_history_item, null);
		}
		GaugeValue value = _values.get(_valueCount-1-position);	// get in reverse order
		TextView temp = (TextView) convertView.findViewById(R.id.text_history_date);
		temp.setText(DateUtils.dateToLocalizedString(value.getUpdatedTimestamp()));
		
		temp = (TextView) convertView.findViewById(R.id.text_history_text);
		temp.setText(value.getValue());
		return convertView;
	}

}
