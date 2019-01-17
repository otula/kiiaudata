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
package com.otula.utils;

import java.util.concurrent.atomic.AtomicBoolean;

import com.otula.datatypes.Meters;
import com.otula.datatypes.Settings;
import com.otula.utils.HTTPClient.HTTPClientListener;
import com.otula.utils.HTTPClient.Status;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Looper;

/**
 * Background service for handling meter value sending thru HTTPClient.
 *
 */
public class ValueService extends Service{
	public static final String EXTRA_OPERATION_TYPE = "operation_type";
	private static final int ERROR_THRESHOLD = 5;
	private static final String CLASS_NAME = ValueService.class.toString();
	private static boolean RUNNING = false;
	private static int ERROR_COUNT = 0;
	private ValueSendThread _sendThread = null;

	/**
	 * 
	 *
	 */
	public enum OperationType{
		UNKNOWN(0),
		STOP_SERVICE(1),	// request this service is to be stopped
		UPDATE_SETTINGS(2),	// notify that settings have been changed and should be re-read
		CHECK_VALUES(3);	// re-read the list of values from database, can be used to notify that new values are available

		private int _value;

		private OperationType(int value){
			_value = value;
		}

		public int toInt(){
			return _value;
		}

		public static OperationType fromInt(Integer value){
			if(value != null){
				for(OperationType t : OperationType.values()){
					if(t._value == value){
						return t;
					}
				}
			}
			return UNKNOWN;
		}
	}	// enum OperationType


	@Override
	public IBinder onBind(Intent intent) {
		// nothing needed
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		switch(OperationType.fromInt(intent.getIntExtra(EXTRA_OPERATION_TYPE, OperationType.UNKNOWN.toInt()))){
			case CHECK_VALUES:
				LogUtils.debug(CLASS_NAME, "onStartCommand", "Received check values command.");
				_sendThread.valuesChanged();
				break;
			case STOP_SERVICE:
				LogUtils.debug(CLASS_NAME, "onStartCommand", "Received stop service command.");
				stopSelf();
				break;
			case UPDATE_SETTINGS:
				LogUtils.debug(CLASS_NAME, "onStartCommand", "Received update settings command.");
				_sendThread.settingsChanged();
				break;
			default:
				LogUtils.error(CLASS_NAME, "onStartCommand", "Unknown operation type.");
		}
		return START_STICKY;
	}

	@Override
	public void onCreate() {
		_sendThread = new ValueSendThread(this, new Settings(this));
		_sendThread.start();
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		_sendThread.requestStop();
		super.onDestroy();
	}
	
	/**
	 * 
	 * @return true if the service is running
	 */
	public static boolean isRunning(){
		return RUNNING;
	}
	
	/**
	 * 
	 * @return true if the service has pending errors
	 */
	public static boolean hasPendingErrors(){
		return (ERROR_COUNT > 0 ? true : false);
	}

	/**
	 *
	 *
	 */
	private class ValueSendThread extends Thread implements HTTPClientListener{
		private MeterDBHelper _dbHelper = null;
		private HTTPClient _client = null;
		private boolean _stopRequested = false;
		private Settings _settings = null;
		private ValueService _valueService = null;
		private Meters _meters = null;
		private boolean _sendInProgress = false;
		private AtomicBoolean _valuesChanged = new AtomicBoolean(false);

		/**
		 * 
		 * @param _valueService
		 * @param settings
		 */
		public ValueSendThread(ValueService valueService, Settings settings){
			_settings = settings;
			_valueService = valueService;
		}
		
		/**
		 * 
		 * @return true if the loop can continue
		 */
		private boolean continueLoop(){
			if(_sendInProgress){	// do not interrupt the send process
				return true;
			}
			if(_valuesChanged.getAndSet(false)){	// retrieve new values if values have been changed
				LogUtils.debug(CLASS_NAME, "continueLoop", "Reloading changed values from the database.");
				_meters = _dbHelper.getUnsentValues();
			}
			if(_stopRequested){	// if stop has been requested
				if(ERROR_COUNT > ERROR_THRESHOLD){
					LogUtils.warn(CLASS_NAME, "continueLoop", "Stop required and multiple errors persists, stopping the service.");
					return false;
				}else if(_meters == null){	// the list of pending requests is empty
					return false;
				}else{
					LogUtils.debug(CLASS_NAME, "continueLoop", "Stop has been requested, but there are pending send requests.");
					return true;
				}
			}
			return true;
		}

		@Override
		public void run() {
			LogUtils.debug(CLASS_NAME, "run", "Started.");
			_stopRequested = false;
			_client = new HTTPClient(_valueService, _settings, this);
			_dbHelper = MeterDBHelper.getHelper(_valueService, _settings);
			ERROR_COUNT = 0;
			RUNNING = true;
			Looper.prepare();

			while(continueLoop()){	// check that pre-conditions for the loop are valid
				if(_meters == null){
					LogUtils.debug(CLASS_NAME, "run", "No meters to send.");
				}else{
					if(!_sendInProgress){	// no previously started transfer in progress	
						_sendInProgress = true;
						_client.sendMeters(_meters);
					}else{ // wait for the previous send to finish
						LogUtils.debug(CLASS_NAME, "run", "Send in progress, waiting...");
					}
				}

				long interval = _settings.getValueSendInterval();
				LogUtils.debug(CLASS_NAME, "run", "Waiting "+interval+" ms for next check...");
				try {
					Thread.sleep(interval);
				} catch (InterruptedException ex) {
					LogUtils.warn(CLASS_NAME, "run", ex.toString());
				}
			}
			
			_dbHelper.closeDatabase();
			_client = null;
			Looper.myLooper().quit();
			LogUtils.debug(CLASS_NAME, "run", "Stopped.");
			RUNNING = false;
		}

		/**
		 * 
		 */
		public void valuesChanged(){
			_stopRequested = false;
			_valuesChanged.set(true);
		}

		/**
		 * 
		 */
		public void requestStop(){
			_stopRequested = true;
		}

		/**
		 * 
		 */
		public void settingsChanged(){
			_settings = new Settings(_valueService);
		}

		@Override
		public void metersRetrieved(Status status, Meters meters) {
			// nothing needed
		}

		@Override
		public void metersSent(Status status) {
			if(status == Status.OK){
				LogUtils.debug(CLASS_NAME, "metersSent", "Values successfully sent.");
				_dbHelper.setValuesSent(_meters, true);
				_meters = null;
				ERROR_COUNT = 0;
			}else{
				LogUtils.error(CLASS_NAME, "metersSent", "Failed to send values, code: "+status.toString());
				if(++ERROR_COUNT > ERROR_THRESHOLD){
					LogUtils.warn(CLASS_NAME, "metersSent", "Request has failed "+ERROR_COUNT+" times.");
				}
			}
			_sendInProgress = false;
		}

		@Override
		public void credentialsChecked(Status status) {
			// nothing needed
		}
	}	// ValueSendThread
}
