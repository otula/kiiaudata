/**
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
package service.tut.pori.kiiau.datatypes;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

import core.tut.pori.http.JSONResponseData;

/**
 * List of alerts.
 */
public class Alerts extends JSONResponseData {
	@SerializedName(Definitions.JSON_NAME_ALERTS)
	private List<Alert> _alerts = null;
	
	/**
	 * @return the alerts
	 */
	public List<Alert> getAlerts() {
		return _alerts;
	}
	
	/**
	 * @param alerts the alerts to set
	 */
	public void setAlerts(List<Alert> alerts) {
		_alerts = alerts;
	}
	
	/**
	 * 
	 * @param alert
	 */
	public void addAlert(Alert alert){
		if(_alerts == null){
			_alerts = new ArrayList<>();
		}
		_alerts.add(alert);
	}
}
