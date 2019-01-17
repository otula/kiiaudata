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
package service.tut.pori.kiiau;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import service.tut.pori.kiiau.datatypes.Alert;
import service.tut.pori.kiiau.datatypes.Alert.AlertStatus;
import service.tut.pori.kiiau.datatypes.Alerts;
import service.tut.pori.kiiau.datatypes.Gauge;
import service.tut.pori.kiiau.datatypes.Locations;
import service.tut.pori.kiiau.datatypes.Meter;
import service.tut.pori.kiiau.datatypes.Meters;
import service.tut.pori.kiiau.datatypes.Meters.ValueValidity;
import service.tut.pori.kiiau.datatypes.Statistics;
import service.tut.pori.kiiau.parser.WatchLoggerParser;
import core.tut.pori.context.ServiceInitializer;
import core.tut.pori.http.parameters.DataGroups;
import core.tut.pori.http.parameters.Limits;
import core.tut.pori.http.parameters.SortOptions;
import core.tut.pori.users.UserIdentity;

/**
 * The core methods for Uimahalli Service.
 *
 */
public final class UimahalliCore {
	private static final EnumSet<AlertStatus> DEFAULT_STATUSES = EnumSet.of(AlertStatus.NEW);
	private static final Logger LOGGER = Logger.getLogger(UimahalliCore.class);
	
	/**
	 * 
	 */
	private UimahalliCore(){
		// nothing needed
	}

	/**
	 * 
	 * @param authenticatedUser 
	 * @param dataGroups 
	 * @param sortOptions 
	 * @param limits 
	 * @param tagIds 
	 * @return measurements or null if none was found
	 */
	public static Meters getMeasurements(UserIdentity authenticatedUser, DataGroups dataGroups, Limits limits, SortOptions sortOptions, List<String> tagIds) {
		return ServiceInitializer.getDAOHandler().getDAO(UimahalliDAO.class).getMeters(authenticatedUser, dataGroups, limits, sortOptions, tagIds);
	}

	/**
	 * 
	 * @param authenticatedUser
	 * @param meters
	 * @throws IllegalArgumentException
	 */
	public static void postMeasurements(UserIdentity authenticatedUser, Meters meters) throws IllegalArgumentException {
		if(Meters.isEmpty(meters)){
			LOGGER.warn("Ignored empty meters.");
			return;
		}
		
		List<Gauge> gauges = new ArrayList<>();
		Set<String> gaugeIds = new HashSet<>();
		for(Meter meter : meters.getMeters()){
			if(Meter.isEmpty(meter)){
				LOGGER.warn("Ignored empty meter.");
			}else{
				for(Gauge gauge : meter.getGauges()){
					gauges.add(gauge);	//add gauge to list
					gaugeIds.add(gauge.getId());	//add gauge id to check for authenticated insert
				}
			}
		}
		if(gaugeIds.isEmpty()){
			LOGGER.warn("Ignored meters without gauges.");
			return;
		}
		
		UimahalliDAO dao = ServiceInitializer.getDAOHandler().getDAO(UimahalliDAO.class);
		if(!dao.hasPermissions(authenticatedUser, gaugeIds)){
			throw new IllegalArgumentException("Bad meter or gauge ids.");
		}
		
		if(!dao.resolveDataTypes(gauges)){
			throw new IllegalArgumentException("Bad Gauges.");
		}
		
		if(Meters.hasValidValues(meters) != ValueValidity.VALID){
			throw new IllegalArgumentException("Bad Meters.");
		}
		
		for(Gauge gauge : gauges){
			dao.addGaugeValues(gauge);
		} // for gauges
	}
	
	/**
	 * 
	 * @param authenticatedUser
	 * @param meter
	 * @throws IllegalArgumentException
	 */
	public static void addMeter(UserIdentity authenticatedUser, Meter meter) throws IllegalArgumentException{
		String tagId = meter.getId();
		if(StringUtils.isBlank(tagId) || StringUtils.isBlank(meter.getName())){
			throw new IllegalArgumentException("Invalid meter.");
		}
		
		Long userId = authenticatedUser.getUserId();
		if(!userId.equals(meter.getUserId())){
			LOGGER.warn("Meter user identity does not match the authenticated user id, setting user id to: "+userId);
			meter.setUserId(userId);
		}
		
		if(meter.getLocationX() != null){
			if(meter.getLocationY() == null || meter.getLocationId() == null){
				throw new IllegalArgumentException("Invalid location.");
			}
		}else if(meter.getLocationY() != null && (meter.getLocationId() == null || meter.getLocationX() == null)){
			throw new IllegalArgumentException("Invalid location.");
		}
		
		if(!ServiceInitializer.getDAOHandler().getDAO(UimahalliDAO.class).addMeter(meter)){
			throw new IllegalArgumentException("Bad tag Id: "+tagId);
		}
	}
	
