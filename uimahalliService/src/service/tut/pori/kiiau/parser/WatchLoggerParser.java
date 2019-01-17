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
package service.tut.pori.kiiau.parser;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.log4j.Logger;

import service.tut.pori.kiiau.Definitions;
import service.tut.pori.kiiau.UimahalliCore;
import service.tut.pori.kiiau.datatypes.Alert;
import service.tut.pori.kiiau.datatypes.Alert.AlertStatus;
import service.tut.pori.kiiau.datatypes.Alert.AlertType;
import service.tut.pori.kiiau.datatypes.Gauge;
import service.tut.pori.kiiau.datatypes.GaugeValue;
import service.tut.pori.kiiau.datatypes.Meter;
import service.tut.pori.kiiau.datatypes.Meters;
import core.tut.pori.http.parameters.DataGroups;
import core.tut.pori.users.UserIdentity;

/**
 * Helper class for parsing WatchLogger log files.
 * 
 * this class is NOT thread-safe
 *
 */
public class WatchLoggerParser {
	/** temperature unit for gauges */
	public static final String TEMPERATURE_GAUGE_UNIT = "C";
	/** humidity unit for gauges */
	public static final String HUMIDITY_GAUGE_UNIT = "RH";
	private static final Logger LOGGER = Logger.getLogger(WatchLoggerParser.class);
	private static final String SAMPLE_DATA_ROW = "\"SD\"";
	private static final String INVALID_DATE = "\"2000.01.01 00:00:00\""; // rubbish data sometimes present in the log files
	private FastDateFormat _dateFormat = null;
	private Alert _lastLowTemperatureAlert = null;
	private Alert _lastHighTemperatureAlert = null;
	private Alert _lastLowHumidityAlert = null;
	private Alert _lastHighHumidityAlert = null;
	
	/**
	 * 
	 */
	public WatchLoggerParser() {
		_dateFormat = FastDateFormat.getInstance("\"yyyy.MM.dd HH:mm:ss\"");
	}

