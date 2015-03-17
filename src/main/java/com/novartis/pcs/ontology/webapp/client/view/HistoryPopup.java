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

import static com.google.gwt.event.dom.client.KeyCodes.KEY_BACKSPACE;
import static com.google.gwt.event.dom.client.KeyCodes.KEY_DELETE;
import static com.google.gwt.event.dom.client.KeyCodes.KEY_ENTER;

import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.google.gwt.cell.client.DateCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.SimplePager.TextLocation;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.InlineHyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.HasRows;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.Range;
import com.novartis.pcs.ontology.entity.CuratorAction;
import com.novartis.pcs.ontology.entity.Relationship;
import com.novartis.pcs.ontology.entity.Synonym;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.entity.VersionedEntity;
import com.novartis.pcs.ontology.webapp.client.OntoBrowserServiceAsync;

public class HistoryPopup implements OntoBrowserPopup, AsyncCallback<List<CuratorAction>>, KeyDownHandler {
	private static final int TIMEOUT = 300; // milliseconds
	
	private static class ActionDateColumn extends Column<CuratorAction, Date> 
			implements Comparator<CuratorAction> {		
		public ActionDateColumn() {
			super(new DateCell(DateTimeFormat.getFormat(PredefinedFormat.DATE_SHORT)));
			setSortable(true);
		}
		
		@Override
		public Date getValue(CuratorAction action) {
			return action.getActionDate();
		}

		@Override
		public int compare(CuratorAction action1, CuratorAction action2) {
			return action2.getActionDate().compareTo(action1.getActionDate());
		}
	};
		
	private static class CuratorColumn extends ComparableTextColumn<CuratorAction> {
		@Override
		public String getValue(CuratorAction action) {
			return action.getCurator().getUsername();
		}
	};
	
	private static class ActionColumn extends ComparableTextColumn<CuratorAction> {
		@Override
		public String getValue(CuratorAction action) {
			return action.getAction().toString();
		}
	};
	
	private static class EntityColumn extends ComparableTextColumn<CuratorAction> {
		@Override
		public String getValue(CuratorAction action) {
			String className = action.getEntity().getClass().getName();
			return className.substring(className.lastIndexOf('.')+1);
		}
	};
	
	private static class OntologyColumn extends ComparableTextColumn<CuratorAction> {
		@Override
		public String getValue(CuratorAction action) {
			Term term = getTerm(action.getEntity());
			
			return term != null ? term.getOntology().getName() : null;
		}
	};
	
	private static class TermColumn extends Column<CuratorAction, InlineHyperlink> 
			implements Comparator<CuratorAction> {
		public TermColumn() {
			super(new HyperlinkCell());
			setSortable(true);
		}

		@Override
		public InlineHyperlink getValue(CuratorAction action) {
			Term term = getTerm(action.getEntity());
			
			return term != null ? 
					new InlineHyperlink(term.getName(), term.getReferenceId()) : null;
		}

		@Override
		public int compare(CuratorAction action1, CuratorAction action2) {
			Term term1 = getTerm(action1.getEntity());
			Term term2 = getTerm(action2.getEntity());
			String name1 = term1 != null ? term1.getName() : null;
			String name2 = term2 != null ? term2.getName() : null;
			return name1 != null ? name1.compareToIgnoreCase(name2)
					: name2 != null ? 1 : 0;
		}
	};
	
	private static class EntityValueColumn extends ComparableTextColumn<CuratorAction> {	
		@Override
		public String getValue(CuratorAction action) {
			VersionedEntity entity = action.getEntity();
			String value = null;
			if(entity instanceof Relationship) {
				Relationship relationship = (Relationship)entity;
				value = relationship.getType().getRelationship()
						+ " " + relationship.getRelatedTerm();
			} else if(entity instanceof Synonym) {
				Synonym synonym = (Synonym)entity;
				value = synonym.getSynonym();
			}
			
			return value;
		}
	};
		
	private static class CommentsColumn extends ComparableTextColumn<CuratorAction> {
		@Override
		public String getValue(CuratorAction action) {
			return action.getComments();
		}
	};
	
	private final OntoBrowserServiceAsync service;
	private final DialogBox dialogBox = new DialogBox(false, false);
	private final BusyIndicatorHandler busyIndicator = new WidgetBusyIndicatorHandler(dialogBox.getCaption().asWidget());
	
	private final ProvidesKey<CuratorAction> keyProvider = new EntityKeyProvider<CuratorAction>();
	private final CuratorActionListDataProvider dataProvider = new CuratorActionListDataProvider(keyProvider);
	private final CellTable<CuratorAction> table = new CellTable<CuratorAction>(keyProvider);
	private final TextBox filterTextBox = new TextBox();
	private Timer timer = null;
	
