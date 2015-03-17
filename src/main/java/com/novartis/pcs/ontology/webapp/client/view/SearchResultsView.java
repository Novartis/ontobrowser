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
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineHyperlink;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import com.novartis.pcs.ontology.service.search.result.HTMLSearchResult;
import com.novartis.pcs.ontology.service.search.result.InvalidQuerySyntaxException;
import com.novartis.pcs.ontology.webapp.client.OntoBrowserServiceAsync;
import com.novartis.pcs.ontology.webapp.client.event.SearchEvent;
import com.novartis.pcs.ontology.webapp.client.event.SearchHandler;

public class SearchResultsView extends OntoBrowserView 
		implements SearchHandler {
	private final SearchOptionsProvider searchOptions;
	private final UnorderedList ul = new UnorderedList();
	private String lastPattern = null;
	private BusyIndicatorHandler busyIndicator;
	
	public SearchResultsView(EventBus eventBus,
			OntoBrowserServiceAsync service, 
			SearchOptionsProvider searchOptions) {
		super(eventBus,service);
		this.searchOptions = searchOptions;
						
		initWidget(createPanel());
		addStyleName("search-results padded-border vert-scroll");
		
		eventBus.addHandler(SearchEvent.TYPE, this);
	}

	@Override
	public void onSearch(SearchEvent event) {
		final String pattern = event.getPattern();
		final Object source = event.getSource(); 
		lastPattern = pattern;
		
		if(source != null && busyIndicator == null && source instanceof Widget) {
			busyIndicator = new WidgetBusyIndicatorHandler((Widget)source, "busy-icon-left", "error-icon-left");
		}
		
		if(pattern != null && pattern.length() > 1) {
			if(busyIndicator != null) { 
				busyIndicator.busy();
			}
			
			service.search(pattern, searchOptions.includeSynonyms(), 
					new AsyncCallback<List<HTMLSearchResult>>() {
				public void onFailure(Throwable caught) {
					ul.clear();
					if(busyIndicator != null) {
						busyIndicator.idle();
					}
					if(caught instanceof InvalidQuerySyntaxException) {
						if(busyIndicator != null) {
							busyIndicator.error();
						}
					} else {
						GWT.log("Failed to load search results", caught);
						ErrorView.instance().onUncaughtException(caught);
					}
				}
	
				public void onSuccess(List<HTMLSearchResult> results) {
					if (pattern == lastPattern) {
						String filterByOntology = searchOptions.filterByOntology();
						ul.clear();
						for (HTMLSearchResult result : results) {
							if(filterByOntology == null 
									|| filterByOntology.equals(result.getOntology())) {
								ListItem li = new ListItem();
								InlineHyperlink hyperlink = new InlineHyperlink(
										SafeHtmlUtils.fromTrustedString(result.getHtml()), 
										result.getReferenceId());
								li.add(hyperlink);
								
								if(filterByOntology == null) {
									InlineLabel ontology = new InlineLabel(result.getOntology());
									ontology.addStyleName("search-result-tag");
									li.add(ontology);
								}
								
								if(result.isSynonym()) {
									InlineLabel synonym = new InlineLabel("synonym");
									synonym.addStyleName("search-result-tag");
									li.add(synonym);
								}
								
								ul.add(li);
							}
						}
					} else {
						GWT.log("Ignoring results from previous search: " + pattern);
					}
					if(busyIndicator != null) {
						busyIndicator.idle();
					}
				}
			});
		} else {
			ul.clear();
		}
	}
	
	private Panel createPanel() {
		Label label = new Label("Search Results");
		label.addStyleName("search-header");
		label.getElement().getStyle().setDisplay(Display.BLOCK);
		
		ul.addStyleName("search-results");
		
		Panel panel = new FlowPanel();
		panel.add(label);
		panel.add(ul);
	    return panel;
	}
	
}
