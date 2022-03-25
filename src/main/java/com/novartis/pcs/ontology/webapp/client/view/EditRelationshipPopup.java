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

import java.util.List;

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
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.novartis.pcs.ontology.entity.Relationship;
import com.novartis.pcs.ontology.entity.RelationshipType;
import com.novartis.pcs.ontology.webapp.client.OntoBrowserServiceAsync;
import com.novartis.pcs.ontology.webapp.client.event.RelationshipUpdatedEvent;

public class EditRelationshipPopup extends EditPopup implements OntoBrowserEditPopup<Relationship>, ClickHandler {
	private final OntoBrowserServiceAsync service;
	private final EventBus eventBus;
	private final DialogBox dialogBox = new DialogBox(false, true);
	private final BusyIndicatorHandler busyIndicator = new WidgetBusyIndicatorHandler(dialogBox.getCaption().asWidget());
	private final Label ontologyLabel = new Label();
	private final Label termLabel = new Label();
	private final ListBox typeDropBox = new ListBox();
	private final Label relatedTermLabel = new Label();
	private final Label errorLabel = new Label();
	private final Button updateButton = new Button("Update");
		
	private Relationship relationship;
	
	public EditRelationshipPopup(OntoBrowserServiceAsync service,
			final EventBus eventBus) {
		this.service = service;
		this.eventBus = eventBus;
				
		dialogBox.setText("Edit Relationship");
		dialogBox.setGlassEnabled(true);
		dialogBox.setAnimationEnabled(true);
		dialogBox.addStyleName("gwt-ModalDialogBox");
		
		addRelationshipTypes();
		addDialogWidgets();
		
		updateButton.addClickHandler(this);
	}
			
	@Override
	public void onClick(ClickEvent event) {
		if(relationship != null) {
			updateButton.setEnabled(false);
			errorLabel.setText(null);
			busyIndicator.busy();
			
			service.updateRelationship(relationship.getId(),
					typeDropBox.getValue(typeDropBox.getSelectedIndex()),
					new AsyncCallback<Relationship>() {
				public void onFailure(Throwable caught) {
					GWT.log("Failed to update relationship", caught);
					errorLabel.setText(caught.getMessage());
					updateButton.setEnabled(true);
					busyIndicator.idle();
				}
	
				public void onSuccess(Relationship relationship) {
					eventBus.fireEvent(new RelationshipUpdatedEvent(relationship));
					busyIndicator.idle();
					dialogBox.hide();
					updateButton.setEnabled(true);
				}
			});
			
		}
	}

	@Override
	public void setEntity(Relationship relationship) {
		String type = relationship.getType().getRelationship();
		this.relationship = relationship;
		ontologyLabel.setText(relationship.getTerm().getOntology().getName());
		termLabel.setText(relationship.getTerm().getName());
		relatedTermLabel.setText(relationship.getRelatedTerm().getName());
		for(int i = 0; i < typeDropBox.getItemCount(); i++) {
			String value = typeDropBox.getValue(i);
			if(type.equals(value)) {
				typeDropBox.setSelectedIndex(i);
				break;
			}
		}
	}

	@Override
	public Relationship getEntity() {
		return relationship;
	}
	
	private void addRelationshipTypes() {
		service.loadAllRelationshipTypes(new AsyncCallback<List<RelationshipType>>() {
			public void onFailure(Throwable caught) {
				GWT.log("Failed to load relationship types", caught);
				typeDropBox.clear();
				ErrorView.instance().onUncaughtException(caught);
			}

			public void onSuccess(List<RelationshipType> types) {
				for(RelationshipType type : types) {
					String label = type.getRelationship().replace('_', ' ');
					String value = type.getRelationship();
					typeDropBox.addItem(label, value);
				}
			}
		});
	}
	
	private void addDialogWidgets() {
		VerticalPanel vertPanel = new VerticalPanel();
		Grid grid = new Grid(4,2);
		HorizontalPanel buttonsHPanel = new HorizontalPanel();
		Button cancelButton = new Button("Cancel");
		
		cancelButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				dialogBox.hide();
			}
		});
		
		grid.addStyleName("gwt-Grid");
		errorLabel.addStyleName("dialog-error");
		buttonsHPanel.addStyleName("dialog-buttons");
		vertPanel.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
				
		int row = 0, col = 0;
		grid.setWidget(row, col, new Label("Ontology:"));
		grid.setWidget(++row, col, new Label("Term:"));
		grid.setWidget(++row, col, new Label("Relationship:"));
		grid.setWidget(++row, col, new Label("Related Term:"));
		row = 0;
		col = 1;
		grid.setWidget(row, col, ontologyLabel);
		grid.setWidget(++row, col, termLabel);
		grid.setWidget(++row, col, typeDropBox);
		grid.setWidget(++row, col, relatedTermLabel);
		
		for(row = col = 0; row < grid.getRowCount(); row++) {
			Label label = (Label)grid.getWidget(row, col);
			label.addStyleName("dialog-label");
		}
		
		buttonsHPanel.add(updateButton);
		buttonsHPanel.add(cancelButton);
		
		vertPanel.add(grid);
		vertPanel.add(errorLabel);
		vertPanel.add(buttonsHPanel);
		dialogBox.setWidget(vertPanel);
	}
}
