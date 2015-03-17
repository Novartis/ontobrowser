/* 

Copyright 2015 Novartis Institutes for Biomedical Research

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*/
package com.novartis.pcs.ontology.webapp.client.view;

import com.google.gwt.user.client.ui.Widget;

public class WidgetBusyIndicatorHandler implements BusyIndicatorHandler {
	private final Widget widget;
	private final String style;
	private final String errorStyle;
	private int count;
	private boolean error;
	
	public WidgetBusyIndicatorHandler(Widget widget) {
		this(widget, null, null);
	}
	
	public WidgetBusyIndicatorHandler(Widget widget, String style, String errorStyle) {
		this.widget = widget;
		this.style = style == null ? "busy-icon-right" : style;
		this.errorStyle = errorStyle == null ? "error-icon-right" : errorStyle;
		this.count = 0;
		this.error = false;
		
		if(style == null) {
			widget.addStyleName("busy-icon-right-padded");
		}
		
	}

	@Override
	public void busy() {
		if(error) {
			widget.removeStyleName(errorStyle);
			error = false;
		}
		
		if(count++ == 0) {
			widget.addStyleName(style);
		}
	}

	@Override
	public void idle() {
		if(count > 0 && --count == 0) {
			widget.removeStyleName(style);
		}
	}

	@Override
	public void error() {
		if(!error) {
			widget.addStyleName(errorStyle);
			error = true;
		}
	}
}
