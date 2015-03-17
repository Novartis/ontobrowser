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

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.HasKeyboardPagingPolicy.KeyboardPagingPolicy;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.InlineHyperlink;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.ProvidesKey;
import com.novartis.pcs.ontology.entity.Curator;
import com.novartis.pcs.ontology.entity.CuratorApprovalWeight.Entity;
import com.novartis.pcs.ontology.entity.InvalidEntityException;
import com.novartis.pcs.ontology.entity.Relationship;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.service.util.StatusChecker;
import com.novartis.pcs.ontology.webapp.client.OntoBrowserServiceAsync;
import com.novartis.pcs.ontology.webapp.client.event.EntityDeletedEvent;
import com.novartis.pcs.ontology.webapp.client.event.EntityDeletedHandler;
import com.novartis.pcs.ontology.webapp.client.event.EntityUpdatedEvent;
import com.novartis.pcs.ontology.webapp.client.event.EntityUpdatedHandler;
import com.novartis.pcs.ontology.webapp.client.event.RelationshipDeletedEvent;
import com.novartis.pcs.ontology.webapp.client.event.RelationshipUpdatedEvent;
import com.novartis.pcs.ontology.webapp.client.event.ViewTermEvent;
import com.novartis.pcs.ontology.webapp.client.event.ViewTermHandler;
import com.novartis.pcs.ontology.webapp.client.image.ImageResources;

