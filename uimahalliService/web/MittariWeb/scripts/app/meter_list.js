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

define(['jquery', 'definitions', "i18n!app/nls/translations"], function(jQ, definitions, Translations) {

	var container = null;
	
	var initialize = function(containerElement){
		if(containerElement == undefined || containerElement == null){
			return;
		}
		container = containerElement;
		var params = "?data_groups=gauges";
		var dataInterface = definitions.uriRestInterface + definitions.methodMeasurementInterface + params;
	
		$.ajax({
			cache: false,	//try to bypass the cache
			dataType: 'json',
			url: dataInterface, 
			success: function(data) {
				meterBuilder(data.meters);
			}
		});
	};
	
	/**
	 * Create meter+gauge listing
	 * @param {Object} meterList
	 */
	function meterBuilder(meterList){
		if(meterList == undefined){
			return;
		}
		container.empty();	//clear the container of old elements
		for(var i=0; i<meterList.length; ++i){
			var meter = meterList[i];
			var meterDiv = document.createElement('div');
			meterDiv.id = "meter_"+meter.id;
			meterDiv.className = 'meter';
			
			var meterTitle = document.createElement('div');
			meterTitle.className = 'title';
			var meterLink = document.createElement('a');
			meterLink.setAttribute('href', '#'+meter.id);
			meterLink.textContent = meter.name;
			meterLink.onclick = showCharts;
			meterTitle.appendChild(meterLink);
			meterDiv.appendChild(meterTitle);
			
			var gaugesDiv = document.createElement('div');
			gaugesDiv.className = 'gauges';
			if(meter.gauges != undefined && meter.gauges.length > 0){
				var gaugeList = meter.gauges;
				for(var k=0; k<gaugeList.length; ++k){
					var gauge = gaugeList[k];
					if(gauge.dataType == "STRING"){
						continue;
					}
					var gaugeDiv = document.createElement('div');
					gaugeDiv.className = 'gauge';
					var gaugeTitle = document.createElement('div');
					gaugeTitle.className = 'title';
					gaugeTitle.textContent = gauge.name;
					gaugeDiv.appendChild(gaugeTitle);
					var gaugeDescription = document.createElement('div');					
					gaugeDescription.className = 'description';
					gaugeDescription.textContent = gauge.description;
					gaugeDiv.appendChild(gaugeDescription);
					
					//populate element with rest of the details
					var gaugeTable = document.createElement('div');
					gaugeTable.className = "table";
					gaugeDiv.appendChild(gaugeTable);
					gaugesDiv.appendChild(gaugeDiv);
					for (var prop in gauge) {
						var element = null;
						//create element containing message
						var messageElement = document.createElement('span');
						var contentElement = document.createElement('span');
						messageElement.className = "cell";
						contentElement.className = "cell";
						switch(prop){
							case "dataType":
								break;
							case "id":
								//TODO make link to the flotter also
								break;
							case "index":
								break;
							case "max":
								break;
							case "maxIncrease":
								break;
							case "min":
								break;
							case "minIncrease":
								break;
							case "option":
								break;
							case "unit":
								element = document.createElement('div');
								contentElement.textContent = gauge[prop];
								messageElement.textContent = Translations.TextUnit;
								break;
							case "cumulative":
								/*
								element = document.createElement('div');
								contentElement = document.createElement('input');
								contentElement.setAttribute('type', 'checkbox');
								contentElement.setAttribute('disabled', 'disabled');
								contentElement.className = "cell";
								if(gauge[prop] === true){
									contentElement.setAttribute('checked','checked');
								}
								messageElement.textContent = Translations.TextCumulative;
								break;
								*/
							default:
								break;
						}
						if(element != null){
							element.className = "table-row";
							element.appendChild(messageElement);
							element.appendChild(contentElement);
							gaugeTable.appendChild(element);
						}
					}
				}
				meterDiv.appendChild(gaugesDiv);
			}
			container.append(meterDiv);
		}
	}
	
	function showCharts(event){
		var meterId = event.target.href.substring(event.target.href.indexOf('#')+1);
		var chartDivId = "meter_charts_"+meterId;

		require(['jquery', 'app/flotter', 'flot'], function(jq, Flotter) {
			var flotChart = null;
			if($("#"+chartDivId).length < 1){
				flotChart = document.createElement("div");
				flotChart.id = chartDivId;
				flotChart.className = "meter-charts";
			}else{
				flotChart = $("#"+chartDivId);
				flotChart.empty();
			}
			$("#meter_"+meterId).append(flotChart);
			var flotter = new Flotter($(flotChart), meterId);
			flotter.initialize();
		});
		event.preventDefault();
		return false;
	}

	//returns the public functions (require.js stuff)
	return {
		initialize: initialize
	};

});
