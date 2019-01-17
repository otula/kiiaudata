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

import com.otula.datatypes.Meters;
import com.otula.datatypes.Settings;
import com.otula.uiutils.DialogUtils;
import com.otula.uiutils.DialogUtils.DialogListener;
import com.otula.utils.HTTPClient;
import com.otula.utils.MeterDBHelper;
import com.otula.utils.HTTPClient.HTTPClientListener;
import com.otula.utils.HTTPClient.Status;
import com.otula.utils.LogUtils;

import android.os.Bundle;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Toast;

/**
 * This activity is for managing settings of the application.
 * 
 */
public class SettingsActivity extends Activity implements OnClickListener, HTTPClientListener, DialogListener, OnCheckedChangeListener {
	private static final String CLASS_NAME = SettingsActivity.class.toString();
	private static final int TAG_CONFIRMATION_SYNC = 1;
	private Settings _settings = null;
	private HTTPClient _client = null;
	private EditText _inputUsername = null;
	private EditText _inputPassword = null;
	private EditText _inputServiceUri = null;
	private EditText _inputPointId = null;
	private CheckBox _checkboxReplaceData = null;
	private CheckBox _checkboxAllowInvalidValues = null;
	private ProgressDialog _progressDialog = null;
	private boolean _retrieveMetersOnCredentialsCheck = false;	// if true, new meters will be retrieved (synced) upon successful credential check
	private boolean _finishOnCredentialsCheck = false; // if true the activity will finish on successful credential check
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		findViewById(R.id.settings_button_sync).setOnClickListener(this);
		