public class RelatedTermsView extends OntoBrowserView implements ViewTermHandler, 
		EntityUpdatedHandler<Relationship>, EntityDeletedHandler<Relationship> {
	private static class RelatedTermColumn extends Column<Relationship, InlineHyperlink> {
		public RelatedTermColumn() {
			super(new HyperlinkCell());
		}
		
		@Override
		public InlineHyperlink getValue(Relationship relationship) {
			Term term = relationship.getRelatedTerm();
			return new InlineHyperlink(term.getName(), term.getReferenceId()); 
		}
	};
	
	private static class RelationshipColumn extends TextColumn<Relationship> {
		@Override
		public String getValue(Relationship relationship) {
			return relationship.getType().getRelationship();
		}
	};
	
	private static class StatusColumn extends TextColumn<Relationship> {
		@Override
		public String getValue(Relationship relationship) {
			return relationship.getStatus().toString();
		}
	};
		
	private final Panel panel = new SimplePanel();
	
	private final ProvidesKey<Relationship> keyProvider = new EntityKeyProvider<Relationship>();
	private final CellTable<Relationship> table = new CellTable<Relationship>(keyProvider);
	
	private Term currentTerm;
	
	public RelatedTermsView(EventBus eventBus, OntoBrowserServiceAsync service) {
		super(eventBus, service);
						
		table.addStyleName("gwt-CellTable");
		table.setWidth("100%");
		table.setKeyboardPagingPolicy(KeyboardPagingPolicy.CURRENT_PAGE);
		table.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
		table.setSelectionModel(new NoSelectionModel<Relationship>());
		
		table.addColumn(new RelationshipColumn(), "Relationship");
		table.addColumn(new RelatedTermColumn(), "Related Term");
		table.addColumn(new StatusColumn(), "Status");
				
		panel.add(table);
		initWidget(panel);
		addStyleName("padded-border vert-scroll fixed-height");
		
		eventBus.addHandler(ViewTermEvent.TYPE, this);
		eventBus.addHandler(RelationshipUpdatedEvent.TYPE, this);
		eventBus.addHandler(RelationshipDeletedEvent.TYPE, this);
	}

	@Override
	public void onViewTerm(ViewTermEvent event) {
		currentTerm = event.getTerm();
		
		if(!currentTerm.getOntology().isCodelist()) {
			Set<Relationship> decendents = currentTerm.getRelationships();
			List<Relationship> list = Collections.emptyList();
			if(decendents != null && !decendents.isEmpty()) {
				list = new ArrayList<Relationship>();
				for(Relationship relationship : decendents) {
					if(StatusChecker.isValid(relationship)) {
						list.add(relationship);
					}
				}
			}
			table.setRowData(list);
			table.setRowCount(list.size(), true);
			
		}
	}

	@Override
	public void onEntityUpdated(EntityUpdatedEvent<Relationship> event) {
		Relationship relationship = event.getEntity();
		if(currentTerm != null && currentTerm.equals(relationship.getTerm())) {
			Set<Relationship> relationships = currentTerm.getRelationships();
			relationships.remove(relationship);
			relationships.add(relationship);
			onViewTerm(new ViewTermEvent(currentTerm));
		}		
	}

	@Override
	public void onEntityDeleted(EntityDeletedEvent<Relationship> event) {
		Relationship relationship = event.getEntity();
		if(currentTerm != null && currentTerm.equals(relationship.getTerm())) {
			Set<Relationship> relationships = currentTerm.getRelationships();
			relationships.remove(relationship);
			onViewTerm(new ViewTermEvent(currentTerm));
		}
	}
	
	public void setCurator(final Curator curator) {
		final ReplaceRelationshipPopup replaceRelationshipView = new ReplaceRelationshipPopup(service, eventBus);
		
		IconActionCell.Delegate<Relationship> delegate = new IconActionCell.Delegate<Relationship>() {
	    	@Override
	    	public void execute(final Relationship relationship) {
	    		switch(relationship.getStatus()) {
	    		case PENDING:
	    			service.delete(relationship, 
							new AsyncCallback<Void>() {
						@Override
						public void onFailure(Throwable caught) {
							GWT.log("Failed to delete pending relationship", caught);
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
							eventBus.fireEvent(new RelationshipDeletedEvent(relationship));
							Term term = relationship.getTerm();
							Term relatedTerm = relationship.getRelatedTerm();
							if(term.equals(currentTerm) || relatedTerm.equals(currentTerm)) {
								eventBus.fireEvent(new ViewTermEvent(currentTerm));
							}
						}
					});
	    			break;
	    		case APPROVED:
	    			replaceRelationshipView.setRelationship(relationship);
	    			replaceRelationshipView.show();
	    			/*
	    			service.obsoleteRelationship(relationship.getId(), 0, null, new AsyncCallback<Relationship>() {
						@Override
						public void onFailure(Throwable caught) {
							GWT.log("Failed to obsolete relationship", caught);
							if(caught instanceof InvalidEntityException) {
								InvalidEntityException e = (InvalidEntityException)caught;
								//errorLabel.setText(e.getMessage() + ": " + e.getEntity());
								//errorLabel.setVisible(true);
							} else {
								ErrorView.instance().onUncaughtException(caught);
							}
						}
	
						@Override
						public void onSuccess(Relationship relationship) {
							eventBus.fireEvent(new RelationshipUpdatedEvent(relationship));
							Term term = relationship.getTerm();
							Term relatedTerm = relationship.getRelatedTerm();
							if(term.equals(currentTerm) || relatedTerm.equals(currentTerm)) {
								eventBus.fireEvent(new ViewTermEvent(currentTerm));
							}
						}
					});
					*/
	    			break;
	    		
	    		}
	    	}
	    };
	    		    
	    Column<Relationship, Relationship> deleteColumn = 
	    		new Column<Relationship, Relationship>(new IconActionCell<Relationship>(ImageResources.INSTANCE.deleteIcon(),
	    				delegate)) {
	    	@Override
	    	public Relationship getValue(Relationship relationship) {
    			switch(relationship.getStatus()) {
	    		case PENDING:
	    			return curator.equals(relationship.getCreatedBy()) ? relationship : null;
	    		case APPROVED:
	    			return BigDecimal.ONE.equals(curator.getEntityApprovalWeight(Entity.TERM_RELATIONSHIP)) ? relationship : null;
	    		}
	    		
	    		return null;
	    	}
	    };
	    deleteColumn.setCellStyleNames("icon-action");
	    table.addColumn(deleteColumn, SafeHtmlUtils.fromSafeConstant("&nbsp;"));
	}
}
