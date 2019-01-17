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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import service.tut.pori.kiiau.datatypes.Alert;
import service.tut.pori.kiiau.datatypes.Alert.AlertStatus;
import service.tut.pori.kiiau.datatypes.Alert.AlertType;
import service.tut.pori.kiiau.datatypes.Alerts;
import service.tut.pori.kiiau.datatypes.Gauge;
import service.tut.pori.kiiau.datatypes.Gauge.DataType;
import service.tut.pori.kiiau.datatypes.GaugeStatistics;
import service.tut.pori.kiiau.datatypes.GaugeValue;
import service.tut.pori.kiiau.datatypes.Location;
import service.tut.pori.kiiau.datatypes.Locations;
import service.tut.pori.kiiau.datatypes.Meter;
import service.tut.pori.kiiau.datatypes.MeterStatistics;
import service.tut.pori.kiiau.datatypes.Meters;
import service.tut.pori.kiiau.datatypes.Statistics;
import core.tut.pori.dao.SQLDAO;
import core.tut.pori.dao.SQLSelectBuilder;
import core.tut.pori.dao.SQLSelectBuilder.OrderDirection;
import core.tut.pori.dao.clause.AndClause;
import core.tut.pori.dao.clause.JoinClause;
import core.tut.pori.dao.clause.RawClause;
import core.tut.pori.dao.clause.SQLClause.SQLType;
import core.tut.pori.dao.clause.UpdateClause;
import core.tut.pori.dao.clause.WhereClause.ClauseType;
import core.tut.pori.dao.SQLUpdateBuilder;
import core.tut.pori.http.parameters.DataGroups;
import core.tut.pori.http.parameters.Limits;
import core.tut.pori.http.parameters.SortOptions;
import core.tut.pori.http.parameters.SortOptions.Option;
import core.tut.pori.users.UserIdentity;

/**
 * DAO for saving, retrieving and modifying meter and gauge details, and for updating collected gauge values.
 *
 */
public class UimahalliDAO extends SQLDAO {
	private static final Logger LOGGER = Logger.getLogger(UimahalliDAO.class);
	
	/* tables */
	private static final String TABLE_ALERTS = DATABASE+".uh_alerts";
	private static final String TABLE_GAUGE_VALUES = DATABASE+".uh_gauge_values";
	private static final String TABLE_GAUGES = DATABASE+".uh_gauges";
	private static final String TABLE_LOCATIONS = DATABASE+".uh_locations";
	private static final String TABLE_METERS = DATABASE+".uh_meters";
	
	/* columns */
	private static final String COLUMN_CUMULATIVE = "cumulative";
	private static final String COLUMN_DATA_TYPE = "data_type";
	private static final String COLUMN_DESCRIPTION = "description";
	private static final String COLUMN_FLOOR_PLAN_URL = "floor_plan_url";
	private static final String COLUMN_GAUGE_ID = "gauge_id";
	private static final String COLUMN_GAUGE_VALUE_ID = "gauge_value_id";
	private static final String COLUMN_GAUGE_INDEX = "gauge_index";
	private static final String COLUMN_LOCATION_ID = "location_id";
	private static final String COLUMN_LOCATION_X = "location_x";
	private static final String COLUMN_LOCATION_Y = "location_y";
	private static final String COLUMN_MAX = "max";
	private static final String COLUMN_MAX_INCREASE = "max_increase";
	private static final String COLUMN_METER_ID = "meter_id";
	private static final String COLUMN_MIN = "min";
	private static final String COLUMN_MIN_INCREASE = "min_increase";
	private static final String COLUMN_NAME = "name";
	private static final String COLUMN_OPTIONS = "options";
	private static final String COLUMN_STATUS = "status";
	private static final String COLUMN_TAG_ID = "id";
	private static final String COLUMN_TYPE = "type";
	private static final String COLUMN_UNIT = "unit";
	private static final String COLUMN_VALUE = "value";
	private static final String COLUMN_ALERT_ID = "alert_id";
	private static final String COLUMN_VAR_POP = "VAR_POP(value)";
	private static final String COLUMN_AVG = "AVG(value)";
	private static final String COLUMN_STDDEV_POP = "STDDEV_POP(value)";
	
	/* sql scripts */
	private static final String[] SQL_COLUMNS_GET_LOCATIONS = {COLUMN_LOCATION_ID, COLUMN_NAME, COLUMN_FLOOR_PLAN_URL};
	private static final String[] SQL_COLUMNS_RESOLVE_DATA_TYPES = {COLUMN_GAUGE_ID, COLUMN_DATA_TYPE};
	private static final String[] SQL_COLUMNS_GAUGE_VALUES = {COLUMN_GAUGE_ID, COLUMN_VALUE, COLUMN_ROW_CREATED};
	private static final String[] SQL_COLUMNS_STATISTICS = {COLUMN_TAG_ID, COLUMN_GAUGE_ID};
	
