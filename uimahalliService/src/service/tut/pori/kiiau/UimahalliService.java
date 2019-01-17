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

import service.tut.pori.kiiau.datatypes.Alert.AlertStatus;
import service.tut.pori.kiiau.datatypes.Gauge;
import service.tut.pori.kiiau.datatypes.Meter;
import service.tut.pori.kiiau.datatypes.Meters;

import com.google.gson.Gson;

import core.tut.pori.http.JSONResponse;
import core.tut.pori.http.annotations.HTTPAuthenticationParameter;
import core.tut.pori.http.annotations.HTTPMethodParameter;
import core.tut.pori.http.annotations.HTTPService;
import core.tut.pori.http.annotations.HTTPServiceMethod;
import core.tut.pori.http.parameters.AuthenticationParameter;
import core.tut.pori.http.parameters.DataGroups;
import core.tut.pori.http.parameters.Limits;
import core.tut.pori.http.parameters.LongParameter;
import core.tut.pori.http.parameters.SortOptions;
import core.tut.pori.http.parameters.StringParameter;
import core.tut.pori.utils.JSONFormatter;
import core.tut.pori.utils.StringUtils;

/**
 * Service definitions for uimahalli service
 *
 */
@HTTPService(name=Definitions.SERVICE_UIMAHALLI)
public class UimahalliService {
	private Gson _parser = JSONFormatter.createGsonSerializer();

	/**
	 * 
	 * @param authenticatedUser
	 * @param dataGroups
	 * @param limits 
	 * @param sortOptions 
	 * @param tagIds
	 * @return response
	 */
	@HTTPServiceMethod(name = Definitions.METHOD_GET_MEASUREMENTS, acceptedMethods = {core.tut.pori.http.Definitions.METHOD_GET})
	public JSONResponse getMeasurements(
			@HTTPAuthenticationParameter(showLoginPrompt=true) AuthenticationParameter authenticatedUser,
			@HTTPMethodParameter(name=DataGroups.PARAMETER_DEFAULT_NAME, required=false) DataGroups dataGroups,
			@HTTPMethodParameter(name=Limits.PARAMETER_DEFAULT_NAME, required=false) Limits limits,
			@HTTPMethodParameter(name = Definitions.PARAMETER_SORT, required = false) SortOptions sortOptions,
			@HTTPMethodParameter(name=Definitions.PARAMETER_TAG_ID, required=false) StringParameter tagIds
			)
	{
		return new JSONResponse(UimahalliCore.getMeasurements(authenticatedUser.getUserIdentity(), dataGroups, limits, sortOptions, tagIds.getValues()));
	}
	
	/**
	 * 
	 * @param authenticatedUser
	 * @param data 
	 * @return response
	 */
	@HTTPServiceMethod(name = Definitions.METHOD_POST_MEASUREMENTS, acceptedMethods = {core.tut.pori.http.Definitions.METHOD_POST})
	public JSONResponse postMeasurements (
			@HTTPAuthenticationParameter(showLoginPrompt=true) AuthenticationParameter authenticatedUser,
			@HTTPMethodParameter(name=Definitions.PARAMETER_VALUE_CONTENT) StringParameter data
			)
	{
		UimahalliCore.postMeasurements(authenticatedUser.getUserIdentity(), _parser.fromJson(data.getValue(), Meters.class));
		return null; // return null for default json response
	}
	
	/**
	 * 
	 * @param authenticatedUser
	 * @return response
	 */
	@HTTPServiceMethod(name = Definitions.METHOD_CHECK_CREDENTIALS, acceptedMethods = {core.tut.pori.http.Definitions.METHOD_GET})
	public JSONResponse checkCredentials(
			@HTTPAuthenticationParameter(showLoginPrompt=true) AuthenticationParameter authenticatedUser
			)
	{
		return null;
	}
	
	/**
	 * 
	 * @param authenticatedUser
	 * @param data 
	 * @param tagId 
	 * @return response
	 */
	@HTTPServiceMethod(name = Definitions.METHOD_ADD_GAUGE, acceptedMethods = {core.tut.pori.http.Definitions.METHOD_POST})
	public JSONResponse addGauge (
			@HTTPAuthenticationParameter(showLoginPrompt=true) AuthenticationParameter authenticatedUser,
			@HTTPMethodParameter(name=Definitions.PARAMETER_VALUE_CONTENT) StringParameter data,
			@HTTPMethodParameter(name=Definitions.PARAMETER_TAG_ID) StringParameter tagId
			)
	{
		UimahalliCore.addGauge(authenticatedUser.getUserIdentity(), _parser.fromJson(data.getValue(), Gauge.class), tagId.getValue());
		return null; // return null for default json response
	}
	
