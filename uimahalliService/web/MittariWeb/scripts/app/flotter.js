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

define(['i18n!app/nls/translations', 'definitions', 'app/statistics', 'jquery', 'flot', 'flot_time', 'flot_navigate', 'flot_crosshair', 'flot_tooltip', 'flot_selection'], function(Translations, definitions, statistics) {

	//members
	Flotter.prototype.container = null;
	Flotter.prototype.tagId = null;
	Flotter.prototype.chart = null;
	Flotter.prototype.chartOverview = null;
	Flotter.prototype.pointDetails = null;
	Flotter.prototype.plot = null;
	Flotter.prototype.plotChangeOverTime = null;
	Flotter.prototype.plotOverview = null;
	Flotter.prototype.previousPoint = null;
	Flotter.prototype.legendUpdatedTimestamp = 0;
	Flotter.prototype.meters = null;	//retrieved meters
	
	//options
	Flotter.prototype.tooltipsOn = true;
	Flotter.prototype.daysBefore = 86400000 * 365 * 2;
	
	// Start with the constructor
	function Flotter(flotContainerElement, tagId) {
		//members
		this.container = flotContainerElement;
		this.tagId = tagId;
	}
	
	/**
	 * Getter function for meters
	 * @return {Object} meters
	 */
	Flotter.prototype.getMeters = function(){
		return this.meters;
	};
    
	/**
	 * initializing function
	 */
    Flotter.prototype.initialize = function(){
		if(this.container == undefined || this.container == null){
			return;
		}
		var params = "?data_groups=all";
		if(this.tagId != undefined && this.tagId != null && this.tagId != ""){
			params += "&tag_id="+this.tagId;
		}
		this.chart = $(document.createElement('div'));
		this.chart.addClass('chart-area');
		this.chart[0].id = "chart_"+Math.random().toString(36).substring(2);
		this.container.append(this.chart);	//create chart placeholder
		//creates container for the overview chart
		this.chartOverview = $(document.createElement('div')); 
		this.chartOverview.addClass('chart-overview-area');
		this.container.append(this.chartOverview);
		this.pointDetails = $(document.createElement('div'));
		this.pointDetails.addClass('table chart-point-details');
		this.pointDetails.append('<div class="table-header">'+
				'<span class="cell">'+Translations.TextMeterName+'</span>'+
				'<span class="cell">'+Translations.TextDate+'</span>'+
				'<span class="cell">'+Translations.TextValue+'</span>'+
				'<span class="cell">'+Translations.TextUnit+'</span>'+
				'<span class="cell">'+Translations.TextEuro+'</span>'+
				'<span class="cell">'+Translations.TextEuroPerWeek+'</span>'+
				'<span class="cell">'+Translations.TextEuroPerMonth+'</span>'+
				'</div>');
		this.container.append(this.pointDetails);
		this.chart.bind("plothover", this.plotHoverHandler.bind(this));
	
		$.ajax({
			cache: false,	//try to bypass the cache
			dataType: 'json',
			url: definitions.uriRestInterface + definitions.methodMeasurementInterface + params, 
			success: this.chartDataHandler.bind(this)
		});
	};
	
	Flotter.prototype.chartDataHandler = function(data){
		this.meters = data.meters;
		var series = this.seriesBuilder(data.meters);
		var yaxesRanges = [];
		var minX = null;
		var maxX = null;
		for(var i=0; i<series.length; ++i){
			var axisLabelPosition = i%2 == 0 ? "left" : "right";	//alternate every second axis label between left and right
			//set minimum zoom range (i.e. how close you can zoom) together with hard limits on panning
			yaxesRanges.push({ panRange: [series[i].minY < 0 ? series[i].minY*1.5 : 0, series[i].maxY.toPrecision(2)*1.5],
				position: axisLabelPosition});
			if(minX == null){
				minX = Number(series[i].minX);
			}else{
				minX = Math.min(minX, series[i].minX);
			}
			if(maxX == null){
				maxX = Number(series[i].maxX);
			}else{
				maxX = Math.max(maxX, series[i].maxX);
			}
		}
		
		//generate gauge point detail below the main chart
		for(var j=0; j<this.meters.length; ++j){
			var meter = this.meters[j];
			for(var k=0; k<meter.gauges.length; ++k){
				var gauge = meter.gauges[k];
				
				var gaugeTabularDataLink = document.createElement('a');
				gaugeTabularDataLink.setAttribute('href', '#'+gauge.id);
				gaugeTabularDataLink.setAttribute('target', '_blank');
				gaugeTabularDataLink.textContent = gauge.name;
				gaugeTabularDataLink.className = "cell";
				gaugeTabularDataLink.onclick = this.gaugeTabularDataRequestHandler.bind(this);
				
				var tRow = document.createElement("div");
				tRow.className = "table-row";
				this.pointDetails.append(tRow);
				
				if(gauge.dataType === "STRING"){	//show STRING "comments" only if there are gaugevalues available, also generate an link to the "table data"
					if(gauge.gaugeValues !== undefined){
						var lastDate = parseDateFromISO8601(gauge.gaugeValues[gauge.gaugeValues.length-1].updated);
						var lastValue = gauge.gaugeValues[gauge.gaugeValues.length-1].value;
						$(tRow).append(gaugeTabularDataLink);
						$(tRow).append('<span class="cell chart-date">'+new Date(lastDate).toLocaleString()+'</span>'+
								'<span class="cell chart-value">'+lastValue+'</span>');
					}
				}else{
					$(tRow).append(gaugeTabularDataLink);
					$(tRow).append('<span class="cell chart-date"></span>'+
						'<span class="cell chart-value"></span>'+
						'<span class="cell">'+gauge.unit+'</span>'+
						'<span class="cell chart-euro-day"></span>'+
						'<span class="cell chart-euro-week"></span>'+
						'<span class="cell chart-euro-month"></span>');
				}
			}
		}
		
		var options = {
			//series:{ lines: { show: true }, points: { show: false } },
			//604800000 is 7 days in milliseconds
			xaxis: { mode: "time", timezone: "browser", 
				zoomRange: [604800000, null], panRange: [minX-604800000, maxX+604800000] },
			yaxes: yaxesRanges,
			zoom: {	interactive: true },
			pan: { interactive: true },
			crosshair: { mode: "x" },
			grid: {	hoverable: true, clickable: false, autoHighlight: false },
			legend: { position: "nw", backgroundColor: null },
	        tooltip: this.tooltipsOn,
	        tooltipOpts: {
	          content: Translations.ToolTipFormat,
	          xDateFormat: Translations.ToolTipDateFormat
	        }
		};
		this.plot = $.plot(this.chart, series, options);
		
		// Create the overview plot
		this.plotOverview = $.plot(this.chartOverview, series, {
			legend: { show: false },
			series: {
				lines: { show: true },
				shadowSize: 0
			},
			yaxes: yaxesRanges,
			xaxis: { mode: "time", timezone: "browser" },
			selection: { mode: "x" }
		});
		//bind plotselected event to rangeSelectHandler
		$(this.chartOverview).bind("plotselected", this.rangeSelectHandler.bind(this));
		$('body').scrollTop($('#meter_'+this.tagId).offset().top);	//scroll to top of the container
		
		//set default view port [Current-2 years -- Current]
		this.rangeSelectHandler(null, { xaxis: { from: Math.max(maxX-this.daysBefore, minX)-604800000, to: maxX+604800000 }});
			//yaxes: [{ from: this.tempMin, to: this.tempMax }, { from: this.rhMin, to: this.rhMax }]});
		//TODO FIXME giving a strange amount of yaxes will break the rangeSelectHandler
	};
	
	/**
	 * Handle clicks of tabular data requests and offer to save data as .CSV file (might be broken, or non conforming to CSV format)
	 * @param {Event} event
	 * @return {Boolean} handler state
	 */
	Flotter.prototype.gaugeTabularDataRequestHandler = function(event){
		var gaugeId = event.target.href.substring(event.target.href.indexOf('#')+1);
		for(var j=0; j<this.meters.length; ++j){
			var meter = this.meters[j];
			for(var k=0; k<meter.gauges.length; ++k){
				var gauge = meter.gauges[k];
				if(gaugeId === gauge.id){
					if(gauge.gaugeValues !== undefined){
						var csvContent = Translations.TextMeterName+";"+meter.name+";"+gauge.name+"\n";
						csvContent += Translations.TextDate+";"+Translations.TextValue+"\n";
						var gaugeValues = gauge.gaugeValues;
						for(var i=gaugeValues.length-1; i>=0; --i){	//begin from the latest value
							var datetime = gaugeValues[i].updated
								.replace('T', ' ').substring(0,19);
							csvContent += datetime+";"+gaugeValues[i].value+"\n";
						}
						var fileName = meter.name+"_"+gauge.name+"_"+new Date().valueOf()+".csv";
						var blob = new Blob(["\ufeff", csvContent], { type: 'text/csv;charset=utf-8;' });	//fix utf-8 issue (http://stackoverflow.com/questions/23816005/anchor-tag-download-attribute-not-working-bug-in-chrome-35-0-1916-114)
						if(event.target.download !== undefined){	//not IE	
							var csvUrl = URL.createObjectURL(blob);
							event.target.setAttribute('href', csvUrl);
							event.target.setAttribute('download', fileName);
							return true;
						}else if(navigator.msSaveOrOpenBlob !== undefined){	//IE	e.g. wizardy from http://stackoverflow.com/questions/14964035/how-to-export-javascript-array-info-to-csv-on-client-side
							navigator.msSaveOrOpenBlob(blob, fileName);
							event.preventDefault();
							return false;
						}else{	
							console.log("not supported");
							event.preventDefault();
							return false;
						}
					}
				}
			}
		}
	};
	
	/**
	 * Create chart series for flot chart
	 * @param {Object} meterList
	 * @return {Array} array of data sets
	 */
	Flotter.prototype.seriesBuilder = function(meterList){
		var series = [];
		var chartIndex = 1;
		for(var i=0; i<meterList.length; ++i){
			var gauges = meterList[i].gauges;
			for(var k=0; k<gauges.length; ++k){
				var gauge = gauges[k];
				if(gauge.dataType === "STRING"){
					continue;
				}
				var dataset = this.datasetBuilder(gauge, chartIndex);
				if(dataset != null){
					series.push(dataset);
					++chartIndex;
				}
			}
		}		
		return series;
	};
	
	/**
	 * Create suitable dataset for flot charts from the given gauge data
	 * @param {Object} gauge
	 * @param {Number} chartIndex
	 * @return {Object} the built data set 
	 */
	Flotter.prototype.datasetBuilder = function(gauge, chartIndex){
		if(gauge == undefined || gauge == null || gauge.gaugeValues == undefined || gauge.gaugeValues == null){
			return null;
		}
		var itemCount = gauge.gaugeValues.length;
		if(itemCount < 1){
			return null;
		}
		//set some options for the serie
		var dataSet = { lines:{ show: false }, 
				points:{ show: true, radius: 3, fill: false }, 
				shadowSize: null, 
				label: gauge.name, 
				data: [], 
				yaxis: chartIndex, 
				unit: gauge.unit };
		var detailedDataSet = { lines:{ show: false }, bars:{ show: true }, label: gauge.name, data: []};
		var previousPoint = null;
		
		//put the first point to the data set list
		var firstValue = gauge.gaugeValues[0];
		var firstPoint = [parseDateFromISO8601(firstValue.updated), Number(firstValue.value)];
		dataSet.minX = Number(firstPoint[0]);
		dataSet.maxX = Number(firstPoint[0]);
		dataSet.minY = Number(firstPoint[1]);
		dataSet.maxY = Number(firstPoint[1]);
		dataSet.data.push(firstPoint);
		detailedDataSet.data.push([firstPoint[0],null]);	//just put null as first entry (no changes to earlier)
		previousPoint = firstPoint;
		
		for(var i=1; i < itemCount; ++i){	//starting from the second because first is already inserted
			var value = gauge.gaugeValues[i];
			var point = [parseDateFromISO8601(value.updated), Number(value.value)];
			dataSet.data.push(point);
			
			//check for min&max values
			dataSet.minX = Math.min(dataSet.minX, point[0]);
			dataSet.maxX = Math.max(dataSet.maxX, point[0]);
			dataSet.minY = Math.min(dataSet.minY, point[1]);
			dataSet.maxY = Math.max(dataSet.maxY, point[1]);
			
			if(gauge.dataType !== "STRING"){
				var differenceBetweenDataPoints = (point[1] - previousPoint[1]);
				if(gauge.cumulative){
					var timeBetweenDataPoints = (point[0]-previousPoint[0]) / 86400000;		//in days
					detailedDataSet.data.push([point[0], differenceBetweenDataPoints/timeBetweenDataPoints]);
				}else{
					detailedDataSet.data.push([point[0], differenceBetweenDataPoints]);
				}
			}
			previousPoint = point;
		}
		
		if(gauge.dataType !== "STRING"){
			detailedDataSet.minX = dataSet.minX;
			detailedDataSet.maxX = dataSet.maxX;
			this.drawChart(gauge, detailedDataSet);
		}
		return dataSet;
	};
	
	Flotter.prototype.drawChart = function(gauge, dataset){
		if(dataset == undefined || dataset == null){
			return;
		}
		var detailChart = $(document.createElement('div'));
		detailChart.addClass('chart-area');
		this.container.append(detailChart);
		var options = {
			points:{ show: true, radius: 3, fill: false }, 
			xaxis: { mode: "time", timezone: "browser", panRange: [dataset.minX-2592000000, dataset.maxX+2592000000] },
			yaxis: { panRange: [gauge.min, null]},
			zoom: {	interactive: true },
			pan: { interactive: true },
			bars: {
		        show: true,
		        barWidth : 1800000 //width of a bar is 30min
		    },
			grid: {	hoverable: true, clickable: false, autoHighlight: false },
	        tooltip: this.tooltipsOn,
	        tooltipOpts: {
	          content: Translations.ToolTipFormat,
	          xDateFormat: Translations.ToolTipDateFormat
	        }};
		this.plotChangeOverTime = $.plot(detailChart, [dataset], options);
		detailChart.bind("plothover", this.plotHoverHandler.bind(this));
	};
	
	Flotter.prototype.rangeSelectHandler = function (event, ranges) {
		// do the zooming for main plot
		$.each(this.plot.getXAxes(), function(_, axis) {
			axis.options.min = ranges.xaxis.from;
			axis.options.max = ranges.xaxis.to;
		});		
		//zooming for the calculated daily changes plot
		if(this.plotChangeOverTime){
			$.each(this.plotChangeOverTime.getXAxes(), function(_, axis) {
				axis.options.min = ranges.xaxis.from;
				axis.options.max = ranges.xaxis.to;
			});
		}
		if(ranges.yaxes){
			var i=0;
			$.each(this.plot.getYAxes(), function(_, axis) {
				axis.options.min = ranges.yaxes[i].from;
				axis.options.max = ranges.yaxes[i].to;
				++i;
			});
			if(this.plotChangeOverTime){
				i=0;
				$.each(this.plotChangeOverTime.getYAxes(), function(_, axis) {
					axis.options.min = ranges.yaxes[i].from;
					axis.options.max = ranges.yaxes[i].to;
					++i;
				});
			}
		}
		this.plot.setupGrid();
		this.plot.draw();
		if(this.plotChangeOverTime){
			this.plotChangeOverTime.setupGrid();
			this.plotChangeOverTime.draw();
		}
	};
	
	Flotter.prototype.plotHoverHandler = function(event, pos, item) {
		//if(event.target.id === this.chart[0].id && event.timeStamp > this.legendUpdatedTimestamp) {
		if(event.timeStamp > this.legendUpdatedTimestamp) {
			this.updateLegend(pos);
			this.legendUpdatedTimestamp = event.timeStamp+100;	//wait for 100 milliseconds before doing anything again
		}
	};
	
	/**
	 * Function for plothover event. Updates the legend with currently targeted data point(s).
	 * @param {Number} plotPosition
	 */
	Flotter.prototype.updateLegend = function(plotPosition) {
		var pos = plotPosition;
		var axes = this.plot.getAxes();
//		if (pos.x < axes.xaxis.min || pos.x > axes.xaxis.max || pos.y < axes.yaxis.min || pos.y > axes.yaxis.max) {
		if (pos.x >= axes.xaxis.min && pos.x <= axes.xaxis.max) {
			var i, dataset = this.plot.getData();
			var legendDates = this.pointDetails.find('.chart-date');
			var legendValues = this.pointDetails.find('.chart-value');
			var legendEuros = this.pointDetails.find('.chart-euro-day');
			var legendEuroWeeks = this.pointDetails.find('.chart-euro-week');
			var legendEuroMonth= this.pointDetails.find('.chart-euro-month');
			for (i = 0; i < dataset.length; ++i) {
				var series = dataset[i];
				var legendData = legendLabelFormatter(series.label, series, pos);
				if(legendData){
					legendDates.eq(i).text(new Date(legendData.date).toLocaleString());
					legendValues.eq(i).text(legendData.value);
					legendEuros.eq(i).text(statistics.getEuroDay(series, legendData.index, legendData.unit));
					legendEuroWeeks.eq(i).text(statistics.getEuroWeek(series, legendData.index, legendData.unit));
					legendEuroMonth.eq(i).text(statistics.getEuroMonth(series, legendData.index, legendData.unit));
				}
			}
		}
	};
	
	/**
	 * Function to format labels seen in the legend labels
	 * @param {String} label
	 * @param {Object} series
	 * @param {Number} pos
	 * @return {Object} formatted label
	 */
	function legendLabelFormatter(label, series, pos){
		if(pos == undefined){
			return null;
		}
		
		var count = series.data.length-1;
		var imax = count;
		var imin = 0;
		var key = 0;
		// binary search for looking up the nearest point
		while(true){
			if(imax-imin < 32){	//break if difference is less than 32 indexes
				key = imin;
				break;
			}
			// calculate the midpoint for roughly equal partition
			var imid = Math.floor((imax-imin)/2)+imin;
			if (series.data[imid][0] < pos.x){
				// change min index to search upper subarray
				imin = imid+1;
			}else{        
				// change max index to search lower subarray
				imax = imid-1;
			}
		}
		var j = key;
		// Find the nearest points, x-wise
		for (j; j < imax; ++j) {
			if (series.data[j][0] >= pos.x) {
				break;
			}
		}
		var y = series.data[j];
		
		return { label: label, value: y[1], date: y[0], unit: series.unit, index: j };
	}

	/**
	 * Function to parse dates from e.g. "2012-01-02T12:00:00+0200" to 1325498400
	 * @param {String} isoDateString date string to parse
	 * @return {Number} the unix time 
	 */
	function parseDateFromISO8601(isoDateString) {
		var parts = isoDateString.match(/\d+/g);
		var isoTime = Date.UTC(parts[0], parts[1] - 1, parts[2], parts[3], parts[4], parts[5]);
		if(parts.length > 6){
			var hourOffset = Number(parts[6].substring(0,2));
			var minOffset = Number(parts[6].substring(2,4));
			var offset = (hourOffset + minOffset/60)*3600000;
			if(isoDateString.substring(19,20) === "+"){
				isoTime = isoTime -	offset;		
			}else if(isoDateString.substring(19,20) === "-"){
				isoTime = isoTime + offset;
			}
		}
		return isoTime;
	}
	
	//returns the public functions (require.js stuff)
	return Flotter;

});