	private static final String SQL_CALCULATE_STATISTICS = "SELECT "+COLUMN_STDDEV_POP+", "+COLUMN_AVG+", "+COLUMN_VAR_POP+", "+COLUMN_COUNT+" FROM "+TABLE_GAUGE_VALUES+" WHERE "+COLUMN_GAUGE_ID+"=? AND "+COLUMN_ROW_CREATED+">? AND "+COLUMN_ROW_CREATED+"<?";
	private static final int[] SQL_CALCULATE_STATISTICS_SQL_TYPES = {SQLType.STRING.toInt(), SQLType.TIMESTAMP.toInt(), SQLType.TIMESTAMP.toInt()};
	
	private static final String SQL_COUNT_TAG_ID = "SELECT "+COLUMN_COUNT+" FROM "+TABLE_METERS+" WHERE "+COLUMN_TAG_ID+"=?";
	private static final int[] SQL_COUNT_TAG_ID_SQL_TYPES = {SQLType.STRING.toInt()};
	
	private static final String SQL_INSERT_ALERT = "INSERT INTO "+TABLE_ALERTS+" ("+COLUMN_STATUS+", "+COLUMN_TYPE+", "+COLUMN_GAUGE_VALUE_ID+", "+COLUMN_TAG_ID+") VALUES (?,?,?,?)";
	private static final int[] SQL_INSERT_ALERT_SQL_TYPES = {SQLType.INTEGER.toInt(), SQLType.INTEGER.toInt(), SQLType.LONG.toInt(), SQLType.STRING.toInt()};
	
	private static final String SQL_INSERT_GAUGE = "INSERT INTO "+TABLE_GAUGES+" ("+COLUMN_GAUGE_ID+", "+COLUMN_METER_ID+", "+COLUMN_GAUGE_INDEX+", "+COLUMN_NAME+", "+COLUMN_DESCRIPTION+", "+COLUMN_DATA_TYPE+", "+COLUMN_OPTIONS+", "+COLUMN_UNIT+", "+COLUMN_MIN+", "+COLUMN_MAX+", "+COLUMN_MIN_INCREASE+", "+COLUMN_MAX_INCREASE+", "+COLUMN_CUMULATIVE+") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
	private static final int[] SQL_INSERT_GAUGE_SQL_TYPES = {SQLType.STRING.toInt(), SQLType.LONG.toInt(), SQLType.INTEGER.toInt(), SQLType.STRING.toInt(), SQLType.STRING.toInt(), SQLType.STRING.toInt(), SQLType.STRING.toInt(), SQLType.STRING.toInt(), SQLType.DOUBLE.toInt(), SQLType.DOUBLE.toInt(), SQLType.DOUBLE.toInt(), SQLType.DOUBLE.toInt(), SQLType.INTEGER.toInt()};
	
	private static final String SQL_INSERT_METER = "INSERT INTO "+TABLE_METERS+" ("+COLUMN_TAG_ID+", "+COLUMN_NAME+", "+COLUMN_LOCATION_ID+", "+COLUMN_USER_ID+", "+COLUMN_LOCATION_X+", "+COLUMN_LOCATION_Y+") VALUES (?,?,?,?,?,?)";
	private static final int[] SQL_INSERT_METER_SQL_TYPES = {SQLType.STRING.toInt(), SQLType.STRING.toInt(), SQLType.LONG.toInt(), SQLType.LONG.toInt(), SQLType.DOUBLE.toInt(), SQLType.DOUBLE.toInt()};

	private static final String SQL_SELECT_METER_ID = "SELECT "+COLUMN_COUNT+", "+COLUMN_METER_ID+" FROM "+TABLE_METERS+" WHERE "+COLUMN_TAG_ID+"=? AND "+COLUMN_USER_ID+"=?";
	private static final int[] SQL_SELECT_METER_ID_SQL_TYPES = {SQLType.STRING.toInt(), SQLType.LONG.toInt()};
	
	private static final String SQL_SET_ALERT_STATUS = "UPDATE "+TABLE_ALERTS+" SET "+COLUMN_STATUS+"=? WHERE "+COLUMN_TAG_ID+"=?";
	private static final int[] SQL_SET_ALERT_STATUS_SQL_TYPES = {SQLType.INTEGER.toInt(), SQLType.STRING.toInt()};
	
	private static final SortOptions DEFAULT_SORT_OPTIONS;
	static{
		DEFAULT_SORT_OPTIONS = new SortOptions();
		DEFAULT_SORT_OPTIONS.addSortOption(new SortOptions.Option(service.tut.pori.kiiau.datatypes.Definitions.JSON_NAME_DATE, OrderDirection.ASCENDING, null));
	}
	
	/**
	 * 
	 * @param status
	 * @param tagId
	 */
	public void setAlertStatus(AlertStatus status, String tagId){
		LOGGER.debug("Alerts updated: "+getJdbcTemplate().update(SQL_SET_ALERT_STATUS, new Object[]{status.toInt(), tagId}, SQL_SET_ALERT_STATUS_SQL_TYPES));
	}
	
