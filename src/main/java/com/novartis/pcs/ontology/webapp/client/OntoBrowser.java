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
package com.novartis.pcs.ontology.webapp.client;

import java.math.BigDecimal;
import java.util.List;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.novartis.pcs.ontology.entity.Curator;
import com.novartis.pcs.ontology.entity.CuratorApprovalWeight.Entity;
import com.novartis.pcs.ontology.entity.Ontology;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.webapp.client.event.ViewTermEvent;
import com.novartis.pcs.ontology.webapp.client.view.AddRelationshipPopup;
import com.novartis.pcs.ontology.webapp.client.view.AddSynonymPopup;
import com.novartis.pcs.ontology.webapp.client.view.ApproveRejectPopup;
import com.novartis.pcs.ontology.webapp.client.view.ChangePasswordPopup;
import com.novartis.pcs.ontology.webapp.client.view.CreateChildTermPopup;
import com.novartis.pcs.ontology.webapp.client.view.CrossRefPopup;
import com.novartis.pcs.ontology.webapp.client.view.ErrorView;
import com.novartis.pcs.ontology.webapp.client.view.HistoryPopup;
import com.novartis.pcs.ontology.webapp.client.view.LegendPopup;
import com.novartis.pcs.ontology.webapp.client.view.OntoBrowserPopup;
import com.novartis.pcs.ontology.webapp.client.view.RelatedTermsView;
import com.novartis.pcs.ontology.webapp.client.view.ReplaceTermPopup;
import com.novartis.pcs.ontology.webapp.client.view.SVGView;
import com.novartis.pcs.ontology.webapp.client.view.SearchInputView;
import com.novartis.pcs.ontology.webapp.client.view.SearchOptionsView;
import com.novartis.pcs.ontology.webapp.client.view.SearchResultsView;
import com.novartis.pcs.ontology.webapp.client.view.TermDetailsView;
import com.novartis.pcs.ontology.webapp.client.view.TermSynonymsView;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class OntoBrowser implements EntryPoint, ValueChangeHandler<String> {
	private final EventBus eventBus = new SimpleEventBus();
	
	/**
	 * Create a remote service proxy to talk to the server-side service.
	 */
	private final OntoBrowserServiceAsync service = 
			GWT.create(OntoBrowserService.class);
	
	private final MenuBar menuBar = new MenuBar();
	
	/**
	 * This is the entry point method.
	 */
	@Override
	public void onModuleLoad() {
		GWT.setUncaughtExceptionHandler(ErrorView.instance());
		History.addValueChangeHandler(this);
		layoutViews();
		
		final String historyToken = History.getToken();
		if(historyToken != null && historyToken.length() > 0) {
			History.fireCurrentHistoryState();
		}
		
		service.loadRootTerms(new AsyncCallback<List<Term>>() {			
			@Override
			public void onSuccess(List<Term> terms) {
				createOntologyMenu(terms);
								
				// If the application starts with no history token,
				// redirect to a new initial state.
				if(!terms.isEmpty() && 
						(historyToken == null || historyToken.length() == 0)) {
					History.newItem(terms.get(0).getReferenceId());
				}
			}
			
			@Override
			public void onFailure(Throwable caught) {
				GWT.log("Failed to load root terms", caught);
				ErrorView.instance().onUncaughtException(caught);
			}
		});
				
		service.loadCurrentCurator(new AsyncCallback<Curator>() {			
			@Override
			public void onSuccess(Curator curator) {
				createPopups(curator);
				// synonymsView.setCurator(curator);
				// relationshipsView.setCurator(curator);
			}
			
			@Override
			public void onFailure(Throwable caught) {
				GWT.log("Failed to load curator", caught);
				ErrorView.instance().onUncaughtException(caught);
			}
		});
	}
	
	@Override
	public void onValueChange(ValueChangeEvent<String> event) {
		final String historyToken = event.getValue();
		if(historyToken != null && historyToken.length() > 0) {
			GWT.log("History token: " + historyToken);
			service.loadTerm(historyToken, new AsyncCallback<Term>() {
				public void onFailure(Throwable caught) {
					GWT.log("Failed to load term: " + historyToken, caught);
					ErrorView.instance().onUncaughtException(caught);
				}

				public void onSuccess(Term term) {
					if(term != null) {
						eventBus.fireEvent(new ViewTermEvent(term));
					}
				}
			});
		}
	}
	
	private void layoutViews() {
		TermDetailsView termView = new TermDetailsView(eventBus, service);
		TermSynonymsView synonymsView = new TermSynonymsView(eventBus, service);
		RelatedTermsView relationshipsView = new RelatedTermsView(eventBus, service);
		SearchInputView searchInputView = new SearchInputView(eventBus, service);
		SearchOptionsView searchOptionsView = new SearchOptionsView(eventBus, service);
		SearchResultsView searchResultsView = new SearchResultsView(eventBus, service, searchOptionsView);
		SVGView svgView = new SVGView(eventBus, service);
		//CodeListView codelistView = new CodeListView(eventBus, service);
				
		DockLayoutPanel layoutPanel = new DockLayoutPanel(Unit.PX);
		HorizontalPanel southPanel = new HorizontalPanel();	
		southPanel.add(termView);
		southPanel.add(synonymsView);
		southPanel.add(relationshipsView);
		
		southPanel.setCellWidth(termView, "33.33%");
		southPanel.setCellWidth(synonymsView, "33.33%");
		southPanel.setCellWidth(relationshipsView, "33.33%");
		southPanel.setWidth("100%");
		
		FlowPanel eastPanel = new FlowPanel();										
		eastPanel.add(searchInputView);
		eastPanel.add(searchOptionsView);
		eastPanel.add(searchResultsView);
				
		layoutPanel.addNorth(menuBar, 30);
		layoutPanel.addSouth(southPanel, 197);		
		layoutPanel.addEast(eastPanel, 300);	
		layoutPanel.add(svgView);
				
		RootLayoutPanel.get().add(layoutPanel);
	}
		
	private void createOntologyMenu(List<Term> terms) {
		MenuBar menu = new MenuBar(true);
		menu.setAnimationEnabled(true);
		
		for(final Term term : terms) {
			Ontology ontology = term.getOntology();
			if(!ontology.isCodelist()) {
				menu.addItem(ontology.getName(), new Command() {
					public void execute() {
						History.newItem(term.getReferenceId());
					}
				});
			}
		}
				
		menuBar.insertItem(new MenuItem("Ontology", menu), 0);
	}
	
	private void createPopups(Curator curator) {
		if(curator != null) {					
			CreateChildTermPopup createTermPopup = new CreateChildTermPopup(service, eventBus);
			CrossRefPopup crossRefPopup = new CrossRefPopup(service, eventBus, curator, createTermPopup);
			ApproveRejectPopup approveRejectPopup = new ApproveRejectPopup(service, eventBus, curator);
			AddSynonymPopup addSynonymPopup = new AddSynonymPopup(service, eventBus);
			AddRelationshipPopup addRelationshipPopup = new AddRelationshipPopup(service, eventBus);
			
			createTermPopup.setSynonymProvider(crossRefPopup);
						
			MenuBar menu = new MenuBar(true);
			menu.setAnimationEnabled(true);
			createPopupMenuItem(menu, "Map Synonyms", crossRefPopup);
			createPopupMenuItem(menu, "Approve", approveRejectPopup);
			createPopupMenuItem(menu, "Create Child Term", createTermPopup);
			createPopupMenuItem(menu, "Add Synonym", addSynonymPopup);
			createPopupMenuItem(menu, "Add Relationship", addRelationshipPopup);
			
			if(BigDecimal.ONE.equals(curator.getEntityApprovalWeight(Entity.TERM))) {
				ReplaceTermPopup replaceTermPopup = new ReplaceTermPopup(service, eventBus);
				createPopupMenuItem(menu, "Obsolete Term", replaceTermPopup);
			}
			
			menuBar.addItem("Curate", menu);
		}			
		
		createPopupMenuItem(menuBar, "History", new HistoryPopup(eventBus, service));
		createPopupMenuItem(menuBar, "Legend", new LegendPopup());
		
		if(curator != null && curator.getPassword() != null) {
			ChangePasswordPopup changePasswordPopup = new ChangePasswordPopup(service);
			if(curator.isPasswordExpired()) {
				changePasswordPopup.show();
			}
			menuBar.addSeparator();
			createPopupMenuItem(menuBar, "Change Password", changePasswordPopup);
		}
	}
	
	private void createPopupMenuItem(MenuBar menu, final String text, final OntoBrowserPopup popup) {
		menu.addItem(text, new Command() {
			public void execute() {
				popup.show();
			}
		});
	}
}
