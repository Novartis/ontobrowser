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
import java.util.List;

import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.SimplePager.TextLocation;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.HasRows;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.Range;
import com.novartis.pcs.ontology.entity.ControlledVocabularyContext;
import com.novartis.pcs.ontology.entity.ControlledVocabularyDomain;
import com.novartis.pcs.ontology.entity.ControlledVocabularyTerm;
import com.novartis.pcs.ontology.entity.ControlledVocabularyTermLink;
import com.novartis.pcs.ontology.entity.Datasource;
import com.novartis.pcs.ontology.webapp.client.OntoBrowserServiceAsync;

public class ControlledVocabularyTermLinksView {	
	public static class NameColumn extends TextColumn<ControlledVocabularyTermLink> 
			implements Comparator<ControlledVocabularyTermLink> {
		public NameColumn() {
			setSortable(true);
		}
		
		@Override
		public String getValue(ControlledVocabularyTermLink link) {
			return link.getLinkedControlledVocabularyTerm().getName();
		}
		
		@Override
		public int compare(ControlledVocabularyTermLink link1, ControlledVocabularyTermLink link2) {
			if(link1 == link2) {
				return 0;
			}
			
			String name1 = link1.getLinkedControlledVocabularyTerm().getName();
			String name2 = link2.getLinkedControlledVocabularyTerm().getName();
			
			return name1.compareToIgnoreCase(name2);
		}
	};
	
	public static class DomainColumn extends TextColumn<ControlledVocabularyTermLink> 
	implements Comparator<ControlledVocabularyTermLink> {
		public DomainColumn() {
			setSortable(true);
		}

		@Override
		public String getValue(ControlledVocabularyTermLink link) {
			ControlledVocabularyDomain domain = link.getLinkedControlledVocabularyTerm()
					.getControlledVocabulary().getDomain(); 
			return domain.getName();
		}

		@Override
		public int compare(ControlledVocabularyTermLink link1, ControlledVocabularyTermLink link2) {
			if(link1 == link2) {
				return 0;
			}

			ControlledVocabularyDomain domain1 = link1.getLinkedControlledVocabularyTerm()
					.getControlledVocabulary()
					.getDomain();
			ControlledVocabularyDomain domain2 = link2.getLinkedControlledVocabularyTerm()
					.getControlledVocabulary()
					.getDomain();

			return domain1.getName().compareToIgnoreCase(domain2.getName());
		}
	};
	
	public static class ContextColumn extends TextColumn<ControlledVocabularyTermLink> 
			implements Comparator<ControlledVocabularyTermLink> {
		public ContextColumn() {
			setSortable(true);
		}

		@Override
		public String getValue(ControlledVocabularyTermLink link) {
			ControlledVocabularyTerm term = link.getLinkedControlledVocabularyTerm();
			ControlledVocabularyContext context = term.getControlledVocabulary().getContext(); 
			return context.getName();
		}
		
		@Override
		public int compare(ControlledVocabularyTermLink link1, ControlledVocabularyTermLink link2) {
			if(link1 == link2) {
				return 0;
			}

			ControlledVocabularyContext context1 = link1.getLinkedControlledVocabularyTerm()
					.getControlledVocabulary().getContext();
			ControlledVocabularyContext context2 = link2.getLinkedControlledVocabularyTerm()
					.getControlledVocabulary().getContext();

			return context1.getName().compareToIgnoreCase(context2.getName());
		}
	};
		
	public static class SourceColumn extends TextColumn<ControlledVocabularyTermLink> 
			implements Comparator<ControlledVocabularyTermLink> {
		public SourceColumn() {
			setSortable(true);
		}

		@Override
		public String getValue(ControlledVocabularyTermLink link) {
			ControlledVocabularyTerm term = link.getLinkedControlledVocabularyTerm();
			Datasource datasource = term.getControlledVocabulary().getDatasource(); 
			return datasource.getAcronym();
		}
		
		@Override
		public int compare(ControlledVocabularyTermLink link1, ControlledVocabularyTermLink link2) {
			if(link1 == link2) {
				return 0;
			}		
			
			Datasource datasource1 = link1.getLinkedControlledVocabularyTerm()
					.getControlledVocabulary().getDatasource();
			Datasource datasource2 = link2.getLinkedControlledVocabularyTerm()
					.getControlledVocabulary().getDatasource();
			
			return datasource1.getAcronym().compareToIgnoreCase(datasource2.getAcronym());
		}
	};
	
	public static class UsageColumn extends Column<ControlledVocabularyTermLink, Number> 
			implements Comparator<ControlledVocabularyTermLink> {
		public UsageColumn() {
			super(new NumberCell());
			setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LOCALE_END);
			setSortable(true);
		}