	/**
	 * Set gauge values from the given gauge (if the gauge contains values)
	 * 
	 * @param gauge
	 */
	public void addGaugeValues(final Gauge gauge){
		if(Gauge.isValuesEmpty(gauge)){
			LOGGER.debug("Ignored empty gauge...");
			return;
		}
		getTransactionTemplate().execute(new TransactionCallback<Void>() {

			@Override
			public Void doInTransaction(TransactionStatus status) {
				JdbcTemplate t = getJdbcTemplate();
				
				Collection<GaugeValue> gaugeValues = gauge.getValues();
				Map<Date, GaugeValue> values = new HashMap<>(gaugeValues.size());
				for(GaugeValue gv : gaugeValues){
					values.put(gv.getUpdatedTimestamp(), gv);
				}
				String gaugeId = gauge.getId();
				SQLSelectBuilder sql = new SQLSelectBuilder(TABLE_GAUGE_VALUES);
				sql.addSelectColumn(COLUMN_ROW_CREATED);
				sql.addSelectColumn(COLUMN_GAUGE_VALUE_ID);
				sql.addWhereClause(new AndClause(COLUMN_GAUGE_ID, gaugeId, SQLType.STRING));
				sql.addWhereClause(new AndClause(COLUMN_ROW_CREATED, values.keySet(), SQLType.TIMESTAMP));
				List<Map<String, Object>> rows = t.queryForList(sql.toSQLString(), sql.getValues(), sql.getValueTypes());
				
				if(!rows.isEmpty()){
					for (Map<String, Object> map : rows) {
						GaugeValue value = values.remove(new Date(((Date)map.get(COLUMN_ROW_CREATED)).getTime())); // convert sql timestamp to java.util.date
						value.setRowId((Long)map.get(COLUMN_GAUGE_VALUE_ID));
					}
				}
				
				gaugeValues = values.values();
				if(gaugeValues.isEmpty()){
					LOGGER.debug("No new gauge values for gauge, id: "+gaugeId);
				}else{
					LOGGER.debug("Inserting "+gaugeValues.size()+" new gauge values for gauge, id: "+gaugeId);
					SimpleJdbcInsert insert = new SimpleJdbcInsert(t);
					insert.setTableName(TABLE_GAUGE_VALUES);
					insert.setGeneratedKeyName(COLUMN_GAUGE_VALUE_ID);
					insert.withoutTableColumnMetaDataAccess();
					insert.usingColumns(SQL_COLUMNS_GAUGE_VALUES);
					Map<String,Object> map = new HashMap<>(SQL_COLUMNS_GAUGE_VALUES.length);
					map.put(COLUMN_GAUGE_ID, gaugeId);
					for(GaugeValue gv : gaugeValues){
						map.put(COLUMN_VALUE, gv.getValue());
						map.put(COLUMN_ROW_CREATED, gv.getUpdatedTimestamp());
						gv.setRowId((Long)insert.executeAndReturnKey(map));
					}
				}
				return null;
			}
		});
	}
	
	/**
	 * 
	 * @param authenticatedUser
	 * @param locationIds
	 * @return locations or null if none was found
	 */
	public Locations getLocations(UserIdentity authenticatedUser, long[] locationIds) {
		SQLSelectBuilder sql = new SQLSelectBuilder(TABLE_LOCATIONS);
		sql.addSelectColumns(SQL_COLUMNS_GET_LOCATIONS);
		sql.addOrderBy(COLUMN_NAME, OrderDirection.ASCENDING);
		sql.addOrderBy(COLUMN_LOCATION_ID, OrderDirection.ASCENDING);
		sql.addWhereClause(new AndClause(COLUMN_USER_ID, authenticatedUser.getUserId(), SQLType.LONG));
		if(!ArrayUtils.isEmpty(locationIds)){
			LOGGER.debug("Adding location id filter...");
			sql.addWhereClause(new AndClause(COLUMN_LOCATION_ID, locationIds));
		}
		
		List<Map<String, Object>> rows = getJdbcTemplate().queryForList(sql.toSQLString(), sql.getValues(), sql.getValueTypes());
		if(rows.isEmpty()){
			LOGGER.debug("No locations found.");
			return null;
		}
		
		Locations locations = new Locations();
		for(Map<String, Object> row : rows){
			locations.addLocation(extractLocation(row));
		}
		return locations;
	}
	
	/**
	 * 
	 * @param row
	 * @return location extracted from the given row map
	 */
	private Location extractLocation(Map<String, Object> row){
		Location location = new Location();
		for(Entry<String, Object> e : row.entrySet()){
			switch(e.getKey()){
				case COLUMN_LOCATION_ID:
					location.setLocationId((Long) e.getValue());
					break;
				case COLUMN_NAME:
					location.setName((String) e.getValue());
					break;
				case COLUMN_FLOOR_PLAN_URL:
					location.setFloorPlanUrl((String) e.getValue());
					break;
				case COLUMN_USER_ID: // ignore
					break;
				default:
					LOGGER.warn("Ignored unknown column: "+e.getKey());
					break;
			} // switch
		} // for
		return location;
	}
	
