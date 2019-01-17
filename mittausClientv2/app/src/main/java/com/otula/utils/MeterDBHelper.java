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

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.otula.datatypes.Gauge.DataType;
import com.otula.datatypes.GaugeValue;
import com.otula.datatypes.Meter;
import com.otula.datatypes.Gauge;
import com.otula.datatypes.Gauge.Option;
import com.otula.datatypes.Meters;
import com.otula.datatypes.Settings;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Class handling the SQLite database.
 * 
 */
public class MeterDBHelper {
	private static final String CLASS_NAME = MeterDBHelper.class.toString();
	private static final int DATABASE_VERSION = 20140414;
	private static final String DATABASE_BASE_URI = "meters_db_";
	private static final String TABLE_METERS = "meters";	// table for core detail of a measurement (meters)
	private static final String TABLE_GAUGES = "gauges";	// the actual measurement points (gauges)
	private static final String TABLE_GAUGE_VALUES = "gauge_values";	// the measured values

	private static final String COLUMN_METER_ID = "meter_id";
	private static final String COLUMN_NAME = "name";
	private static final String COLUMN_UPDATED = "updated";
	private static final String COLUMN_GAUGE_ID = "gauge_id";
	private static final String COLUMN_DESCRIPTION = "description";
	private static final String COLUMN_VALUE = "value";
	private static final String COLUMN_UNIT = "unit";
	private static final String COLUMN_MIN = "min";
	private static final String COLUMN_MAX = "max";
	private static final String COLUMN_SENT = "sent";
	private static final String COLUMN_GAUGE_INDEX = "gauge_index";
	private static final String COLUMN_OPTIONS = "options";
	private static final String COLUMN_GAUGE_VALUE_ID = "value_id";
	private static final String COLUMN_DATATYPE = "datatype";
	private static final String COLUMN_MIN_INCREASE = "min_increase";
	private static final String COLUMN_MAX_INCREASE = "max_increase";
	private static final String COLUMN_CUMULATIVE = "cumulative";

	private SQLiteDatabase _database = null;
	private String _databaseName = null;

	/**
	 * filters for retrieving data, note that ids are always retrieved
	 *
	 */
	public enum DataFilter{
		BASE_DETAILS,
		GAUGES,
		GAUGE_VALUES,
		GAUGE_STATISTICS	//calculates average, median and retrieves last value (newest)
	}
	
	/**
	 * 
	 * @param context
	 * @param settings
	 * @return new helper or null on failure
	 */
	public static MeterDBHelper getHelper(Context context, Settings settings){
		if(!settings.isValid()){
			return null;
		}else{
			return new MeterDBHelper(context, settings);
		}
	}

	/**
	 * 
	 * @param context
	 * @param settings username is used for database initialization, must be unique for each user
	 */
	private MeterDBHelper(Context context, Settings settings){
		_databaseName = DATABASE_BASE_URI+settings.getUsername();
		_database = (new MeterSQLiteHelper(context,_databaseName)).getWritableDatabase();
	}

	/**
	 * NOTE: this does NOT use transactions, and may be slow, you should use addMeters() instead,
	 * when adding new points. ie. This is only an internal helper function.
	 * 
	 * 
	 * @param m
	 */
	private void addMeter(Meter m){
		ContentValues values = new ContentValues();
		String id = m.getId();
		values.put(COLUMN_METER_ID, id);
		values.put(COLUMN_NAME, m.getName());
		Date updatedDate = new Date();
		values.put(COLUMN_UPDATED, updatedDate.getTime());
		_database.insert(TABLE_METERS, null, values);

		List<Gauge> gauges = m.getGauges();
		if(gauges != null){
			int pointCount = gauges.size();
			for(int i=0;i<pointCount;++i){
				addGauge(gauges.get(i), id);
			}
			LogUtils.debug(CLASS_NAME, "addMeter", "Added "+pointCount+" gauges for meter, id: "+id);
		}else{
			LogUtils.warn(CLASS_NAME, "addMeter", "No gauges for meter, id: "+id);
		}
		m.setUpdatedTimestamp(updatedDate);
	}