		@Override
		public Number getValue(ControlledVocabularyTermLink link) {
			return link.getUsage();
		}
		
		@Override
		public int compare(ControlledVocabularyTermLink link1, ControlledVocabularyTermLink link2) {
			if(link1 == link2) {
				return 0;
			}
			
			int usage1 = link1.getUsage();
			int usage2 = link2.getUsage();
			
			return usage1 == usage2 ? 0 : usage1 < usage2 ? -1 : 1;
		}
	};
	
	private final OntoBrowserServiceAsync service;
	private final PopupPanel dialogBox = new PopupPanel(true, true);
			
	private final ProvidesKey<ControlledVocabularyTermLink> keyProvider = new EntityKeyProvider<ControlledVocabularyTermLink>();
	private final ListDataProvider<ControlledVocabularyTermLink> dataProvider = new ListDataProvider<ControlledVocabularyTermLink>(keyProvider);
	private final CellTable<ControlledVocabularyTermLink> table = new CellTable<ControlledVocabularyTermLink>(keyProvider);
	private final Label emptyTableWidget = new Label("No linked terms");	
	
	public ControlledVocabularyTermLinksView(OntoBrowserServiceAsync service) {
		this.service = service;
		//dialogBox.setText("Linked Terms");
		dialogBox.setGlassEnabled(true);
		dialogBox.setAnimationEnabled(true);
		//dialogBox.addStyleName("gwt-ModalDialogBox");
		
		dataProvider.addDataDisplay(table);
						
		addTableColumns();
		addDialogWidgets();
	}
	
	
	public void show(ControlledVocabularyTerm term) {		
		table.setVisibleRange(0, table.getPageSize());
		// Bit of a hack - set the empty table widget to the
		// loading widget while the data is loaded from the server
		table.setEmptyTableWidget(table.getLoadingIndicator());
		dataProvider.getList().clear();
		dialogBox.center();
		
		service.loadControlledVocabularyTermLinks(term, new AsyncCallback<List<ControlledVocabularyTermLink>>() {	
			@Override
			public void onSuccess(List<ControlledVocabularyTermLink> links) {
				dataProvider.getList().addAll(links);
				
				// Reset the empty table widget - see hack above
				table.setEmptyTableWidget(emptyTableWidget);
				
				// if client side sorting matches default server side
				// sorting then no need to sort again on client
				if(table.getColumnSortList().size() > 0 
						&& !(table.getColumnSortList().get(0).getColumn() instanceof UsageColumn)
						&& table.getColumnSortList().get(0).isAscending()) {
					ColumnSortEvent.fire(table, table.getColumnSortList());
				}
			}
			
			@Override
			public void onFailure(Throwable caught) {
				GWT.log("Failed to load controlled vocab term links", caught);
				ErrorView.instance().onUncaughtException(caught);			
			}
		});
	}

	@SuppressWarnings("unchecked")
	private void addTableColumns() {
		
	    table.setWidth("100%");
	    table.setPageSize(10);
	    table.addStyleName("gwt-CellTable");
	    table.addStyleName("spaced-vert");
	    table.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
		table.setSelectionModel(new NoSelectionModel<ControlledVocabularyTermLink>(keyProvider));
	    
		
		table.addColumn(new NameColumn(), "Linked Term");
		table.addColumn(new DomainColumn(), "Domain");
		table.addColumn(new ContextColumn(), "Context");
		table.addColumn(new SourceColumn(), "Source");
		table.addColumn(new UsageColumn(), "Usage");
			
		ListHandler<ControlledVocabularyTermLink> sortHandler 
				= new ListHandler<ControlledVocabularyTermLink>(dataProvider.getList());
		for(int i = 1; i < table.getColumnCount(); i++) {
			Column<ControlledVocabularyTermLink, ?> column = table.getColumn(i);
			if(column.isSortable() && column instanceof Comparator<?>) {
				sortHandler.setComparator(column, (Comparator<ControlledVocabularyTermLink>)column);
			}
		}
				
		table.addColumnSortHandler(sortHandler);
		table.getColumnSortList().push(table.getColumn(table.getColumnCount()-1));
		// Second time to reverse sort order
		table.getColumnSortList().push(table.getColumn(table.getColumnCount()-1));
	}
	
	private void addDialogWidgets() {
		VerticalPanel vertPanel = new VerticalPanel();					
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
				
		vertPanel.addStyleName("dialog-vpanel");
						
		
		vertPanel.add(table);
		vertPanel.add(pager);
	    
		pager.setDisplay(table);
		pager.addStyleName("centered-hortz");    
	    				
		dialogBox.setWidget(vertPanel);
	}	
}