		_inputUsername = (EditText)findViewById(R.id.settings_input_username);
		_inputUsername.addTextChangedListener(new SettingsTextWatcher(_inputUsername));
		_inputPassword = (EditText)findViewById(R.id.settings_input_password);
		_inputPassword.addTextChangedListener(new SettingsTextWatcher(_inputPassword));
		_inputServiceUri = (EditText)findViewById(R.id.settings_input_service_uri);
		_inputServiceUri.addTextChangedListener(new SettingsTextWatcher(_inputServiceUri));
		_inputPointId = (EditText)findViewById(R.id.settings_input_point);
		_inputPointId.addTextChangedListener(new SettingsTextWatcher(_inputPointId));
		_checkboxReplaceData = ((CheckBox)findViewById(R.id.settings_checkbox_replace_data));
		_checkboxReplaceData.setOnCheckedChangeListener(this);
		_checkboxAllowInvalidValues = ((CheckBox)findViewById(R.id.settings_checkbox_allow_invalid_values));
		_checkboxAllowInvalidValues.setOnCheckedChangeListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		menu.findItem(R.id.menu_open_settings).setEnabled(false);
		return true;
	}

	@Override
	protected void onPause() {
		_settings.saveSettings();
		super.onPause();
	}

	@Override
	public void onBackPressed() {
		findViewById(R.id.settings_layout_focusable).requestFocus();	// force all text edits out-of-focus
		_finishOnCredentialsCheck = true;
		checkCredentials();
	}

	@Override
	protected void onResume() {
		loadSettings();
		super.onResume();
		Toast.makeText(this, R.string.settings_help, Toast.LENGTH_SHORT).show();
	}
	
	/**
	 * 
	 */
	private void loadSettings(){
		_settings = new Settings(this);
		_inputUsername.setText(_settings.getUsername());
		_inputPassword.setText(_settings.getPassword());
		_inputServiceUri.setText(_settings.getServiceUri());
		_inputPointId.setText(_settings.getPointId());
		_checkboxReplaceData.setChecked(_settings.isReplaceStoredDataOnSync());
		_checkboxAllowInvalidValues.setChecked(!_settings.isForceValidValues());
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
			case R.id.menu_open_graph:
				startActivity(new Intent(this,GraphActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
				return true;
			case R.id.menu_open_form:
				startActivity(new Intent(this,FormActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
				return true;
			default:
				LogUtils.warn(CLASS_NAME, "onOptionsItemSelected", "Unknown item id: "+item.getItemId());
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
			case R.id.settings_button_sync:
				DialogUtils.showConfirmationDialog(this, R.string.settings_notification_sync, this, false, TAG_CONFIRMATION_SYNC);
				break;
			default:
				LogUtils.warn(CLASS_NAME, "onClick", "Unknown view id: "+v.getId());
				break;
		}
		
	}
	
	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		switch(buttonView.getId()){
			case R.id.settings_checkbox_allow_invalid_values:
				_settings.setForceValidValues(!isChecked);
				break;
			case R.id.settings_checkbox_replace_data:
				_settings.setReplaceStoredDataOnSync(isChecked);
				break;
			default:
				LogUtils.warn(CLASS_NAME, "onCheckedChanged", "Unhandeled checkbox.");
				break;
		}
	}
	
	/**
	 *
	 */
	private void checkCredentials(){
		_progressDialog = ProgressDialog.show(this, getString(R.string.settings_title_loading), getString(R.string.settings_notification_checking_credentials));
		if(_client == null){
			_client = new HTTPClient(this,_settings,this);
		}
		_client.checkCredentials();
	}
	
	/**
	 * you should not call this directly, use checkCredentials instead
	 */
	@Override
	public void credentialsChecked(Status status) {
		_progressDialog.dismiss();
		if(status != Status.OK){
			_progressDialog = null;
			DialogUtils.showErrorDialog(this, status.toResourceId(), false);
		}else if(_retrieveMetersOnCredentialsCheck){
			_progressDialog = ProgressDialog.show(this, getString(R.string.settings_title_loading), getString(R.string.settings_notification_loading_meters));
			_client.retrieveMeters(_settings.isReplaceStoredDataOnSync());
		}else{
			_progressDialog = null;
			if(_finishOnCredentialsCheck){
				finish();
			}
		}
		_retrieveMetersOnCredentialsCheck = false;	// reset to default
		_finishOnCredentialsCheck = false; // reset to default
	}

	@Override
	public void metersRetrieved(Status status, Meters meters) {
		if(status != Status.OK){
			_progressDialog.dismiss();
			DialogUtils.showErrorDialog(this, status.toResourceId(), false);
		}else if(Meters.isEmpty(meters)){
			_progressDialog.dismiss();
			Toast.makeText(this, R.string.settings_warning_no_meters, Toast.LENGTH_SHORT).show();
		}else{
			MeterDBHelper helper = MeterDBHelper.getHelper(this,_settings);
			meters.setSent(true);
			helper.setMeters(meters, !_settings.isReplaceStoredDataOnSync());
			helper.closeDatabase();
			_progressDialog.dismiss();
			Toast.makeText(this, R.string.settings_notification_meters_updated, Toast.LENGTH_SHORT).show();
		}
		_progressDialog = null;
	}

	@Override
	public void metersSent(Status status) {
		// nothing needed
	}
	
	@Override
	public void dialogClosed(ConfirmationStatus status, int tag) {
		if(status == ConfirmationStatus.REJECTED){
			return;
		}
		_retrieveMetersOnCredentialsCheck = true;
		checkCredentials();
	}
	
	@Override
	public <T> void selectionClosed(T id, String text, int tag) {
		// nothing needed
	}
	
	/**
	 * watches for changes in the text edits
	 *
	 */
	private class SettingsTextWatcher implements TextWatcher{
		private int _fieldId;
		
		/**
		 * 
		 * @param textField
		 */
		public SettingsTextWatcher(EditText textField){
			_fieldId = textField.getId();
		}

		@Override
		public void afterTextChanged(Editable s) {
			switch(_fieldId){
				case R.id.settings_input_username:
					_settings.setUsername(s.toString());
					break;
				case R.id.settings_input_password:
					_settings.setPassword(s.toString());
					break;
				case R.id.settings_input_service_uri:
					_settings.setServiceUri(s.toString());
					break;
				case R.id.settings_input_point:
					_settings.setPointId(s.toString());
					break;
				default:
					LogUtils.warn(CLASS_NAME, "afterTextChanged", "Unknown view id: "+_fieldId);
					break;
			}
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			// nothing needed
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,int count) {
			// nothing needed
		}
	}	// SettingsTextWatcher
}
