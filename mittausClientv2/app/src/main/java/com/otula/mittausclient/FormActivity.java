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
package com.otula.mittausclient;

import java.util.Arrays;
import java.util.EnumSet;

import com.otula.datatypes.Meter;
import com.otula.datatypes.Meters;
import com.otula.datatypes.Settings;
import com.otula.datatypes.Meter.ValueValidity;
import com.otula.uiutils.DialogUtils;
import com.otula.uiutils.FormInputAdapter;
import com.otula.uiutils.DialogUtils.DialogListener;
import com.otula.utils.LogUtils;
import com.otula.utils.MeterDBHelper;
import com.otula.utils.NFCHelper;
import com.otula.utils.ValueService;
import com.otula.utils.MeterDBHelper.DataFilter;
import com.otula.utils.NFCHelper.ErrorCode;
import com.otula.utils.NFCHelper.NFCHelperListener;
import com.otula.utils.ValueService.OperationType;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.Toast;

/**
 * This activity is for typing in the meter values.
 *
 */
public class FormActivity extends Activity implements NFCHelperListener, OnClickListener, DialogListener {
	private static final String CLASS_NAME = FormActivity.class.toString();
	private static final int TAG_UNSAVED_CHANGES = 0;
	private static final int TAG_NO_DIALOG = 0;	// dialog was not shown
	private static final int TAG_INVALID_VALUES = 1;
	private static final int TAG_ABORT_INPUT = 2;
	private FormInputAdapter _inputAdapter = null;
	private Settings _settings = null;
	private NFCHelper _nfc = null;
	private MeterDBHelper _dbHelper = null;
	private Button _buttonSendValues = null;
	private boolean _valuesSaved = false;
	private Meter _pendingMeter = null;	// temporary variable for storing meter during dialogs
	private String _previousMeterId = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_form);
		_inputAdapter = new FormInputAdapter(this, (ScrollView)findViewById(R.id.form_inputs));
		_buttonSendValues = (Button) findViewById(R.id.form_button_send_values);
		_buttonSendValues.setOnClickListener(this);
		_nfc = NFCHelper.getNFCHelper(this, this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);	// Inflate the menu; this adds items to the action bar if it is present.
		menu.findItem(R.id.menu_open_form).setEnabled(false);
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		_settings = new Settings(this);
		if(_settings.isValid()){
			if(_dbHelper == null){
				_dbHelper = MeterDBHelper.getHelper(this,_settings);
			}
			if(_dbHelper.hasUnsentValues() && startService((new Intent(this,ValueService.class)).putExtra(ValueService.EXTRA_OPERATION_TYPE, OperationType.CHECK_VALUES.toInt())) == null){ // start service immediately if there are unsent values
				DialogUtils.showErrorDialog(this, R.string.form_error_failed_to_start_service, false);
			}
			if(_nfc == null || !_nfc.start()){
				DialogUtils.showErrorDialog(this, R.string.form_error_nfc_failure, true);
			}
		}else{
			Toast.makeText(this, R.string.error_invalid_settings, Toast.LENGTH_LONG).show();
			startActivity(new Intent(this,SettingsActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
		}
	}

	@Override
	protected void onPause() {
		if(_nfc != null && !_nfc.stop()){
			LogUtils.warn(CLASS_NAME, "onPause", "Failed to stop NFC.");
		}	
		super.onPause();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		if(_nfc == null){
			super.onNewIntent(intent);
		}else if(!_nfc.handleIntent(intent)){
			super.onNewIntent(intent);
		}
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onBackPressed()
	 */
	@Override
	public void onBackPressed() {
		if(Meter.isEmpty(_inputAdapter.getValues())){
			LogUtils.debug(CLASS_NAME, "onBackPressed", "Back button ignored.");
		}else{
			DialogUtils.showConfirmationDialog(this, R.string.form_warning_abort_input, this, false, TAG_ABORT_INPUT);
		}
	}

	@Override
	protected void onDestroy() {
		if(_dbHelper != null){
			_dbHelper.closeDatabase();
			_dbHelper = null;
		}
		startService((new Intent(this,ValueService.class)).putExtra(ValueService.EXTRA_OPERATION_TYPE, OperationType.STOP_SERVICE.toInt()));
		super.onDestroy();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
			case R.id.menu_open_settings:
				startActivity(new Intent(this,SettingsActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
				return true;
			case R.id.menu_open_graph:
				Intent i = new Intent(this,GraphActivity.class);
				i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				if(_previousMeterId != null){
					i.putExtra(GraphActivity.EXTRA_METER_ID, _previousMeterId);
				}
				startActivity(i);
				return true;
			default:
				LogUtils.warn(CLASS_NAME, "onOptionsItemSelected", "Unknown item id: "+item.getItemId());
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void meterDetected(String id) {
		Meters meters = _dbHelper.getMeters(Arrays.asList(id), EnumSet.of(DataFilter.BASE_DETAILS, DataFilter.GAUGES, DataFilter.GAUGE_STATISTICS));
		if(Meters.isEmpty(meters)){
			LogUtils.warn(CLASS_NAME, "meterDetected", "Detected unknown id: "+id);
		}else{
			confirmMeterChange(meters.getMeters().get(0));
		}
	}

	/**
	 * 
	 * @param newMeter
	 */
	private void confirmMeterChange(Meter newMeter){
		if(Meter.areTheSame(_inputAdapter.getValues(), newMeter)){
			LogUtils.debug(CLASS_NAME, "confirmMeterChange", "Given meter was same as the old one, ignoring...");
			return;
		}
		if(_pendingMeter != null){
			LogUtils.debug(CLASS_NAME, "confirmMeterChange", "Ignored meter change: dialog open.");
			return;
		}
		_pendingMeter = newMeter;
		_previousMeterId = newMeter.getId();
		if(!_valuesSaved){	// no saved values
			Meter meter = _inputAdapter.getValues();
			if(meter != null){	// there is a previous meter
				if(meter.hasValidValues() == ValueValidity.NO_VALUES){	// ...but no values have been added
					Toast.makeText(this, R.string.form_notification_values_not_modified, Toast.LENGTH_SHORT).show();
				}else{
					DialogUtils.showConfirmationDialog(this, R.string.form_warning_unsaved_changes, this, false, TAG_UNSAVED_CHANGES);
					return;
				}
			}
		}

		dialogClosed(ConfirmationStatus.ACCEPTED, TAG_NO_DIALOG);	// just skip the dialog
	}

	@Override
	public void nfcError(ErrorCode code) {
		DialogUtils.showErrorDialog(this, R.string.form_error_nfc_failure, false);
	}

	@Override
	public void onClick(View v) {
		Meter meter = _inputAdapter.getValues();
		if(Meter.isEmpty(meter)){
			return;
		}
		switch(meter.hasValidValues()){ // it should not be possible to click the sent button when a meter is not selected
			case INVALID:
			case GREATER_THAN_THRESHOLD:
			case LOWER_THAN_THRESHOLD:
				if(_settings.isForceValidValues()){
					Toast.makeText(this, R.string.form_error_invalid_values, Toast.LENGTH_SHORT).show();
				}else{
					DialogUtils.showConfirmationDialog(this, R.string.form_warning_invalid_values, this, false, TAG_INVALID_VALUES);
				}
				break;
			case NO_VALUES:
				Toast.makeText(this, R.string.form_notification_values_not_modified, Toast.LENGTH_SHORT).show();
				break;
			case VALID:
				saveValues(meter);
				break;
			default:
				LogUtils.error(CLASS_NAME, "onClick", "Unknown validity.");
				break;
		}
	}
	
	/**
	 * helper method for saving values
	 * 
	 * this will:
	 *  - save values to database
	 *  - start value uploader service (if not started)
	 *  - reset pendingMeter & valuesSaved variables
	 *  - reset ui elements
	 *  
	 *  @param meter meter to save
	 */
	private void saveValues(Meter meter){
		_dbHelper.addGaugeValues(meter.getGauges());
		if(startService((new Intent(this,ValueService.class)).putExtra(ValueService.EXTRA_OPERATION_TYPE, OperationType.CHECK_VALUES.toInt())) == null){
			DialogUtils.showErrorDialog(this, R.string.form_error_failed_to_start_service, false);
		}
		_valuesSaved = true;
		clearForm();
		Toast.makeText(this, R.string.form_notification_values_saved, Toast.LENGTH_LONG).show();
		setTitle(getString(R.string.form_title_previous) + " - " + getTitle());	//change title after the values have been saved (add "previous" or such)
	}

	@Override
	public void dialogClosed(ConfirmationStatus status, int tag) {
		LogUtils.debug(CLASS_NAME, "dialogClosed", "Dialog closed, tag: "+tag);
		switch(tag){
			case TAG_INVALID_VALUES: //if for checking if the dialog was answer for sending invalid values
				if(status == ConfirmationStatus.ACCEPTED){
					saveValues(_inputAdapter.getValues());
				}
				break;
			case TAG_ABORT_INPUT:
				if(status == ConfirmationStatus.ACCEPTED){
					clearForm();
				}
				break;
			default:
				if(status == ConfirmationStatus.ACCEPTED){
					if(Meter.isEmpty(_pendingMeter)){
						_buttonSendValues.setVisibility(View.GONE);
					}else{
						setTitle(_pendingMeter.getName());
						_buttonSendValues.setVisibility(View.VISIBLE);
					}			
					_inputAdapter.meterChanged(_pendingMeter);
					_valuesSaved = false;
				}
				_pendingMeter = null;
				break;
		}
	}

	@Override
	public <T> void selectionClosed(T id, String text, int tag) {
		// nothing needed
	}
	
	private void clearForm(){
		_pendingMeter = null;
		_inputAdapter.meterChanged(null);
		_buttonSendValues.setVisibility(View.GONE);
		setTitle(R.string.app_name);
	}
}
