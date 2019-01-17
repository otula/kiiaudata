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

requirejs.config({
    //paths config is relative to the baseUrl, and
    //never includes a ".js" extension since
    //the paths config could be for a directory.
	paths: {
		jquery: 	'lib/jquery-2.1.1.min',
		flot: 		'lib/jquery.flot/jquery.flot.min',
		flot_time: 	'lib/jquery.flot/jquery.flot.time.min',
		flot_navigate: 'lib/jquery.flot/jquery.flot.navigate.min',
		flot_crosshair: 'lib/jquery.flot/jquery.flot.crosshair.min',
		flot_tooltip: 'lib/jquery.flot/jquery.flot.tooltip.min',
		flot_selection: 'lib/jquery.flot/jquery.flot.selection.min'
	},
	shim:{
		'flot': ['jquery'],
		'flot_time': ['jquery', 'flot'],
		'flot_navigate': ['jquery', 'flot'],
		'flot_crosshair': ['jquery', 'flot'],
		'flot_tooltip' : ['jquery', 'flot'],
		'flot_selection' : ['jquery', 'flot']
	},
//    config: {
//        //Set the config for the i18n module ID
//        i18n: {
//            locale: 'en'	//manually set to english locale
//        }
//    },
	enforceDefine: false
});

require(['jquery', 'app/meter_list'], function(jq, app) {
	$('#meter-container').append('<div id="meters" class="meters"></div>');
	app.initialize($('#meters'));
});

