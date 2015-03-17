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
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.novartis.pcs.ontology.entity.Datasource;
import com.novartis.pcs.ontology.entity.Synonym;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.webapp.client.OntoBrowserServiceAsync;
import com.novartis.pcs.ontology.webapp.client.event.SynonymUpdatedEvent;

public class ReplaceSynonymPopup implements OntoBrowserPopup, ClickHandler {
	private final OntoBrowserServiceAsync service;
	private final EventBus eventBus;
	private final DialogBox dialogBox = new DialogBox(false, true);
	private final BusyIndicatorHandler busyIndicator = new WidgetBusyIndicatorHandler(dialogBox.getCaption().asWidget());
	private final Label ontologyHeadingLabel = new Label("Ontology:");
	private final Label ontologyLabel = new Label();
	private final Label termLabel = new Label();
	private final Label synonymLabel = new Label();
	private final Label sourceLabel = new Label();
	private final Label referenceIdLabel = new Label();
	private final TextArea commentsField = new TextArea();
	private final Label errorLabel = new Label();
	private final Button obsoleteButton = new Button("Obsolete");
	
	private Synonym synonym;
		
	public ReplaceSynonymPopup(final OntoBrowserServiceAsync service,
			final EventBus eventBus) {
		this.service = service;
		this.eventBus = eventBus;
				
		dialogBox.setText("Confirm synonym obsoletion");
		dialogBox.setGlassEnabled(true);
		dialogBox.setAnimationEnabled(true);
		dialogBox.addStyleName("gwt-ModalDialogBox");
		
		addDialogWidgets();
		
		commentsField.getElement().getParentElement().addClassName("text-area-right-padded");
		commentsField.setWidth("100%");
		commentsField.setVisibleLines(2);
		commentsField.setCharacterWidth(64);
		
		errorLabel.setVisible(false);
		
		obsoleteButton.addClickHandler(this);
	}
			
	public Synonym getSynonym() {
		return synonym;
	}

	public void setSynonym(Synonym synonym) {
		this.synonym = synonym;
	}

	@Override
	public void show() {
		if(synonym != null) {
			updateLabels();
			dialogBox.center();
		}
	}

	@Override
	public void onClick(ClickEvent event) {
		if(synonym != null) {
			obsoleteButton.setEnabled(false);
			errorLabel.setVisible(false);
			busyIndicator.busy();
			service.obsoleteSynonym(synonym.getId(), 0,	commentsField.getValue(),
					new AsyncCallback<Synonym>() {
				public void onFailure(Throwable caught) {
					GWT.log("Failed to obsolete term", caught);
					errorLabel.setText(caught.getMessage());
					errorLabel.setVisible(true);
					busyIndicator.idle();
					obsoleteButton.setEnabled(true);
				}
	
				public void onSuccess(Synonym synonym) {
					eventBus.fireEvent(new SynonymUpdatedEvent(synonym));
					busyIndicator.idle();
					dialogBox.hide();
					errorLabel.setVisible(false);
					commentsField.setValue(null);
					obsoleteButton.setEnabled(true);
				}
			});
		}
	}
	
	private void updateLabels() {
		if(synonym != null) {
			Term term = synonym.getTerm();
			Datasource datasource = null;
			String referenceId = null;
			if(synonym.getControlledVocabularyTerm() != null) {
				datasource = synonym.getControlledVocabularyTerm()
						.getControlledVocabulary().getDatasource();
				referenceId = synonym.getControlledVocabularyTerm()
						.getReferenceId();
			} else if(synonym.getDatasource() != null) {
				datasource = synonym.getDatasource();
				referenceId = synonym.getReferenceId();
			}
			
			ontologyHeadingLabel.setText(term.getOntology().isCodelist() ? 
					"Codelist:" : "Ontology:");
			ontologyLabel.setText(term.getOntology().getName());
			termLabel.setText(term.getName());
			synonymLabel.setText(synonym.getSynonym());
			sourceLabel.setText(datasource != null ? datasource.getAcronym() : null);
			referenceIdLabel.setText(referenceId);
		} else {
			ontologyLabel.setText(null);
			termLabel.setText(null);
			synonymLabel.setText(null);
			sourceLabel.setText(null);
			referenceIdLabel.setText(null);
		}
	}
		
	private void addDialogWidgets() {
		VerticalPanel vertPanel = new VerticalPanel();
		SimplePanel decPanel = new SimplePanel();
    	Grid grid = new Grid(5,2);
		HorizontalPanel buttonsHPanel = new HorizontalPanel();
		
		Label comments = new Label("Comments:");
		Button cancelButton = new Button("Cancel");
		
		decPanel.addStyleName("decorator-panel");		
		comments.addStyleName("dialog-label");
								
		cancelButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				dialogBox.hide();
				synonym = null;
				commentsField.setValue(null);
				errorLabel.setVisible(false);
			}
		});
		
		grid.addStyleName("gwt-Grid");
		errorLabel.addStyleName("dialog-error");
		buttonsHPanel.addStyleName("dialog-buttons");
		buttonsHPanel.addStyleName("centered-hortz");
		vertPanel.addStyleName("dialog-vpanel");		
		//vertPanel.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
				
		int row = 0, col = 0;
		grid.setWidget(row, col, ontologyHeadingLabel);
		grid.setWidget(++row, col, new Label("Term:"));
		grid.setWidget(++row, col, new Label("Synonym:"));
		grid.setWidget(++row, col, new Label("Source:"));
		grid.setWidget(++row, col, new Label("Reference Id:"));
		row = 0;
		col = 1;
		grid.setWidget(row, col, ontologyLabel);
		grid.setWidget(++row, col, termLabel);
		grid.setWidget(++row, col, synonymLabel);
		grid.setWidget(++row, col, sourceLabel);
		grid.setWidget(++row, col, referenceIdLabel);
		
		for(row = col = 0; row < grid.getRowCount(); row++) {
			Label label = (Label)grid.getWidget(row, col);
			label.addStyleName("dialog-label");
		}			
								
		buttonsHPanel.add(obsoleteButton);
		buttonsHPanel.add(cancelButton);
		
		decPanel.setWidget(grid);
		//vertPanel.add(grid);
		vertPanel.add(decPanel);
		vertPanel.add(comments);
		vertPanel.add(commentsField);
		vertPanel.add(errorLabel);
		vertPanel.add(buttonsHPanel);
		
		dialogBox.setWidget(vertPanel);
	}
}