	/**
	 * 
	 * @param authenticatedUser
	 * @param dataGroups
	 * @param sortOptions 
	 * @param limits 
	 * @param tagIds
	 * @return meters or null if none was found
	 */
	public Meters getMeters(UserIdentity authenticatedUser, DataGroups dataGroups, Limits limits, SortOptions sortOptions, List<String> tagIds) {
		SQLSelectBuilder sql = new SQLSelectBuilder(TABLE_METERS);
		if(tagIds != null && !tagIds.isEmpty()){
			LOGGER.debug("Adding tag id filter...");
			sql.addWhereClause(new AndClause(COLUMN_TAG_ID, tagIds.toArray(), SQLType.STRING));
		}
		sql.addWhereClause(new AndClause(COLUMN_USER_ID, authenticatedUser.getUserId(), SQLType.LONG));	//get data of logged user
		sql.addOrderBy(COLUMN_LOCATION_ID, OrderDirection.ASCENDING);
		sql.addOrderBy(COLUMN_NAME, OrderDirection.ASCENDING);
		
		Meters meterList = null;
		JdbcTemplate template = getJdbcTemplate();
		meterList = extractMeters(template.queryForList(sql.toSQLString(), sql.getValues(), sql.getValueTypes()), meterList);
		if(DataGroups.hasDataGroup(DataGroups.DATA_GROUP_ALL, dataGroups) || DataGroups.hasDataGroup(Definitions.DATA_GROUP_GAUGES, dataGroups)){
			getGauges(meterList, dataGroups, limits, sortOptions);	//get only meters and gauges
		}
		return meterList;
	}
	
	/**
	 * Makes a check if the user has permission to add gauge values which were posted.
	 * @param authenticatedUser
	 * @param gaugeIds
	 * @return true if the user has permissions for the given gauge ids
	 */
	public boolean hasPermissions(UserIdentity authenticatedUser, Set<String> gaugeIds){
		SQLSelectBuilder sql = new SQLSelectBuilder(TABLE_GAUGES);
		sql.addSelectColumn(COLUMN_COUNT);
		sql.addWhereClause(new AndClause(COLUMN_GAUGE_ID, gaugeIds, SQLType.STRING));
		sql.addWhereClause(new RawClause(COLUMN_METER_ID+" IN (SELECT "+COLUMN_METER_ID+" FROM "+TABLE_METERS+" WHERE "+COLUMN_USER_ID+"=?)", new Object[]{authenticatedUser.getUserId()}, new SQLType[]{SQLType.LONG}, ClauseType.AND));
		return (getJdbcTemplate().queryForObject(sql.toSQLString(), sql.getValues(), sql.getValueTypes(), Long.class) == gaugeIds.size());
	}
	
	/**
	 * 
	 * @param meterList
	 * @param dataGroups
	 * @param sortOptions 
	 * @param limits 
	 */
	private void getGauges(Meters meterList, DataGroups dataGroups, Limits limits, SortOptions sortOptions){
		if(Meters.isEmpty(meterList)){
			return;
		}
		List<Meter> meters = meterList.getMeters();
		JdbcTemplate template = getJdbcTemplate();
		SQLSelectBuilder sql = new SQLSelectBuilder(TABLE_GAUGES);
		sql.addOrderBy(COLUMN_GAUGE_INDEX, OrderDirection.ASCENDING);
		for (Meter meter : meters) {
			sql.clearWhereClauses();
			sql.addWhereClause(new AndClause(COLUMN_METER_ID, meter.getMeterId(), SQLType.LONG));
			List<Gauge> gaugeList = null;
			gaugeList = extractGauges(template.queryForList(sql.toSQLString(), sql.getValues(), sql.getValueTypes()), gaugeList);
			meter.setGauges(gaugeList);
			if(DataGroups.hasDataGroup(DataGroups.DATA_GROUP_ALL, dataGroups) || DataGroups.hasDataGroup(Definitions.DATA_GROUP_GAUGE_VALUES, dataGroups)){
				getGaugeValues(gaugeList, limits, sortOptions);
			}
		}
	}
	
	/**
	 * 
	 * @param gaugeList
	 * @param limits
	 * @param sortOptions
	 */
	private void getGaugeValues(List<Gauge> gaugeList, Limits limits, SortOptions sortOptions){
		if(gaugeList == null || gaugeList.isEmpty()){
			return;
		}
		JdbcTemplate template = getJdbcTemplate();
		SQLSelectBuilder sql = new SQLSelectBuilder(TABLE_GAUGE_VALUES);
		setOrderBy(sql, sortOptions);
		sql.setLimits(limits);
		for (Gauge gauge : gaugeList) {
			sql.clearWhereClauses();
			sql.addWhereClause(new AndClause(COLUMN_GAUGE_ID, gauge.getId(), SQLType.STRING));
			List<GaugeValue> gaugeValueList = null;
			gaugeValueList = extractGaugeValues(template.queryForList(sql.toSQLString(), sql.getValues(), sql.getValueTypes()), gaugeValueList);
			template.queryForList(sql.toSQLString(), sql.getValues(), sql.getValueTypes());
			gauge.setValues(gaugeValueList);
		}
	}
	
