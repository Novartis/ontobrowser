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

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.novartis.pcs.ontology.entity.CrossReference;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.webapp.client.OntoBrowserServiceAsync;
import com.novartis.pcs.ontology.webapp.client.event.EntityDeletedEvent;
import com.novartis.pcs.ontology.webapp.client.event.EntityDeletedHandler;
import com.novartis.pcs.ontology.webapp.client.event.EntityUpdatedEvent;
import com.novartis.pcs.ontology.webapp.client.event.EntityUpdatedHandler;
import com.novartis.pcs.ontology.webapp.client.event.TermDeletedEvent;
import com.novartis.pcs.ontology.webapp.client.event.TermUpdatedEvent;
import com.novartis.pcs.ontology.webapp.client.event.ViewTermEvent;
import com.novartis.pcs.ontology.webapp.client.event.ViewTermHandler;
import com.novartis.pcs.ontology.webapp.client.util.UrlValidator;

public class TermDetailsView extends OntoBrowserView implements ViewTermHandler, 
		EntityUpdatedHandler<Term>, EntityDeletedHandler<Term> {
	private final String[] LABELS = {
			"Id", "Ontology", "Term", "Definition", "Source", "Reference Id", "Comments", "Created By", "Status" 
		};
		
	private final Panel panel = new SimplePanel();
	private final Grid grid = new Grid(LABELS.length, 2);
	
	private Term currentTerm;
		
	public TermDetailsView(EventBus eventBus, OntoBrowserServiceAsync service) {
		super(eventBus, service);
		
		for(int i = 0; i < LABELS.length; i++) {
			grid.setText(i, 0, LABELS[i] + ":");
		}
				
		CellFormatter cellFormatter = grid.getCellFormatter(); 
		for(int row = 0; row < grid.getRowCount(); row++) {
			cellFormatter.addStyleName(row, 0, "label");
		}
		
		panel.add(grid);
		initWidget(panel);
		addStyleName("padded-border vert-scroll fixed-height");
		
		eventBus.addHandler(ViewTermEvent.TYPE, this);
		eventBus.addHandler(TermUpdatedEvent.TYPE, this);
		eventBus.addHandler(TermDeletedEvent.TYPE, this);
	}

	@Override
	public void onViewTerm(ViewTermEvent event) {
		currentTerm = event.getTerm();
		int row = 0;
		grid.setText(row++, 1, currentTerm.getReferenceId());
		grid.setText(row, 0, currentTerm.getOntology().isCodelist() ?
				"Codelist:" : "Ontology:");	
		grid.setText(row++, 1, currentTerm.getOntology().getName());
		if(UrlValidator.validate(currentTerm.getUrl())) {
			Anchor a = new Anchor(currentTerm.getName(), currentTerm.getUrl().trim());
			a.setTarget("TermUrl");
			grid.setWidget(row++, 1, a);
		} else {
			grid.setText(row++, 1, currentTerm.getName());
		}
		
		grid.setText(row++, 1, currentTerm.getDefinition());
		
		boolean xrefPresent = false;
		for(CrossReference xref : currentTerm.getCrossReferences()) {
			if(!xref.isDefinitionCrossReference()
					&& xref.getDatasource() != null) {
				grid.setText(row++, 1, xref.getDatasource().getAcronym());
				
				if(xref.getReferenceId() != null) {
					grid.setText(row++, 1, xref.getReferenceId());
				} else {
					grid.setText(row++, 1, null);
				}
				xrefPresent = true;
				break;
			}
		}
		
		if(!xrefPresent) {
			grid.setText(row++, 1, null);
			grid.setText(row++, 1, null);
		}
		
		grid.setText(row++, 1, currentTerm.getComments());
		grid.setText(row++, 1, currentTerm.getCreatedBy().getUsername());
		grid.setText(row++, 1, currentTerm.getStatus().toString());
	}

	@Override
	public void onEntityUpdated(EntityUpdatedEvent<Term> event) {
		Term term = event.getEntity();
		if(currentTerm != null && currentTerm.equals(term)) {
			onViewTerm(new ViewTermEvent(term));
		}
	}

	@Override
	public void onEntityDeleted(EntityDeletedEvent<Term> event) {
				
	}
}
