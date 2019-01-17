/**
 * Copyright 2015 Tampere University of Technology, Pori Department
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

"use strict";

define(['i18n!app/nls/translations', 'jquery'], function(Translations) {
	
	var units = ['kWh', 'm^3', 'MWh'];
	var unitPrice = [0.0449+0.0286, 1.17+1.75, 45.32];
	
	function getPrice(unit){
		return unitPrice[units.indexOf(unit)];
	}
	
	function getEuroDay(series, seriesIndex, unit){
		var price = getPrice(unit);
		var milliSecondsInDay = 86400000;
		if(seriesIndex > 0 && price !== undefined){
			//var increase = series.data[seriesIndex][1] - series.data[seriesIndex-1][1];
			//return Number(increase*price).toFixed(2);
			var currentTime = series.data[seriesIndex][0];
			var lastDayTime = currentTime-milliSecondsInDay;
			var lastDayIndex = seriesIndex-1;
			while(lastDayIndex > 0 && series.data[lastDayIndex][0] > lastDayTime){
				--lastDayIndex;
			}
			var increase = series.data[seriesIndex][1] - series.data[lastDayIndex][1];
			var timeFactor = (series.data[seriesIndex][0] - series.data[lastDayIndex][0])/milliSecondsInDay;
			return Number(increase*price/timeFactor).toFixed(2);
		}
		//otherwise return
		return;
	}
	
	function getEuroWeek(series, seriesIndex, unit){
		var price = getPrice(unit);
		var milliSecondsInWeek = 604800000;	//86400000*7
		if(seriesIndex > 0 && price !== undefined){
			var currentTime = series.data[seriesIndex][0];
			var lastWeekTime = currentTime-milliSecondsInWeek;
			var lastWeekIndex = seriesIndex-1;
			while(lastWeekIndex > 0 && series.data[lastWeekIndex][0] > lastWeekTime){
				--lastWeekIndex;
			}
			var increase = series.data[seriesIndex][1] - series.data[lastWeekIndex][1];
			var timeFactor = (series.data[seriesIndex][0] - series.data[lastWeekIndex][0])/milliSecondsInWeek;
			return Number(increase*price/timeFactor).toFixed(2);
		}

		//otherwise
		return;
	}
	
	function getEuroMonth(series, seriesIndex, unit){
		var price = getPrice(unit);
		var milliSecondsInMonth = 2592000000;	//86400000*30
		if(seriesIndex > 0 && price !== undefined){
			var currentTime = series.data[seriesIndex][0];
			var lastMonthTime = currentTime-milliSecondsInMonth;
			var lastMonthIndex = seriesIndex-1;
			while(lastMonthIndex > 0 && series.data[lastMonthIndex][0] > lastMonthTime){
				--lastMonthIndex;
			}
			var increase = series.data[seriesIndex][1] - series.data[lastMonthIndex][1];
			var timeFactor = (series.data[seriesIndex][0] - series.data[lastMonthIndex][0])/milliSecondsInMonth;
			return Number(increase*price/timeFactor).toFixed(2);
		}
		
		//otherwise
		return;
	}
	
	function getEuroTotal(series, seriesIndex, unit){
		var price = getPrice(unit);
		if(seriesIndex > 0 && price !== undefined){
			var increase = series.data[seriesIndex][1] - series.data[0][1];
			return Number(increase*price).toFixed(2);
		}

		return;
	}
	
	return{ 
		getEuroDay: getEuroDay,
		getEuroWeek: getEuroWeek,
		getEuroMonth: getEuroMonth,
		getEuroTotal: getEuroTotal
	};
});