	/**
	 * 
	 * @param authenticatedUser
	 * @param data 
	 * @return response
	 */
	@HTTPServiceMethod(name = Definitions.METHOD_ADD_METER, acceptedMethods = {core.tut.pori.http.Definitions.METHOD_POST})
	public JSONResponse addMeter (
			@HTTPAuthenticationParameter(showLoginPrompt=true) AuthenticationParameter authenticatedUser,
			@HTTPMethodParameter(name=Definitions.PARAMETER_VALUE_CONTENT) StringParameter data
			)
	{
		UimahalliCore.addMeter(authenticatedUser.getUserIdentity(), _parser.fromJson(data.getValue(), Meter.class));
		return null; // return null for default json response
	}
	
	/**
	 * 
	 * @param authenticatedUser
	 * @param data 
	 * @return response
	 */
	@HTTPServiceMethod(name = Definitions.METHOD_MODIFY_METER, acceptedMethods = {core.tut.pori.http.Definitions.METHOD_PATCH})
	public JSONResponse modifyMeter (
			@HTTPAuthenticationParameter(showLoginPrompt=true) AuthenticationParameter authenticatedUser,
			@HTTPMethodParameter(name=Definitions.PARAMETER_VALUE_CONTENT) StringParameter data
			)
	{
		UimahalliCore.modifyMeter(authenticatedUser.getUserIdentity(), _parser.fromJson(data.getValue(), Meter.class));
		return null; // return null for default json response
	}
	
	/**
	 * 
	 * @param authenticatedUser
	 * @param locationId
	 * @return response
	 */
	@HTTPServiceMethod(name = Definitions.METHOD_GET_LOCATIONS, acceptedMethods = {core.tut.pori.http.Definitions.METHOD_GET})
	public JSONResponse getLocations (
			@HTTPAuthenticationParameter(showLoginPrompt=true) AuthenticationParameter authenticatedUser,
			@HTTPMethodParameter(name=Definitions.PARAMETER_LOCATION_ID, required=false) LongParameter locationId
			)
	{
		return new JSONResponse(UimahalliCore.getLocations(authenticatedUser.getUserIdentity(), locationId.getValues()));
	}
	
	/**
	 * 
	 * @param authenticatedUser
	 * @param data
	 * @return response
	 */
	@HTTPServiceMethod(name = Definitions.METHOD_DATA_PARSER, acceptedMethods = {core.tut.pori.http.Definitions.METHOD_POST})
	public JSONResponse dataParser (
			@HTTPAuthenticationParameter(showLoginPrompt=true) AuthenticationParameter authenticatedUser,
			@HTTPMethodParameter(name=Definitions.PARAMETER_VALUE_CONTENT) StringParameter data
			)
	{
		UimahalliCore.parseData(authenticatedUser.getUserIdentity(), data.getValue());
		return null; // return null for default json response
	}

	/**
	 * 
	 * @param authenticatedUser
	 * @param endDate
	 * @param startDate
	 * @param tagId
	 * @return response
	 */
	@HTTPServiceMethod(name = Definitions.METHOD_DATA_STATISTICS, acceptedMethods = {core.tut.pori.http.Definitions.METHOD_GET})
	public JSONResponse dataStatistics (
			@HTTPAuthenticationParameter(showLoginPrompt=true) AuthenticationParameter authenticatedUser,
			@HTTPMethodParameter(name=Definitions.PARAMETER_END_DATE, required=false) StringParameter endDate,
			@HTTPMethodParameter(name=Definitions.PARAMETER_START_DATE) StringParameter startDate,
			@HTTPMethodParameter(name=Definitions.PARAMETER_TAG_ID, required=false) StringParameter tagId
			)
	{
		return new JSONResponse(UimahalliCore.getStatistics(authenticatedUser.getUserIdentity(), StringUtils.ISOStringToDate(endDate.getValue()), StringUtils.ISOStringToDate(startDate.getValue()), tagId.getValues()));
	}
	
	/**
	 * 
	 * @param authenticatedUser
	 * @param alertStatus
	 * @return response
	 */
	@HTTPServiceMethod(name = Definitions.METHOD_GET_ALERTS, acceptedMethods = {core.tut.pori.http.Definitions.METHOD_GET})
	public JSONResponse getAlerts(
			@HTTPAuthenticationParameter(showLoginPrompt=true) AuthenticationParameter authenticatedUser,
			@HTTPMethodParameter(name=Definitions.PARAMETER_ALERT_STATUS, required=false) StringParameter alertStatus
			)
	{
		return new JSONResponse(UimahalliCore.getAlerts(authenticatedUser.getUserIdentity(), AlertStatus.fromString(alertStatus.getValues())));
	}
}