	/**
	 * 
	 * @param authenticatedUser
	 * @param meter
	 * @throws IllegalArgumentException
	 */
	public static void modifyMeter(UserIdentity authenticatedUser, Meter meter) throws IllegalArgumentException {
		String tagId = meter.getId();
		if(StringUtils.isBlank(tagId)){
			throw new IllegalArgumentException("Tag id is missing.");
		}
		
		if(!ServiceInitializer.getDAOHandler().getDAO(UimahalliDAO.class).modifyMeter(authenticatedUser, meter)){
			throw new IllegalArgumentException("Failed to update the given meter, tag id: "+tagId);
		}
	}
	
	/**
	 * 
	 * @param authenticatedUser
	 * @param gauge
	 * @param tagId
	 * @throws IllegalArgumentException
	 */
	public static void addGauge(UserIdentity authenticatedUser, Gauge gauge, String tagId) throws IllegalArgumentException{
		if(gauge.getIndex() == null || StringUtils.isBlank(gauge.getName()) || StringUtils.isBlank(gauge.getDescription()) || gauge.getDataType() == null || gauge.getOptions() == null){
			throw new IllegalArgumentException("Invalid gauge.");
		}
		
		UimahalliDAO dao = ServiceInitializer.getDAOHandler().getDAO(UimahalliDAO.class);
		Long meterId = dao.getMeterId(authenticatedUser, tagId);
		if(meterId == null){
			throw new IllegalArgumentException("Permission denied for the given tag Id.");
		}
		
		if(StringUtils.isBlank(gauge.getId())){
			String uuid = UUID.randomUUID().toString();
			LOGGER.warn("No gauge id given, generated a new random id: "+uuid);
			gauge.setId(uuid);
		}
		
		dao.addGauge(gauge, meterId);
	}
	
	/**
	 * 
	 * @param authenticatedUser
	 * @param locationIds
	 * @return locations or null if none was found
	 */
	public static Locations getLocations(UserIdentity authenticatedUser, long[] locationIds) {
		return ServiceInitializer.getDAOHandler().getDAO(UimahalliDAO.class).getLocations(authenticatedUser, locationIds);
	}
	
	/**
	 * 
	 * @param authenticatedUser
	 * @param data
	 * @throws IllegalArgumentException
	 */
	public static void parseData(UserIdentity authenticatedUser, String data) throws IllegalArgumentException{
		WatchLoggerParser parser = new WatchLoggerParser();
		Meter meter = parser.parse(authenticatedUser, data);
		if(meter == null){
			throw new IllegalArgumentException("Failed to add meter data.");
		}
		UimahalliDAO dao = ServiceInitializer.getDAOHandler().getDAO(UimahalliDAO.class);
		dao.setAlertStatus(AlertStatus.CHECKED, meter.getId()); // reset alert status for all previous alerts
		
		Meters meters = new Meters();
		meters.setMeters(Arrays.asList(meter));
		postMeasurements(authenticatedUser, meters);
		
		Alert temp = parser.getLastLowTemperatureAlert();
		if(temp != null){
			dao.addAlert(temp);
		}
		temp = parser.getLastHighTemperatureAlert();
		if(temp != null){
			dao.addAlert(temp);
		}
		temp = parser.getLastLowHumidityAlert();
		if(temp != null){
			dao.addAlert(temp);
		}
		temp = parser.getLastHighHumidityAlert();
		if(temp != null){
			dao.addAlert(temp);
		}
	}

	/**
	 * 
	 * @param authenticatedUser
	 * @param endDate
	 * @param startDate
	 * @param tagIds
	 * @return statistics or null if none was found
	 */
	public static Statistics getStatistics(UserIdentity authenticatedUser, Date endDate, Date startDate, List<String> tagIds){
		if(endDate == null){
			LOGGER.debug("No end date, using current date.");
			endDate = new Date();
		}
		
		return ServiceInitializer.getDAOHandler().getDAO(UimahalliDAO.class).calculateStatistics(authenticatedUser, endDate, startDate, tagIds);
	}

	/**
	 * 
	 * @param authenticatedUser
	 * @param statusFilter
	 * @return alerts or null if none was found
	 */
	public static Alerts getAlerts(UserIdentity authenticatedUser, EnumSet<AlertStatus> statusFilter) {
		if(statusFilter == null){
			LOGGER.debug("No status filter, setting default filter...");
			statusFilter = DEFAULT_STATUSES;
		}
		return ServiceInitializer.getDAOHandler().getDAO(UimahalliDAO.class).getAlerts(authenticatedUser, statusFilter);
	}
}
