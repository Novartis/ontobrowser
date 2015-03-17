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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.HasKeyboardPagingPolicy.KeyboardPagingPolicy;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import com.novartis.pcs.ontology.entity.Relationship;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.entity.VersionedEntity.Status;
import com.novartis.pcs.ontology.webapp.client.OntoBrowserServiceAsync;
import com.novartis.pcs.ontology.webapp.client.event.RelationshipUpdatedEvent;
import com.novartis.pcs.ontology.webapp.client.event.ViewTermEvent;

public class ReplaceRelationshipPopup implements OntoBrowserPopup, ClickHandler {
	private static class RelatedTermColumn extends TextColumn<Relationship> {
		@Override
		public String getValue(Relationship relationship) {
			Term term = relationship.getRelatedTerm();
			return term.getName(); 
		}
	};
	
	private static class RelationshipColumn extends TextColumn<Relationship> {
		@Override
		public String getValue(Relationship relationship) {
			return relationship.getType().getRelationship();
		}
	};
	
	private static class StatusColumn extends TextColumn<Relationship> {
		@Override
		public String getValue(Relationship relationship) {
			return relationship.getStatus().toString();
		}
	};
		
	private final OntoBrowserServiceAsync service;
	private final EventBus eventBus;
	private final DialogBox dialogBox = new DialogBox(false, true);
	private final BusyIndicatorHandler busyIndicator = new WidgetBusyIndicatorHandler(dialogBox.getCaption().asWidget());
	private final Label ontologyLabel = new Label();
	private final Label termLabel = new Label();
	private final Label typeLabel = new Label();
	private final Label relatedTermLabel = new Label();
	private final Label replacementLabel = new Label("Relaced By:");
	private final TextArea commentsField = new TextArea();
	private final Label errorLabel = new Label();
	private final Button obsoleteButton = new Button("Obsolete");
	
	private final ProvidesKey<Relationship> keyProvider = new EntityKeyProvider<Relationship>();
	private final CellTable<Relationship> table = new CellTable<Relationship>(keyProvider);
	private final SingleSelectionModel<Relationship> selection = new SingleSelectionModel<Relationship>(keyProvider);
	
	private Relationship relationship;
		
	public ReplaceRelationshipPopup(final OntoBrowserServiceAsync service,
			final EventBus eventBus) {
		this.service = service;
		this.eventBus = eventBus;
				
		dialogBox.setText("Confirm relationship obsoletion");
		dialogBox.setGlassEnabled(true);
		dialogBox.setAnimationEnabled(true);
		dialogBox.addStyleName("gwt-ModalDialogBox");
		
		setupTable();
		addDialogWidgets();
		
		commentsField.getElement().getParentElement().addClassName("text-area-right-padded");
		commentsField.setWidth("100%");
		commentsField.setVisibleLines(2);
		commentsField.setCharacterWidth(64);
		
		errorLabel.addStyleName("dialog-error");
		errorLabel.setVisible(false);
		
		obsoleteButton.addClickHandler(this);
	}
			
	public Relationship getRelationship() {
		return relationship;
	}

	public void setRelationship(Relationship relationship) {
		this.relationship = relationship;
	}

	@Override
	public void show() {
		if(relationship != null) {
			updateLabels();
			dialogBox.center();
		}
	}

	@Override
	public void onClick(ClickEvent event) {
		if(relationship != null) {
			Relationship selected = selection.getSelectedObject();
			obsoleteButton.setEnabled(false);
			errorLabel.setVisible(false);
			busyIndicator.busy();
			
			service.obsoleteRelationship(relationship.getId(), 
					selected != null ? selected.getId() : 0, 
					commentsField.getValue(),
					new AsyncCallback<Relationship>() {
				public void onFailure(Throwable caught) {
					GWT.log("Failed to obsolete relationship", caught);
					errorLabel.setText(caught.getMessage());
					errorLabel.setVisible(true);
					busyIndicator.idle();
					obsoleteButton.setEnabled(true);
				}
	
				public void onSuccess(Relationship relationship) {
					eventBus.fireEvent(new RelationshipUpdatedEvent(relationship));
					eventBus.fireEvent(new ViewTermEvent(relationship.getTerm()));
					busyIndicator.idle();
					dialogBox.hide();
					commentsField.setValue(null);
					obsoleteButton.setEnabled(true);
				}
			});
		}
	}
	
