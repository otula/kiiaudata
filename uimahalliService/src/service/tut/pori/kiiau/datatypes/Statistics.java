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
 * Statistics for a list of meters.
 * 
 */
public class Statistics extends JSONResponseData {
	@SerializedName(value=Definitions.JSON_NAME_METER_STATISTICS)
	private List<MeterStatistics> _meterStatistics = null;

	/**
	 * @return the meterStatistics
	 */
	public List<MeterStatistics> getMeterStatistics() {
		return _meterStatistics;
	}

	/**
	 * @param meterStatistics the meterStatistics to set
	 */
	public void setMeterStatistics(List<MeterStatistics> meterStatistics) {
		_meterStatistics = meterStatistics;
	}
	
	/**
	 * 
	 * @param meterStatistics
	 */
	public void addMeterStatistics(MeterStatistics meterStatistics){
		if(_meterStatistics == null){
			_meterStatistics = new ArrayList<>();
		}
		_meterStatistics.add(meterStatistics);
	}
}