	/**
	 * NOTE: this does NOT use transactions, and may be slow, you should use addMeters() instead,
	 * when adding new points. ie. This is only an internal helper function.
	 * 
	 * @param gauge
	 */
	private void addGauge(Gauge g, String meterId){
		Date updated = new Date();
		ContentValues values = new ContentValues();
		values.put(COLUMN_GAUGE_INDEX, g.getIndex());
		values.put(COLUMN_METER_ID, meterId);
		values.put(COLUMN_NAME, g.getName());
		String tmp = g.getDescription();
		if(tmp != null){
			values.put(COLUMN_DESCRIPTION, tmp);
		}
		tmp = g.getUnit();
		if(tmp != null){
			values.put(COLUMN_UNIT, tmp);
		}
		Double limit = g.getMin();
		if(limit != null){
			values.put(COLUMN_MIN, limit);
		}
		limit = g.getMax();
		if(limit != null){
			values.put(COLUMN_MAX, limit);
		}
		limit = g.getMinIncrease();
		if(limit != null){
			values.put(COLUMN_MIN_INCREASE, limit);
		}
		limit = g.getMaxIncrease();
		if(limit != null){
			values.put(COLUMN_MAX_INCREASE, limit);
		}
		
		values.put(COLUMN_CUMULATIVE, g.isCumulative());	//TODO check if variable is null at some point? 
		values.put(COLUMN_OPTIONS, Option.toOptionString(g.getOptions()));
		values.put(COLUMN_UPDATED, updated.getTime());
		values.put(COLUMN_GAUGE_ID, g.getId());
		values.put(COLUMN_DATATYPE, g.getDataType().toDataTypeString());
		_database.insert(TABLE_GAUGES, null, values);
		g.setUpdatedTimestamp(updated);
		addGaugeValues(g);
	}
	
	/**
	 * set all values contained in the meters (in gauges) to the value sent
	 * @param meters the gauge values MUST have rowIds set
	 * @param sent
	 */
	public void setValuesSent(Meters meters, boolean sent){
		if(Meters.isEmpty(meters)){
			LogUtils.warn(CLASS_NAME, "setValuesSent", "Empty meter list.");
			return;
		}
		int valueCount = 0;
		StringBuilder sql = new StringBuilder(COLUMN_GAUGE_VALUE_ID+" IN (");
		for(Iterator<Meter> mIter = meters.getMeters().iterator();mIter.hasNext();){
			List<Gauge> gauges = mIter.next().getGauges();
			if(gauges != null){
				for(Iterator<Gauge> gIter = gauges.iterator();gIter.hasNext();){
					List<GaugeValue> values = gIter.next().getValues();
					if(values != null){
						for(Iterator<GaugeValue> vIter = values.iterator();vIter.hasNext();){
							long rowId = vIter.next().getRowId();
							sql.append(rowId);
							sql.append(',');
							++valueCount;
						}	// for values
					}	// if
				}	// for gauges
			}	// if
		}	// for meters
		if(valueCount < 1){
			LogUtils.debug(CLASS_NAME, "setValuesSent", "No values given.");
		}else{
			sql.setCharAt(sql.length()-1, ')');
			ContentValues values = new ContentValues(1);
			values.put(COLUMN_SENT, CommonUtils.booleanToInt(sent));
			_database.update(TABLE_GAUGE_VALUES, values, sql.toString(), null);
		}
	}
	
	/**
	 * 
	 * @return true if there are values which have not been sent
	 */
	public boolean hasUnsentValues(){
		return (DatabaseUtils.queryNumEntries(_database, TABLE_GAUGE_VALUES, COLUMN_SENT+"=0") > 0);
	}
	
	/**
	 * 
	 * Note: only ids are retrieved from the database for meters and gauges
	 * 
	 * @return list of meters (and their gauges and gauge values) with unsent values or null if none
	 */
	public Meters getUnsentValues(){
		Cursor c = _database.rawQuery("SELECT gv.*, g."+COLUMN_METER_ID+", g."+COLUMN_GAUGE_ID+" FROM "+TABLE_GAUGE_VALUES+" gv JOIN "+TABLE_GAUGES+" g ON gv."+COLUMN_GAUGE_ID+"=g."+COLUMN_GAUGE_ID+" WHERE "+COLUMN_SENT+"="+CommonUtils.booleanToInt(false), null);
		Meters meters = null;
		if(c.moveToFirst()){
			int valueIdIndex = c.getColumnIndex(COLUMN_GAUGE_VALUE_ID), gaugeIdIndex = c.getColumnIndex(COLUMN_GAUGE_ID), valueIndex = c.getColumnIndex(COLUMN_VALUE), sentIndex = c.getColumnIndex(COLUMN_SENT), updatedIndex = c.getColumnIndex(COLUMN_UPDATED), meterIdIndex = c.getColumnIndex(COLUMN_METER_ID);
			List<Meter> meterList = new ArrayList<Meter>();
			List<Gauge> gaugeList = new ArrayList<Gauge>();
			do{
				GaugeValue value = new GaugeValue(c.getLong(valueIdIndex));
				value.setValue(c.getString(valueIndex));
				value.setSent(CommonUtils.intToBoolean(c.getInt(sentIndex)));
				value.setUpdatedTimestamp(new Date(c.getLong(updatedIndex)));
				
				String gaugeId = c.getString(gaugeIdIndex);
				Gauge gauge = null;
				for(Iterator<Gauge> gIter = gaugeList.iterator();gIter.hasNext();){
					Gauge temp = gIter.next();
					if(temp.getId().equals(gaugeId)){
						gauge = temp;
						break;
					}
				}	// for gauges
				if(gauge == null){
					gauge = new Gauge(gaugeId);
					gaugeList.add(gauge);
					String meterId = c.getString(meterIdIndex);
					Meter meter = null;
					for(Iterator<Meter> mIter = meterList.iterator();mIter.hasNext();){
						Meter temp = mIter.next();
						if(temp.getId().equals(meterId)){
							meter = temp;
							break;
						}
					}	// for meters
					if(meter == null){
						meter = new Meter(meterId);
						meterList.add(meter);
					}
					meter.addGauge(gauge);
				}
				gauge.addGaugeValue(value);
			}while(c.moveToNext());
			meters = Meters.getMeters(meterList);
		}
		c.close();
		return meters;
	}

