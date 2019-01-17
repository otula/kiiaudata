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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpResponseException;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.client.methods.HttpUriRequest;
import cz.msebera.android.httpclient.impl.client.BasicResponseHandler;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.protocol.HTTP;
import cz.msebera.android.httpclient.util.EntityUtils;

import com.google.gson.JsonSyntaxException;
import com.otula.datatypes.Definitions;
import com.otula.datatypes.Meters;
import com.otula.datatypes.Settings;
import com.otula.mittausclient.R;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Base64;

/**
 * Class implementing communication with server API/interfaces.
 */
public class HTTPClient {
	private static final String CLASS_NAME = HTTPClient.class.toString();
	private static final String PARAMETER_DATA_GROUPS = "data_groups";
	private static final String PARAMETER_TYPE = "type";
	private static final String PARAMETER_POINT_ID = "point";
	private static final String INTERFACE_TYPE = "json";
	private static final String HEADER_AUTHORIZATION = "Authorization";
	private static final String HEADER_CONTENT_TYPE = "Content-Type";
	private static final String METHOD_GET_MEASUREMENTS = "MeasurementInterface";
	private static final String METHOD_POST_MEASUREMENTS = "MeasurementInterface";
	private static final String METHOD_CHECK_CREDENTIALS = "CredentialInterface";
	private static final String PARAMETER_VALUE_CONTENT = "data";
	private static final String DATA_GROUP_ALL = "all";
	private static final String DATA_GROUP_GAUGES = "gauges";
	private Settings _settings = null;
	private HTTPClientListener _listener = null;
	private Context _context = null;
	
	/**
	 * 
	 *
	 */
	public enum Status{
		OK,
		SERVICE_UNAVAILABLE,	// server was unreachable
		SERVICE_FAILURE, // server was reached, but retrieving the data failed (no valid data returned)
		BAD_SETTINGS,	// settings contain a bad value
		ERROR,	// application error occurred
		BAD_CREDENTIALS;
		
		/**
		 * 
		 * @return the id for the resource string describing the error code
		 */
		public int toResourceId(){
			switch(this){
				case BAD_CREDENTIALS:
					return R.string.error_credentials_error;
				case BAD_SETTINGS:
					return R.string.error_bad_settings;
				case ERROR:
					return R.string.error_application_error;
				case OK:
					return R.string.error_ok;
				case SERVICE_FAILURE:
					return R.string.error_service_failure;
				case SERVICE_UNAVAILABLE:
					return R.string.error_service_unavailable;
				default:
					LogUtils.warn(CLASS_NAME, "toString", "Unhandeled Status code.");
					return R.string.error_unknown_error;
			}
		}
	}
	
	/**
	 * 
	 * @param context
	 * @param settings
	 * @param listener
	 */
	public HTTPClient(Context context, Settings settings, HTTPClientListener listener){
		_listener = listener;
		_context = context;
		settingsUpdated(settings);
	}
	
	/**
	 * 
	 * @param retrieveValues
	 */
	public void retrieveMeters(boolean retrieveValues){
		(new RetrievalTask()).execute(retrieveValues);
	}
	
	/**
	 * 
	 * @param settings set new settings, note that this has no effect on on-going/started requests
	 */
	public void settingsUpdated(Settings settings){
		_settings = Settings.getSettings(settings);
	}
	
	/**
	 * 
	 * @param meters
	 */
	public void sendMeters(Meters meters){
		(new SendTask()).execute(meters);
	}
	
	/**
	 * 
	 */
	public void checkCredentials(){
		(new CredentialsCheckTask()).execute();
	}
	
	/**
	 * 
	 *
	 */
	private class SendTask extends AsyncTask<Meters, Void, Status>{

		@Override
		protected com.otula.utils.HTTPClient.Status doInBackground(Meters... params) {
			if(!_settings.isValid()){
				return com.otula.utils.HTTPClient.Status.BAD_SETTINGS;
			}
			
			String content = CommonUtils.createGsonSerializer().toJson(params[0]);
			if(content == null || content.isEmpty()){
				LogUtils.error(CLASS_NAME, "doInBackground", "Failed to serialize to JSON.");
				return com.otula.utils.HTTPClient.Status.ERROR;
			}
			
			StringBuilder uri = new StringBuilder(_settings.getServiceUri());
			uri.append(METHOD_POST_MEASUREMENTS);	// the / should already be there if uri was properly set through settings
			uri.append("?"+PARAMETER_TYPE+"="+INTERFACE_TYPE);

			HttpPost post = new HttpPost(uri.toString());
			addAuthenticationHeader(post);
			try {
				post.setEntity(new UrlEncodedFormEntity(Arrays.asList(new BasicNameValuePair(PARAMETER_VALUE_CONTENT, content)), Definitions.DEFAULT_CHARSET));
				post.setHeader(HEADER_CONTENT_TYPE, Definitions.DEFAULT_CONTENT_TYPE); // force the content type, just in case
			} catch (UnsupportedEncodingException ex) {
				LogUtils.error(CLASS_NAME, "doInBackground", ex.toString());
				return com.otula.utils.HTTPClient.Status.ERROR;
			}

			CloseableHttpClient client = HttpClients.createDefault();
			try {
				LogUtils.debug(CLASS_NAME, "doInBackground", "Server responded: "+client.execute(post, new BasicResponseHandler()));
			} catch (IOException ex) {
				LogUtils.error(CLASS_NAME, "doInBackground", ex.toString());
				return com.otula.utils.HTTPClient.Status.SERVICE_UNAVAILABLE;
			}finally{
				if(client != null){
					try {
						client.close();
					} catch (IOException ex) {
						LogUtils.error(CLASS_NAME, "doInBackground", ex.toString());
					}
				}
			}
			return com.otula.utils.HTTPClient.Status.OK;
		}

