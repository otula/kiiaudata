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

import com.otula.utils.CommonUtils;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {
	private static final String SETTING_PREFERENCE_FILE = "MITTAUS_SETTINGS";
	private static final int SETTING_PREFERENCE_FILE_MODE = 0;	// 0 = private
	private static final String SEPARATOR_METHOD = "/";
	private static final String SETTING_USERNAME = "username";
	private static final String SETTING_PASSWORD = "password";
	private static final String SETTING_SERVICE_URI = "service_uri";
	private static final String SETTING_POINT_ID = "point_id";
	private static final String SETTING_VALUE_SEND_INTERVAL = "value_send_interval";
	private static final String SETTING_SHOW_LIMITS_ON_GRAPH = "show_limits";
	private static final String SETTING_MAX_GRAPH_POINTS = "max_graph_points";
	private static final String SETTING_FORCE_VALID_VALUES = "force_valid_values";
	private static final String SETTING_REPLACE_STORED_DATA_ON_SYNC = "replace_stored_data";
	private Context _context = null;
	private String _username = null;
	private String _password = null;
	private String _serviceUri = null;
	private String _pointId = null;
	private boolean _showGraphLimits = true;
	private boolean _replaceStoredDataOnSync = false;
	private long _valueSendInterval = 60000; // in ms, how often values will be sent
	private int _maxGraphPoints = 300;
	private boolean _forceValidValues = true;

	/**
	 * 
	 * @param context
	 */
	public Settings(Context context){
		_context = context;
		loadSettings();
	}
	
	/**
	 * @param settings
	 * @return create copy of the given settings, note that the new Settings will share the same context as the old one, or null if null passed
	 * 
	 */
	public static Settings getSettings(Settings settings){
		if(settings == null){
			return null;
		}
		Settings s = new Settings(settings._context);
		s._username = settings._username;
		s._password = settings._password;
		s._serviceUri = settings._serviceUri;
		s._pointId = settings._pointId;
		s._valueSendInterval = settings._valueSendInterval;
		s._showGraphLimits = settings._showGraphLimits;
		s._maxGraphPoints = settings._maxGraphPoints;
		s._replaceStoredDataOnSync = settings._replaceStoredDataOnSync;
		s._forceValidValues = settings._forceValidValues;
		return s;
	}

	/**
	 * 
	 */
	public void loadSettings(){
		SharedPreferences settings = _context.getSharedPreferences(SETTING_PREFERENCE_FILE, SETTING_PREFERENCE_FILE_MODE);
		_username = settings.getString(SETTING_USERNAME, "user");
		_password = settings.getString(SETTING_PASSWORD, "");
		_serviceUri = settings.getString(SETTING_SERVICE_URI, null);
		_pointId = settings.getString(SETTING_POINT_ID, null);
		_valueSendInterval = settings.getLong(SETTING_VALUE_SEND_INTERVAL, _valueSendInterval);
		_showGraphLimits = settings.getBoolean(SETTING_SHOW_LIMITS_ON_GRAPH, _showGraphLimits);
		_maxGraphPoints = settings.getInt(SETTING_MAX_GRAPH_POINTS, _maxGraphPoints);
		_replaceStoredDataOnSync = settings.getBoolean(SETTING_REPLACE_STORED_DATA_ON_SYNC, _replaceStoredDataOnSync);
		_forceValidValues = settings.getBoolean(SETTING_FORCE_VALID_VALUES, _forceValidValues);
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean isValid(){
		if(_username == null || _password == null || _serviceUri == null){
			return false;
		}else{
			return true;
		}
	}

	/**
	 * 
	 */
	public void saveSettings(){
		SharedPreferences.Editor modifiableSettings = _context.getSharedPreferences(SETTING_PREFERENCE_FILE, SETTING_PREFERENCE_FILE_MODE).edit();
		modifiableSettings.clear();
		if(_username != null){
			modifiableSettings.putString(SETTING_USERNAME, _username);
		}
		if(_password != null){
			modifiableSettings.putString(SETTING_PASSWORD, _password);
		}
		modifiableSettings.putLong(SETTING_VALUE_SEND_INTERVAL, _valueSendInterval);
		modifiableSettings.putBoolean(SETTING_SHOW_LIMITS_ON_GRAPH, _showGraphLimits);
		modifiableSettings.putInt(SETTING_MAX_GRAPH_POINTS, _maxGraphPoints);
		modifiableSettings.putBoolean(SETTING_REPLACE_STORED_DATA_ON_SYNC, _replaceStoredDataOnSync);
		modifiableSettings.putBoolean(SETTING_FORCE_VALID_VALUES, _forceValidValues);
		if(_serviceUri != null){
			modifiableSettings.putString(SETTING_SERVICE_URI, _serviceUri);
		}
		if(_pointId != null){
			modifiableSettings.putString(SETTING_POINT_ID, _pointId);
		}
		modifiableSettings.commit();
	}

	public String getUsername() {
		return _username;
	}

	public void setUsername(String username) {
		if(username == null || username.isEmpty()){
			_username = null;
		}else{
			_username = username;
		}	
	}

	public String getPassword() {
		return _password;
	}

	public void setPassword(String password) {
		if(password == null || password.isEmpty()){
			password = null;
		}else{
			_password = password;
		}	
	}

	public String getServiceUri() {
		return _serviceUri;
	}

	public void setServiceUri(String serviceUri) {
		if(!CommonUtils.isValidUrl(serviceUri)){
			_serviceUri = null;
		}else if(!serviceUri.endsWith(SEPARATOR_METHOD)){	// make sure the trailing / is present
			_serviceUri = serviceUri+SEPARATOR_METHOD;
		}else{
			_serviceUri = serviceUri;
		}
	}

	public String getPointId() {
		return _pointId;
	}

	public void setPointId(String pointId) {
		if(pointId == null || pointId.isEmpty()){
			pointId = null;
		}else{
			_pointId = pointId;
		}	
	}

	public long getValueSendInterval() {
		return _valueSendInterval;
	}

	public void setValueSendInterval(long valueSendInterval) {
		_valueSendInterval = valueSendInterval;
	}

	public boolean isShowGraphLimits() {
		return !_showGraphLimits;
	}

	public void setShowGraphLimits(boolean showGraphLimits) {
		_showGraphLimits = showGraphLimits;
	}

	public int getMaxGraphPoints() {
		return _maxGraphPoints;
	}

	public void setMaxGraphPoints(int maxGraphPoints) {
		_maxGraphPoints = maxGraphPoints;
	}

	public boolean isReplaceStoredDataOnSync() {
		return _replaceStoredDataOnSync;
	}

	public void setReplaceStoredDataOnSync(boolean replaceStoredDataOnSync) {
		_replaceStoredDataOnSync = replaceStoredDataOnSync;
	}

	/**
	 * @return the forceValidValues
	 */
	public boolean isForceValidValues() {
		return _forceValidValues;
	}

	/**
	 * @param forceValidValues the forceValidValues to set
	 */
	public void setForceValidValues(boolean forceValidValues) {
		_forceValidValues = forceValidValues;
	}
}
