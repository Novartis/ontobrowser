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

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.Widget;
import com.novartis.pcs.ontology.webapp.client.OntoBrowserServiceAsync;

public class OntoBrowserView extends ResizeComposite {
	protected final EventBus eventBus;
	protected final OntoBrowserServiceAsync service;
	
	public OntoBrowserView(EventBus eventBus, OntoBrowserServiceAsync service) {
		super();
		this.eventBus = eventBus;
		this.service = service;
	}
	
	@Override
	public void onResize() {
		Widget widget = super.getWidget();
		if(widget instanceof RequiresResize) {
			((RequiresResize)widget).onResize();
		}
	}
}
