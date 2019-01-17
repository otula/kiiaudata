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

/**
 * Statistics for a single meter.
 * 
 */
@SuppressWarnings("deprecation")
public class MeterStatistics {
	@SerializedName(value=Definitions.JSON_NAME_GAUGE_STATISTICS)
	private List<GaugeStatistics> _gaugeStatistics = null;
	@SerializedName(value=Definitions.JSON_NAME_TAG_ID)
	private String _tagId = null;

	/**
	 * @return the gaugeStatistics
	 */
	public List<GaugeStatistics> getGaugeStatistics() {
		return _gaugeStatistics;
	}

	/**
	 * @param gaugeStatistics the gaugeStatistics to set
	 */
	public void setGaugeStatistics(List<GaugeStatistics> gaugeStatistics) {
		_gaugeStatistics = gaugeStatistics;
	}

	/**
	 * @return the tagId
	 */
	public String getTagId() {
		return _tagId;
	}

	/**
	 * @param tagId the tagId to set
	 */
	public void setTagId(String tagId) {
		_tagId = tagId;
	}
	
	/**
	 * 
	 * @param gaugeStatistics
	 */
	public void addGaugeStatistics(GaugeStatistics gaugeStatistics){
		if(_gaugeStatistics == null){
			_gaugeStatistics = new ArrayList<>();
		}
		_gaugeStatistics.add(gaugeStatistics);
	}
	
	/**
	 * 
	 * @param meterStatistics
	 * @return true if the given meter statistics has no content or if null was passed
	 */
	public static boolean isEmpty(MeterStatistics meterStatistics){
		if(meterStatistics == null){
			return true;
		}else{
			return meterStatistics.isEmpty();
		}
	}
	
	/**
	 * 
	 * @return true if the statistics contain no content
	 */
	protected boolean isEmpty(){
		if(_gaugeStatistics == null){
			return true;
		}else{
			return _gaugeStatistics.isEmpty();
		}
	}
}
