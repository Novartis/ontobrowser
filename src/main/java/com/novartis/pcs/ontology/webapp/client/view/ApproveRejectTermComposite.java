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

import java.util.Comparator;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.ui.InlineHyperlink;
import com.novartis.pcs.ontology.entity.Curator;
import com.novartis.pcs.ontology.entity.CuratorApprovalWeight.Entity;
import com.novartis.pcs.ontology.entity.Relationship;
import com.novartis.pcs.ontology.entity.Synonym;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.service.util.StatusChecker;
import com.novartis.pcs.ontology.webapp.client.OntoBrowserServiceAsync;
import com.novartis.pcs.ontology.webapp.client.event.RelationshipDeletedEvent;
import com.novartis.pcs.ontology.webapp.client.event.SynonymDeletedEvent;
import com.novartis.pcs.ontology.webapp.client.event.TermDeletedEvent;
import com.novartis.pcs.ontology.webapp.client.event.TermUpdatedEvent;
import com.novartis.pcs.ontology.webapp.client.event.ViewTermEvent;

public class ApproveRejectTermComposite extends ApproveRejectComposite<Term> {	
	
	private static class OntologyColumn extends ComparableTextColumn<Term> {
		@Override
		public String getValue(Term term) {
			return term.getOntology().getName();
		}
	};
	
	private static class TermColumn extends Column<Term, InlineHyperlink> 
			implements Comparator<Term> {
		public TermColumn() {
			super(new HyperlinkCell());
			setSortable(true);
		}

		@Override
		public InlineHyperlink getValue(Term term) {
			return new InlineHyperlink(term.getName(), term.getReferenceId());
		}

		@Override
		public int compare(Term term1, Term term2) {
			return term1 == term2 ? 0 : term1.getName().compareToIgnoreCase(term2.getName());
		}
	};
		
	public ApproveRejectTermComposite(OntoBrowserServiceAsync service,
			EventBus eventBus, Curator curator, BusyIndicatorHandler busyIndicatorHandler) {
		super(Entity.TERM, service, eventBus, curator, busyIndicatorHandler, 
				new EditTermPopup(service, eventBus, curator));
		eventBus.addHandler(TermUpdatedEvent.TYPE, this);
	}
	
	@Override
	protected void addTableColumns(CellTable<Term> table) {
	    table.addColumn(new OntologyColumn(), "Ontology");
	    table.addColumn(new TermColumn(), "Term");		
	}
	
	@Override
	protected void fireUpdateEvent(Term term) {	
		eventBus.fireEvent(new TermUpdatedEvent(term));
				
		if(term.equals(currentTerm)) {
			eventBus.fireEvent(new ViewTermEvent(term));	
		}
	}

	@Override
	protected void fireDeleteEvent(Term term) {
		for(Synonym synonym : term.getSynonyms()) {
			eventBus.fireEvent(new SynonymDeletedEvent(synonym));
		}
		
		for(Relationship relationship : term.getRelationships()) {
			eventBus.fireEvent(new RelationshipDeletedEvent(relationship));
		}
		
		eventBus.fireEvent(new TermDeletedEvent(term));
				
		if(term.equals(currentTerm)) {
			Term parent = null;
			for(Relationship relationship : term.getRelationships()) {
				if(StatusChecker.isValid(relationship)) {
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
		
	}
}
