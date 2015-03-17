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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.HasKeyboardPagingPolicy.KeyboardPagingPolicy;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.ProvidesKey;
import com.novartis.pcs.ontology.entity.ControlledVocabularyTerm;
import com.novartis.pcs.ontology.entity.Curator;
import com.novartis.pcs.ontology.entity.CuratorApprovalWeight.Entity;
import com.novartis.pcs.ontology.entity.Datasource;
import com.novartis.pcs.ontology.entity.InvalidEntityException;
import com.novartis.pcs.ontology.entity.Synonym;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.service.util.StatusChecker;
import com.novartis.pcs.ontology.service.util.SynonymComparator;
import com.novartis.pcs.ontology.webapp.client.OntoBrowserServiceAsync;
import com.novartis.pcs.ontology.webapp.client.event.EntityDeletedEvent;
import com.novartis.pcs.ontology.webapp.client.event.EntityDeletedHandler;
import com.novartis.pcs.ontology.webapp.client.event.EntityUpdatedEvent;
import com.novartis.pcs.ontology.webapp.client.event.EntityUpdatedHandler;
import com.novartis.pcs.ontology.webapp.client.event.SynonymDeletedEvent;
import com.novartis.pcs.ontology.webapp.client.event.SynonymUpdatedEvent;
import com.novartis.pcs.ontology.webapp.client.event.ViewTermEvent;
import com.novartis.pcs.ontology.webapp.client.event.ViewTermHandler;
import com.novartis.pcs.ontology.webapp.client.image.ImageResources;

