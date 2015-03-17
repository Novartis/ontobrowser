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
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.ProvidesKey;
import com.novartis.pcs.ontology.entity.Relationship;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.service.util.StatusChecker;
import com.novartis.pcs.ontology.webapp.client.OntoBrowserServiceAsync;
import com.novartis.pcs.ontology.webapp.client.event.SearchEvent;
import com.novartis.pcs.ontology.webapp.client.event.ViewTermEvent;
import com.novartis.pcs.ontology.webapp.client.event.ViewTermHandler;

public class ReplaceTermPopup implements OntoBrowserPopup, ViewTermHandler, ClickHandler {
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
	private final Label replaceByTermLabel = new Label("Replace by");
	private final TextArea commentsField = new TextArea();
	private final Label errorLabel = new Label();
	private final Button obsoleteButton = new Button("Obsolete");
	
	private final ProvidesKey<Term> keyProvider = new EntityKeyProvider<Term>();
	private final ListDataProvider<Term> recentlyViewedDataProvider = new ListDataProvider<Term>(keyProvider);
	private final CellTable<Term> recentlyViewTable = new CellTable<Term>(keyProvider);
	
	private final ListDataProvider<Term> recentlyCreatedDataProvider = new ListDataProvider<Term>(keyProvider);
	private final CellTable<Term> recentlyCreatedTable = new CellTable<Term>(keyProvider);
	
	private Term obsoleteTerm;
	private Term currentTerm;
	
	public ReplaceTermPopup(final OntoBrowserServiceAsync service,
			final EventBus eventBus) {
		this.service = service;
		this.eventBus = eventBus;
				
		dialogBox.setText("Confirm term obsoletion");
		dialogBox.setGlassEnabled(false);
		dialogBox.setAnimationEnabled(true);
		
		recentlyViewedDataProvider.addDataDisplay(recentlyViewTable);
		recentlyCreatedDataProvider.addDataDisplay(recentlyCreatedTable);
		
		setupTable(recentlyViewTable);
		setupTable(recentlyCreatedTable);
		addDialogWidgets();
		
		commentsField.getElement().getParentElement().addClassName("text-area-right-padded");
		commentsField.setWidth("100%");
		commentsField.setVisibleLines(2);
		
		obsoleteButton.addClickHandler(this);

		eventBus.addHandler(ViewTermEvent.TYPE, this);
	}
	
	@Override
	public void show() {
		obsoleteTerm = currentTerm;
		updateLabels();
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
				terms.remove(obsoleteTerm);
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
		currentTerm = event.getTerm();
		updateLabels();
		
		List<Term> lastViewed = recentlyViewedDataProvider.getList();
		if(lastViewed.contains(currentTerm)) {
			lastViewed.remove(currentTerm);
		}
		
		lastViewed.add(0, currentTerm);
		
		if(lastViewed.size() > MAX_LAST_VIEWED) {
			lastViewed.remove(MAX_LAST_VIEWED-1);
		}
	}
	
	@Override
	public void onClick(ClickEvent event) {
		if(obsoleteTerm != null) {
			obsoleteButton.setEnabled(false);
			errorLabel.setText(null);
			busyIndicator.busy();
			service.obsoleteTerm(obsoleteTerm.getId(),
					!obsoleteTerm.equals(currentTerm) ?
							currentTerm.getId() : 0,
					commentsField.getValue(),
					new AsyncCallback<Term>() {
				public void onFailure(Throwable caught) {
					GWT.log("Failed to obsolete term", caught);
					errorLabel.setText(caught.getMessage());
					busyIndicator.idle();
					obsoleteButton.setEnabled(true);
				}
	
				public void onSuccess(Term term) {
					if(term.getReplacedBy() != null) {
						eventBus.fireEvent(new ViewTermEvent(term.getReplacedBy()));
					} else if(term.equals(currentTerm)) {
						Term parent = null;
						for(Relationship relationship : term.getRelationships()) {
							if(StatusChecker.isValid(relationship.getRelatedTerm())) {
								parent = relationship.getRelatedTerm();
								if(relationship.getType().getRelationship().equals("is_a")) {
									break;
								}
							}
						}
						if(parent != null) {
							eventBus.fireEvent(new ViewTermEvent(parent));
						}
					}
					
					busyIndicator.idle();
					dialogBox.hide();
					commentsField.setValue(null);
					obsoleteButton.setEnabled(true);
				}
			});
		}
	}
	
	private void updateLabels() {
		if(obsoleteTerm != null) {
			ontologyLabel.setText(obsoleteTerm.getOntology().getName());
			termLabel.setText(obsoleteTerm.getName());
			if(!obsoleteTerm.equals(currentTerm)) {
				replaceByTermLabel.removeStyleName("dialog-message");
				replaceByTermLabel.setText(currentTerm.getName());
				
				if(!currentTerm.getOntology().equals(obsoleteTerm.getOntology())) {
					errorLabel.setText("Warning: terms are from different ontologies");
				} else {
					errorLabel.setText(null);
				}
			} else {
				replaceByTermLabel.addStyleName("dialog-message");
				replaceByTermLabel.setText("Select replacement term below or from main window");
			}
		} else {
			ontologyLabel.setText(null);
			termLabel.setText(null);
			replaceByTermLabel.setText(null);
			errorLabel.setText(null);
		}
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
		Grid grid = new Grid(3,2);
		HorizontalPanel buttonsHPanel = new HorizontalPanel();
		DisclosurePanel recentlyViewedPanel = new DisclosurePanel("Recently Viewed Terms");
		DisclosurePanel recentlyCreatedPanel = new DisclosurePanel("Recently Created Terms");
		Label comments = new Label("Comments:");
		Button cancelButton = new Button("Cancel");
		
		comments.addStyleName("dialog-label");
		
		recentlyViewedPanel.getHeader().addStyleName("dialog-label");
		recentlyViewedPanel.add(recentlyViewTable);
		
		recentlyCreatedPanel.getHeader().addStyleName("dialog-label");
		recentlyCreatedPanel.add(recentlyCreatedTable);
				
		cancelButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				dialogBox.hide();
				obsoleteTerm = null;
				commentsField.setValue(null);
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
		grid.setWidget(++row, col, new Label("Replaced By:"));
		row = 0;
		col = 1;
		grid.setWidget(row, col, ontologyLabel);
		grid.setWidget(++row, col, termLabel);
		grid.setWidget(++row, col, replaceByTermLabel);
		
		for(row = col = 0; row < grid.getRowCount(); row++) {
			Label label = (Label)grid.getWidget(row, col);
			label.addStyleName("dialog-label");
		}			
								
		buttonsHPanel.add(obsoleteButton);
		buttonsHPanel.add(cancelButton);
		
		vertPanel.add(grid);
		vertPanel.add(recentlyViewedPanel);
		vertPanel.add(recentlyCreatedPanel);
		vertPanel.add(comments);
		vertPanel.add(commentsField);
		vertPanel.add(errorLabel);
		vertPanel.add(buttonsHPanel);
		dialogBox.setWidget(vertPanel);
	}
}