	/**
	 * 
	 * @param meterIds if null, all meters will be retrieved
	 * @param filters the list of data types to be returned
	 * @return list of meters or null if none was found
	 */
	public Meters getMeters(List<String> meterIds, EnumSet<DataFilter> filters){
		String selection = null;
		if(meterIds != null && !meterIds.isEmpty()){
			StringBuilder sql = new StringBuilder(COLUMN_METER_ID+" IN (");
			sql.append(DatabaseUtils.sqlEscapeString(meterIds.get(0)));
			for(int i=1,count=meterIds.size();i<count;++i){
				sql.append(',');
				sql.append(DatabaseUtils.sqlEscapeString(meterIds.get(i)));
			}
			sql.append(')');
			selection = sql.toString();
		}
		Cursor c = _database.query(TABLE_METERS, (filters != null && filters.contains(DataFilter.BASE_DETAILS) ? new String[]{COLUMN_METER_ID, COLUMN_NAME, COLUMN_UPDATED} : new String[]{COLUMN_METER_ID}), selection, null, null, null, null);
		List<Meter> meters = new ArrayList<Meter>();
		if(c.moveToFirst() ){
			int idIndex = c.getColumnIndex(COLUMN_METER_ID), nameIndex = c.getColumnIndex(COLUMN_NAME), updatedIndex = c.getColumnIndex(COLUMN_UPDATED);
			do{
				Meter m = new Meter(c.getString(idIndex));

				if(nameIndex >= 0){
					m.setName(c.getString(nameIndex));
				}

				if(updatedIndex >= 0){
					m.setUpdatedTimestamp(new Date(c.getLong(updatedIndex)));
				}
				meters.add(m);
			}while(c.moveToNext());
		}
		c.close();
		if(meters.isEmpty()){
			return null;
		}
		Meters retval = Meters.getMeters(meters);
		if(filters.contains(DataFilter.GAUGES)){
			getGauges(retval, filters);
		}
		return retval;
	}

	/**
	 * 
	 * replace the previously stored meters with the list of meters
	 * 
	 * @param meters
	 * @param keepValueHistory if true, the previously stored value history is kept for existing meters 
	 * (Note: the values  that belong to meters that do not exist in the passed list will be removed in any case)
	 */
	public void setMeters(Meters meters, boolean keepValueHistory){
		if(Meters.isEmpty(meters)){
			LogUtils.warn(CLASS_NAME, "setMeters", "Ignoring empty meter list.");
		}else if(keepValueHistory){
			List<Gauge> oldGauges = getGauges(null, EnumSet.of(DataFilter.GAUGE_VALUES));
			HashMap<Gauge, List<GaugeValue>> newGauges = new HashMap<Gauge, List<GaugeValue>>();
			for(Iterator<Meter> mIter = meters.getMeters().iterator();mIter.hasNext();){
				List<Gauge> nGauges = mIter.next().getGauges();
				if(nGauges != null){
					for(Iterator<Gauge> nIter = nGauges.iterator();nIter.hasNext();){	// temporarily remove values from the meters so that the values won't get added
						Gauge gauge = nIter.next();
						List<GaugeValue> gValues = gauge.getValues();
						if(gValues != null && !gValues.isEmpty()){
							LogUtils.warn(CLASS_NAME, "setMeters", "Values ignored for gauge, id: "+gauge.getId());
						}
						gauge.setValues(null);	// in any case
						newGauges.put(gauge, gValues);	// store the values so that they won't get lost
						if(oldGauges != null){
							String id = gauge.getId();
							for(Iterator<Gauge> oIter = oldGauges.iterator();oIter.hasNext();){
								if(oIter.next().getId().equals(id)){	// this gauge already exists
									oIter.remove();
								}
							}	// for old gauges
						}
					}	// for new gauges
				}	// if
			}	// for meters
			
			if(oldGauges != null){
				removeGauges(oldGauges, null);	// remove all old gauges that do not exist anymore
			}
			removeAllMeters(EnumSet.of(DataFilter.BASE_DETAILS,DataFilter.GAUGES));	// from the rest, remove everything except gauge values
			
			addMeters(meters);

			for(Entry<Gauge, List<GaugeValue>> e : newGauges.entrySet()){	// restore the gauge values if there were any
				e.getKey().setValues(e.getValue());
			}
		}else{
			removeAllMeters(null);	// if there is no desire to keep old data, we can just drop everything...
			addMeters(meters);	// ...and add the new meter details
		}
	}
	
