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
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.InlineHyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.ProvidesKey;
import com.novartis.pcs.ontology.entity.RelationshipType;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.webapp.client.OntoBrowserServiceAsync;
import com.novartis.pcs.ontology.webapp.client.event.SearchEvent;
import com.novartis.pcs.ontology.webapp.client.event.ViewTermEvent;
import com.novartis.pcs.ontology.webapp.client.event.ViewTermHandler;

public class AddRelationshipPopup implements OntoBrowserPopup, ViewTermHandler, ClickHandler {
	private int MAX_LAST_VIEWED = 10;
	
	private static class OntologyColumn extends TextColumn<Term> {
		public OntologyColumn() {
			
		}
		
		@Override
		public String getValue(Term term) {
			return term.getOntology().getName();
		}
	};
	
	private static class TermColumn extends Column<Term, InlineHyperlink> {
		public TermColumn() {
			super(new HyperlinkCell());
		}

		@Override
		public InlineHyperlink getValue(Term term) {
			return new InlineHyperlink(term.getName(), term.getReferenceId());
		}
	};
	
	private final OntoBrowserServiceAsync service;
	private final EventBus eventBus;
	private final DialogBox dialogBox = new DialogBox(false, false);
	private final BusyIndicatorHandler busyIndicator = new WidgetBusyIndicatorHandler(dialogBox.getCaption().asWidget());
	private final Label ontologyLabel = new Label();
	private final Label termLabel = new Label();
	private final ListBox typeDropBox = new ListBox(false);
	private final Label relatedTermLabel = new Label();
	private final Label errorLabel = new Label();
	private final Button addButton = new Button("Add");
	
	private final ProvidesKey<Term> keyProvider = new EntityKeyProvider<Term>();
	private final ListDataProvider<Term> recentlyViewedDataProvider = new ListDataProvider<Term>(keyProvider);
	private final CellTable<Term> recentlyViewTable = new CellTable<Term>(keyProvider);
	
	private final ListDataProvider<Term> recentlyCreatedDataProvider = new ListDataProvider<Term>(keyProvider);
	private final CellTable<Term> recentlyCreatedTable = new CellTable<Term>(keyProvider);
	
	private Term currentTerm;
	private Term relatedTerm;
	
	public AddRelationshipPopup(final OntoBrowserServiceAsync service,
			final EventBus eventBus) {
		this.service = service;
		this.eventBus = eventBus;
				
		dialogBox.setText("Add Relationship");
		dialogBox.setGlassEnabled(false);
		dialogBox.setAnimationEnabled(true);
		
		recentlyViewedDataProvider.addDataDisplay(recentlyViewTable);
		recentlyCreatedDataProvider.addDataDisplay(recentlyCreatedTable);
		
		addRelationshipTypes();
		setupTable(recentlyViewTable);
		setupTable(recentlyCreatedTable);
		addDialogWidgets();
		
		addButton.addClickHandler(this);
						
		eventBus.addHandler(ViewTermEvent.TYPE, this);
	}
	
	@Override
	public void show() {
		List<Term> lastViewed = recentlyViewedDataProvider.getList();
		if(!lastViewed.isEmpty()
				&& lastViewed.get(0).equals(currentTerm)) {
			lastViewed.remove(0);
		}
						
		dialogBox.show();
		eventBus.fireEvent(new SearchEvent(""));
		busyIndicator.busy();
		recentlyCreatedDataProvider.getList().clear();
		service.loadLastCreatedTerms(MAX_LAST_VIEWED, new AsyncCallback<List<Term>>() {
			@Override
			public void onFailure(Throwable caught) {
				GWT.log("Failed to load last created terms", caught);
				busyIndicator.idle();
				ErrorView.instance().onUncaughtException(caught);						
			}

			@Override
			public void onSuccess(List<Term> terms) {
				List<Term> list = recentlyCreatedDataProvider.getList();
				for(Term term : terms) {
					if(!term.getOntology().isCodelist()) {
						list.add(term);
					}
				}
				busyIndicator.idle();
			}
		});
	}