	public HistoryPopup(EventBus eventBus, OntoBrowserServiceAsync service) {
		this.service = service;
		
		dialogBox.setText("Curation History");
		dialogBox.getCaption().asWidget().addStyleName("busy-icon-right-padded");
		dialogBox.setGlassEnabled(false);
		dialogBox.setAnimationEnabled(true);
		
		filterTextBox.addKeyDownHandler(this);
		dataProvider.addDataDisplay(table);
		
		setupTable();
		addDialogWidgets();
	}
	

	@Override
	public void show() {
		dataProvider.getList().clear();
		dialogBox.show();
		busyIndicator.busy();
		service.loadCuratorActions(this);
	}

	@Override
	public void onFailure(Throwable caught) {
		GWT.log("Failed to load curator actions/history", caught);
		busyIndicator.idle();
		ErrorView.instance().onUncaughtException(caught);		
	}

	@Override
	public void onSuccess(List<CuratorAction> actions) {
		dataProvider.getList().addAll(actions);
		busyIndicator.idle();
	}
	
	@Override
	public void onKeyDown(KeyDownEvent event) {
		int keyCode = event.getNativeKeyCode();
		if((keyCode == KEY_BACKSPACE || keyCode >= KEY_DELETE) 
				&& keyCode != 91 && keyCode != 93) { // Command/Window keyboard keys
			cancelTimer();
			timer = new Timer() {
				public void run() {
					dataProvider.setFilter(filterTextBox.getValue());
				}
			};

			timer.schedule(TIMEOUT);
		} else if(keyCode == KEY_ENTER) {
			cancelTimer();
			dataProvider.setFilter(filterTextBox.getValue());
		}
	}

	private void cancelTimer() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}
		
	@SuppressWarnings("unchecked")
	protected void setupTable() {	    
	    table.setWidth("100%");
	    table.addStyleName("gwt-CellTable");
	    table.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
	    table.setSelectionModel(new NoSelectionModel<CuratorAction>(keyProvider));
	    
	    table.addColumn(new ActionDateColumn(), "Date");
	    table.addColumn(new CuratorColumn(), "Curator");
	    table.addColumn(new ActionColumn(), "Action");
	    table.addColumn(new EntityColumn(), "Entity");
	    table.addColumn(new OntologyColumn(), "Ontology/Codelist");
	    table.addColumn(new TermColumn(), "Term");
	    table.addColumn(new EntityValueColumn(), "Relationship/Synonym");
	    table.addColumn(new CommentsColumn(), "Comments");
	    	    	    
		ListHandler<CuratorAction> sortHandler = new ListHandler<CuratorAction>(dataProvider.getList());
		for(int i = 0; i < table.getColumnCount(); i++) {
			Column<CuratorAction, ?> column = table.getColumn(i);
			if(column.isSortable() && column instanceof Comparator<?>) {
				sortHandler.setComparator(column, (Comparator<CuratorAction>)column);
			}
		}
		table.addColumnSortHandler(sortHandler);
		table.getColumnSortList().push(table.getColumn(0));
	}
	
	private void addDialogWidgets() {
		VerticalPanel vertPanel = new VerticalPanel();					
		HorizontalPanel filterPanel = new HorizontalPanel();
		Label filterLabel = new Label("Keyword Filter:");
		SimplePager.Resources pagerResources = GWT.create(SimplePager.Resources.class);
		SimplePager pager = new SimplePager(TextLocation.CENTER, pagerResources, false, 0, true) {
			@Override
			public void setPageStart(int index) {
				HasRows display = getDisplay();
				if (display != null) {
					Range range = display.getVisibleRange();
					int pageSize = range.getLength();
					if (isRangeLimited() && display.isRowCountExact()) {
						index = Math.min(index, display.getRowCount() - 1);
					}
					index = Math.max(0, index);
					if (index != range.getStart()) {
						display.setVisibleRange(index, pageSize);
					}
				}
			}
		};
		Button closeButton = new Button("Close");
		closeButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				dialogBox.hide();
			}
		});
		
		filterLabel.addStyleName("dialog-label");
		
		filterPanel.addStyleName("dialog-hpanel");
		filterPanel.add(filterLabel);
		filterPanel.add(filterTextBox);
		filterPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
						
		pager.addStyleName("centered-hortz");				    
		pager.setDisplay(table);
		
		vertPanel.addStyleName("dialog-vpanel");
		vertPanel.add(filterPanel);
		vertPanel.add(table);
		vertPanel.add(pager);
	    vertPanel.add(closeButton);
	    vertPanel.setCellHorizontalAlignment(closeButton, HasHorizontalAlignment.ALIGN_CENTER);
		dialogBox.setWidget(vertPanel);
	}
	
	private static Term getTerm(VersionedEntity entity) {
		Term term = null;
		if(entity instanceof Term) {
			term = (Term)entity;
		} else if(entity instanceof Synonym) {
			Synonym synonym = (Synonym)entity;
			term = synonym.getTerm();
		} else if(entity instanceof Relationship) {
			Relationship relationship = (Relationship)entity;
			term = relationship.getTerm();
		}
		return term;
	}
}