	/**
	 * 
	 * @param filters list of filters, if null everything will be removed
	 */
	public void removeAllMeters(EnumSet<DataFilter> filters){
		LogUtils.debug(CLASS_NAME, "removeAllMeters", "Removing all meters.");
		_database.delete(TABLE_METERS, null, null);
		if(filters == null || filters.contains(DataFilter.GAUGES)){
			removeAllGauges(filters);
		}else if(filters.contains(DataFilter.GAUGE_VALUES)){	// for debug/logging only
			LogUtils.warn(CLASS_NAME, "removeAllMeters", DataFilter.GAUGE_VALUES.toString()+" given, but no "+DataFilter.GAUGES.toString()+", will not remove gauge values.");
		}
	}
	
	/**
	 * 
	 * @param filters list of filters, if null everything will be removed
	 */
	public void removeAllGauges(EnumSet<DataFilter> filters){
		LogUtils.debug(CLASS_NAME, "removeAllGauges", "Removing all gauges.");
		_database.delete(TABLE_GAUGES, null, null);
		if(filters == null || filters.contains(DataFilter.GAUGE_VALUES)){
			removeAllGaugeValues();
		}
	}
	
	/**
	 * 
	 */
	public void removeAllGaugeValues(){
		LogUtils.debug(CLASS_NAME, "removeAllGaugeValues", "Removing all gauge values.");
		_database.delete(TABLE_GAUGE_VALUES, null, null);
	}

	/**
	 * retrieves the gauges for the list of meters, all gauges will be added to the correct meters,
	 * for convenience, the gauges will also be returned in a single list
	 * 
	 * @param meters all meters MUST have id set, if null all gauges are returned
	 * @param filters
	 * @return list of gauges or null if none
	 */
	private List<Gauge> getGauges(Meters meters, EnumSet<DataFilter> filters){
		if(filters == null || filters.isEmpty()){
			LogUtils.warn(CLASS_NAME, "getGauges", "Empty filter list.");
			return null;
		}

		List<Meter> meterList = (Meters.isEmpty(meters) ? null : meters.getMeters());
		int meterCount = 0;

		String selection = null;
		if(meterList != null){
			StringBuilder sql = new StringBuilder(COLUMN_METER_ID+" IN (");
			meterCount = meterList.size();
			sql.append(DatabaseUtils.sqlEscapeString(meterList.get(0).getId()));
			for(int i=1;i<meterCount;++i){
				sql.append(',');
				sql.append(DatabaseUtils.sqlEscapeString(meterList.get(i).getId()));
			}
			sql.append(')');
			selection =  sql.toString();
		}	

		Cursor c = _database.query(TABLE_GAUGES, (filters != null && filters.contains(DataFilter.GAUGES) ? new String[]{"*"} : new String[]{COLUMN_GAUGE_ID, COLUMN_GAUGE_INDEX, COLUMN_METER_ID}), selection, null, null, null, COLUMN_GAUGE_INDEX+" ASC");
		List<Gauge> gauges = null;
		if(c.moveToFirst() ){
			gauges = new ArrayList<Gauge>(c.getCount());
			int gaugeIdIndex = c.getColumnIndex(COLUMN_GAUGE_ID), indexIndex = c.getColumnIndex(COLUMN_GAUGE_INDEX), meterIdIndex = c.getColumnIndex(COLUMN_METER_ID), nameIndex = c.getColumnIndex(COLUMN_NAME), descriptionIndex = c.getColumnIndex(COLUMN_DESCRIPTION), unitIndex = c.getColumnIndex(COLUMN_UNIT), minIndex = c.getColumnIndex(COLUMN_MIN), maxIndex = c.getColumnIndex(COLUMN_MAX), optionsIndex = c.getColumnIndex(COLUMN_OPTIONS), updatedIndex = c.getColumnIndex(COLUMN_UPDATED), dataTypeIndex = c.getColumnIndex(COLUMN_DATATYPE), minIncreaseIndex = c.getColumnIndex(COLUMN_MIN_INCREASE), maxIncreaseIndex = c.getColumnIndex(COLUMN_MAX_INCREASE), cumulativeIndex = c.getColumnIndex(COLUMN_CUMULATIVE);
			do{
				Gauge g = new Gauge(c.getString(gaugeIdIndex));
				g.setIndex(c.getInt(indexIndex));

				if(nameIndex >= 0){
					g.setName(c.getString(nameIndex));
				}
				if(descriptionIndex >= 0 && !c.isNull(descriptionIndex)){
					g.setDescription(c.getString(descriptionIndex));
				}
				if(unitIndex >= 0 && !c.isNull(unitIndex)){
					g.setUnit(c.getString(unitIndex));
				}
				if(minIndex >= 0 && !c.isNull(minIndex)){
					g.setMin(c.getDouble(minIndex));
				}
				if(maxIndex >= 0 && !c.isNull(maxIndex)){
					g.setMax(c.getDouble(maxIndex));
				}
				if(optionsIndex >= 0 && !c.isNull(optionsIndex)){
					g.setOptions(Option.fromOptionString(c.getString(optionsIndex)));
				}
				if(updatedIndex >= 0){
					g.setUpdatedTimestamp(new Date(c.getLong(updatedIndex)));
				}
				if(dataTypeIndex >= 0){
					g.setDataType(DataType.fromDataTypeString(c.getString(dataTypeIndex)));
				}
				if(minIncreaseIndex >= 0 && !c.isNull(minIncreaseIndex)){
					g.setMinIncrease(c.getDouble(minIncreaseIndex));
				}
				if(maxIncreaseIndex >= 0 && !c.isNull(maxIncreaseIndex)){
					g.setMaxIncrease(c.getDouble(maxIncreaseIndex));
				}
				if(cumulativeIndex >= 0 && !c.isNull(cumulativeIndex)){
					g.setCumulative(CommonUtils.intToBoolean(c.getInt(cumulativeIndex)));
				}

				if(meterList != null){
					String meterId = c.getString(meterIdIndex);
					for(int i=0;i<meterCount;++i){
						Meter m = meterList.get(i);
						if(meterId.equals(m.getId())){
							m.addGauge(g);
							break;
						}
					}
				}
				getGaugeStatistics(g, filters);
				gauges.add(g);
			}while(c.moveToNext());
		}
		c.close();
		if(gauges == null || gauges.isEmpty()){
			return null;
		}
		if(filters.contains(DataFilter.GAUGE_VALUES)){
			getGaugeValues(gauges, filters);
		}
		return gauges;
	}
	