public class TermSynonymsView extends OntoBrowserView implements ViewTermHandler,
		EntityUpdatedHandler<Synonym>, EntityDeletedHandler<Synonym> {
	public static class SynonymColumn extends Column<Synonym, String> {
		public SynonymColumn() {
			super(new ClickableTextCell());
		}

		@Override
		public String getValue(Synonym synonym) {
			return synonym.getSynonym();
		}
	};
	
	public static class TypeColumn extends TextColumn<Synonym> {
		@Override
		public String getValue(Synonym synonym) {
			return synonym.getType().toString();
		}
	};
	
	public static class ContextColumn extends TextColumn<Synonym> {
		@Override
		public String getValue(Synonym synonym) {
			String context = null;
			ControlledVocabularyTerm vocabTerm = synonym.getControlledVocabularyTerm();
			if(vocabTerm != null) {
				context = vocabTerm.getControlledVocabulary()
						.getContext()
						.getName();
			}
			return context;
		}
	};
	
	public static class SourceColumn extends TextColumn<Synonym> {
		@Override
		public String getValue(Synonym synonym) {
			String value = null;
			ControlledVocabularyTerm vocabTerm = synonym.getControlledVocabularyTerm();
			if(vocabTerm != null) {
				value = vocabTerm
						.getControlledVocabulary()
						.getDatasource()
						.getAcronym();
			} else if(synonym.getDatasource() != null) {
				value = synonym.getDatasource().getAcronym();
			}
			return value;
		}
	};
	
	private static class ReferenceIdColumn extends TextColumn<Synonym> {
		@Override
		public String getValue(Synonym synonym) {
			return synonym.getReferenceId();
		}
	};
	
	
	private static class StatusColumn extends TextColumn<Synonym> {
		@Override
		public String getValue(Synonym synonym) {
			return synonym.getStatus().toString();
		}
	};
		
	private final Panel panel = new SimplePanel();
	
	private final ProvidesKey<Synonym> keyProvider = new EntityKeyProvider<Synonym>();
	private final CellTable<Synonym> table = new CellTable<Synonym>(keyProvider);
	private final ControlledVocabularyTermLinksView linkedTermsView;
	
	private Term currentTerm;
	
	public TermSynonymsView(EventBus eventBus, OntoBrowserServiceAsync service) {
		super(eventBus, service);
		this.linkedTermsView = new ControlledVocabularyTermLinksView(service);
				
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
		synonymColumn.setCellStyleNames("clickable-text nowrap");
				
		table.addStyleName("gwt-CellTable");
		table.setWidth("100%");
		table.setKeyboardPagingPolicy(KeyboardPagingPolicy.CURRENT_PAGE);
		table.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
		table.setSelectionModel(new NoSelectionModel<Synonym>());
		
		table.addColumn(synonymColumn, "Synonym");
		table.addColumn(new TypeColumn(), "Type");
		table.addColumn(new ContextColumn(), "Context");
		table.addColumn(new SourceColumn(), "Source");
		table.addColumn(new ReferenceIdColumn(), "Id");
		table.addColumn(new StatusColumn(), "Status");
		
		panel.add(table);
		
		initWidget(panel);
		addStyleName("padded-border vert-scroll fixed-height");
		
		eventBus.addHandler(ViewTermEvent.TYPE, this);
		eventBus.addHandler(SynonymUpdatedEvent.TYPE, this);
		eventBus.addHandler(SynonymDeletedEvent.TYPE, this);
	}

	@Override
	public void onViewTerm(ViewTermEvent event) {
		currentTerm = event.getTerm();	
								
		Set<Synonym> synoyms = currentTerm.getSynonyms();
		List<Synonym> list = Collections.emptyList();
		if(synoyms != null && !synoyms.isEmpty()) {
			list = new ArrayList<Synonym>();
			for(Synonym synonym : synoyms) {
				if(StatusChecker.isValid(synonym)) {
					list.add(synonym);
				}
			}
			Collections.sort(list, new SynonymComparator());
		}
		table.setRowData(list);
		table.setRowCount(list.size(), true);
	}

	@Override
	public void onEntityUpdated(EntityUpdatedEvent<Synonym> event) {
		Synonym synonym = event.getEntity();
		if(currentTerm != null && currentTerm.equals(synonym.getTerm())) {
			Set<Synonym> synoyms = currentTerm.getSynonyms();
			synoyms.remove(synonym);
			synoyms.add(synonym);
			onViewTerm(new ViewTermEvent(currentTerm));
		}
	}

	@Override
	public void onEntityDeleted(EntityDeletedEvent<Synonym> event) {
		Synonym synonym = event.getEntity();
		if(currentTerm != null && currentTerm.equals(synonym.getTerm())) {
			Set<Synonym> synoyms = currentTerm.getSynonyms();
			synoyms.remove(synonym);
			onViewTerm(new ViewTermEvent(currentTerm));
		}		
	}
	
	public void setCurator(final Curator curator) {
		final ReplaceSynonymPopup replaceSynonymView = new ReplaceSynonymPopup(service, eventBus);
		
	    IconActionCell.Delegate<Synonym> delegate = new IconActionCell.Delegate<Synonym>() {
	    	@Override
	    	public void execute(final Synonym synonym) {
	    		switch(synonym.getStatus()) {
	    		case PENDING:
	    			service.delete(synonym, 
							new AsyncCallback<Void>() {
						@Override
						public void onFailure(Throwable caught) {
							GWT.log("Failed to delete pending synonym", caught);
							if(caught instanceof InvalidEntityException) {
								InvalidEntityException e = (InvalidEntityException)caught;
								//errorLabel.setText(e.getMessage() + ": " + e.getEntity());
								//errorLabel.setVisible(true);
							} else {
								ErrorView.instance().onUncaughtException(caught);
							}
						}
	
						@Override
						public void onSuccess(Void nothing) {
							eventBus.fireEvent(new SynonymDeletedEvent(synonym));
						}
					});
	    			break;
	    		case APPROVED:
	    			replaceSynonymView.setSynonym(synonym);
	    			replaceSynonymView.show();
	    			
	    			/*
	    			service.obsoleteSynonym(synonym.getId(), 0, null, new AsyncCallback<Synonym>() {
						@Override
						public void onFailure(Throwable caught) {
							GWT.log("Failed to obsolete synonym", caught);
							if(caught instanceof InvalidEntityException) {
								InvalidEntityException e = (InvalidEntityException)caught;
								//errorLabel.setText(e.getMessage() + ": " + e.getEntity());
								//errorLabel.setVisible(true);
							} else {
								ErrorView.instance().onUncaughtException(caught);
							}
						}
	
						@Override
						public void onSuccess(Synonym synonym) {
							eventBus.fireEvent(new SynonymUpdatedEvent(synonym));
						}
					});
					*/
	    			break;
	    		
	    		}
	    	}
	    };
	    		    
	    Column<Synonym, Synonym> deleteColumn = 
	    		new Column<Synonym, Synonym>(new IconActionCell<Synonym>(ImageResources.INSTANCE.deleteIcon(),
	    				delegate)) {
	    	@Override
	    	public Synonym getValue(Synonym synonym) {
	    		Datasource datasource = null;
    			if(synonym.getControlledVocabularyTerm() != null) {
    				datasource = synonym.getControlledVocabularyTerm()
    						.getControlledVocabulary().getDatasource();
    			} else if(synonym.getDatasource() != null) {
    				datasource = synonym.getDatasource();
    			}
	    		
	    		if(datasource != null && datasource.isInternal()) {
	    			switch(synonym.getStatus()) {
		    		case PENDING:
		    			return curator.equals(synonym.getCreatedBy()) ? synonym : null;
		    		case APPROVED:
		    			return BigDecimal.ONE.equals(curator.getEntityApprovalWeight(Entity.TERM_SYNONYM)) ? synonym : null;
		    		}
	    		}
	    		return null;
	    	}
	    };
	    deleteColumn.setCellStyleNames("icon-action");
	    table.addColumn(deleteColumn, SafeHtmlUtils.fromSafeConstant("&nbsp;"));
	}
}