	/**
	 * extracts the contents of the given rowmap into the passed list or creates a new list if null was passed
	 * 
	 * @param rowMap
	 * @param meterList can be null
	 * @return the passed list, a new list or null if nothing was added
	 */
	private Meters extractMeters(List<Map<String,Object>> rowMap, Meters meterList){
		if(rowMap.isEmpty()){
			return null;
		}
		if(meterList == null){
			meterList = new Meters();
			List<Meter> meters = new ArrayList<>();
			for(Iterator<Map<String,Object>> rowIter = rowMap.iterator();rowIter.hasNext();){
				Meter meter = new Meter();				
				for(Entry<String,Object> rowEntry : rowIter.next().entrySet()){
					switch(rowEntry.getKey()){
						case COLUMN_METER_ID:
							meter.setMeterId((Long)rowEntry.getValue());
							break;
						case COLUMN_TAG_ID:
							meter.setId((String)rowEntry.getValue());
							break;
						case COLUMN_NAME:
							meter.setName((String)rowEntry.getValue());
							break;
						case COLUMN_LOCATION_ID:
							meter.setLocationId((Long) rowEntry.getValue());
							break;
						case COLUMN_USER_ID:
							meter.setUserId((Long) rowEntry.getValue());
							break;
						case COLUMN_LOCATION_X:
							meter.setLocationX((Double) rowEntry.getValue());
							break;
						case COLUMN_LOCATION_Y:
							meter.setLocationY((Double) rowEntry.getValue());
							break;
						default:
							LOGGER.warn("Ignored unknown column name: "+rowEntry.getKey());
							break;
					}
				}
				meters.add(meter);
			}
			meterList.setMeters(meters);
		}
		return meterList;
	}
	
	/**
	 * extracts the contents of the given rowmap into the passed list or creates a new list if null was passed
	 * 
	 * @param rowMap
	 * @param gaugeList can be null
	 * @return the passed list, a new list or null if nothing was added
	 */
	private List<Gauge> extractGauges(List<Map<String,Object>> rowMap, List<Gauge> gaugeList){
		if(rowMap.isEmpty()){
			return null;
		}
		if(gaugeList == null){
			gaugeList = new ArrayList<>();
			for(Iterator<Map<String,Object>> rowIter = rowMap.iterator();rowIter.hasNext();){
				Gauge gauge = new Gauge();				
				for(Entry<String,Object> rowEntry : rowIter.next().entrySet()){
					switch(rowEntry.getKey()){
						case COLUMN_GAUGE_ID:
							gauge.setId((String)rowEntry.getValue());
							break;
						case COLUMN_GAUGE_INDEX:
							gauge.setIndex((Integer)rowEntry.getValue());
							break;
						case COLUMN_NAME:
							gauge.setName((String)rowEntry.getValue());
							break;
						case COLUMN_DESCRIPTION:
							gauge.setDescription((String)rowEntry.getValue());
							break;
						case COLUMN_DATA_TYPE:
							gauge.setDataType(DataType.fromDataTypeString((String)rowEntry.getValue()));
							break;
						case COLUMN_OPTIONS:
							gauge.setOptionsString((String)rowEntry.getValue());
							break;
						case COLUMN_UNIT:
							gauge.setUnit((String)rowEntry.getValue());
							break;
						case COLUMN_MIN:
							gauge.setMin((Double)rowEntry.getValue());
							break;
						case COLUMN_MIN_INCREASE:
							gauge.setMinIncrease((Double)rowEntry.getValue());
							break;
						case COLUMN_MAX:
							gauge.setMax((Double)rowEntry.getValue());
							break;
						case COLUMN_MAX_INCREASE:
							gauge.setMaxIncrease((Double)rowEntry.getValue());
							break;
						case COLUMN_CUMULATIVE:
							gauge.setCumulative(BooleanUtils.toBooleanObject((Integer)rowEntry.getValue()));
							break;
						case COLUMN_METER_ID:
							//do nothing
							break;
						default:
							LOGGER.warn("Ignored unknown column name: "+rowEntry.getKey());
							break;
					}
				}
				gaugeList.add(gauge);
			}
		}
		return gaugeList;
	}
	
	/**
	 * extracts the contents of the given rowmap into the passed list or creates a new list if null was passed
	 * 
	 * @param rowMap
	 * @param gaugeValueList can be null
	 * @return the passed list, a new list or null if nothing was added
	 */
	private List<GaugeValue> extractGaugeValues(List<Map<String,Object>> rowMap, List<GaugeValue> gaugeValueList){
		if(rowMap.isEmpty()){
			return null;
		}
		if(gaugeValueList == null){
			gaugeValueList = new ArrayList<>();
			for(Iterator<Map<String,Object>> rowIter = rowMap.iterator();rowIter.hasNext();){
				GaugeValue gaugevalue = new GaugeValue();				
				for(Entry<String,Object> rowEntry : rowIter.next().entrySet()){
					switch(rowEntry.getKey()){
						case COLUMN_GAUGE_VALUE_ID:
							gaugevalue.setRowId((Long)rowEntry.getValue());
							break;
						case COLUMN_VALUE:
							gaugevalue.setValue((String)rowEntry.getValue());
							break;
						case COLUMN_ROW_CREATED:
							gaugevalue.setUpdatedTimestamp((Date)rowEntry.getValue());
							break;
						case COLUMN_GAUGE_ID:
							//do nothing
							break;
						default:
							LOGGER.warn("Ignored unknown column name: "+rowEntry.getKey());
							break;
					}
				}
				gaugeValueList.add(gaugevalue);
			}
		}
		return gaugeValueList;
	}

