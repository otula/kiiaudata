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
package com.otula.mittausclient;

import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.SeriesSelection;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.tools.PanListener;
import org.achartengine.tools.ZoomEvent;
import org.achartengine.tools.ZoomListener;

import com.otula.datatypes.Definitions;
import com.otula.datatypes.Gauge;
import com.otula.datatypes.GaugeValue;
import com.otula.datatypes.Meter;
import com.otula.datatypes.Meters;
import com.otula.datatypes.Settings;
import com.otula.uiutils.DialogUtils;
import com.otula.uiutils.TextHistoryAdapter;
import com.otula.uiutils.DialogUtils.DialogListener;
import com.otula.uiutils.GaugeValueDialog;
import com.otula.utils.DateUtils;
import com.otula.utils.LogUtils;
import com.otula.utils.MeterDBHelper;
import com.otula.utils.MeterDBHelper.DataFilter;

import android.os.AsyncTask;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.graphics.Point;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils.TruncateAt;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This activity is for showing charts of the collected data on device.<br/>
 *  
 * uses the achartengine from google code:
 * https://code.google.com/p/achartengine/
 * 
 * 
 * the code is a heavily modified version of:
 * http://code.google.com/p/achartengine/source/browse/trunk/achartengine/demo/org/achartengine/chartdemo/demo/chart/XYChartBuilder.java
 *
 */
