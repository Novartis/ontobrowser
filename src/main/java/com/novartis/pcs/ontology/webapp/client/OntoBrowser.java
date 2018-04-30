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
import java.util.ArrayList;
import java.util.Collection;
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
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.novartis.pcs.ontology.entity.Curator;
import com.novartis.pcs.ontology.entity.CuratorApprovalWeight.Entity;
import com.novartis.pcs.ontology.entity.Ontology;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.webapp.client.event.ViewTermEvent;
import com.novartis.pcs.ontology.webapp.client.view.AddRelationshipPopup;
import com.novartis.pcs.ontology.webapp.client.view.AddSynonymPopup;
import com.novartis.pcs.ontology.webapp.client.view.ApproveRejectPopup;
import com.novartis.pcs.ontology.webapp.client.view.ChangePasswordPopup;
import com.novartis.pcs.ontology.webapp.client.view.CodeListView;
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
	private final Collection<MenuItem> ontologyMenuItems = new ArrayList<MenuItem>();
	
	private final DockLayoutPanel layoutPanel = new DockLayoutPanel(Unit.PX);
	private final FlowPanel centrePanel = new FlowPanel();
	private final HorizontalPanel southPanel = new HorizontalPanel();
	
	private final SVGView svgView = new SVGView(eventBus, service);
	private final CodeListView codelistView = new CodeListView(eventBus, service);
	
	private Widget currentCentrePanel = null;
	
	/**
	 * This is the entry point method.
	 */
	@Override
	public void onModuleLoad() {
		GWT.setUncaughtExceptionHandler(ErrorView.instance());
		History.addValueChangeHandler(this);
		layoutViews(false);
		RootLayoutPanel.get().add(layoutPanel);
		
		final String historyToken = History.getToken();
		if(historyToken != null && historyToken.length() > 0) {
			History.fireCurrentHistoryState();
		}
		
		service.loadRootTerms(new AsyncCallback<List<Term>>() {			
			@Override
			public void onSuccess(List<Term> terms) {
				createMenus(terms);
								
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
						boolean codelist = term.getOntology().isCodelist(); 
						layoutViews(codelist);
						
						for(MenuItem menuItem : ontologyMenuItems) {
							menuItem.setEnabled(!codelist);
						}
						
						eventBus.fireEvent(new ViewTermEvent(term));
					}
				}
			});
		}
	}
	
	private void layoutViews(boolean isCodelist) {
		if(currentCentrePanel == null) {
			Panel eastPanel = createEastPanel();
			createCentrePanel();
			createSouthPanel();
						
			layoutPanel.addNorth(menuBar, 30);		
			layoutPanel.addEast(eastPanel, 330);
		}
	
		if(!isCodelist && currentCentrePanel != svgView) {
			southPanel.setWidth("100%");
			layoutPanel.addSouth(southPanel, 197);	
			
			if(currentCentrePanel != null) {
				layoutPanel.remove(currentCentrePanel);
				layoutPanel.remove(codelistView);
			}
						
			layoutPanel.add(svgView);
			currentCentrePanel = svgView;
		} else if(isCodelist && currentCentrePanel != centrePanel) {
			if(currentCentrePanel != null) {
				layoutPanel.remove(currentCentrePanel);
			}
			layoutPanel.remove(southPanel);
			layoutPanel.addWest(codelistView, 440);
			layoutPanel.add(centrePanel);
			currentCentrePanel = centrePanel;
		}
	}
	
	private Panel createCentrePanel() {
		TermDetailsView termView = new TermDetailsView(eventBus, service);
		TermSynonymsView synonymsView = new TermSynonymsView(eventBus, service);
		synonymsView.addStyleName("remaining-height");
		
		centrePanel.add(termView);
		centrePanel.add(synonymsView);
		
		return centrePanel;
		
	}
	
	private Panel createEastPanel() {
		SearchInputView searchInputView = new SearchInputView(eventBus, service);
		SearchOptionsView searchOptionsView = new SearchOptionsView(eventBus, service);
		SearchResultsView searchResultsView = new SearchResultsView(eventBus, service, searchOptionsView);
		
		FlowPanel eastPanel = new FlowPanel();										
		eastPanel.add(searchInputView);
		eastPanel.add(searchOptionsView);
		eastPanel.add(searchResultsView);
		
		return eastPanel;
	}
	
	private Panel createSouthPanel() {
		TermDetailsView termView = new TermDetailsView(eventBus, service);
		TermSynonymsView synonymsView = new TermSynonymsView(eventBus, service);
		RelatedTermsView relationshipsView = new RelatedTermsView(eventBus, service);
		
		synonymsView.addStyleName("fixed-height");
		
		southPanel.add(termView);
		southPanel.add(synonymsView);
		southPanel.add(relationshipsView);
		
		southPanel.setCellWidth(termView, "33.33%");
		southPanel.setCellWidth(synonymsView, "33.33%");
		southPanel.setCellWidth(relationshipsView, "33.33%");
		
		return southPanel;
	}
		
	private void createMenus(List<Term> terms) {
		MenuBar ontologyMenu = new MenuBar(true);
		MenuBar codelistMenu = new MenuBar(true);
		
		ontologyMenu.setAnimationEnabled(true);
		codelistMenu.setAnimationEnabled(true);
		
		for(final Term term : terms) {
			Ontology ontology = term.getOntology();
			MenuBar menu = ontology.isCodelist() ?
					codelistMenu : ontologyMenu;
			menu.addItem(ontology.getName(), new Command() {
				public void execute() {
					History.newItem(term.getReferenceId());
				}
			});

		}
		
		menuBar.insertItem(new MenuItem("Ontology", ontologyMenu), 0);
		menuBar.insertItem(new MenuItem("Codelist", codelistMenu), 1);
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
			createPopupMenuItem(menu, "Add Synonym", addSynonymPopup);
			ontologyMenuItems.add(createPopupMenuItem(menu, "Add Relationship", addRelationshipPopup));
			ontologyMenuItems.add(createPopupMenuItem(menu, "Create Child Term", createTermPopup));
			
			if(BigDecimal.ONE.equals(curator.getEntityApprovalWeight(Entity.TERM))) {
				ReplaceTermPopup replaceTermPopup = new ReplaceTermPopup(service, eventBus);
				ontologyMenuItems.add(createPopupMenuItem(menu, "Obsolete Term", replaceTermPopup));
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
	
	private MenuItem createPopupMenuItem(MenuBar menu, final String text, final OntoBrowserPopup popup) {
		return menu.addItem(text, new Command() {
			public void execute() {
				popup.show();
			}
		});
	}
}
