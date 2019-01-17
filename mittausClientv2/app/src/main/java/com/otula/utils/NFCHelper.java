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

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NfcAdapter;
import android.nfc.Tag;

/**
 * NFC helper class that uses foreground dispatch for detecting NFC tags.<br/>
 * 
 * Usage: 
 * <ul>
 *  <li>create instance to your activity</li>
 *  <li>pass the activity as a parameter</li>
 *  <li>override onNewIntent on your activity and pass the received intents to handleIntent() of this class</li>
 * </ul>
 */
public class NFCHelper {
	private static final String CLASS_NAME = NFCHelper.class.toString();
	private static final int PENDING_INTENT_REQUEST_CODE = 0;
	private static final String[][] TECH_LISTS = null;
	private NFCHelperListener _listener = null;
	private boolean _running = false;
	private NfcAdapter _adapter = null;
	private Activity _activity = null;
	private PendingIntent _pIntent = null;
	private IntentFilter _iFilters[] = null;
	
	/**
	 * 
	 *
	 */
	public enum ErrorCode{
		NO_ERROR
	}
	
	/**
	 * 
	 * @param listener
	 * @param activity
	 * @return
	 */
	public static NFCHelper getNFCHelper(NFCHelperListener listener, Activity activity){
		NfcAdapter adapter = NfcAdapter.getDefaultAdapter(activity);
		if(adapter == null){
			LogUtils.error(CLASS_NAME, "getNFCHelper", "Failed to get default NFC adapter.");
			return null;
		}
		try {
			return new NFCHelper(listener, activity, adapter);
		} catch (MalformedMimeTypeException ex) {
			LogUtils.error(CLASS_NAME, "getNFCHelper", ex.toString());
			return null;
		}
	}
	
	/**
	 * 
	 * @param listener
	 * @param activity
	 * @param adapter
	 * @throws MalformedMimeTypeException
	 */
	private NFCHelper(NFCHelperListener listener, Activity activity, NfcAdapter adapter) throws MalformedMimeTypeException{
		_iFilters = new IntentFilter[]{new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED), new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED), new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)};
		_listener = listener;
		_activity = activity;
		_adapter = adapter;
		_pIntent = PendingIntent.getActivity(_activity, PENDING_INTENT_REQUEST_CODE, new Intent(_activity, _activity.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean start(){
		if(_running){
			LogUtils.debug(CLASS_NAME, "start", "Already running.");
			return true;
		}
		_running = true;
		_adapter.enableForegroundDispatch(_activity, _pIntent, _iFilters, TECH_LISTS);
		return _running;
	}
	
	/**
	 * 
	 * @param intent
	 * @return true if the intent was handled
	 */
	public boolean handleIntent(Intent intent){
		Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		if(tag == null){
			LogUtils.debug(CLASS_NAME, "handleIntent", "Received intent without Tag.");
			return false;
		}else{
			byte[] id = tag.getId();
			if(id.length < 1){
				LogUtils.warn(CLASS_NAME, "handleIntent", "Discovered tag without a valid id.");
			}else{
				_listener.meterDetected(HexUtils.encodeHexString(id));
			}
			return true;
		}	
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean stop(){
		if(!_running){
			LogUtils.debug(CLASS_NAME, "stop", "Already stopped.");
			return true;
		}
		
		_adapter.disableForegroundDispatch(_activity);
		_running = false;
		return !_running;
	}
	
	/**
	 * 
	 *
	 */
	public interface NFCHelperListener{
		public void meterDetected(String id);
		public void nfcError(ErrorCode code);
	}
}
