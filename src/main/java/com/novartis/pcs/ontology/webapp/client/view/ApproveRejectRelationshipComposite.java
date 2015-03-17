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
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.InlineHyperlink;
import com.novartis.pcs.ontology.entity.Curator;
import com.novartis.pcs.ontology.entity.CuratorApprovalWeight.Entity;
import com.novartis.pcs.ontology.entity.Relationship;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.webapp.client.OntoBrowserServiceAsync;
import com.novartis.pcs.ontology.webapp.client.event.RelationshipDeletedEvent;
import com.novartis.pcs.ontology.webapp.client.event.RelationshipUpdatedEvent;
import com.novartis.pcs.ontology.webapp.client.event.ViewTermEvent;

public class ApproveRejectRelationshipComposite extends ApproveRejectComposite<Relationship> {	
	private static class OntologyColumn extends ComparableTextColumn<Relationship> {
		@Override
		public String getValue(Relationship relationship) {
			return relationship.getTerm().getOntology().getName();
		}
	};
	
	private static class TermColumn extends
			Column<Relationship, InlineHyperlink> implements
			Comparator<Relationship> {
		public TermColumn() {
			super(new HyperlinkCell());
			setSortable(true);
		}

		@Override
		public InlineHyperlink getValue(Relationship relationship) {
			Term term = relationship.getTerm();
			return new InlineHyperlink(term.getName(), term.getReferenceId());
		}

		@Override
		public int compare(Relationship relship1, Relationship relship2) {
			Term term1 = relship1.getTerm();
			Term term2 = relship2.getTerm();
			return term1 == term2 ? 0 : term1.getName().compareToIgnoreCase(
					term2.getName());
		}
	};

	private static class RelationshipColumn extends TextColumn<Relationship> {
		@Override
		public String getValue(Relationship relationship) {
			return relationship.getType().getRelationship();
		}
	};

	private static class RelatedTermColumn extends
			Column<Relationship, InlineHyperlink> implements
			Comparator<Relationship> {
		public RelatedTermColumn() {
			super(new HyperlinkCell());
			setSortable(true);
		}

		@Override
		public InlineHyperlink getValue(Relationship relationship) {
			Term term = relationship.getRelatedTerm();
			return new InlineHyperlink(term.getName(), term.getReferenceId());
		}

		@Override
		public int compare(Relationship relship1, Relationship relship2) {
			Term term1 = relship1.getRelatedTerm();
			Term term2 = relship2.getRelatedTerm();
			return term1 == term2 ? 0 : term1.getName().compareToIgnoreCase(
					term2.getName());
		}
	};
		
	public ApproveRejectRelationshipComposite(OntoBrowserServiceAsync service,
			EventBus eventBus, Curator curator, BusyIndicatorHandler busyIndicatorHandler) {
		super(Entity.TERM_RELATIONSHIP, service, eventBus, curator, busyIndicatorHandler, 
				new EditRelationshipPopup(service, eventBus));
		eventBus.addHandler(RelationshipUpdatedEvent.TYPE, this);
	}
	
	@Override
	protected void addTableColumns(CellTable<Relationship> table) {
	    table.addColumn(new OntologyColumn(), "Ontology");
	    table.addColumn(new TermColumn(), "Term");
	    table.addColumn(new RelationshipColumn(), "Relationship");
	    table.addColumn(new RelatedTermColumn(), "Related Term");
	}
	
	@Override
	protected void fireUpdateEvent(Relationship relationship) {
		eventBus.fireEvent(new RelationshipUpdatedEvent(relationship));
		
		Term term = relationship.getTerm();
		Term relatedTerm = relationship.getRelatedTerm();
		if(term.equals(currentTerm) || relatedTerm.equals(currentTerm)) {
			eventBus.fireEvent(new ViewTermEvent(currentTerm));
		}
	}

	@Override
	protected void fireDeleteEvent(Relationship relationship) {
		eventBus.fireEvent(new RelationshipDeletedEvent(relationship));
		
		Term term = relationship.getTerm();
		Term relatedTerm = relationship.getRelatedTerm();
		if(term.equals(currentTerm) || relatedTerm.equals(currentTerm)) {
			eventBus.fireEvent(new ViewTermEvent(currentTerm));
		}
		
	}
}
