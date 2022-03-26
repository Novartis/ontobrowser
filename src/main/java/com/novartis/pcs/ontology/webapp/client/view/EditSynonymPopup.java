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

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.novartis.pcs.ontology.entity.Synonym;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.webapp.client.OntoBrowserServiceAsync;
import com.novartis.pcs.ontology.webapp.client.event.SynonymUpdatedEvent;
import com.novartis.pcs.ontology.webapp.client.util.UrlValidator;

public class EditSynonymPopup extends EditPopup implements OntoBrowserEditPopup<Synonym>, ClickHandler {
	private final static String[] LABELS = {
			"Ontology/Codelist", "Term", "Synonym", "Type" 
		};
		
	private final OntoBrowserServiceAsync service;
	private final EventBus eventBus;
	private final DialogBox dialogBox = new DialogBox(false, true);
	private final BusyIndicatorHandler busyIndicator = new WidgetBusyIndicatorHandler(dialogBox.getCaption().asWidget());
		
	private final Grid grid = new Grid(LABELS.length, 2);
	private final ListBox typeDropBox = new ListBox(false);
	private final Label typeError = new Label();
	private final Button updateButton = new Button("Update");
	
	private Synonym synonym;
	
	public EditSynonymPopup(OntoBrowserServiceAsync service,
			EventBus eventBus) {
		this.service = service;
		this.eventBus = eventBus;
				
		for(Synonym.Type type : Synonym.Type.values()) {
			typeDropBox.addItem(type.toString(), type.name());
		}
		
		dialogBox.setText("Edit Synonym");
		dialogBox.setGlassEnabled(true);
		dialogBox.setAnimationEnabled(true);
		dialogBox.addStyleName("gwt-ModalDialogBox");
		
		grid.addStyleName("gwt-Grid");
		for(int i = 0; i < LABELS.length; i++) {
			grid.setText(i, 0, LABELS[i] + ":");
		}
				
		CellFormatter cellFormatter = grid.getCellFormatter(); 
		for(int row = 0; row < grid.getRowCount(); row++) {
			cellFormatter.addStyleName(row, 0, "dialog-label");
		}
		
		grid.setWidget(grid.getRowCount()-1, 1, typeDropBox);
		
		typeError.addStyleName("dialog-error");
		
		addDialogWidgets();
		
		updateButton.addClickHandler(this);
	}
			
	@Override
	public void onClick(ClickEvent event) {
		if(synonym != null) {
			Synonym.Type type = Synonym.Type.valueOf(
					typeDropBox.getValue(typeDropBox.getSelectedIndex()));
			
			updateButton.setEnabled(false);
			typeError.setText(null);
			busyIndicator.busy();
			
			service.updateSynonym(synonym.getId(), type,
					new AsyncCallback<Synonym>() {
				public void onFailure(Throwable caught) {
					GWT.log("Failed to update synonym", caught);
					typeError.setText(caught.getMessage());
					updateButton.setEnabled(true);
					busyIndicator.idle();
				}
	
				public void onSuccess(Synonym synonym) {
					eventBus.fireEvent(new SynonymUpdatedEvent(synonym));
					busyIndicator.idle();
					dialogBox.hide();
					updateButton.setEnabled(true);
				}
			});
			
		}
	}
	
	private void addDialogWidgets() {
		VerticalPanel dialogVPanel = new VerticalPanel();
		HorizontalPanel buttonsHPanel = new HorizontalPanel();
		Button cancelButton = new Button("Cancel");
				
		cancelButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				typeError.setText(null);
				dialogBox.hide();
			}
		});
		
		buttonsHPanel.add(updateButton);
		buttonsHPanel.add(cancelButton);
		buttonsHPanel.addStyleName("dialog-buttons");	
		
		dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_LEFT);
		dialogVPanel.add(grid);		
		dialogVPanel.add(typeError);
		dialogVPanel.add(buttonsHPanel);
		dialogVPanel.setCellHorizontalAlignment(buttonsHPanel, VerticalPanel.ALIGN_CENTER);
		
				
		dialogBox.setWidget(dialogVPanel);
	}

	@Override
	public void setEntity(Synonym synonym) {
		this.synonym = synonym;
		Term term = synonym.getTerm();
		int row = 0;
		grid.setText(row++, 1, term.getOntology().getName());
		if(UrlValidator.validate(term.getUrl())) {
			Anchor a = new Anchor(term.getName(), term.getUrl().trim());
			a.setTarget("TermUrl");
			grid.setWidget(row++, 1, a);
		} else {
			grid.setText(row++, 1, term.getName());
		}
		grid.setText(row++, 1, synonym.getSynonym());
		typeDropBox.setSelectedIndex(synonym.getType().ordinal());
		
	}

	@Override
	public Synonym getEntity() {
		return synonym;
	}
}
