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

import com.google.gson.annotations.SerializedName;

/**
 * Statistics for a gauge.
 * 
 */
@SuppressWarnings("deprecation")
public class GaugeStatistics {
	@SerializedName(value=Definitions.JSON_NAME_GAUGE_ID)
	private String _gaugeId = null;
	@SerializedName(value=Definitions.JSON_NAME_STANDARD_DEVIATION)
	private Double _standardDeviation = null;
	@SerializedName(value=Definitions.JSON_NAME_AVERAGE)
	private Double _average = null;
	@SerializedName(value=Definitions.JSON_NAME_VARIANCE)
	private Double _variance = null;
	
	/**
	 * @return the gaugeId
	 */
	public String getGaugeId() {
		return _gaugeId;
	}
	
	/**
	 * @param gaugeId the gaugeId to set
	 */
	public void setGaugeId(String gaugeId) {
		_gaugeId = gaugeId;
	}
	
	/**
	 * @return the standardDeviation
	 */
	public Double getStandardDeviation() {
		return _standardDeviation;
	}
	
	/**
	 * @param standardDeviation the standardDeviation to set
	 */
	public void setStandardDeviation(Double standardDeviation) {
		_standardDeviation = standardDeviation;
	}
	
	/**
	 * @return the average
	 */
	public Double getAverage() {
		return _average;
	}
	
	/**
	 * @param average the average to set
	 */
	public void setAverage(Double average) {
		_average = average;
	}

	/**
	 * @return the variance
	 */
	public Double getVariance() {
		return _variance;
	}

	/**
	 * @param variance the variance to set
	 */
	public void setVariance(Double variance) {
		_variance = variance;
	}
}