	private void updateLabels() {
		if(relationship != null) {
			Term term = relationship.getTerm();					
			ontologyLabel.setText(term.getOntology().getName());
			termLabel.setText(term.getName());
			typeLabel.setText(relationship.getType().getRelationship());
			relatedTermLabel.setText(relationship.getRelatedTerm().getName());
			
			Set<Relationship> relationships = term.getRelationships();
			List<Relationship> list = Collections.emptyList();
			if(relationships != null && !relationships.isEmpty()) {
				list = new ArrayList<Relationship>();
				for(Relationship r : relationships) {
					if(r.getStatus().equals(Status.APPROVED) && !r.equals(relationship)) {
						list.add(r);
					}
				}
			}
			table.setRowData(list);
			table.setRowCount(list.size(), true);
			
			if(list.isEmpty()) {
				replacementLabel.setVisible(false);
				table.setVisible(false);
			} else {
				replacementLabel.setVisible(true);
				table.setVisible(true);
			}
			
		} else {
			ontologyLabel.setText(null);
			termLabel.setText(null);
			typeLabel.setText(null);
			relatedTermLabel.setText(null);
			
			table.setRowData(Collections.<Relationship>emptyList());
			table.setRowCount(0, true);
		}
	}
	
	private void setupTable() {
		Column<Relationship, Boolean> checkColumn = new Column<Relationship, Boolean>(
				new CheckboxCell(true, false)) {
			@Override
			public Boolean getValue(Relationship object) {
				return selection.isSelected(object);
			}
		};
		
		table.addStyleName("gwt-CellTable");
		table.addStyleName("decorator-panel");
		table.setWidth("100%");
		table.setKeyboardPagingPolicy(KeyboardPagingPolicy.CURRENT_PAGE);
		table.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);	
				
		table.addColumn(checkColumn);
		table.setColumnWidth(checkColumn, 16, Unit.PX);
		
		table.addColumn(new RelationshipColumn(), "Relationship");
		table.addColumn(new RelatedTermColumn(), "Related Term");
		table.addColumn(new StatusColumn(), "Status");
		
		table.setSelectionModel(selection, 
				DefaultSelectionEventManager.<Relationship>createCheckboxManager(0));
		
	}
		
	private void addDialogWidgets() {
		VerticalPanel vertPanel = new VerticalPanel();
		SimplePanel decPanel = new SimplePanel();
		Grid grid = new Grid(4,2);
		HorizontalPanel buttonsHPanel = new HorizontalPanel();
		
		Label commentsLabel = new Label("Comments:");
		Button cancelButton = new Button("Cancel");
		
		decPanel.addStyleName("decorator-panel");
		replacementLabel.addStyleName("dialog-label");
		commentsLabel.addStyleName("dialog-label");
		
		cancelButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				dialogBox.hide();
				relationship = null;
				commentsField.setValue(null);
				errorLabel.setVisible(false);
			}
		});
		
		grid.addStyleName("gwt-Grid");
		buttonsHPanel.addStyleName("dialog-buttons");
		buttonsHPanel.addStyleName("centered-hortz");
		vertPanel.addStyleName("dialog-vpanel");		
		//vertPanel.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
				
		int row = 0, col = 0;
		grid.setWidget(row, col, new Label("Ontology:"));
		grid.setWidget(++row, col, new Label("Term:"));
		grid.setWidget(++row, col, new Label("Relationship:"));
		grid.setWidget(++row, col, new Label("Related Term:"));
		row = 0;
		col = 1;
		grid.setWidget(row, col, ontologyLabel);
		grid.setWidget(++row, col, termLabel);
		grid.setWidget(++row, col, typeLabel);
		grid.setWidget(++row, col, relatedTermLabel);
		
		for(row = col = 0; row < grid.getRowCount(); row++) {
			Label label = (Label)grid.getWidget(row, col);
			label.addStyleName("dialog-label");
		}			
								
		buttonsHPanel.add(obsoleteButton);
		buttonsHPanel.add(cancelButton);
		
		decPanel.setWidget(grid);
		//vertPanel.add(grid);
		vertPanel.add(decPanel);
		vertPanel.add(replacementLabel);
		vertPanel.add(table);
		vertPanel.add(commentsLabel);
		vertPanel.add(commentsField);
		vertPanel.add(errorLabel);
		vertPanel.add(buttonsHPanel);
		
		dialogBox.setWidget(vertPanel);
	}
}