	/**
	 * 
	 * @param authenticatedUser used to validate permissions to access the parsed meter
	 * @param input
	 * @return meter parsed from the input
	 * @throws IllegalArgumentException
	 */
	public Meter parse(UserIdentity authenticatedUser, String input) throws IllegalArgumentException {
		String[] rows = StringUtils.split(input, "\n");
		if(rows == null || rows.length < 2){
			throw new IllegalArgumentException("Row count was < 2. No log contents?");
		}
		
		//first row contains the TAG ID (fifth token)
		String tagId = StringUtils.split(rows[0], ',')[4];
		tagId = StringUtils.replace(tagId, "\"", "");
		
		//retrieve meter from database, do access check at the same time
		Meters meters = UimahalliCore.getMeasurements(authenticatedUser, new DataGroups(Definitions.DATA_GROUP_GAUGES), null, null, Arrays.asList(tagId));
		if(meters == null || meters.getMeters() == null || meters.getMeters().isEmpty()){
			return null;	//not authorized
		}
		
		Meter meter = meters.getMeters().get(0);
		List<Gauge> gauges = meter.getGauges();
		Gauge tGauge = null;
		Gauge hGauge = null;
		for (Gauge gauge : gauges) { // find correct gauges
			if(TEMPERATURE_GAUGE_UNIT.equals(gauge.getUnit())){
				tGauge = gauge;
			}else if(HUMIDITY_GAUGE_UNIT.equals(gauge.getUnit())){
				hGauge = gauge;
			}
		}
		
		double lowestTemp = tGauge.getMin(); // stores the temporary value used for comparison
		double highestTemp = tGauge.getMax(); // stores the temporary value used for comparison
		double lowestHum = hGauge.getMin(); // stores the temporary value used for comparison
		double highestHum = hGauge.getMax(); // stores the temporary value used for comparison
		GaugeValue lowestTempValue = null;
		GaugeValue highestTempValue = null;
		GaugeValue lowestHumValue = null;
		GaugeValue highestHumValue = null;
		int valueCount = 0;
		for(int i=1; i<rows.length; ++i){
			String[] row = StringUtils.split(rows[i], ',');
			if(!SAMPLE_DATA_ROW.equals(row[0])){
				break;
			}
			if(INVALID_DATE.equals(row[1])){ // ignore rows with bad timestamps
				continue;
			}
			
			Date rowCreated = null;
			try {
				rowCreated = _dateFormat.parse(row[1]);
			} catch (ParseException ex) {
				LOGGER.error(ex, ex);
				throw new IllegalArgumentException("Failed to parse date: "+row[1]);
			}
			
			GaugeValue tempValue = new GaugeValue(row[2], rowCreated);
			tGauge.addGaugeValue(tempValue);
			double value = Double.parseDouble(tempValue.getValue());
			if(value < lowestTemp){
				lowestTemp = value;
				lowestTempValue = tempValue;
			}else if(value > highestTemp){
				highestTemp = value;
				highestTempValue = tempValue;
			}
			
			tempValue = new GaugeValue(row[3], rowCreated);
			hGauge.addGaugeValue(tempValue);
			value = Double.parseDouble(tempValue.getValue());
			if(value < lowestHum){
				lowestHum = value;
				lowestHumValue = tempValue;
			}else if(value > highestHum){
				highestHum = value;
				highestHumValue = tempValue;
			}
			
			++valueCount;
		}
		
		if(valueCount < 1){
			throw new IllegalArgumentException("No values.");
		}
		LOGGER.debug("Parsed value pairs: "+valueCount);
		
		if(lowestTempValue != null){
			LOGGER.debug("Temperature value under min limit found.");
			_lastLowTemperatureAlert = new Alert();
			_lastLowTemperatureAlert.setGaugeId(tGauge.getId());
			_lastLowTemperatureAlert.setTagId(meter.getId());
			_lastLowTemperatureAlert.setStatus(AlertStatus.NEW);
			_lastLowTemperatureAlert.setType(AlertType.LOW_TEMPERATURE);
			_lastLowTemperatureAlert.setValue(lowestTempValue);
		}
		
		if(highestTempValue != null){
			LOGGER.debug("Temperature value over max limit found.");
			_lastHighTemperatureAlert = new Alert();
			_lastHighTemperatureAlert.setGaugeId(tGauge.getId());
			_lastHighTemperatureAlert.setTagId(meter.getId());
			_lastHighTemperatureAlert.setStatus(AlertStatus.NEW);
			_lastHighTemperatureAlert.setType(AlertType.HIGH_TEMPERATURE);
			_lastHighTemperatureAlert.setValue(highestTempValue);
		}
		
		if(lowestHumValue != null){
			LOGGER.debug("Humidity value under min limit found.");
			_lastLowHumidityAlert = new Alert();
			_lastLowHumidityAlert.setGaugeId(tGauge.getId());
			_lastLowHumidityAlert.setTagId(meter.getId());
			_lastLowHumidityAlert.setStatus(AlertStatus.NEW);
			_lastLowHumidityAlert.setType(AlertType.LOW_HUMIDITY);
			_lastLowHumidityAlert.setValue(lowestHumValue);
		}
		
		if(highestHumValue != null){
			LOGGER.debug("Humidity value over max limit found.");
			_lastHighHumidityAlert = new Alert();
			_lastHighHumidityAlert.setGaugeId(tGauge.getId());
			_lastHighHumidityAlert.setTagId(meter.getId());
			_lastHighHumidityAlert.setStatus(AlertStatus.NEW);
			_lastHighHumidityAlert.setType(AlertType.HIGH_HUMIDITY);
			_lastHighHumidityAlert.setValue(highestHumValue);
		}
		
		return meter;
	}

	/**
	 * @return the lastLowTemperatureAlert
	 */
	public Alert getLastLowTemperatureAlert() {
		return _lastLowTemperatureAlert;
	}

	/**
	 * @return the lastHighTemperatureAlert
	 */
	public Alert getLastHighTemperatureAlert() {
		return _lastHighTemperatureAlert;
	}

	/**
	 * @return the lastLowHumidityAlert
	 */
	public Alert getLastLowHumidityAlert() {
		return _lastLowHumidityAlert;
	}

	/**
	 * @return the lastHighHumidityAlert
	 */
	public Alert getLastHighHumidityAlert() {
		return _lastHighHumidityAlert;
	}
}