		@Override
		protected void onPostExecute(com.otula.utils.HTTPClient.Status result) {
			_listener.metersSent(result);
		}
	}
	
	/**
	 * 
	 * @param request
	 */
	private void addAuthenticationHeader(HttpUriRequest request){
		String username = _settings.getUsername();
		String password = _settings.getPassword();
		if(username == null || password == null){
			LogUtils.warn(CLASS_NAME, "addAuthenticationHeader", "No username or password given.");
			return;
		}
		request.addHeader(HEADER_AUTHORIZATION, "Basic " + Base64.encodeToString((username + ":" + password).getBytes(), Base64.DEFAULT).trim());
	}
	
	/**
	 * 
	 *
	 */
	private class CredentialsCheckTask extends AsyncTask<Void, Void, Status>{

		@Override
		protected com.otula.utils.HTTPClient.Status doInBackground(Void... params) {
			if(!_settings.isValid()){
				return com.otula.utils.HTTPClient.Status.BAD_SETTINGS;
			}
			
			StringBuilder uri = new StringBuilder(_settings.getServiceUri());
			uri.append(METHOD_CHECK_CREDENTIALS);

			CloseableHttpClient client = HttpClients.createDefault();
			try {
				HttpGet get = new HttpGet(uri.toString());
				addAuthenticationHeader(get);
				LogUtils.debug(CLASS_NAME, "doInBackground", "Server responded: "+client.execute(get, new BasicResponseHandler()));
				return com.otula.utils.HTTPClient.Status.OK;
			} catch (HttpResponseException ex){
				LogUtils.error(CLASS_NAME, "doInBackground", ex.toString());
				return com.otula.utils.HTTPClient.Status.BAD_CREDENTIALS;
			} catch (IOException ex) {
				LogUtils.error(CLASS_NAME, "doInBackground", ex.toString());
				return com.otula.utils.HTTPClient.Status.SERVICE_UNAVAILABLE;
			} finally {
				if(client != null){
					try {
						client.close();
					} catch (IOException ex) {
						LogUtils.error(CLASS_NAME, "doInBackground", ex.toString());
					}
				}
			}
		}

		@Override
		protected void onPostExecute(com.otula.utils.HTTPClient.Status result) {
			_listener.credentialsChecked(result);
		}
	}	// CredentialsCheckTask
	
	/**
	 * 
	 *
	 */
	private class RetrievalTask extends AsyncTask<Boolean, Void, Status>{
		private Meters _meters = null;
		
		@Override
		protected com.otula.utils.HTTPClient.Status doInBackground(Boolean... params) {
			if(!_settings.isValid()){
				return com.otula.utils.HTTPClient.Status.BAD_SETTINGS;
			}
			StringBuilder uri = new StringBuilder(_settings.getServiceUri());
			uri.append(METHOD_GET_MEASUREMENTS);	// the / should already be there if uri was properly set through settings
			uri.append("?"+PARAMETER_TYPE+"="+INTERFACE_TYPE);
			
			String pointId = _settings.getPointId();
			if(pointId != null && !pointId.isEmpty()){
				uri.append("&"+PARAMETER_POINT_ID+"=");
				uri.append(pointId);
			}
			if(params[0]){
				uri.append("&"+PARAMETER_DATA_GROUPS+"="+DATA_GROUP_ALL);
			}else{
				uri.append("&"+PARAMETER_DATA_GROUPS+"="+DATA_GROUP_GAUGES);
			}

			CloseableHttpClient client = HttpClients.createDefault();
			try {
				HttpGet get = new HttpGet(uri.toString());
				addAuthenticationHeader(get);
				HttpResponse response = client.execute(get);
				int code = response.getStatusLine().getStatusCode();
				if(code < 200 || code >= 300){
					LogUtils.debug(CLASS_NAME, "doInBackground", "Server responded: "+code);
					return com.otula.utils.HTTPClient.Status.SERVICE_FAILURE;
				}else{
					HttpEntity entity = response.getEntity();
					if(entity != null){
						try{
							//manually convert to UTF-8 because the default implementation just doesn't get it
							_meters = CommonUtils.createGsonSerializer().fromJson(EntityUtils.toString(entity, HTTP.UTF_8), Meters.class);
						}finally{
							entity.consumeContent();
						}
					}else{
						LogUtils.debug(CLASS_NAME, "doInBackground", "No content.");
						return com.otula.utils.HTTPClient.Status.SERVICE_FAILURE;
					}
				}
				
			} catch (IOException ex) {
				LogUtils.error(CLASS_NAME, "doInBackground", ex.toString());
				return com.otula.utils.HTTPClient.Status.SERVICE_UNAVAILABLE;
			} catch (JsonSyntaxException ex){
				LogUtils.error(CLASS_NAME, "doInBackground", ex.toString());
				return com.otula.utils.HTTPClient.Status.SERVICE_FAILURE;
			} finally {
				if(client != null){
					try {
						client.close();
					} catch (IOException ex) {
						LogUtils.error(CLASS_NAME, "doInBackground", ex.toString());
					}
				}
			}
			return com.otula.utils.HTTPClient.Status.OK;
		}

		@Override
		protected void onPostExecute(com.otula.utils.HTTPClient.Status result) {
			_listener.metersRetrieved(result, _meters);
		}
	}
	
	/**
	 * 
	 *
	 */
	public interface HTTPClientListener{
		/**
		 * 
		 * @param status
		 * @param meters can be null on failure (or if no results were received), check the status
		 */
		void metersRetrieved(Status status, Meters meters);
		void metersSent(Status status);
		void credentialsChecked(Status status);
	}
}