	@Override
	public void onViewTerm(ViewTermEvent event) {
		Term term = event.getTerm();
		if(dialogBox.isShowing()) {
			relatedTerm = term;
		} else {
			currentTerm = term;
			relatedTerm = null;
		}
		updateLabels();
		
		List<Term> lastViewed = recentlyViewedDataProvider.getList();
		if(lastViewed.contains(term)) {
			lastViewed.remove(term);
		}
		if(!term.getOntology().isCodelist()) {
			lastViewed.add(0, term);
		}
		
		if(lastViewed.size() > MAX_LAST_VIEWED) {
			lastViewed.remove(MAX_LAST_VIEWED-1);
		}
	}
	
	@Override
	public void onClick(ClickEvent event) {
		if(currentTerm != null && relatedTerm != null) {
			addButton.setEnabled(false);
			errorLabel.setText(null);
			busyIndicator.busy();
			service.addRelationship(currentTerm.getReferenceId(),
					relatedTerm.getReferenceId(),
					typeDropBox.getValue(typeDropBox.getSelectedIndex()),
					new AsyncCallback<Term>() {
				public void onFailure(Throwable caught) {
					GWT.log("Failed to create new child term", caught);
					errorLabel.setText(caught.getMessage());
					busyIndicator.idle();
				}
	
				public void onSuccess(Term term) {
					busyIndicator.idle();
					dialogBox.hide();
					eventBus.fireEvent(new ViewTermEvent(term));
				}
			});
		}
	}
	
	private void updateLabels() {
		if(currentTerm != null) {
			ontologyLabel.setText(currentTerm.getOntology().getName());
			termLabel.setText(currentTerm.getName());
			if(relatedTerm != null) {
				relatedTermLabel.removeStyleName("dialog-message");
				relatedTermLabel.setText(relatedTerm.getName());
				
				if(!currentTerm.getOntology().equals(relatedTerm.getOntology())) {
					errorLabel.setText("Warning: terms are from different ontologies");
				} else {
					errorLabel.setText(null);
				}
				
				addButton.setEnabled(true);
			} else {
				relatedTermLabel.addStyleName("dialog-message");
				relatedTermLabel.setText("Select related term below or from main window");
				addButton.setEnabled(false);
			}
		} else {
			ontologyLabel.setText(null);
			termLabel.setText(null);
			relatedTermLabel.setText(null);
			addButton.setEnabled(false);
		}
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
	
	protected void setupTable(CellTable<Term> table) {	    
		table.setWidth("100%");
		table.addStyleName("gwt-CellTable");
		table.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
		table.setSelectionModel(new NoSelectionModel<Term>(keyProvider));
	    
		table.addColumn(new OntologyColumn(), "Ontology");
		table.addColumn(new TermColumn(), "Term");
	}
	
	private void addDialogWidgets() {
		VerticalPanel vertPanel = new VerticalPanel();
		Grid grid = new Grid(4,2);
		HorizontalPanel buttonsHPanel = new HorizontalPanel();
		DisclosurePanel recentlyViewedPanel = new DisclosurePanel("Recently Viewed Terms");
		DisclosurePanel recentlyCreatedPanel = new DisclosurePanel("Recently Created Terms");
		Button cancelButton = new Button("Cancel");
		
		recentlyViewedPanel.getHeader().addStyleName("dialog-label");
		recentlyViewedPanel.add(recentlyViewTable);
		
		recentlyCreatedPanel.getHeader().addStyleName("dialog-label");
		recentlyCreatedPanel.add(recentlyCreatedTable);
				
		cancelButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				dialogBox.hide();
				if(currentTerm != null) {
					eventBus.fireEvent(new ViewTermEvent(currentTerm));
				}
			}
		});
		
		grid.addStyleName("gwt-Grid");
		errorLabel.addStyleName("dialog-error");
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
		grid.setWidget(++row, col, typeDropBox);
		grid.setWidget(++row, col, relatedTermLabel);
		
		for(row = col = 0; row < grid.getRowCount(); row++) {
			Label label = (Label)grid.getWidget(row, col);
			label.addStyleName("dialog-label");
		}
				
		buttonsHPanel.add(addButton);
		buttonsHPanel.add(cancelButton);
		
		vertPanel.add(grid);
		vertPanel.add(recentlyViewedPanel);
		vertPanel.add(recentlyCreatedPanel);
		vertPanel.add(errorLabel);
		vertPanel.add(buttonsHPanel);
		dialogBox.setWidget(vertPanel);
	}
}
