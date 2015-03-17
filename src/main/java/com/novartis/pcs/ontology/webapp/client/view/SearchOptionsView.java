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
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Panel;
import com.novartis.pcs.ontology.entity.Ontology;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.webapp.client.OntoBrowserServiceAsync;
import com.novartis.pcs.ontology.webapp.client.event.SearchEvent;
import com.novartis.pcs.ontology.webapp.client.event.SearchHandler;

public class SearchOptionsView extends OntoBrowserView 
		implements SearchOptionsProvider, SearchHandler, 
		ChangeHandler, ClickHandler {
	private final ListBox ontologiesDropBox = new ListBox();
	private final CheckBox includeSynonyms = new CheckBox();
	
	private String currentSearchPattern;
	private Object currentSearchSource;
		
	public SearchOptionsView(EventBus eventBus, OntoBrowserServiceAsync service) {
		super(eventBus, service);
		
		ontologiesDropBox.addItem("all");
		ontologiesDropBox.addChangeHandler(this);
		includeSynonyms.addClickHandler(this);		
		
		initWidget(createPanel());
		addStyleName("padded-border");
		
		eventBus.addHandler(SearchEvent.TYPE, this);
		service.loadRootTerms(new AsyncCallback<List<Term>>() {			
			@Override
			public void onSuccess(List<Term> terms) {				
				for(Term term : terms) {
					Ontology ontology = term.getOntology();
					String name = ontology.getName();
					ontologiesDropBox.addItem(name);
				}
			}
			
			@Override
			public void onFailure(Throwable caught) {
				GWT.log("Failed to load root terms", caught);
				ErrorView.instance().onUncaughtException(caught);
			}
		});
	}
	
	@Override
	public boolean includeSynonyms() {
		return includeSynonyms.getValue();
	}

	@Override
	public String filterByOntology() {
		int index = ontologiesDropBox.getSelectedIndex();
		return  index > 0 ? ontologiesDropBox.getValue(index) : null;
	}
	
	@Override
	public void onSearch(SearchEvent event) {
		currentSearchSource = event.getSource();
		currentSearchPattern = event.getPattern();
	}

	@Override
	public void onClick(ClickEvent event) {
		fireSearchEvent();
	}

	@Override
	public void onChange(ChangeEvent event) {
		fireSearchEvent();		
	}
	
	private void fireSearchEvent() {
		if(currentSearchPattern != null && currentSearchPattern.trim().length() > 0) {
			if(currentSearchSource != null) {
				eventBus.fireEventFromSource(new SearchEvent(currentSearchPattern), currentSearchSource);
			} else {
				eventBus.fireEvent(new SearchEvent(currentSearchPattern));
			}
		}
	}
	
	private Panel createPanel() {
		Label label = new Label("Search Options");
		label.addStyleName("search-header");
		label.getElement().getStyle().setDisplay(Display.BLOCK);
		
		Grid grid = new Grid(2,2);
		grid.setText(0, 0, "Ontology/Codelist:");
		grid.setWidget(0, 1, ontologiesDropBox);
		grid.setText(1, 0, "Include Synonyms:");
		grid.setWidget(1, 1, includeSynonyms);
		grid.addStyleName("search-options");
		
		CellFormatter cellFormatter = grid.getCellFormatter();
		for(int i = 0; i < grid.getRowCount(); i++) {
			cellFormatter.addStyleName(i, 0, "search-option");
		}
		
		Panel panel = new FlowPanel();
		panel.add(label);
		panel.add(grid);
	    return panel;
	}
}
