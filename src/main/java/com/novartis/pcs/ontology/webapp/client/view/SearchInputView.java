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

import static com.google.gwt.event.dom.client.KeyCodes.KEY_BACKSPACE;
import static com.google.gwt.event.dom.client.KeyCodes.KEY_DELETE;
import static com.google.gwt.event.dom.client.KeyCodes.KEY_ENTER;

import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.TextBox;
import com.novartis.pcs.ontology.webapp.client.OntoBrowserServiceAsync;
import com.novartis.pcs.ontology.webapp.client.event.SearchEvent;
import com.novartis.pcs.ontology.webapp.client.event.SearchHandler;

public class SearchInputView extends OntoBrowserView implements KeyDownHandler, SearchHandler {
	private static final int TIMEOUT = 300; // milliseconds

	private final TextBox searchInput = new TextBox();
	private Timer timer = null;

	public SearchInputView(EventBus eventBus, OntoBrowserServiceAsync service) {
		super(eventBus,service);
		
		DOM.setElementAttribute(searchInput.getElement(), 
				"placeholder", "Search");
		
		searchInput.addKeyDownHandler(this);	
		
		initWidget(searchInput);
		addStyleName("search");
		eventBus.addHandler(SearchEvent.TYPE, this);
	}
	
	@Override
	public void onSearch(SearchEvent event) {
		if(event.getSource() != searchInput) {
			searchInput.setValue(event.getPattern());
			searchInput.setFocus(true);
		}
	}

	@Override
	public void onKeyDown(KeyDownEvent event) {
		int keyCode = event.getNativeKeyCode();
		if((keyCode == KEY_BACKSPACE || keyCode >= KEY_DELETE) 
				&& keyCode != 91 && keyCode != 93) { // Command/Window keyboard keys
			cancelTimer();
			timer = new Timer() {
				public void run() {
					eventBus.fireEventFromSource(new SearchEvent(searchInput.getValue()), searchInput);
				}
			};

			timer.schedule(TIMEOUT);
		} else if(keyCode == KEY_ENTER) {
			cancelTimer();
			eventBus.fireEventFromSource(new SearchEvent(searchInput.getValue()), searchInput);
		}
	}

	private void cancelTimer() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}
}
