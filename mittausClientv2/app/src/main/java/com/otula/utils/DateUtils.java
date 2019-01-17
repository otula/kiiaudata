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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;


/**
 * common methods for parsing & formatting date
 *
 */
public final class DateUtils {
	private static final String CLASS_NAME = DateUtils.class.toString();
	@SuppressLint("SimpleDateFormat")
	private static final SimpleDateFormat ISO_DATE = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	@SuppressLint("SimpleDateFormat")
	private static final SimpleDateFormat AXIS_DATE = new SimpleDateFormat("dd.MM.\nyyyy");
	private static final DateFormat LOCALIZED_DATE = SimpleDateFormat.getDateTimeInstance();
	
	/**
	 * 
	 */
	private DateUtils(){
		// nothing needed
	}
	
	/**
	 * 
	 * @param date
	 * @return
	 */
	public static String dateToString(Date date){
		synchronized (ISO_DATE) {
			return ISO_DATE.format(date);
		}
	}
	
	/**
	 * 
	 * @param date
	 * @return
	 */
	public static String dateToAxisDateString(Date date){
		synchronized (AXIS_DATE) {
			return AXIS_DATE.format(date);
		}
	}
	
	/**
	 * 
	 * @param date
	 * @return
	 */
	public static String dateToLocalizedString(Date date){
		synchronized (LOCALIZED_DATE) {
			return LOCALIZED_DATE.format(date);
		}
	}
	
	/**
	 * 
	 * @param date
	 * @return
	 */
	public static Date stringToDate(String date){
		try {
			if(date.endsWith("Z")){
				date = date.substring(0, date.length()-1)+"+0000";
			}
			synchronized (ISO_DATE) {
				return ISO_DATE.parse(date);
			}	
		} catch (ParseException ex) {//+0300
			LogUtils.error(CLASS_NAME, "stringToDate", ex.toString());
			return null;
		}
	}
	
	/**
	 * Method to calculate duration between two dates in days
	 * @param start
	 * @param end
	 * @return
	 */
	public static double durationAsDays(long start, long end){
		return Math.abs(start - end) / 86400000.0;	//ms*s*min*h*day = 1000*60*60*24 = 86400000 
	}
	
	/**
	 * Overloaded method to calculate duration between two dates in days
	 * @param start
	 * @param end
	 * @return
	 */
	public static double durationAsDays(Date start, Date end){
		return durationAsDays(start.getTime(), end.getTime()); 
	}
}