	/**
	 * 
	 * @param gauges
	 * @return true if data types were successfully resolved
	 */
	public boolean resolveDataTypes(final List<Gauge> gauges) {
		if(gauges == null || gauges.isEmpty()){
			LOGGER.error("Empty gauge list.");
			return false;
		}
		
		final Set<String> ids = new HashSet<>();
		for(Gauge g : gauges){
			String id = g.getId();
			if(id == null){
				LOGGER.error("Gauge without id.");
				return false;
			}
			ids.add(id);
		}
		
		SQLSelectBuilder sql = new SQLSelectBuilder(TABLE_GAUGES);
		sql.addSelectColumns(SQL_COLUMNS_RESOLVE_DATA_TYPES);
		sql.addWhereClause(new AndClause(COLUMN_GAUGE_ID, ids.toArray(), SQLType.STRING));
		getJdbcTemplate().query(sql.toSQLString(), sql.getValues(), sql.getValueTypes(), new RowCallbackHandler() {
			@Override
			public void processRow(ResultSet set) throws SQLException {
				String id = set.getString(COLUMN_GAUGE_ID);
				ids.remove(id);
				DataType dataType = DataType.fromDataTypeString(set.getString(COLUMN_DATA_TYPE));
				for(Gauge g : gauges){
					if(id.equals(g.getId())){
						g.setDataType(dataType);
					}
				} // for
			}
		});
		
		if(!ids.isEmpty()){
			LOGGER.warn("List contained non-existing gauges.");
			return false;
		}else{
			return true;
		}
	}

	/**
	 * 
	 * @param authenticatedUser
	 * @param tagId
	 * @return the database row id for the given tag id, if the meter was owned by the user
	 */
	public Long getMeterId(UserIdentity authenticatedUser, String tagId) {
		return (Long) getJdbcTemplate().queryForMap(SQL_SELECT_METER_ID, new Object[]{tagId, authenticatedUser.getUserId()}, SQL_SELECT_METER_ID_SQL_TYPES).get(COLUMN_METER_ID);
	}
	
	/**
	 * 
	 * @param meter
	 * @return true if successfully created, false if the given tagId was reserved
	 */
	public boolean addMeter(final Meter meter) {
		return getTransactionTemplate().execute(new TransactionCallback<Boolean>() {

			@Override
			public Boolean doInTransaction(TransactionStatus status) {
				JdbcTemplate t = getJdbcTemplate();
				String tagId = meter.getId();
				if(t.queryForObject(SQL_COUNT_TAG_ID, new Object[]{tagId}, SQL_COUNT_TAG_ID_SQL_TYPES, Long.class) > 0){
					LOGGER.warn("The given tag id was already in use, tag id: "+tagId);
					return false;
				}
				
				t.update(SQL_INSERT_METER, new Object[]{tagId, meter.getName(), meter.getLocationId(), meter.getUserId(), meter.getLocationX(), meter.getLocationY()}, SQL_INSERT_METER_SQL_TYPES);
				return true;
			}
		});
	}
	
	/**
	 * User can only update his/her own meters, the given authenticatedUser will be used as a filter, the userId given in the meter will be ignored.
	 * 
	 * The update is done by row id (if given), otherwise by tagId.
	 * 
	 * @param authenticatedUser
	 * @param meter
	 * @return true on success
	 */
	public boolean modifyMeter(UserIdentity authenticatedUser, Meter meter) {
		SQLUpdateBuilder sql = new SQLUpdateBuilder(TABLE_METERS);
		
		String name = meter.getName();
		if(!StringUtils.isBlank(name)){
			sql.addUpdateClause(new UpdateClause(COLUMN_NAME, name, SQLType.STRING));
		}
		Long locationId = meter.getLocationId();
		if(locationId != null){
			sql.addUpdateClause(new UpdateClause(COLUMN_LOCATION_ID, locationId, SQLType.LONG));
		}
		Double temp = meter.getLocationX();
		if(temp != null){
			sql.addUpdateClause(new UpdateClause(COLUMN_LOCATION_X, temp, SQLType.DOUBLE));
		}
		temp = meter.getLocationY();
		if(temp != null){
			sql.addUpdateClause(new UpdateClause(COLUMN_LOCATION_Y, temp, SQLType.DOUBLE));
		}
		
		String tagId = meter.getId();
		Long meterId = meter.getMeterId();
		if(meterId == null){
			LOGGER.debug("Updating by "+COLUMN_TAG_ID+": "+tagId);
			sql.addWhereClause(new AndClause(COLUMN_TAG_ID, tagId, SQLType.STRING));
		}else{
			LOGGER.debug("Updating by "+COLUMN_METER_ID+" id: "+meterId);
			sql.addWhereClause(new AndClause(COLUMN_METER_ID, meterId, SQLType.LONG));
			if(tagId != null){
				LOGGER.debug("Updating tagId.");
				sql.addUpdateClause(new UpdateClause(COLUMN_TAG_ID, tagId, SQLType.STRING));
			}
		}
		if(sql.getUpdateClauseCount() < 1){
			LOGGER.warn("Nothing to update for meter id: "+meterId+", tag id: "+tagId);
			return false;
		}
		sql.addWhereClause(new AndClause(COLUMN_USER_ID, authenticatedUser.getUserId(), SQLType.LONG));
		
		if(sql.execute(getJdbcTemplate()) < 1){
			LOGGER.debug("Nothing updated for meter id: "+meterId+", tag id: "+tagId);
		}
		return true;
	}
	
