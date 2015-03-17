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

import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.InlineHyperlink;
import com.novartis.pcs.ontology.entity.ControlledVocabularyTerm;
import com.novartis.pcs.ontology.entity.Curator;
import com.novartis.pcs.ontology.entity.CuratorApprovalWeight.Entity;
import com.novartis.pcs.ontology.entity.Synonym;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.webapp.client.OntoBrowserServiceAsync;
import com.novartis.pcs.ontology.webapp.client.event.SynonymDeletedEvent;
import com.novartis.pcs.ontology.webapp.client.event.SynonymUpdatedEvent;
import com.novartis.pcs.ontology.webapp.client.event.ViewTermEvent;

public class ApproveRejectSynonymComposite extends ApproveRejectComposite<Synonym> {	
	private static class OntologyColumn extends ComparableTextColumn<Synonym> {
		@Override
		public String getValue(Synonym synonym) {
			return synonym.getTerm().getOntology().getName();
		}
	};
	
	private static class TermColumn extends Column<Synonym, InlineHyperlink> 
			implements Comparator<Synonym> {
		public TermColumn() {
			super(new HyperlinkCell());
			setSortable(true);
		}

		@Override
		public InlineHyperlink getValue(Synonym synonym) {
			Term term = synonym.getTerm();
			return new InlineHyperlink(term.getName(), term.getReferenceId());
		}

		@Override
		public int compare(Synonym synonym1, Synonym synonym2) {
			Term term1 = synonym1.getTerm();
			Term term2 = synonym2.getTerm();
			return term1 == term2 ? 0 : term1.getName().compareToIgnoreCase(term2.getName());
		}
	};
		
	private static class SynonymColumn extends Column<Synonym, String> 
			implements Comparator<Synonym> { 
		public SynonymColumn() {
			super(new ClickableTextCell());
			setSortable(true);
		}

		@Override
		public String getValue(Synonym synonym) {
			return synonym.getSynonym();
		}
		
		@Override
		public int compare(Synonym s1, Synonym s2) {
			return s1 == s2 ? 0 : s1.getSynonym().compareToIgnoreCase(s2.getSynonym());
		}
	};
	
	private static class TypeColumn extends ComparableTextColumn<Synonym> { 
		@Override
		public String getValue(Synonym synonym) {
			return synonym.getType().toString();
		}
	};
	
	private static class SourceColumn extends ComparableTextColumn<Synonym> { 
		@Override
		public String getValue(Synonym synonym) {
			String value = null;
			ControlledVocabularyTerm vocabTerm = synonym.getControlledVocabularyTerm();
			if(vocabTerm != null) {
				value = vocabTerm
						.getControlledVocabulary()
						.getDatasource()
						.getAcronym();
			}
			return value;
		}
	};
	
	public static class UsageColumn extends Column<Synonym, Number> 
			implements Comparator<Synonym> {
		public UsageColumn() {
			super(new NumberCell());
			setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LOCALE_END);
			setSortable(true);
		}

		@Override
		public Number getValue(Synonym synonym) {
			Number usage = null;
			ControlledVocabularyTerm vocabTerm = synonym.getControlledVocabularyTerm();
			if(vocabTerm != null) {
				usage = new Integer(vocabTerm.getUsage());
			}
			
			return usage;
		}

		@Override
		public int compare(Synonym synonym1, Synonym synonym2) {
			if(synonym1 == synonym2) {
				return 0;
			}
			
			int usage1 = 0;
			int usage2 = 0;
			
			ControlledVocabularyTerm vocabTerm = synonym1.getControlledVocabularyTerm();
			if(vocabTerm != null) {
				usage1 = vocabTerm.getUsage();
			}
			
			vocabTerm = synonym2.getControlledVocabularyTerm();
			if(vocabTerm != null) {
				usage2 = vocabTerm.getUsage();
			}
						
			return usage1 == usage2 ? 0 : usage1 < usage2 ? -1 : 1;
		}
	};
		
	private final ControlledVocabularyTermLinksView linkedTermsView;
		
	public ApproveRejectSynonymComposite(OntoBrowserServiceAsync service,
			EventBus eventBus, Curator curator, BusyIndicatorHandler busyIndicatorHandler) {
		super(Entity.TERM_SYNONYM, service, eventBus, curator, busyIndicatorHandler, 
				new EditSynonymPopup(service, eventBus));
		this.linkedTermsView = new ControlledVocabularyTermLinksView(service);
		eventBus.addHandler(SynonymUpdatedEvent.TYPE, this);
	}
	
	@Override
	protected void addTableColumns(CellTable<Synonym> table) {
		SynonymColumn synonymColumn = new SynonymColumn();
		
		synonymColumn.setFieldUpdater(new FieldUpdater<Synonym, String>() {
			@Override
			public void update(int index, Synonym synonym, String value) {
				ControlledVocabularyTerm term = synonym.getControlledVocabularyTerm();
				if(term != null) {
					linkedTermsView.show(term);
				}
			}
		});
		synonymColumn.setCellStyleNames("clickable-text");
		
	    table.addColumn(new OntologyColumn(), "Ontology/Codelist");
	    table.addColumn(new TermColumn(), "Term");
	    table.addColumn(synonymColumn, "Synonym");
	    table.addColumn(new TypeColumn(), "Type");
	    table.addColumn(new SourceColumn(), "Source");
	    table.addColumn(new UsageColumn(), "Usage");
	}
	
	@Override
	protected void fireUpdateEvent(Synonym synonym) {
		eventBus.fireEvent(new SynonymUpdatedEvent(synonym));
		
		Term term = synonym.getTerm();
		if(term.equals(currentTerm)) {
			eventBus.fireEvent(new ViewTermEvent(term));
		}
	}

	@Override
	protected void fireDeleteEvent(Synonym synonym) {
		eventBus.fireEvent(new SynonymDeletedEvent(synonym));
	}
}