public class GraphActivity extends FragmentActivity implements OnClickListener, DialogListener, GaugeValueDialog.DialogListener {
	public static final String EXTRA_METER_ID = "meter_id";
	public static final String EXTRA_GAUGE_ID = "gauge_id";
	private static final String CLASS_NAME = GraphActivity.class.toString();
	private static final int TAG_SELECT_METER = 1;
	private static final int GAUGE_SELECTION_COUNT = 3;
	private static final double AXIS_RANGE_OFFSET = 0.10;	// how much axis range (min to max) will be more than the actual min/max values, in percentage
	private static final int X_AXIS_LABEL_COUNT = 5;	// number of labels to be shown on x axis
	private static final double POINT_SELECTION_RADIUS = 0.30;	// how far from the user can click from a point for the event to register, as a % of screen width
	private MeterDBHelper _dbHelper = null;
	private XYMultipleSeriesDataset _dataset = null;	// container for datasets
	private XYMultipleSeriesRenderer _renderer = null;	// chart renderer
	private GraphicalView _viewChart = null;
	private HashMap<Meter, String> _meterDetails = null;	// meter, name map for meters
	private LinearLayout _layoutGaugeSelector = null;
	private Meter _currentMeter = null;
	private Gauge _currentGauge = null;
	private List<Gauge> _gaugeDetails = null; // for currently active meter
	private int _gaugeLabelWidth = 1;
	private HorizontalScrollView _scrollerGauge = null;
	private TextView _labelSelectedMeter = null;
	private RelativeLayout _layoutGaugeSelectorContainer = null;
	private LinearLayout _layoutGraphContainer = null;
	private ListView _listTextHistoryContainer = null;
	private double _currentYMax = 1;
	private double _currentYMin = 0;
	private long _currentXMax = 1;
	private long _currentXMin = 0;
	private Settings _settings = null;
	private XYSeries _minLimit = null;
	private XYSeries _maxLimit = null;
	private XYSeriesRenderer _minLimitRenderer = null;
	private XYSeriesRenderer _maxLimitRenderer = null;
	private PanListener _panListener = null;
	private ZoomListener _zoomListener = null;
	private GaugeValue _currentValue = null;
	private TextHistoryAdapter _textHistoryAdapter = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_graph);

		findViewById(R.id.graph_layout_select_meter).setOnClickListener(this);
		_labelSelectedMeter = (TextView) findViewById(R.id.graph_label_selected_meter);
		_layoutGaugeSelector = (LinearLayout) findViewById(R.id.graph_layout_gauge_selector);
		_layoutGaugeSelectorContainer = (RelativeLayout) findViewById(R.id.graph_layout_gauge_selector_container);

		_dataset = new XYMultipleSeriesDataset();
		_renderer = new XYMultipleSeriesRenderer();
		_renderer.setApplyBackgroundColor(true);
		_renderer.setBackgroundColor(getResources().getColor(R.color.color_coral_sea));
		_renderer.setMarginsColor(getResources().getColor(R.color.color_blue_sea));
		_renderer.setLabelsColor(getResources().getColor(R.color.color_deep_sea));
		_renderer.setAxesColor(getResources().getColor(R.color.color_deep_sea));
		_renderer.setXLabelsColor(Color.BLACK);
		_renderer.setYLabelsColor(0,Color.BLACK);
		_renderer.setAxisTitleTextSize(24);
		_renderer.setLabelsTextSize(16);
		_renderer.setMargins(new int[] { 15, 35, 35, 35 });
		_renderer.setZoomButtonsVisible(false);	//hide zoom buttons
		_renderer.setPointSize(5);
		_renderer.setPanEnabled(true);
		_renderer.setZoomEnabled(true);
		_renderer.setXTitle("\n"+getString(R.string.graph_label_x_axis));
		_renderer.setXLabels(0); // disable default labels on x axis
		_renderer.setYLabelsAlign(Align.LEFT); // put the Y labels on the left of the axis
		_renderer.setShowLegend(false);	//hide legends
		//show major grid lines for Y-axis
		_renderer.setShowGridY(true);
		_renderer.setShowGrid(true);
		_renderer.setGridColor(Color.BLACK, 0);
		
		_viewChart = ChartFactory.getLineChartView(this, _dataset, _renderer);
		_viewChart.setOnTouchListener(new ChartTouchListener());	// note: we cannot use default onclick or onlongclick listeners, these will mess up with the chart pan&zoom
		
		_listTextHistoryContainer = (ListView) findViewById(R.id.graph_layout_text_history_container);
		_listTextHistoryContainer.setAdapter((_textHistoryAdapter = new TextHistoryAdapter(this)));
		_layoutGraphContainer  = (LinearLayout) findViewById(R.id.graph_layout_chart_container);
		_layoutGraphContainer.addView(_viewChart, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		_scrollerGauge = (HorizontalScrollView)findViewById(R.id.graph_scroller_gauge_selector);
		_scrollerGauge.setOnTouchListener(new View.OnTouchListener() {	
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				int x = _scrollerGauge.getScrollX()+_gaugeLabelWidth;
				int childCount = _layoutGaugeSelector.getChildCount();
				View nearest = null;
				int nearestDistance = 0;
				for(int i=0;i<childCount;++i){
					View w = _layoutGaugeSelector.getChildAt(i);
					int center = (int) w.getX();
					int distance = Math.abs(center-x);
					if(w.getTag() == null){	// ignore empty textedits
						continue;
					}else if(nearest == null){
						nearest = w;
						nearestDistance = distance;
					}else if(distance < nearestDistance){
						nearestDistance = distance;
						nearest = w;
					}
				}
				if(nearest != null){
					initializeLayout(_currentMeter.getId(), ((Gauge)nearest.getTag()).getId());
					if(event.getAction() == MotionEvent.ACTION_UP){
						centerGaugeSelectorTo(_currentGauge);	// scroll the scroller to correct spot manually
						return true;	// catch the event to prevent scrolling functionality
					}
				}
				return false;	// let the scroller scroll naturall
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();

		Point size = new Point();
		getWindowManager().getDefaultDisplay().getSize(size);	//get the screen width in pixels
		int margins = getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin) * 2;	//get margin size in pixes times two
		_gaugeLabelWidth = (size.x - margins) / GAUGE_SELECTION_COUNT;	//calculate correct width for gauge labels
		_renderer.setSelectableBuffer((int) (size.x*POINT_SELECTION_RADIUS));	// how close to the chart points user must click for the event to register
		
		_settings = new Settings(this);
		if(_settings.isValid()){
			_dbHelper = MeterDBHelper.getHelper(this, _settings);
			initializeMeterDetails();
			Intent i = getIntent();
			initializeLayout(i.getStringExtra(EXTRA_METER_ID), i.getStringExtra(EXTRA_GAUGE_ID));
			centerGaugeSelectorTo(_currentGauge);
			Toast.makeText(this, R.string.graph_help, Toast.LENGTH_SHORT).show();
			
			if(_zoomListener != null){
				_viewChart.removeZoomListener(_zoomListener);
			}
			if(_panListener != null){
				_viewChart.removePanListener(_panListener);
			}
			
			if(_settings.isShowGraphLimits()){	// show max & min as horizontal line
				setMinimumAndMaximumLines();
				_viewChart.addPanListener((_panListener = new PanListener() {
					@Override
					public void panApplied() {
						setMinimumAndMaximumLines();
					}
				}));
				_viewChart.addZoomListener((_zoomListener = new ZoomListener() {				
					@Override
					public void zoomReset() {
						setMinimumAndMaximumLines();
					}
					
					@Override
					public void zoomApplied(ZoomEvent event) {
						setMinimumAndMaximumLines();
					}
				}), true, true);
			}else{
				LogUtils.debug(CLASS_NAME, "onResume", "Drawing minimum and maximum limits is disabled.");
			}
		}else{
			Toast.makeText(this, R.string.error_invalid_settings, Toast.LENGTH_SHORT).show();
			finish();
		}
	}

	@Override
	protected void onPause() {
		if(_dbHelper != null){
			_dbHelper.closeDatabase();
			_dbHelper = null;
		}
		super.onPause();
	}

	/**
	 * 
	 */
	private void initializeMeterDetails(){
		Meters meters = _dbHelper.getMeters(null, EnumSet.of(DataFilter.BASE_DETAILS));
		if(Meters.isEmpty(meters)){
			Toast.makeText(this, R.string.graph_warning_no_meters, Toast.LENGTH_SHORT).show();
			_meterDetails = null;
		}else{
			List<Meter> meterList = meters.getMeters();
			int meterCount = meterList.size();
			_meterDetails = new HashMap<Meter, String>(meterCount);
			for(int i=0;i<meterCount;++i){
				Meter m = meterList.get(i);
				_meterDetails.put(m, m.getName());
			}
		}
	}

	/**
	 * Initializes gauges based on _currentMeter
	 */
	private void initializeGaugeDetails(){
		_gaugeDetails = null;
		_layoutGaugeSelector.removeAllViews();

		if(_currentMeter == null){
			_labelSelectedMeter.setText(R.string.graph_label_no_meter_selected);
			_layoutGaugeSelectorContainer.setVisibility(View.GONE);
			setTitle(R.string.title_activity_graph);
			return;
		}
		_gaugeDetails = _currentMeter.getGauges();
		if(_gaugeDetails == null){
			Toast.makeText(this, R.string.graph_warning_no_gauges, Toast.LENGTH_SHORT).show();
			_labelSelectedMeter.setText(R.string.graph_label_no_meter_selected);
			_layoutGaugeSelectorContainer.setVisibility(View.GONE);
		}else{
			int gaugeCount = _gaugeDetails.size();
			TextView t = new TextView(this);
			t.setWidth(_gaugeLabelWidth);
			_layoutGaugeSelector.addView(t);

			for(int i=0;i<gaugeCount;++i){
				Gauge g = _gaugeDetails.get(i);
				t = new TextView(this);
				t.setText(g.getName());
				t.setTag(g);
				t.setTextAppearance(this, R.style.text_medium);
				t.setWidth(_gaugeLabelWidth);
				t.setGravity(Gravity.CENTER);
				t.setEllipsize(TruncateAt.MIDDLE);
				t.setSingleLine(true);
				_layoutGaugeSelector.addView(t);
			}

			t = new TextView(this);
			t.setWidth(_gaugeLabelWidth);
			_layoutGaugeSelector.addView(t);

			_labelSelectedMeter.setText(_currentMeter.getName());
			_layoutGaugeSelectorContainer.setVisibility(View.VISIBLE);
		}
	}

	/**
	 * 
	 * @param gauge
	 */
	private void centerGaugeSelectorTo(Gauge gauge){
		if(gauge != null){
			for(int i=0,count=_layoutGaugeSelector.getChildCount();i<count;++i){
				View w = _layoutGaugeSelector.getChildAt(i);
				Object tag = w.getTag();
				if(tag != null && Gauge.areTheSame(gauge, (Gauge) tag)){
					final int index = i;
					_scrollerGauge.post(new Runnable() {	//magic of a higher level to scroll to correct place
				        @Override
				        public void run () {
				        	int gaugePositionX = (int)_layoutGaugeSelector.getChildAt(index-1).getX();	//index-1 because of spacer items in front of scroller
				        	_scrollerGauge.smoothScrollTo(gaugePositionX, 0);
				        }
				    });
					break;
				}
			}
		}
	}

	/**
	 * 
	 * @param meterId
	 * @param gaugeId
	 */
	private void initializeLayout(String meterId, String gaugeId){
		if(_meterDetails == null){
			LogUtils.debug(CLASS_NAME, "initializeLayout", "No meter details.");
			_layoutGraphContainer.setVisibility(View.GONE);
			_listTextHistoryContainer.setVisibility(View.GONE);
			_currentGauge = null;
			return;
		}

		if(_currentGauge != null && _currentGauge.getId().equals(gaugeId)){	// gaugeIds are unique, so no need to check for meterId
			return;
		}

		if(meterId == null){
			LogUtils.debug(CLASS_NAME, "initializeLayout", "No meterId given. Selecting first meter.");
			meterId = _meterDetails.keySet().iterator().next().getId();
		}	

		if(_currentMeter == null || !meterId.equals(_currentMeter.getId())){	// retrieve gauge details if meter has changed
			_currentMeter = _dbHelper.getMeters(Arrays.asList(meterId), EnumSet.of(DataFilter.BASE_DETAILS,DataFilter.GAUGES)).getMeters().get(0);	//this should _always_ return exactly one meter
			initializeGaugeDetails();
		}

		List<Gauge> gauges = _currentMeter.getGauges();
		_currentGauge = null;
		if(gauges == null){
			Toast.makeText(this, R.string.graph_warning_no_gauges, Toast.LENGTH_SHORT).show();
			_layoutGraphContainer.setVisibility(View.GONE);
			_listTextHistoryContainer.setVisibility(View.GONE);
			_currentGauge = null;
			return;
		}

		if(gaugeId == null){
			LogUtils.debug(CLASS_NAME, "initializeLayout", "No gaugeId given, selecting the first gauge of the meter.");
			_currentGauge = gauges.get(0);
		}else{
			for(Iterator<Gauge> iter = gauges.iterator();iter.hasNext();){
				Gauge g = iter.next();
				if(g.getId().equals(gaugeId)){
					_currentGauge = g;
					break;
				}
			}
		}

		if(_currentGauge == null){
			LogUtils.warn(CLASS_NAME, "initializeLayout", "No currentGauge.");
			_layoutGraphContainer.setVisibility(View.GONE);
			_listTextHistoryContainer.setVisibility(View.GONE);
			return;
		}

		setSelectedGauge(gauges.indexOf(_currentGauge));
		List<GaugeValue> values = _currentGauge.getValues();
		if(values == null){	// check if there are values set
			LogUtils.debug(CLASS_NAME, "initializeLayout", "Current gauge contains no values, checking database for value list.");
			_dbHelper.getGaugeValues(Arrays.asList(_currentGauge), EnumSet.of(DataFilter.GAUGE_VALUES));
		}
		values = _currentGauge.getValues();
		if(values == null){	// no values exist even after database check
			Toast.makeText(this, R.string.graph_notification_no_values, Toast.LENGTH_SHORT).show();
			_layoutGraphContainer.setVisibility(View.GONE);
			return;
		}
		
		switch(_currentGauge.getDataType()){
			case DOUBLE:
			case INTEGER:
				_listTextHistoryContainer.setVisibility(View.GONE);
				initializeGraph(values);
				break;
			case STRING:
				_layoutGraphContainer.setVisibility(View.GONE);
				initializeTextHistory(values);
				break;
			default:
				LogUtils.warn(CLASS_NAME, "initializeLayout", "Unhandeled datatype: "+_currentGauge.getDataType().name());
				_layoutGraphContainer.setVisibility(View.GONE);
				_listTextHistoryContainer.setVisibility(View.GONE);
				break;
		}
	}
	
	/**
	 * Sets "selection" borders around the currently selected gauge.
	 * @param index
	 */
	private void setSelectedGauge(int index){
		for(int i=0; i<_layoutGaugeSelector.getChildCount(); ++i){
			if(i-1 != index){
				_layoutGaugeSelector.getChildAt(i).setBackgroundDrawable(null);
			}else{
				_layoutGaugeSelector.getChildAt(i).setBackgroundResource(R.drawable.border);
			}
		}
		//set title "gauge::meter"
		StringBuffer sb = new StringBuffer();
		sb.append(_currentGauge.getName());
		sb.append("::");
		sb.append(_currentMeter.getName());
		setTitle(sb.toString());
	}
	
	/**
	 * 
	 * @param values not null and not empty list of values
	 */
	private void initializeTextHistory(List<GaugeValue> values){
		_textHistoryAdapter.setGaugeValues(values);
		_listTextHistoryContainer.setVisibility(View.VISIBLE);
	}
	
	/**
	 * 
	 * @param values not null and not empty list of values
	 */
	private void initializeGraph(List<GaugeValue> values){
		_dataset.clear();
		_renderer.removeAllRenderers();
		
		XYSeries currentSeries  = new XYSeries("");	//not null because it can't be null.
		int valueCount = values.size();
		GaugeValue axisBase = values.get(valueCount-1);	// get the last one
		double yAxisBaseValue = Double.valueOf(axisBase.getValue());
		double yMax = yAxisBaseValue;
		double yMin = yAxisBaseValue-1;
		long xAxisBaseValue = axisBase.getUpdatedTimestamp().getTime();
		long xMax = xAxisBaseValue;
		long xMin = xAxisBaseValue-1;
		long previousTime = 0;
		int index = valueCount-_settings.getMaxGraphPoints();	// take the last values of the list
		if(index < 0){
			index = 0;
		}
		while(index<valueCount){
			GaugeValue v = values.get(index++);
			long time = v.getUpdatedTimestamp().getTime();
			if(time == previousTime){	// identical x-values are not permitted in the set, if such exists, add one millisecond
				++time;			
			}
			if(time > xMax){
				xMax = time;
			}else if(time < xMin){
				xMin = time;
			}
			previousTime = time;
			
			double yValue = Double.valueOf(v.getValue());
			if(yValue > yMax){
				yMax = yValue;
			}else if(yValue < yMin){
				yMin = yValue;
			}
			currentSeries.add(time, yValue);
		}

		double yAxisOffset = Math.abs(yMin-yMax)*AXIS_RANGE_OFFSET;
		_currentYMax = yMax+yAxisOffset;
		_currentYMin = yMin-yAxisOffset;	
		_renderer.setYTitle(_currentGauge.getUnit());

		double xAxisOffset = Math.abs(xMin-xMax)*AXIS_RANGE_OFFSET;
		_currentXMax=(long) (xMax+xAxisOffset);
		_currentXMin=(long) (xMin-xAxisOffset);
		setXAxisLabels(xMin, xMax);
		
		_dataset.addSeries(currentSeries);
		XYSeriesRenderer currentRenderer = new XYSeriesRenderer();
		currentRenderer.setPointStyle(PointStyle.CIRCLE);
		currentRenderer.setColor(getResources().getColor(R.color.color_deep_sea));
		currentRenderer.setFillPoints(true);
		currentRenderer.setDisplayChartValues(true);
		currentRenderer.setDisplayChartValuesDistance(10);
		_renderer.addSeriesRenderer(currentRenderer);
		_layoutGraphContainer.setVisibility(View.VISIBLE);	// make sure the graph is visible
		
		centerChart();
		
		if(_settings.isShowGraphLimits()){	// show max & min as horizontal line
			setMinimumAndMaximumLines();
		}else{
			LogUtils.debug(CLASS_NAME, "initializeGraph", "Drawing minimum and maximum limits is disabled.");
		}
	}
	
	/**
	 * sets horizontal lines to the designated minimum and maximum limits
	 * 
	 */
	private void setMinimumAndMaximumLines(){
		Double min = _currentGauge.getMin();
		double xMax = _renderer.getXAxisMax();
		double xMin = _renderer.getXAxisMin();
		if(_minLimit != null){
			_renderer.removeSeriesRenderer(_minLimitRenderer);
			_dataset.removeSeries(_minLimit);
		}
		if(_maxLimit != null){
			_renderer.removeSeriesRenderer(_maxLimitRenderer);
			_dataset.removeSeries(_maxLimit);
		}
		if(min != null){
			_minLimit = new XYSeries(getString(R.string.graph_label_min_limit));	
			_minLimit.add(xMin, min);
			_minLimit.add(xMax, min);
			_dataset.addSeries(_minLimit);
			_minLimitRenderer = new XYSeriesRenderer();
			_minLimitRenderer.setFillPoints(false);
			_minLimitRenderer.setColor(Color.GREEN);
			_renderer.addSeriesRenderer(_minLimitRenderer);
		}else{
			LogUtils.debug(CLASS_NAME, "setMinimumAndMaximumLines", "No minimum limit for values.");
		}
		Double max = _currentGauge.getMax();
		if(max != null){
			_maxLimit = new XYSeries(getString(R.string.graph_label_max_limit));		
			_maxLimit.add(xMin, max);
			_maxLimit.add(xMax, max);
			_dataset.addSeries(_maxLimit);
			_maxLimitRenderer = new XYSeriesRenderer();
			_maxLimitRenderer.setFillPoints(false);
			_maxLimitRenderer.setColor(Color.RED);
			_renderer.addSeriesRenderer(_maxLimitRenderer);
		}else{
			LogUtils.debug(CLASS_NAME, "setMinimumAndMaximumLines", "No maximum limit for values.");
		}
	}
	
	/**
	 * center the chart
	 */
	private void centerChart(){
		LogUtils.debug(CLASS_NAME, "centerChart", "Centering chart.");
		_renderer.setYAxisMax(_currentYMax);
		_renderer.setYAxisMin(_currentYMin);
		_renderer.setXAxisMax(_currentXMax);
		_renderer.setXAxisMin(_currentXMin);
		_viewChart.repaint();
	}
	
	/**
	 * Note: there should be at least difference of 1 between min and max
	 * 
	 * @param minDate
	 * @param maxDate
	 */
	private void setXAxisLabels(long minDate, long maxDate){
		_renderer.clearXTextLabels();
		long interval = 1;	// the minimum step when using long
		long distance = Math.abs(minDate-maxDate);
		if(distance > X_AXIS_LABEL_COUNT){	// check that there are enough values to use
			interval = distance / X_AXIS_LABEL_COUNT;
		}
		for(long i=minDate;i<=maxDate;i+=interval){
			_renderer.addXTextLabel(i, DateUtils.dateToAxisDateString(new Date(i)));
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu); // Inflate the menu; this adds items to the action bar if it is present.
		menu.findItem(R.id.menu_open_graph).setEnabled(false);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
			case R.id.menu_open_settings:
				startActivity(new Intent(this,SettingsActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
				return true;
			case R.id.menu_open_form:
				startActivity(new Intent(this,FormActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
				return true;
			default:
				LogUtils.warn(CLASS_NAME, "onOptionsItemSelected", "Unknown item id: "+item.getItemId());
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onClick(View v) {
		if(_currentMeter != null){
			String currentId = _currentMeter.getId();	// note: currentMeter as an object will not exist in the hashmap, find the correct one by id
			for(Iterator<Meter> mIter = _meterDetails.keySet().iterator();mIter.hasNext();){
				Meter m = mIter.next();
				if(m.getId().equals(currentId)){
					DialogUtils.showSelectionDialog(this, _meterDetails, this, m, TAG_SELECT_METER);
					break;
				}
			}
		}
	}

	@Override
	public void dialogClosed(ConfirmationStatus status, int tag) {
		// nothing needed
	}

	@Override
	public <T> void selectionClosed(T m, String text, int tag) {
		if(text != null){
			initializeLayout(((Meter) m).getId(), null);
			centerGaugeSelectorTo(_currentGauge);
		}
	}
	
	@Override
	public Gauge getCurrentGauge() {
		return _currentGauge;
	}

	@Override
	public GaugeValue getCurrentGaugeValue() {
		return _currentValue;
	}

	@Override
	public GaugeValue getPreviousGaugeValue() {
		if(_currentGauge != null && _currentValue != null){
			List<GaugeValue> values = _currentGauge.getValues();
			if(values != null){
				int index = values.indexOf(_currentValue);
				if(index > 0){
					return (_currentValue = values.get(index-1));
				}
			}
		}
		return null;
	}

	@Override
	public GaugeValue getNextGaugeValue() {
		if(_currentGauge != null && _currentValue != null){
			List<GaugeValue> values = _currentGauge.getValues();
			if(values != null){
				int index = values.indexOf(_currentValue);
				if(index >= 0 && index < values.size()-1){
					return (_currentValue = values.get(index+1));
				}
			}
		}
		return null;
	}
	
	/**
	 * on touch listener that checks for clicks and long-clicks on the chart
	 *
	 * uses an async task (timer) for checking long clicks, a single task is kept alive until a long-click occurres,
	 * or THRESHOLD_LONG_CLICK_MAX amount of milliseconds have passed. This is to limit creation of objects and extra
	 * threads.
	 */
	private class ChartTouchListener implements View.OnTouchListener{
		private static final long INTERVAL_LONG_CLICK_CHECK = 500; // in ms
		private AsyncTask<Void, Void, Boolean> _longClickTimer = null;
		private AtomicLong _downTime = new AtomicLong();	// use atomic just in case, as we are using separate thread
		private float _downX = 0;
		private float _downY = 0;
		
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			switch(event.getAction()){
				case MotionEvent.ACTION_CANCEL:
					_downTime.set(0);
					break;
				case MotionEvent.ACTION_DOWN:
					_downTime.set(System.currentTimeMillis());
					_downX = event.getX();
					_downY = event.getY();
					if(_longClickTimer == null){	// if there is no pre-existing task, create a new one
						_longClickTimer = new AsyncTask<Void, Void, Boolean>(){
							@Override
							protected Boolean doInBackground(Void... params) {
								try{
									for(long i=0;i<Definitions.THRESHOLD_LONG_CLICK_MAX_DURATION;i+=INTERVAL_LONG_CLICK_CHECK){
										Thread.sleep(INTERVAL_LONG_CLICK_CHECK);
										long downTime = _downTime.get();
										if(downTime > 0 && System.currentTimeMillis()-downTime > Definitions.THRESHOLD_LONG_CLICK_MIN_DURATION){
											return true;
										}
									}
								}catch(InterruptedException ex){
									LogUtils.warn(CLASS_NAME, "doInBackground", ex.toString());
								}
								return false;
							}

							@Override
							protected void onPostExecute(Boolean result) {
								if(result){
									centerChart();
									_downTime.set(0);
								}
								_longClickTimer = null;
							}					
						};	// new asyncTask
						_longClickTimer.execute();
					}
					break;
				case MotionEvent.ACTION_MOVE:
					checkForMovement(event);
					break;
				case MotionEvent.ACTION_UP:
					long downTime = _downTime.getAndSet(0);
					if(downTime != 0 && (System.currentTimeMillis()-downTime) < Definitions.THRESHOLD_CLICK_MAX_DURATION && !checkForMovement(event)){
						SeriesSelection selection = _viewChart.getCurrentSeriesAndPoint();
						if(selection == null || selection.getSeriesIndex() != 0){	// if there was no selection or this is not the first series (point values)
							LogUtils.debug(CLASS_NAME, "onTouch", "No usable selection.");
						}else{
							List<GaugeValue> values = _currentGauge.getValues();
							int valueCount = values.size();
							int index = 0;
							//TODO: this will break if the chart shows something else than the last getMaxGraphPoints() of values
							if(valueCount - _settings.getMaxGraphPoints() > 0){
								index = valueCount - _settings.getMaxGraphPoints() + selection.getPointIndex();
							}else{
								index = selection.getPointIndex();
							}
							_currentValue = _currentGauge.getValues().get(index);
							(new GaugeValueDialog()).show(GraphActivity.this.getSupportFragmentManager(), GaugeValueDialog.TAG);
						}
					}
					break;
				default:
					break;	// ignore everything else
			}
			return false;	// always return false so that we do not interfere with the graph pan/zoom controls
		}	
		
		/**
		 * this will automatically reset downTime if movement has occurred
		 * 
		 * @param event
		 * @return true if there were movement
		 */
		private boolean checkForMovement(MotionEvent event){
			if(Math.sqrt(Math.pow(_downX-event.getX(), 2) + Math.pow(_downY-event.getY(), 2)) > Definitions.THRESHOLD_CLICK_MAX_MOVEMENT){
				_downTime.set(0);
				return true;
			}else{
				return false;
			}
		}
	} // class ChartTouchListener
}