	/**
	 * Sets the statistics of the gauge if requested by the passed filters
	 * @param gauge
	 * @param filters
	 */
	public void getGaugeStatistics(Gauge gauge, EnumSet<DataFilter> filters){
		if(gauge == null){
			LogUtils.warn(CLASS_NAME, "getGaugeStatistics", "No gauge.");
			return;
		}
		if(filters == null || !filters.contains(DataFilter.GAUGE_STATISTICS)){
			return;
		}
		switch(gauge.getDataType()){	//do not handle other types than DOUBLE, INTEGER
			case DOUBLE:
			case INTEGER:
				break;
			default: 
				return;
		}

		Cursor c = _database.query(TABLE_GAUGE_VALUES, new String[]{COLUMN_VALUE, COLUMN_UPDATED}, COLUMN_GAUGE_ID + "=" + DatabaseUtils.sqlEscapeString(gauge.getId()), null, null, null, COLUMN_UPDATED + " ASC");
		if(c.moveToFirst()){	//go thru the database result set
			int updatedIndex = c.getColumnIndex(COLUMN_UPDATED), valueIndex = c.getColumnIndex(COLUMN_VALUE);
			if(c.getCount() == 1){
				GaugeValue gv = new GaugeValue(c.getString(valueIndex), new Date(c.getLong(updatedIndex)));
				gauge.setLastValue(gv);
			}else if(c.getCount() > 1){
				ArrayList<Double> values = new ArrayList<Double>(c.getCount());
				long firstDate = -1;
				long lastDate = 0;
//				Double firstValue = null;
				double lastValue = 0;
				long currentDate = 0;
				long previousDate = 0;
				long rowCount = 0;
				long cumulativeRecordCount = 0;
				double sum = 0;
				do{
					double currentValue = c.getDouble(valueIndex);
					currentDate = c.getLong(updatedIndex);
					if(firstDate == -1){	//special case on the first data point
						firstDate = currentDate;
//						firstValue = currentValue;
						values.add(currentValue);
						if(!gauge.isCumulative()){
							sum += currentValue;
						}
					}else if(gauge.isCumulative()){	//no median for cumulative values
						Double previousValue = values.get(values.size()-1);
						values.add(currentValue);
						//calculate daily average
						if(currentDate != previousDate && currentValue > previousValue){	//do not calculate if dates are exactly the same, or the current value is lower than previous value
							double average = (currentValue-previousValue) / DateUtils.durationAsDays(currentDate, previousDate);
							sum += average;
							++cumulativeRecordCount;
						}
					}else{	//ordered list
						int biggerThanIndex = -1;
						boolean added = false;
						for(int i=0; i<values.size(); ++i){
							if(currentValue <= values.get(i)){
								values.add(i, currentValue);
								added = true;
								break;
							}else if(currentValue > values.get(i)){
								biggerThanIndex = i;
							}
						}
						if(biggerThanIndex != -1 && !added){
							values.add(biggerThanIndex+1, currentValue);
						}
						sum += currentValue;
					}
					previousDate = currentDate;
					++rowCount;
				}while(c.moveToNext());
				c.moveToPrevious();	//move to the last accessible cursor position
				lastDate = c.getLong(updatedIndex);
				lastValue = c.getDouble(valueIndex);
//				double duration = DateUtils.durationAsDays(lastDate, firstDate);
				gauge.setLastValue(new GaugeValue(lastValue, new Date(lastDate)));
				//calculate average for all + median for non cumulative gauges
				if(gauge.isCumulative()){	//calculate median when the gauge is not cumulative
					gauge.setAverage(new GaugeValue((sum/cumulativeRecordCount), null));
					//gauge.setAverage(new GaugeValue(((lastValue-firstValue)/duration), null));
				}else{
					gauge.setAverage(new GaugeValue((sum/rowCount), null));
					calculateMedian(gauge, values);
				}
			}
		}
		c.close();
	}
	
