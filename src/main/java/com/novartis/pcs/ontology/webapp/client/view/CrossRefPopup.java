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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.SimplePager.TextLocation;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import com.google.gwt.view.client.HasRows;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.Range;
import com.novartis.pcs.ontology.entity.ControlledVocabulary;
import com.novartis.pcs.ontology.entity.ControlledVocabularyContext;
import com.novartis.pcs.ontology.entity.ControlledVocabularyDomain;
import com.novartis.pcs.ontology.entity.ControlledVocabularyTerm;
import com.novartis.pcs.ontology.entity.Curator;
import com.novartis.pcs.ontology.entity.Datasource;
import com.novartis.pcs.ontology.entity.DuplicateEntityException;
import com.novartis.pcs.ontology.entity.InvalidEntityException;
import com.novartis.pcs.ontology.entity.Synonym;
import com.novartis.pcs.ontology.entity.Synonym.Type;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.service.util.StatusChecker;
import com.novartis.pcs.ontology.webapp.client.OntoBrowserServiceAsync;
import com.novartis.pcs.ontology.webapp.client.event.EntityDeletedEvent;
import com.novartis.pcs.ontology.webapp.client.event.EntityDeletedHandler;
import com.novartis.pcs.ontology.webapp.client.event.EntityUpdatedEvent;
import com.novartis.pcs.ontology.webapp.client.event.EntityUpdatedHandler;
import com.novartis.pcs.ontology.webapp.client.event.SearchEvent;
import com.novartis.pcs.ontology.webapp.client.event.SynonymDeletedEvent;
import com.novartis.pcs.ontology.webapp.client.event.SynonymUpdatedEvent;
import com.novartis.pcs.ontology.webapp.client.event.ViewTermEvent;
import com.novartis.pcs.ontology.webapp.client.event.ViewTermHandler;
import com.novartis.pcs.ontology.webapp.client.image.ImageResources;

