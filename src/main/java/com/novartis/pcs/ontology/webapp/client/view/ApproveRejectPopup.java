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

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.TabPanel;
import com.novartis.pcs.ontology.entity.Curator;
import com.novartis.pcs.ontology.webapp.client.OntoBrowserServiceAsync;

public class ApproveRejectPopup implements OntoBrowserPopup, SelectionHandler<Integer> {	
	private final OntoBrowserServiceAsync service;
	private final EventBus eventBus;
	private final DialogBox dialogBox = new DialogBox(false, false);
	private final BusyIndicatorHandler busyIndicator = new WidgetBusyIndicatorHandler(dialogBox.getCaption().asWidget());
	private final TabPanel tabPanel = new TabPanel();
	private final ApproveRejectTermComposite termTab;
	private final ApproveRejectRelationshipComposite relationshipTab;
	private final ApproveRejectSynonymComposite synonymTab;
	
	private Integer currentTabIndex = new Integer(0);
			
	public ApproveRejectPopup(OntoBrowserServiceAsync service,
			EventBus eventBus, Curator curator) {
		this.service = service;
		this.eventBus = eventBus;
		
		dialogBox.setText("Pending Approval");
		dialogBox.getCaption().asWidget().addStyleName("busy-icon-right-padded");
		dialogBox.setGlassEnabled(false);
		dialogBox.setAnimationEnabled(true);
		
		termTab = new ApproveRejectTermComposite(service, eventBus, curator, busyIndicator);
		relationshipTab = new ApproveRejectRelationshipComposite(service, eventBus, curator, busyIndicator);
		synonymTab = new ApproveRejectSynonymComposite(service, eventBus, curator, busyIndicator);
		
		tabPanel.add(termTab, "Terms");
		tabPanel.add(relationshipTab, "Relationships");
		tabPanel.add(synonymTab, "Synonyms");
		tabPanel.addSelectionHandler(this);
		tabPanel.selectTab(currentTabIndex);
				
		dialogBox.setWidget(tabPanel);
	}
		
	@Override
	public void show() {
		dialogBox.show();
		SelectionEvent.fire(tabPanel, currentTabIndex);
		/*
		dialogBox.setPopupPositionAndShow(new PopupPanel.PositionCallback() {
			@Override
			public void setPosition(int offsetWidth, int offsetHeight) {
				dialogBox.setPopupPosition(
						Window.getClientWidth() - offsetWidth, 0);
			}
		});
		*/
	}

	@Override
	public void onSelection(SelectionEvent<Integer> event) {
		busyIndicator.busy();
		switch(currentTabIndex = event.getSelectedItem()) {
		case 0:
			ApproveRejectPopup.this.service.loadPendingTerms(termTab);
			break;
		case 1:
			ApproveRejectPopup.this.service.loadPendingRelationships(relationshipTab);
			break;
		case 2:
			ApproveRejectPopup.this.service.loadPendingSynonyms(synonymTab);
			break;
		}		
	}
}