	/**
	 * 
	 * @param gauge
	 * @param values Requires a sorted list
	 */
	private void calculateMedian(Gauge gauge, List<Double> values){
		int count = values.size();
		double median = 0;
		boolean isEven = count % 2 == 0;
		int medianTarget = count/2;
		
		if(isEven){
			median = (values.get(medianTarget) + values.get(medianTarget-1))/2;
		}else{
			median = values.get(medianTarget);
		}
		gauge.setMedian(new GaugeValue(median, null));
	}

	/**
	 * retrieves values for the list of gauges, the values will be added in ascending order by date (oldest first)
	 * @param gauges all gauges must have ids set
	 * @param filters
	 */
	public void getGaugeValues(List<Gauge> gauges, EnumSet<DataFilter> filters){
		if(gauges == null || gauges.isEmpty()){
			LogUtils.warn(CLASS_NAME, "getGaugeValues", "Empty gauge list.");
			return;
		}
		int gaugeCount = gauges.size();
		StringBuilder sql = new StringBuilder(COLUMN_GAUGE_ID+" IN (");
		sql.append(DatabaseUtils.sqlEscapeString(gauges.get(0).getId()));
		for(int i=1;i<gaugeCount;++i){
			sql.append(',');
			sql.append(DatabaseUtils.sqlEscapeString(gauges.get(i).getId()));
		}
		sql.append(')');

		Cursor c = _database.query(TABLE_GAUGE_VALUES, (filters != null && filters.contains(DataFilter.GAUGE_VALUES) ? new String[]{"*"} : new String[]{COLUMN_GAUGE_VALUE_ID,COLUMN_GAUGE_ID, COLUMN_UPDATED}), sql.toString(), null, null, null, COLUMN_UPDATED+" ASC");
		if(c.moveToFirst()){
			int valueIdIndex = c.getColumnIndex(COLUMN_GAUGE_VALUE_ID), gaugeIdIndex = c.getColumnIndex(COLUMN_GAUGE_ID), sentIndex = c.getColumnIndex(COLUMN_SENT), updatedIndex = c.getColumnIndex(COLUMN_UPDATED), valueIndex = c.getColumnIndex(COLUMN_VALUE);
			do{
				GaugeValue gv = new GaugeValue(c.getLong(valueIdIndex));
				if(valueIndex >= 0){
					gv.setValue(c.getString(valueIndex));
				}
				if(sentIndex >= 0){
					gv.setSent(CommonUtils.intToBoolean(c.getInt(sentIndex)));
				}
				if(updatedIndex >= 0){
					gv.setUpdatedTimestamp(new Date(c.getLong(updatedIndex)));
				}			

				String gaugeId = c.getString(gaugeIdIndex);
				for(int i=0;i<gaugeCount;++i){
					Gauge g = gauges.get(i);
					if(gaugeId.equals(g.getId())){
						g.addGaugeValue(gv);
						break;
					}
				}
			}while(c.moveToNext());
		}
		c.close();
	}

	/**
	 * remove the listed meters and all their gauges (and gauge values)
	 * @param meters ids must be set
	 * @param filters	if null, everything is removed
	 */
	public void removeMeters(Meters meters, EnumSet<DataFilter> filters){
		if(Meters.isEmpty(meters)){
			LogUtils.warn(CLASS_NAME, "removeMeters", "Ignoring empty meter list.");
		}else{
			List<Meter> meterList = meters.getMeters();
			int meterCount = meterList.size();

			if(filters == null || filters.contains(DataFilter.GAUGES)){
				getGauges(meters, null);
				List<Gauge> gauges = new ArrayList<Gauge>();
				for(int i=0;i<meterCount;++i){	// combine all gauges into a single list
					List<Gauge> gTemp = meterList.get(i).getGauges();
					if(gTemp != null){
						gauges.addAll(gTemp);
					}
				}
				removeGauges(gauges, filters);	// remove the gauges
			}else if(filters != null && filters.contains(DataFilter.GAUGE_VALUES)){	// no GAUGES
				LogUtils.warn(CLASS_NAME, "removeMeters", DataFilter.GAUGE_VALUES.toString()+" given, but no "+DataFilter.GAUGES.toString()+", will not remove gauge values.");
			}

			StringBuilder sql = new StringBuilder(COLUMN_METER_ID+" IN (");
			sql.append(DatabaseUtils.sqlEscapeString(meterList.get(0).getId()));
			for(int i=1;i<meterCount;++i){
				sql.append(',');
				sql.append(DatabaseUtils.sqlEscapeString(meterList.get(i).getId()));
			}
			sql.setCharAt(sql.length()-1, ')');
			int deletedCount = _database.delete(TABLE_METERS, sql.toString(), null);
			if(deletedCount != meterCount){
				LogUtils.warn(CLASS_NAME, "removeMeters", "Passed meter amount "+meterCount+" differs from the deleted meter amount "+deletedCount);
			}
		}
	}