public class CrossRefPopup implements OntoBrowserPopup, ViewTermHandler, 
		ChangeHandler, KeyDownHandler, ClickHandler,
		EntityUpdatedHandler<Synonym>,
		EntityDeletedHandler<Synonym>,
		AsyncCallback<List<ControlledVocabularyTerm>>,
		ControlledVocabularyTermProvider {	
	private static final int TIMEOUT = 300; // milliseconds
	
	public static class NameColumn extends Column<ControlledVocabularyTerm, String> 
			implements Comparator<ControlledVocabularyTerm> {
		public NameColumn() {
			super(new ClickableTextCell());
			setSortable(true);
		}
		
		@Override
		public String getValue(ControlledVocabularyTerm term) {
			return term.getName();
		}
		
		@Override
		public int compare(ControlledVocabularyTerm term1, ControlledVocabularyTerm term2) {
			return term1 == term2 ? 0 : term1.getName().compareToIgnoreCase(term2.getName());
		}
	};
	
	public static class ContextColumn extends ComparableTextColumn<ControlledVocabularyTerm> 
			implements Comparator<ControlledVocabularyTerm> {
		@Override
		public String getValue(ControlledVocabularyTerm term) {
			ControlledVocabularyContext context = term.getControlledVocabulary().getContext(); 
			return context != null ? context.toString() : null;
		}
	};
		
	public static class SourceColumn extends ComparableTextColumn<ControlledVocabularyTerm> 
			implements Comparator<ControlledVocabularyTerm> {
		@Override
		public String getValue(ControlledVocabularyTerm term) {
			Datasource datasource = term.getControlledVocabulary().getDatasource(); 
			return datasource != null ? datasource.getAcronym() : null;
		}
	};
	/*
	public static class ReferenceIdColumn extends ComparableTextColumn<ControlledVocabularyTerm> 
			implements Comparator<ControlledVocabularyTerm> {
				@Override
		public String getValue(ControlledVocabularyTerm term) {
			return term.getReferenceId();
		}
	};
	*/
	public static class UsageColumn extends Column<ControlledVocabularyTerm, Number> 
			implements Comparator<ControlledVocabularyTerm> {
		public UsageColumn() {
			super(new NumberCell());
			setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LOCALE_END);
			setSortable(true);
		}

		@Override
		public Number getValue(ControlledVocabularyTerm term) {
			return term.getUsage();
		}
		
		@Override
		public int compare(ControlledVocabularyTerm term1, ControlledVocabularyTerm term2) {
			if(term1 == term2) {
				return 0;
			}
			
			int usage1 = term1.getUsage();
			int usage2 = term2.getUsage();
			
			return usage1 == usage2 ? 0 : usage1 < usage2 ? -1 : 1;
		}
	};
	/*
	public static class TypeColumn extends
			TextColumn<ControlledVocabularyTerm> implements
			Comparator<ControlledVocabularyTerm> {
		public TypeColumn() {
			setSortable(true);
		}

		@Override
		public String getValue(ControlledVocabularyTerm term) {
			return term.getSynonymType().name();
		}

		@Override
		public int compare(ControlledVocabularyTerm term1,
				ControlledVocabularyTerm term2) {
			if (term1 == term2) {
				return 0;
			}

			Synonym.Type type1 = term1.getSynonymType();
			Synonym.Type type2 = term2.getSynonymType();

			if (type1 == type2) {
				return 0;
			}

			if (type1 != null) {
				return type1 != null ? type1.compareTo(type2) : 1;
			}
			return -1;
		}
	};
	*/
	private final OntoBrowserServiceAsync service;
	private final EventBus eventBus;
	private final Curator curator;
	
	private final ControlledVocabularyTermLinksView linkedTermsView;
	
	private final DialogBox dialogBox = new DialogBox(false, false);
	private final BusyIndicatorHandler busyIndicator = new WidgetBusyIndicatorHandler(dialogBox.getCaption().asWidget());
	private final Label ontologyHeadingLabel = new Label("Ontology:");
	private final Label ontologyLabel = new Label();
	private final Label termLabel = new Label();
	private final ListBox domainDropBox = new ListBox(false);
	private final ListBox contextDropBox = new ListBox(false);
	private final ListBox sourceDropBox = new ListBox(false);
	private final TextBox filterTextBox = new TextBox();
	private final Label errorLabel = new Label();
	private Timer timer = null;
	
	private List<ControlledVocabulary> vocabs;
	private List<ControlledVocabularyDomain> domains = new ArrayList<ControlledVocabularyDomain>();
	private List<ControlledVocabularyContext> contexts = new ArrayList<ControlledVocabularyContext>();
	private List<Datasource> sources = new ArrayList<Datasource>();
	
	private final ProvidesKey<ControlledVocabularyTerm> keyProvider = new EntityKeyProvider<ControlledVocabularyTerm>();
	private final ControlledVocabularyTermListDataProvider dataProvider = new ControlledVocabularyTermListDataProvider(keyProvider);
	private final CellTable<ControlledVocabularyTerm> table = new CellTable<ControlledVocabularyTerm>(keyProvider);
	private final MultiSelectionModel<ControlledVocabularyTerm> selection;
	private final ListBox typeDropBox;
	private final Button addButton;
	private final Button createChildTermButton;
	private final Set<ControlledVocabularyTerm> processing;
	
	private Term currentTerm;
	private ControlledVocabularyTerm selectOnLoad;
	
	public CrossRefPopup(OntoBrowserServiceAsync service,
			EventBus eventBus, Curator curator, 
			final OntoBrowserPopup createChildTermView) {
		this.service = service;
		this.eventBus = eventBus;
		this.curator = curator;
		this.linkedTermsView = new ControlledVocabularyTermLinksView(service);
								
		dialogBox.setText("Vocabulary Mapping");
		dialogBox.setGlassEnabled(false);
		dialogBox.setAnimationEnabled(true);
		
		errorLabel.addStyleName("dialog-error-icon");
		errorLabel.setVisible(false);
		
		domainDropBox.addChangeHandler(this);
		contextDropBox.addChangeHandler(this);
		sourceDropBox.addChangeHandler(this);
		filterTextBox.addKeyDownHandler(this);
		
		dataProvider.addDataDisplay(table);
		
		if(curator != null) {
			selection = new MultiSelectionModel<ControlledVocabularyTerm>(keyProvider);
			
			typeDropBox = new ListBox(false);
			for(Synonym.Type type : Synonym.Type.values()) {
				typeDropBox.addItem(type.toString(), type.name());
			}
			typeDropBox.setSelectedIndex(typeDropBox.getItemCount()-1);
			
			addButton = new Button("Add As Synonym");
			addButton.addClickHandler(this);
			
			createChildTermButton = new Button("Create Child Term");
			
			createChildTermButton.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					if(errorLabel.isVisible()) {
						errorLabel.setVisible(false);
					}
					
					if(createChildTermView != null) {
						createChildTermView.show();
					}
				}
			});
			
			processing = new HashSet<ControlledVocabularyTerm>();
		} else {
			selection = null;
			typeDropBox = null;
			addButton = null;
			createChildTermButton = null;
			processing = null;
		}
		
		addTableColumns();
		addDialogWidgets();
						
		service.loadControlledVocabularies(new AsyncCallback<List<ControlledVocabulary>>() {
			@Override
			public void onFailure(Throwable caught) {
				CrossRefPopup.this.vocabs = null;
			}

			@Override
			public void onSuccess(List<ControlledVocabulary> unmappedVocabs) {
				vocabs = unmappedVocabs;
				for(ControlledVocabulary vocab : vocabs) {
					if(!domains.contains(vocab.getDomain())) {
						domains.add(vocab.getDomain());
						domainDropBox.addItem(vocab.getDomain().getName());
					}
				}
			}
		});
		
		eventBus.addHandler(ViewTermEvent.TYPE, this);
		eventBus.addHandler(SynonymDeletedEvent.TYPE, this);
		eventBus.addHandler(SynonymUpdatedEvent.TYPE, this);
	}
	
	@Override
	public void show() {
		dialogBox.show();
		if(dataProvider.getList().isEmpty()) {
			onChange(null);
		}
	}

	@Override
	public void onViewTerm(ViewTermEvent event) {
		currentTerm = event.getTerm();
		ontologyHeadingLabel.setText(currentTerm.getOntology().isCodelist() ? 
				"Codelist:" : "Ontology:");
		ontologyLabel.setText(currentTerm.getOntology().getName());
		termLabel.setText(currentTerm.getName());
		if(createChildTermButton != null) {
			createChildTermButton.setEnabled(!currentTerm.getOntology().isCodelist());
		}
	}
				
	@Override
	public void onChange(ChangeEvent event) {
		Object source = event != null ? event.getSource() : null;
		
		domainDropBox.setEnabled(false);
		contextDropBox.setEnabled(false);
		sourceDropBox.setEnabled(false);
		
		ControlledVocabularyDomain domain = domains.get(domainDropBox.getSelectedIndex());
		ControlledVocabularyContext context = contextDropBox.getSelectedIndex() > 0 ? 
				contexts.get(contextDropBox.getSelectedIndex()-1) : null;
		Datasource datasource = sourceDropBox.getSelectedIndex() > 0 ? 
				sources.get(sourceDropBox.getSelectedIndex()-1) : null;
		
		busyIndicator.busy();
		
		if(source == null || domainDropBox.equals(source)) {
			service.loadControlledVocabularyTerms(domain, this);
			
			contexts.clear();
			sources.clear();
			
			contextDropBox.clear();
			sourceDropBox.clear();
			
			contextDropBox.addItem("all");
			sourceDropBox.addItem("all");
			
			for(ControlledVocabulary vocab : vocabs) {
				if(vocab.getDomain().equals(domain)) {
					if(!contexts.contains(vocab.getContext())) {
						contexts.add(vocab.getContext());
						contextDropBox.addItem(vocab.getContext().getName());
					}
					
					if(!sources.contains(vocab.getDatasource())) {
						sources.add(vocab.getDatasource());
						sourceDropBox.addItem(vocab.getDatasource().getAcronym());
					}
				}
			}
			
		} else if(contextDropBox.equals(source)) {
			if(context != null) {
				service.loadControlledVocabularyTerms(domain, context, this);
			} else {
				service.loadControlledVocabularyTerms(domain, this);
			}
		} else if(sourceDropBox.equals(source)) {
			if(datasource != null) {
				if(context != null) {
					service.loadControlledVocabularyTerms(domain, context, datasource, this);
				} else {
					service.loadControlledVocabularyTerms(domain, datasource, this);
				}
			} else if(context != null) {
				service.loadControlledVocabularyTerms(domain, context, this);
			} else {
				service.loadControlledVocabularyTerms(domain, this);
			}
		}
		if(selection != null) {
			selection.clear();
		}
		dataProvider.getList().clear();
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
		
	@Override
	public void onFailure(Throwable caught) {
		GWT.log("Failed to load unmapped synonyms", caught);
		domainDropBox.setEnabled(true);
		contextDropBox.setEnabled(true);
		sourceDropBox.setEnabled(true);
		busyIndicator.idle();
		ErrorView.instance().onUncaughtException(caught);
	}

	@Override
	public void onSuccess(List<ControlledVocabularyTerm> terms) {
		if(selection != null) { 
			selection.clear();
		}
		
		dataProvider.getList().clear();
		dataProvider.getList().addAll(terms);
		
		// if client side sorting matches default server side
		// sorting then no need to sort again on client
		if(table.getColumnSortList().size() > 0 
				&& !(table.getColumnSortList().get(0).getColumn() instanceof UsageColumn)
				&& table.getColumnSortList().get(0).isAscending()) {
			ColumnSortEvent.fire(table, table.getColumnSortList());
		}
		
		if(selection != null && selectOnLoad != null) {
			int index = dataProvider.getList().indexOf(selectOnLoad);
			if(index >= 0) {
				index = index/table.getPageSize() * table.getPageSize();
				table.setVisibleRange(index, table.getPageSize());					
				selection.clear();
				selection.setSelected(selectOnLoad, true);
				selectOnLoad = null;
			}
		}
		
		domainDropBox.setEnabled(true);
		contextDropBox.setEnabled(true);
		sourceDropBox.setEnabled(true);
		busyIndicator.idle();
	}
	
	@Override
	public void onClick(ClickEvent event) {
		final Set<ControlledVocabularyTerm> selected = selection.getSelectedSet();
		selected.removeAll(processing);
		if(currentTerm != null && !selected.isEmpty()) {
			processing.addAll(selected);
			busyIndicator.busy();
			
			if(errorLabel.isVisible()) {
				errorLabel.setVisible(false);
			}
			
			service.addSynonyms(currentTerm.getReferenceId(),
					selected, getSynonymType(),
					new AsyncCallback<Term>() {
				public void onFailure(Throwable caught) {
					GWT.log("Failed to create synonym", caught);
					processing.removeAll(selected);
					busyIndicator.idle();
					
					if(caught instanceof DuplicateEntityException) {
						DuplicateEntityException e = (DuplicateEntityException)caught;
						errorLabel.setText(e.getMessage());
						errorLabel.setVisible(true);
					} else if(caught instanceof InvalidEntityException) {
						InvalidEntityException e = (InvalidEntityException)caught;
						errorLabel.setText(e.getMessage() + ": " + e.getEntity());
						errorLabel.setVisible(true);
					} else {
						ErrorView.instance().onUncaughtException(caught);
					}
				}

				public void onSuccess(Term term) {
					processing.removeAll(selected);
					for(ControlledVocabularyTerm t : selected) {
						selection.setSelected(t, false);
					}
					dataProvider.getList().removeAll(selected);
					eventBus.fireEvent(new ViewTermEvent(term));
					busyIndicator.idle();
				}
			});
		}
	}
	
	@Override
	public void onEntityUpdated(EntityUpdatedEvent<Synonym> event) {
		if(vocabs != null) {
			Synonym synonym = event.getEntity();
			ControlledVocabularyTerm vocabTerm = synonym.getControlledVocabularyTerm();
			if(vocabTerm != null && !StatusChecker.isValid(synonym)) {
				ControlledVocabularyDomain domain = vocabTerm.getControlledVocabulary().getDomain();
				int index = domains.indexOf(domain);
				domainDropBox.setSelectedIndex(index);
				selectOnLoad = vocabTerm;
				onChange(null);
			}
		}
	}	
	
	@Override
	public void onEntityDeleted(EntityDeletedEvent<Synonym> event) {
		if(vocabs != null) {
			Synonym synonym = event.getEntity();
			ControlledVocabularyTerm vocabTerm = synonym.getControlledVocabularyTerm();
			if(vocabTerm != null) {
				ControlledVocabularyDomain domain = vocabTerm.getControlledVocabulary().getDomain();
				int index = domains.indexOf(domain);
				domainDropBox.setSelectedIndex(index);
				selectOnLoad = vocabTerm;
				onChange(null);
			}
		}
	}
	
	@Override
	public List<ControlledVocabularyTerm> getTerms() {
		List<ControlledVocabularyTerm> terms = Collections.emptyList();
		if(dialogBox.isShowing()) {
			terms = new ArrayList<ControlledVocabularyTerm>(selection.getSelectedSet());
		}
		return terms;
	}

	@Override
	public void removeTerms(List<ControlledVocabularyTerm> terms) {
		if(terms != null && !terms.isEmpty() && dialogBox.isShowing()) {
			for(ControlledVocabularyTerm t : terms) {
				selection.setSelected(t, false);
			}
			dataProvider.getList().removeAll(terms);
		}
	}
		
	@Override
	public Type getSynonymType() {
		Synonym.Type type = Synonym.Type.RELATED;
		int index = typeDropBox.getSelectedIndex();
		if(index >= 0) {
			type = Synonym.Type.values()[index];
		}
		return type;
	}

	@SuppressWarnings("unchecked")
	private void addTableColumns() {
		/*
	    List<String> typeNames = new ArrayList<String>();
	    for(Synonym.Type type : Synonym.Type.values()) {
			typeNames.add(type.name());
		}
	    
	    Column<ControlledVocabularyTerm, String> typeColumn = 
	    		new Column<ControlledVocabularyTerm, String>(new SelectionCell(typeNames)) {
	    	@Override
	    	public String getValue(ControlledVocabularyTerm term) {
	    		Synonym.Type type = term.getSynonymType();
				if(type == null) {
					type = Synonym.Type.RELATED; 
				}
	    		
	    		return type.name();
	    	}
	    };
	    
	    typeColumn.setFieldUpdater(new FieldUpdater<ControlledVocabularyTerm, String>() {
	    	@Override
	    	public void update(int index, ControlledVocabularyTerm term, String typeName) {
	    		Synonym.Type type = Synonym.Type.valueOf(typeName);  
				term.setSynonymType(type);
	    	}
	    });
	    */
	    
	    IconActionCell.Delegate<ControlledVocabularyTerm> delegate = new IconActionCell.Delegate<ControlledVocabularyTerm>() {
	    	@Override
	    	public void execute(ControlledVocabularyTerm term) {
	    		eventBus.fireEvent(new SearchEvent(term.getName()));
	    		
	    		if(selection != null) {
	    			selection.clear();
	    			selection.setSelected(term, true);
	    		}
	    	}
	    };
	    	    
	    Column<ControlledVocabularyTerm, ControlledVocabularyTerm> searchColumn = 
	    		new Column<ControlledVocabularyTerm, ControlledVocabularyTerm>(
	    				new IconActionCell<ControlledVocabularyTerm>(ImageResources.INSTANCE.searchIcon(), delegate)) {
	    	@Override
	    	public ControlledVocabularyTerm getValue(ControlledVocabularyTerm term) {
	    		return term;
	    	}
	    };
	    searchColumn.setCellStyleNames("icon-action");
	    /*	    	    
	    Column<ControlledVocabularyTerm, ControlledVocabularyTerm> addColumn = 
	    		new Column<ControlledVocabularyTerm, ControlledVocabularyTerm>(new IconActionCell<ControlledVocabularyTerm>(ImageResources.INSTANCE.addIcon(), this)) {
	    	@Override
	    	public ControlledVocabularyTerm getValue(ControlledVocabularyTerm term) {
	    		return term;
	    	}
	    };
	    addColumn.setCellStyleNames("icon-action");
	    
	    // colspans the table header if we use the same Header object
	    Header<String> actionHeader = new Header<String>(new TextCell()) {
			@Override
			public String getValue() {
				return "Action";
			}
	    };
	    */
	    table.setWidth("100%");
	    table.setPageSize(10);
	    table.addStyleName("gwt-CellTable");
	    table.addStyleName("spaced-vert");
	    //table.setKeyboardPagingPolicy(KeyboardPagingPolicy.CHANGE_PAGE);
		table.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
		
		if(curator != null) {
			Column<ControlledVocabularyTerm, Boolean> checkColumn = new Column<ControlledVocabularyTerm, Boolean>(
					new CheckboxCell(true, false)) {
				@Override
				public Boolean getValue(ControlledVocabularyTerm term) {
					return selection.isSelected(term);
				}
			};
						
			table.addColumn(checkColumn, SafeHtmlUtils.fromSafeConstant("&nbsp;"));
			table.setColumnWidth(checkColumn, 16, Unit.PX);
			
			table.setSelectionModel(selection,
		    		DefaultSelectionEventManager.<ControlledVocabularyTerm> createCheckboxManager(0));
	    } else {
	    	table.setSelectionModel(new NoSelectionModel<ControlledVocabularyTerm>(keyProvider));
	    }
		
		NameColumn nameColumn = new NameColumn();
		nameColumn.setFieldUpdater(new FieldUpdater<ControlledVocabularyTerm, String>() {
			@Override
			public void update(int index, ControlledVocabularyTerm term, String value) {
				linkedTermsView.show(term);
			}
		});
		nameColumn.setCellStyleNames("clickable-text");
		
		table.addColumn(searchColumn, SafeHtmlUtils.fromSafeConstant("&nbsp;"));
		table.addColumn(nameColumn, "Unmapped Term");
		table.addColumn(new ContextColumn(), "Context");
		table.addColumn(new SourceColumn(), "Source");
		table.addColumn(new UsageColumn(), "Usage");
		// table.addColumn(typeColumn, "Type");
			
		ListHandler<ControlledVocabularyTerm> sortHandler 
				= new ListHandler<ControlledVocabularyTerm>(dataProvider.getList());
		for(int i = 1; i < table.getColumnCount(); i++) {
			Column<ControlledVocabularyTerm, ?> column = table.getColumn(i);
			if(column.isSortable() && column instanceof Comparator<?>) {
				sortHandler.setComparator(column, (Comparator<ControlledVocabularyTerm>)column);
			}
		}
				
		table.addColumnSortHandler(sortHandler);
		table.getColumnSortList().push(table.getColumn(table.getColumnCount()-1));
		// Second time to reverse sort order
		table.getColumnSortList().push(table.getColumn(table.getColumnCount()-1));
	}
	
	private void addDialogWidgets() {
		VerticalPanel vertPanel = new VerticalPanel();					
		Grid grid = new Grid(4,2); //new Grid(6,2);
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
				if(errorLabel.isVisible()) {
					errorLabel.setVisible(false);
				}
				
				dialogBox.hide();
			}
		});
		
		vertPanel.addStyleName("dialog-vpanel");
		vertPanel.add(errorLabel);
		//vertPanel.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
				
		grid.addStyleName("gwt-Grid");
		int row = 0, col = 0;
		//grid.setWidget(row, col, new Label("Ontology:"));
		//grid.setWidget(++row, col, new Label("Ontology Term:"));
		grid.setWidget(row, col, new Label("Vocabulary Domain:"));
		grid.setWidget(++row, col, new Label("Vocabulary Context:"));
		grid.setWidget(++row, col, new Label("Vocabulary Source:"));
		grid.setWidget(++row, col, new Label("Keyword Filter:"));
		row = 0;
		col = 1;
		//grid.setWidget(row, col, ontologyLabel);
		//grid.setWidget(++row, col, termLabel);
		grid.setWidget(row, col, domainDropBox);
		grid.setWidget(++row, col, contextDropBox);
		grid.setWidget(++row, col, sourceDropBox);
		grid.setWidget(++row, col, filterTextBox);
		
		for(row = col = 0; row < grid.getRowCount(); row++) {
			Label label = (Label)grid.getWidget(row, col);
			label.addStyleName("dialog-label");
		}
		
		FlowPanel flowPanel = new FlowPanel();
		flowPanel.addStyleName("decorator-panel");
		flowPanel.add(grid);
		flowPanel.add(table);
		flowPanel.add(pager);
	    
		pager.setDisplay(table);
		pager.addStyleName("centered-hortz");
	    		
		vertPanel.add(flowPanel);
		//vertPanel.setCellHorizontalAlignment(flowPanel, HasHorizontalAlignment.ALIGN_LEFT);
		/*
		Label tip = new Label("Click icon next to unmapped term to perform auto search in main window");
    	tip.addStyleName("dialog-message");
		tip.addStyleName("info-icon-left");
		vertPanel.add(tip);
		vertPanel.setCellHorizontalAlignment(tip, HasHorizontalAlignment.ALIGN_LEFT);
		*/
			    
	    if(curator != null) {
	    	
	    	Label tip = new Label("Select ontology/codelist term in main window");
	    	tip.addStyleName("dialog-message");
			tip.addStyleName("info-icon-left");
			vertPanel.add(tip);
			//vertPanel.setCellHorizontalAlignment(tip, HasHorizontalAlignment.ALIGN_LEFT);
	    	
	    	SimplePanel decPanel = new SimplePanel();
	    	decPanel.addStyleName("decorator-panel");	
			vertPanel.add(decPanel);
			//vertPanel.setCellHorizontalAlignment(decPanel, HasHorizontalAlignment.ALIGN_LEFT);
	    					    	
	    	grid = new Grid(3,2);
	    	grid.addStyleName("gwt-Grid");
	    		    	
	    	row = 0;
	    	col = 0;

			grid.setWidget(row, col, ontologyHeadingLabel);
			grid.setWidget(++row, col, new Label("Term:"));
			grid.setWidget(++row, col, new Label("Synonym Type:"));
			
			row = 0;
			col = 1;
			grid.setWidget(row, col, ontologyLabel);
			grid.setWidget(++row, col, termLabel);
			grid.setWidget(++row, col, typeDropBox);
			
			for(row = col = 0; row < grid.getRowCount(); row++) {
				Label label = (Label)grid.getWidget(row, col);
				label.addStyleName("dialog-label");
			}
			
			decPanel.setWidget(grid);
				    	
			HorizontalPanel buttonsHPanel = new HorizontalPanel();			
			Button excludeButton = new Button("Exclude");
			
			excludeButton.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					final Set<ControlledVocabularyTerm> selected = selection.getSelectedSet();
					selected.removeAll(processing);
					if(!selected.isEmpty()) {			
						processing.addAll(selected);
						busyIndicator.busy();
						
						if(errorLabel.isVisible()) {
							errorLabel.setVisible(false);
						}
						
						service.excludeControlledVocabularyTerms(selected, new AsyncCallback<Void>() {
							public void onFailure(Throwable caught) {
								GWT.log("Failed to exclude vocab terms", caught);
								processing.removeAll(selected);
								busyIndicator.idle();
								ErrorView.instance().onUncaughtException(caught);
							}

							public void onSuccess(Void v) {
								processing.removeAll(selected);
								for(ControlledVocabularyTerm t : selected) {
									selection.setSelected(t, false);
								}
								dataProvider.getList().removeAll(selected);
								busyIndicator.idle();
							}
						});
					}
				}
			}); 
			
			buttonsHPanel.add(addButton);
			if(createChildTermButton != null) {
				buttonsHPanel.add(createChildTermButton);
			}
			buttonsHPanel.add(excludeButton);
			buttonsHPanel.add(closeButton);
			buttonsHPanel.addStyleName("dialog-buttons");
			buttonsHPanel.addStyleName("centered-hortz");
			vertPanel.add(buttonsHPanel);
			/*
			Label label = new Label("*Use Shift key to select/unselect consecutive rows");
			label.addStyleName("dialog-message");
			vertPanel.add(label);
			*/
		} else {
			vertPanel.add(closeButton);
			vertPanel.setCellHorizontalAlignment(closeButton, HasHorizontalAlignment.ALIGN_CENTER);
		}
				
		dialogBox.setWidget(vertPanel);
	}	
}