	/**
	 * 
	 * @param gauge
	 * @param meterId
	 */
	public void addGauge(Gauge gauge, Long meterId) {
		getJdbcTemplate().update(SQL_INSERT_GAUGE, new Object[]{gauge.getId(), meterId, gauge.getIndex(), gauge.getName(), gauge.getDescription(), gauge.getDataType().toDataTypeString(), gauge.getOptionsString(), gauge.getUnit(), gauge.getMin(), gauge.getMax(), gauge.getMinIncrease(), gauge.getMaxIncrease(), BooleanUtils.toIntegerObject(gauge.isCumulative())}, SQL_INSERT_GAUGE_SQL_TYPES);
	}
	
	/**
	 * 
	 * @param authenticatedUser
	 * @param endDate
	 * @param startDate
	 * @param tagIdFilter
	 * @return statistics or null if none available
	 */
	public Statistics calculateStatistics(UserIdentity authenticatedUser, Date endDate, Date startDate, List<String> tagIdFilter){
		SQLSelectBuilder sql = new SQLSelectBuilder(TABLE_METERS);
		sql.addSelectColumns(SQL_COLUMNS_STATISTICS);
		sql.addJoin(new JoinClause("JOIN "+TABLE_GAUGES+" ON "+TABLE_METERS+"."+COLUMN_METER_ID+"="+TABLE_GAUGES+"."+COLUMN_METER_ID));
		sql.addWhereClause(new AndClause(COLUMN_USER_ID, authenticatedUser.getUserId(), SQLType.LONG));
		sql.addOrderBy(COLUMN_TAG_ID, OrderDirection.ASCENDING); // order by tag id, used in the following loop
		if(tagIdFilter != null && !tagIdFilter.isEmpty()){
			LOGGER.debug("Adding tag id filter...");
			sql.addWhereClause(new AndClause(COLUMN_TAG_ID, tagIdFilter, SQLType.STRING));
		}
		
		JdbcTemplate t = getJdbcTemplate();
		List<Map<String, Object>> rows = t.queryForList(sql.toSQLString(), sql.getValues(), sql.getValueTypes());
		if(rows.isEmpty()){
			LOGGER.debug("No gauges available.");
			return null;
		}
		
		Statistics statistics = new Statistics();
		Object[] args = {null, startDate, endDate};
		MeterStatistics currentMeterStatistics = new MeterStatistics();
		for(Map<String, Object> row : rows){
			String tagId = (String) row.get(COLUMN_TAG_ID);
			if(!tagId.equals(currentMeterStatistics.getTagId())){ // the rows are ordered by tag id, so they should progress in order
				if(!MeterStatistics.isEmpty(currentMeterStatistics)){ // add only if there are statistics
					statistics.addMeterStatistics(currentMeterStatistics);
					currentMeterStatistics = new MeterStatistics(); // we can re-use the old one only if it was empty
				}
				currentMeterStatistics.setTagId(tagId);	// in any case, update the current tag id
			}
			
			String gaugeId = (String) row.get(COLUMN_GAUGE_ID);
			args[0] = gaugeId;
			Map<String, Object> sRow = t.queryForMap(SQL_CALCULATE_STATISTICS, args, SQL_CALCULATE_STATISTICS_SQL_TYPES);
			GaugeStatistics gaugeStatistics = extractGaugeStatistics(sRow);
			if(gaugeStatistics == null){
				LOGGER.debug("No gauge statistics for gauge, id: "+gaugeId);
			}else{
				gaugeStatistics.setGaugeId(gaugeId);
				currentMeterStatistics.addGaugeStatistics(gaugeStatistics);
			}	
		} // for meter-gauge id pairs
		if(!MeterStatistics.isEmpty(currentMeterStatistics)){ // add only if there are statistics
			statistics.addMeterStatistics(currentMeterStatistics);
		}
		
		return statistics;
	}
	