	/**
	 * removes the gauges and all their gauge values
	 * @param gauges ids must be set
	 * @param filters if null everything is removed
	 */
	public void removeGauges(List<Gauge> gauges, EnumSet<DataFilter> filters){
		if(gauges == null || gauges.isEmpty()){
			LogUtils.warn(CLASS_NAME, "removeGauges", "Ignoring empty gauge list.");
		}else{
			int gaugeCount = gauges.size();
			if(filters != null && filters.contains(DataFilter.GAUGE_VALUES)){
				List<GaugeValue> values = new ArrayList<GaugeValue>();
				for(int i=0;i<gaugeCount;++i){	// combine all values
					List<GaugeValue> l = gauges.get(i).getValues();
					if(l != null){
						values.addAll(l);
					}
				}
				removeGaugeValues(values);	// remove the values
			}

			StringBuilder sql = new StringBuilder(COLUMN_GAUGE_ID+" IN (");
			sql.append(DatabaseUtils.sqlEscapeString(gauges.get(0).getId()));
			for(int i=1;i<gaugeCount;++i){
				sql.append(',');
				sql.append(DatabaseUtils.sqlEscapeString(gauges.get(i).getId()));
			}
			sql.append(')');
			int deletedCount = _database.delete(TABLE_GAUGES, sql.toString(), null);
			if(deletedCount != gaugeCount){
				LogUtils.warn(CLASS_NAME, "removeGauges", "Passed gauge amount "+gaugeCount+" differs from the deleted gauge amount "+deletedCount);
			}
		}
	}

	/**
	 * 
	 * @param values ids must be set
	 */
	public void removeGaugeValues(List<GaugeValue> values){
		if(values == null || values.isEmpty()){
			LogUtils.warn(CLASS_NAME, "removeGaugeValues", "Ignoring empty gauge list.");	
		}else{
			int valueCount = values.size();
			StringBuilder sql = new StringBuilder(COLUMN_GAUGE_VALUE_ID+" IN(");
			sql.append(values.get(0).getRowId());
			for(int i=0;i<valueCount;++i){
				sql.append(',');
				sql.append(values.get(i).getRowId());
			}
			sql.append(')');
			int deletedCount = _database.delete(TABLE_GAUGE_VALUES, sql.toString(), null);
			if(deletedCount != valueCount){
				LogUtils.warn(CLASS_NAME, "removeGaugeValues", "Passed value amount "+valueCount+" differs from the deleted value amount "+deletedCount);
			}
		}
	}

	/**
	 * add the list of meters, the object will be updated with the generated ids
	 * 
	 * @param meters
	 */
	public void addMeters(Meters meters){
		if(Meters.isEmpty(meters)){
			LogUtils.warn(CLASS_NAME, "addMeters", "No details.");
			return;
		}

		_database.beginTransaction();
		for(Iterator<Meter> iter = meters.getMeters().iterator();iter.hasNext();){
			addMeter(iter.next());
		}
		_database.setTransactionSuccessful();
		_database.endTransaction();
	}
	
	/**
	 * 
	 * @param gauges
	 */
	public void addGaugeValues(List<Gauge> gauges){
		if(gauges == null || gauges.isEmpty()){
			LogUtils.warn(CLASS_NAME, "addGaugeValues", "No gauges.");
			return;
		}
		_database.beginTransaction();
		for(Iterator<Gauge> iter = gauges.iterator();iter.hasNext();){
			addGaugeValues(iter.next());
		}
		_database.setTransactionSuccessful();
		_database.endTransaction();
	}

	/**
	 * 
	 * @param g
	 */
	private void addGaugeValues(Gauge g){
		List<GaugeValue> values = null;
		if(g == null || (values = g.getValues()) == null || values.isEmpty()){
			LogUtils.warn(CLASS_NAME, "addGaugeValues", "No gauge values.");
			return;
		}else{
			LogUtils.debug(CLASS_NAME, "addGaugeValues", values.size() + " values added for gauge id:"+g.getId());
		}
		
		String gaugeId = g.getId();
		for(Iterator<GaugeValue> iter = values.iterator();iter.hasNext();){
			addGaugeValue(iter.next(), gaugeId);	
		}		
	}

