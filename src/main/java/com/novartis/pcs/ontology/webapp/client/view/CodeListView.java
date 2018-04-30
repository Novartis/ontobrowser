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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.cellview.client.HasKeyboardPagingPolicy.KeyboardPagingPolicy;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.novartis.pcs.ontology.entity.Ontology;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.webapp.client.OntoBrowserServiceAsync;
import com.novartis.pcs.ontology.webapp.client.event.ViewTermEvent;
import com.novartis.pcs.ontology.webapp.client.event.ViewTermHandler;
import com.novartis.pcs.ontology.webapp.client.image.ImageResources;

public class CodeListView extends OntoBrowserView implements ViewTermHandler {	
	private final Panel codelistContainer = new SimplePanel();
	private final ProvidesKey<Term> keyProvider = new EntityKeyProvider<Term>();
	private final TermCell cell = new TermCell();
	private final CellList<Term> list = new CellList<Term>(cell, keyProvider); 
	private final SingleSelectionModel<Term> selection = new SingleSelectionModel<Term>(keyProvider);;
	private final Map<Term,Integer> termIndex = new HashMap<Term,Integer>();
	
	private Ontology currentCodelist;
	
	public CodeListView(EventBus eventBus, OntoBrowserServiceAsync service) {
		super(eventBus,service);
		
		Image emptyListWidget = new Image(ImageResources.INSTANCE.spinner());
		emptyListWidget.setStyleName("float-right");
		list.setEmptyListWidget(emptyListWidget);
		list.setKeyboardPagingPolicy(KeyboardPagingPolicy.CURRENT_PAGE);
		list.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
		list.setSelectionModel(selection);
		selection.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
			public void onSelectionChange(SelectionChangeEvent event) {
				Term selected = selection.getSelectedObject();
				if(selected != null) {
					History.newItem(selected.getReferenceId());
				}
			}
		});
				
		codelistContainer.add(list);
		
		initWidget(codelistContainer);
		addStyleName("padded-border vert-scroll");
		eventBus.addHandler(ViewTermEvent.TYPE, this);
	}

	@Override
	public void onViewTerm(ViewTermEvent event) {
		final Term term = event.getTerm();
		final Ontology ontology = term.getOntology();		
				
		if(ontology.isCodelist()) {			
			if(currentCodelist == null || !currentCodelist.equals(ontology)) {
				list.setRowData(Collections.<Term>emptyList());
				list.setRowCount(0, true);
				termIndex.clear();
				
				service.loadOntologyTerms(ontology.getName(), new AsyncCallback<List<Term>>() {
	
					@Override
					public void onFailure(Throwable caught) {
						GWT.log("Failed to load all ontology terms", caught);
						ErrorView.instance().onUncaughtException(caught);
					}
	
					@Override
					public void onSuccess(List<Term> terms) {
						list.setRowData(terms);
						list.setRowCount(terms.size(), true);
						selection.setSelected(term, true);
						currentCodelist = term.getOntology();
						
						for(int i = 0; i < terms.size(); i++) {
							Term t = terms.get(i);
							termIndex.put(t, i);
							
							if(t.equals(term)) {
								Element row = list.getRowElement(i);
							    row.scrollIntoView();
							}
						}
					}
				});
			} else if(!selection.isSelected(term)) {
				selection.setSelected(term, true);
				
				Integer index = termIndex.get(term);
				if(index != null) {
					Element row = list.getRowElement(index);
				    row.scrollIntoView();
				}
			}
		}
	}
/*
	@Override
	public void onResize() {
		// For some reason GWT throws a ClassCastException on resize.
		try {
			super.onResize();
		} catch(ClassCastException e) {
			GWT.log("ClassCastException on resize", e);
		}
	}
*/
}