	/**
	 * 
	 * @param row
	 * @return statistics extracted from the given row map
	 */
	private GaugeStatistics extractGaugeStatistics(Map<String, Object> row){
		GaugeStatistics gaugeStatistics = new GaugeStatistics();
		for(Entry<String, Object> e : row.entrySet()){
			String column = e.getKey();
			switch(column){
				case COLUMN_AVG:
					gaugeStatistics.setAverage((Double) e.getValue());
					break;
				case COLUMN_VAR_POP:
					gaugeStatistics.setVariance((Double) e.getValue());
					break;
				case COLUMN_STDDEV_POP:
					gaugeStatistics.setStandardDeviation((Double) e.getValue());
					break;
				default:
					if(checkCountColumn(column, e.getValue()) < 1){
						LOGGER.warn("No statistics.");
						return null;
					}
					break;
			} // switch
		} // for statistics
		return gaugeStatistics;
	}

	/**
	 * 
	 * @param authenticatedUser
	 * @param statusFilter
	 * @return alerts or null if none available
	 */
	public Alerts getAlerts(UserIdentity authenticatedUser, EnumSet<AlertStatus> statusFilter) {
		SQLSelectBuilder sql = new SQLSelectBuilder(TABLE_ALERTS);
		sql.addOrderBy(COLUMN_ALERT_ID, OrderDirection.DESCENDING);
		if(statusFilter != null){
			LOGGER.debug("Setting status filter...");
			sql.addWhereClause(new AndClause(COLUMN_STATUS, AlertStatus.toInt(statusFilter)));
		}
		sql.addWhereClause(new RawClause(COLUMN_TAG_ID+" IN (SELECT "+COLUMN_TAG_ID+" FROM "+TABLE_METERS+" WHERE "+COLUMN_USER_ID+"=?)", new Object[]{authenticatedUser.getUserId()}, new SQLType[]{SQLType.LONG}, ClauseType.AND));

		sql.addJoin(new JoinClause("LEFT JOIN "+TABLE_GAUGE_VALUES+" ON "+TABLE_ALERTS+"."+COLUMN_GAUGE_VALUE_ID+"="+TABLE_GAUGE_VALUES+"."+COLUMN_GAUGE_VALUE_ID));
		List<Map<String, Object>> rows = getJdbcTemplate().queryForList(sql.toSQLString(), sql.getValues(), sql.getValueTypes());
		if(rows.isEmpty()){
			LOGGER.debug("No alerts.");
			return null;
		}
		
		Alerts alerts = new Alerts();
		for(Map<String, Object> row : rows){
			alerts.addAlert(extractAlert(row));
		}
		return alerts;
	}
	
	/**
	 * 
	 * @param row
	 * @return alert extracted from the given row map
	 */
	private Alert extractAlert(Map<String, Object> row){
		Alert alert = new Alert();
		GaugeValue value = new GaugeValue();
		for(Entry<String, Object> e : row.entrySet()){
			switch(e.getKey()){
				case COLUMN_ALERT_ID: // ignore
					break;
				case COLUMN_STATUS:
					alert.setStatus(AlertStatus.fromInt((int) e.getValue()));
					break;
				case COLUMN_TYPE:
					alert.setType(AlertType.fromInt((int) e.getValue()));
					break;
				case COLUMN_GAUGE_VALUE_ID:
					value.setRowId((Long) e.getValue());
					break;
				case COLUMN_VALUE:
					value.setValue((String) e.getValue());
					break;
				case COLUMN_ROW_CREATED:
					value.setUpdatedTimestamp((Date) e.getValue());
					break;
				case COLUMN_TAG_ID:
					alert.setTagId((String) e.getValue());
					break;
				case COLUMN_GAUGE_ID:
					alert.setGaugeId((String) e.getValue());
					break;
				default:
					LOGGER.warn("Ignored unknown column: "+e.getKey());
					break;
			}
		}
		alert.setValue(value);
		return alert;
	}
	
	/**
	 * 
	 * @param alert
	 */
	public void addAlert(Alert alert){
		getJdbcTemplate().update(SQL_INSERT_ALERT, new Object[]{alert.getStatus().toInt(), alert.getType().toInt(), alert.getValue().getRowId(), alert.getTagId()}, SQL_INSERT_ALERT_SQL_TYPES);
	}
	
	/**
	 * Helper method to set ordering
	 * @param sql
	 * @param sortOptions
	 */
	private void setOrderBy(SQLSelectBuilder sql, SortOptions sortOptions){
		if(sortOptions == null || !sortOptions.hasValues()){
			sortOptions = DEFAULT_SORT_OPTIONS;
		}
		
		Set<Option> so = sortOptions.getSortOptions(null);
		if(so == null){
			return;
		}
		
		for(Iterator<Option> iter = so.iterator();iter.hasNext();){
			Option o = iter.next();
			String elementName = o.getElementName();
			if(service.tut.pori.kiiau.datatypes.Definitions.JSON_NAME_DATE.equals(elementName)){
				sql.addOrderBy(COLUMN_ROW_CREATED, o.getOrderDirection());
			}else{
				LOGGER.debug("Ignored unknown sort element: "+elementName);
			}
		}
	}
}