	/**
	 * NOTE: this does NOT use transactions, and may be slow, you should use addGaugeValues() instead,
	 * when adding new values. ie. This is only an internal helper function.
	 * 
	 * @param gv
	 * @param gaugeId
	 */
	private void addGaugeValue(GaugeValue gv, String gaugeId){
		String value = gv.getValue();
		if(value == null){
			LogUtils.debug(CLASS_NAME, "addMeasurementValue", "No value.");
			return;
		}
		Date updated = gv.getUpdatedTimestamp();
		if(updated == null){
			updated = new Date();
			gv.setUpdatedTimestamp(updated);
		}
		ContentValues values = new ContentValues();
		values.put(COLUMN_UPDATED, updated.getTime());
		values.put(COLUMN_VALUE, value);
		values.put(COLUMN_GAUGE_ID, gaugeId);
		values.put(COLUMN_SENT, CommonUtils.booleanToInt(gv.isSent()));
		gv.setRowId(_database.insert(TABLE_GAUGE_VALUES, null, values));
	}

	/**
	 * 
	 */
	public void closeDatabase(){
		_database.close();
	}

	/**
	 * 
	 * Databases:
	 * 
	 * meters
	 * ------------
	 * meter_id (TEXT, primary key)
	 * name (TEXT)
	 * updated (INT64, unix time)
	 * 
	 * 
	 * gauges
	 * -----------
	 * gauge_id (TEXT, primary key)
	 * index (INTEGER)
	 * meter_id (TEXT)
	 * name (TEXT)
	 * description (TEXT)
	 * unit (TEXT)
	 * min (DOUBLE)
	 * max (DOUBLE)
	 * min_increase (DOUBLE)
	 * max_increase (DOUBLE)
	 * options (TEXT)
	 * updated (INT64, unix time)
	 * datatype (TEXT)
	 * cumulative (INTEGER, 0 = not cumulative, 1 = is cumulative)
	 * 
	 * 
	 * gauge_values
	 * ------------------
	 * value_id (INT32, primary key)
	 * gauge_id (INT32)
	 * value (TEXT)
	 * sent (INTEGER, 0 = not sent, 1 = sent)
	 * updated (INT64, unix time)
	 *
	 */
	private class MeterSQLiteHelper extends SQLiteOpenHelper{

		/**
		 * 
		 * @param context
		 * @param databaseName
		 */
		public MeterSQLiteHelper(Context context, String databaseName){
			super(context,databaseName,null,DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			LogUtils.debug(CLASS_NAME, "onCreate", "Creating new tables if needed...");
			db.execSQL("CREATE TABLE IF NOT EXISTS "+TABLE_METERS+" ("+
					COLUMN_METER_ID+" TEXT PRIMARY KEY,"+
					COLUMN_NAME+" TEXT NOT NULL,"+
					COLUMN_UPDATED+" INT(64) NOT NULL);");
			db.execSQL("CREATE TABLE IF NOT EXISTS "+TABLE_GAUGES+" ("+
					COLUMN_GAUGE_ID+" TEXT PRIMARY KEY,"+
					COLUMN_GAUGE_INDEX+" INTEGER NOT NULL,"+
					COLUMN_METER_ID+" TEXT NOT NULL,"+
					COLUMN_NAME+" TEXT NOT NULL,"+
					COLUMN_DESCRIPTION+" TEXT DEFAULT NULL,"+
					COLUMN_UNIT+" TEXT DEFAULT NULL,"+
					COLUMN_MIN+" DOUBLE DEFAULT NULL,"+
					COLUMN_MAX+" DOUBLE DEFAULT NULL,"+
					COLUMN_MIN_INCREASE+" DOUBLE DEFAULT NULL,"+
					COLUMN_MAX_INCREASE+" DOUBLE DEFAULT NULL,"+
					COLUMN_OPTIONS+" TEXT DEFAULT NULL,"+
					COLUMN_DATATYPE+" TEXT NOT NULL,"+
					COLUMN_CUMULATIVE+" INTEGER DEFAULT NULL,"+
					COLUMN_UPDATED+" INT(64) NOT NULL);");
			db.execSQL("CREATE TABLE IF NOT EXISTS "+TABLE_GAUGE_VALUES+" ("+
					COLUMN_GAUGE_VALUE_ID+" INTEGER PRIMARY KEY,"+
					COLUMN_GAUGE_ID+" INTEGER NOT NULL,"+
					COLUMN_VALUE+" TEXT NOT NULL,"+
					COLUMN_SENT+" INTEGER DEFAULT 0,"+
					COLUMN_UPDATED+" INT(64) NOT NULL);");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			LogUtils.warn(CLASS_NAME, "onUpgrade", "Dropping all tables for DB upgrade. Version "+oldVersion+" to version "+newVersion+".");
			db.execSQL("DROP TABLE IF EXISTS "+TABLE_METERS+";");
			db.execSQL("DROP TABLE IF EXISTS "+TABLE_GAUGES+";");
			db.execSQL("DROP TABLE IF EXISTS "+TABLE_GAUGE_VALUES+";");
			onCreate(db);
		}
	}	// MeasurementSQLiteHelper
}
