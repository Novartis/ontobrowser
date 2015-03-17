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

import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.cell.client.DateCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.SimplePager.TextLocation;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import com.google.gwt.view.client.HasRows;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.Range;
import com.novartis.pcs.ontology.entity.Curator;
import com.novartis.pcs.ontology.entity.CuratorAction;
import com.novartis.pcs.ontology.entity.CuratorAction.Action;
import com.novartis.pcs.ontology.entity.CuratorApprovalWeight.Entity;
import com.novartis.pcs.ontology.entity.InvalidEntityException;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.entity.VersionedEntity;
import com.novartis.pcs.ontology.entity.VersionedEntity.Status;
import com.novartis.pcs.ontology.webapp.client.OntoBrowserServiceAsync;
import com.novartis.pcs.ontology.webapp.client.event.EntityUpdatedEvent;
import com.novartis.pcs.ontology.webapp.client.event.EntityUpdatedHandler;
import com.novartis.pcs.ontology.webapp.client.event.ViewTermEvent;
import com.novartis.pcs.ontology.webapp.client.event.ViewTermHandler;
import com.novartis.pcs.ontology.webapp.client.image.ImageResources;

public abstract class ApproveRejectComposite<T extends VersionedEntity> extends Composite 
		implements ViewTermHandler, EntityUpdatedHandler<T>, AsyncCallback<List<T>>, KeyDownHandler {
	private static final int TIMEOUT = 300; // milliseconds
	
	private class CreatedByColumn extends ComparableTextColumn<T> {		
		@Override
		public String getValue(T entity) {
			return entity.getCreatedBy().getUsername();
		}
	};
	
	private class CreatedDateColumn extends Column<T, Date> 
			implements Comparator<T> {		
		public CreatedDateColumn() {
			super(new DateCell(DateTimeFormat.getFormat(PredefinedFormat.DATE_SHORT)));
			setSortable(true);
		}

		@Override
		public Date getValue(T entity) {
			return entity.getCreatedDate();
		}

		@Override
		public int compare(T entity1, T entity2) {
			return entity2.getCreatedDate().compareTo(entity1.getCreatedDate());
		}
	};
	
	private class ApprovedByColumn extends TextColumn<T> {
		public ApprovedByColumn() {
			setSortable(false);
		}
		
		@Override
		public String getValue(T entity) {
			StringBuilder buf = new StringBuilder();
			for(CuratorAction action : entity.getCuratorActions()) {
				if(action.getAction().equals(Action.APPROVE)) {
					if(buf.length() > 0) {
						buf.append(", ");
					}
					buf.append(action.getCurator().getUsername());
				}
			}
			
			return buf.toString();
		}
	};
	
	private class RejectedByColumn extends TextColumn<T> {
		public RejectedByColumn() {
			setSortable(false);
		}
		
		@Override
		public String getValue(T entity) {
			StringBuilder buf = new StringBuilder();
			for(CuratorAction action : entity.getCuratorActions()) {
				if(action.getAction().equals(Action.REJECT)) {
					if(buf.length() > 0) {
						buf.append(", ");
					}
					buf.append(action.getCurator().getUsername());
				}
			}
			
			return buf.toString();
		}
	};
	
	protected final Entity entity;
	protected final OntoBrowserServiceAsync service;
	protected final EventBus eventBus;
	protected final Curator curator;
	protected final OntoBrowserEditPopup<T> editView;
	
	private final ProvidesKey<T> keyProvider = new EntityKeyProvider<T>();
	private final VersionedEntityListDataProvider<T> dataProvider = new VersionedEntityListDataProvider<T>(keyProvider);
	private final CellTable<T> table = new CellTable<T>(keyProvider);
	private final MultiSelectionModel<T> selection;
	private final TextArea commentsField;
	private final Button approveButton;
	private final Button rejectButton;
	private final Label errorLabel;
	private final Set<T> processing = new HashSet<T>();
	private final BusyIndicatorHandler busyIndicator;
	private final TextBox filterTextBox = new TextBox();
	private Timer timer = null;
	
	protected Term currentTerm;
	
	public ApproveRejectComposite(Entity entity, 
			OntoBrowserServiceAsync service,
			EventBus eventBus,
			Curator curator,
			BusyIndicatorHandler busyIndicator, 
			OntoBrowserEditPopup<T> editView) {
		this.entity = entity;
		this.service = service;
		this.eventBus = eventBus;
		this.curator = curator;
		this.busyIndicator = busyIndicator;
		this.editView = editView;
		
		filterTextBox.addKeyDownHandler(this);
		dataProvider.addDataDisplay(table);
		
		if(curator != null) {
			errorLabel = new Label();
			errorLabel.addStyleName("dialog-error-icon");
			errorLabel.setVisible(false);
		} else {
			errorLabel = null;
		}
		
		if(curator != null && curator.isAuthorised(entity)) {
			selection = new MultiSelectionModel<T>(keyProvider);
						
			commentsField = new TextArea();
			commentsField.setWidth("100%");
			commentsField.setVisibleLines(2);
			
			approveButton = new Button("Approve");
			approveButton.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					final Set<T> selected = selection.getSelectedSet();
					selected.removeAll(processing);
					
					if(errorLabel.isVisible()) {
						errorLabel.setVisible(false);
					}
					
					if(!selected.isEmpty()) {
						processing.addAll(selected);
						ApproveRejectComposite.this.busyIndicator.busy();
						ApproveRejectComposite.this.service.approve(selected, 
								commentsField.getValue(),
								new AsyncCallback<Set<T>>() {
							@Override
							public void onFailure(Throwable caught) {
								GWT.log("Failed to approve pending items", caught);
								processing.removeAll(selected);
								ApproveRejectComposite.this.busyIndicator.idle();
								if(caught instanceof InvalidEntityException) {
									InvalidEntityException e = (InvalidEntityException)caught;
									errorLabel.setText(e.getMessage() + ": " + e.getEntity());
									errorLabel.setVisible(true);
								} else {
									ErrorView.instance().onUncaughtException(caught);
								}
							}
		
							@Override
							public void onSuccess(Set<T> approved) {
								processing.removeAll(selected);
								for(T entity : selected) {
									selection.setSelected(entity, false);
								}
								dataProvider.getList().removeAll(selected);
								checkStatusChanges(approved);
								commentsField.setValue(null);
								ApproveRejectComposite.this.busyIndicator.idle();
							}
						});
					}
				}
			});
			
			rejectButton = new Button("Reject");
			rejectButton.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					final Set<T> selected = selection.getSelectedSet();
					selected.removeAll(processing);
					
					if(errorLabel.isVisible()) {
						errorLabel.setVisible(false);
					}
					
					if(!selected.isEmpty()) {
						processing.addAll(selected);
						ApproveRejectComposite.this.busyIndicator.busy();
						ApproveRejectComposite.this.service.reject(selected, 
								commentsField.getValue(),
								new AsyncCallback<Set<T>>() {
							@Override
							public void onFailure(Throwable caught) {
								GWT.log("Failed to reject pending items", caught);
								processing.removeAll(selected);
								ApproveRejectComposite.this.busyIndicator.idle();
								if(caught instanceof InvalidEntityException) {
									InvalidEntityException e = (InvalidEntityException)caught;
									errorLabel.setText(e.getMessage() + ": " + e.getEntity());
									errorLabel.setVisible(true);
								} else {
									ErrorView.instance().onUncaughtException(caught);
								}
							}
		
							@Override
							public void onSuccess(Set<T> rejected) {
								processing.removeAll(selected);
								for(T entity : selected) {
									selection.setSelected(entity, false);
								}
								dataProvider.getList().removeAll(selected);
								checkStatusChanges(rejected);
								commentsField.setValue(null);
								ApproveRejectComposite.this.busyIndicator.idle();
							}
						});
					}
				}
			});
		} else {
			selection = null;
			commentsField = null;
			approveButton = null;
			rejectButton = null;
		}
				
		setupTable();
		addWidgets();
		eventBus.addHandler(ViewTermEvent.TYPE, this);
	}
	
	@Override
	public void onViewTerm(ViewTermEvent event) {
		currentTerm = event.getTerm();
	}
					
	@Override
	public void onFailure(Throwable caught) {
		GWT.log("Failed to load pending items", caught);
		busyIndicator.idle();
		ErrorView.instance().onUncaughtException(caught);
	}

	@Override
	public void onSuccess(List<T> entities) {
		if(selection != null) {
			selection.clear();
			errorLabel.setVisible(false);
		}
		dataProvider.getList().clear();
		addAprroveRejectColumns(entities);
		dataProvider.getList().addAll(entities);
		ColumnSortEvent.fire(table, table.getColumnSortList());
		busyIndicator.idle();
	}
	
	@Override
	public void onEntityUpdated(EntityUpdatedEvent<T> event) {
		T entity = event.getEntity();
		int index = dataProvider.getList().indexOf(entity);
		if(index >= 0) {
			dataProvider.getList().set(index, entity);
		}
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
	    	    
	    /* getValue gets called during update. Need to combine objects
	     * and include a flag that is set when update starts. Using default
	     * functionality with Shit key is better anyway and have include
	     * note to user so they are aware of it. See addWidgets below.  
		Header<Boolean> checkHeader = new Header<Boolean>(new CheckboxCell()) {
			@Override
			public Boolean getValue() {
				return false; //selection.getSelectedSet().containsAll(table.getVisibleItems());
				
			}
		};
		
		checkHeader.setUpdater(new ValueUpdater<Boolean>() {
			@Override
			public void update(Boolean value) {
				List<T> displayedItems = table.getVisibleItems();
				for (T pending : displayedItems) {
					selection.setSelected(pending, value);
				}
			}
		});
		*/
	    
	    if(curator != null && curator.isAuthorised(entity)) {
			Column<T, Boolean> checkColumn = new Column<T, Boolean>(
					new DisableableCheckboxCell(true, false)) {
				@Override
				public Boolean getValue(T object) {
					return !object.getCreatedBy().equals(curator) ? selection.isSelected(object) : null;
				}
			};
						
			table.addColumn(checkColumn);
			table.setColumnWidth(checkColumn, 16, Unit.PX);
			
			table.setSelectionModel(selection,
		    		DefaultSelectionEventManager.<T> createCheckboxManager(0));
	    } else {
	    	table.setSelectionModel(new NoSelectionModel<T>(keyProvider));
	    }
	    
	    addTableColumns(table);
	    table.addColumn(new CreatedDateColumn(), "Created");
	    table.addColumn(new CreatedByColumn(), "Created By");
	    
	    if(curator != null) {
		    IconActionCell.Delegate<T> delegate = new IconActionCell.Delegate<T>() {
		    	@Override
		    	public void execute(T entity) {
		    		editView.setEntity(entity);
		    		editView.show();
		    	}
		    };
		    		    
		    Column<T, T> editColumn = 
		    		new Column<T, T>(new IconActionCell<T>(ImageResources.INSTANCE.editIcon(), delegate)) {
		    	@Override
		    	public T getValue(T entity) {
		    		return entity;
		    	}
		    };
		    editColumn.setCellStyleNames("icon-action");
		    table.addColumn(editColumn, SafeHtmlUtils.fromSafeConstant("&nbsp;"));
	    }
	    
	    if(curator != null) {
		    IconActionCell.Delegate<T> delegate = new IconActionCell.Delegate<T>() {
		    	@Override
		    	public void execute(final T entity) {
		    		if(entity != null && curator.equals(entity.getCreatedBy())) {					
						if(errorLabel.isVisible()) {
							errorLabel.setVisible(false);
						}
						if(selection != null) {
							selection.setSelected(entity, false);
						}
						processing.add(entity);
						busyIndicator.busy();
						service.delete(entity, 
								new AsyncCallback<Void>() {
							@Override
							public void onFailure(Throwable caught) {
								GWT.log("Failed to delete pending item", caught);
								processing.remove(entity);
								busyIndicator.idle();
								if(caught instanceof InvalidEntityException) {
									InvalidEntityException e = (InvalidEntityException)caught;
									errorLabel.setText(e.getMessage() + ": " + e.getEntity());
									errorLabel.setVisible(true);
								} else {
									ErrorView.instance().onUncaughtException(caught);
								}
							}
		
							@Override
							public void onSuccess(Void nothing) {
								processing.remove(entity);
								dataProvider.getList().remove(entity);
								busyIndicator.idle();
								fireDeleteEvent(entity);
							}
						});
		    		}
		    	}
		    };
		    		    
		    Column<T, T> deleteColumn = 
		    		new Column<T, T>(new IconActionCell<T>(ImageResources.INSTANCE.deleteIcon(),
		    				delegate)) {
		    	@Override
		    	public T getValue(T entity) {
		    		return curator.equals(entity.getCreatedBy()) ? entity : null;
		    	}
		    };
		    deleteColumn.setCellStyleNames("icon-action");
		    table.addColumn(deleteColumn, SafeHtmlUtils.fromSafeConstant("&nbsp;"));
	    }
	    
		ListHandler<T> sortHandler = new ListHandler<T>(dataProvider.getList());
		for(int i = 0; i < table.getColumnCount(); i++) {
			Column<T, ?> column = table.getColumn(i);
			if(column.isSortable() && column instanceof Comparator<?>) {
				sortHandler.setComparator(column, (Comparator<T>)column);
			}
		}
		table.addColumnSortHandler(sortHandler);
		table.getColumnSortList().push(table.getColumn(
				curator != null && curator.isAuthorised(entity) ? 2 : 1));
	}
	
	protected abstract void addTableColumns(CellTable<T> table);
	
	protected abstract void fireUpdateEvent(T entity);
	
	protected abstract void fireDeleteEvent(T entity);
	
	protected void addWidgets() {
		VerticalPanel vertPanel = new VerticalPanel();
		HorizontalPanel filterPanel = new HorizontalPanel();
		Label filterLabel = new Label("Keyword Filter:");
		
		Button closeButton = new Button("Close");
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
		
		closeButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				Widget parent = ApproveRejectComposite.this.getParent();
				while(parent != null) {
					if(parent instanceof PopupPanel) {
						PopupPanel popup = (PopupPanel)parent;
						popup.hide();
						break;
					}					
					parent = parent.getParent();
				}
			}
		});
		
		filterLabel.addStyleName("dialog-label");
		
		filterPanel.addStyleName("dialog-hpanel");
		filterPanel.addStyleName("spaced-vert");
		filterPanel.add(filterLabel);
		filterPanel.add(filterTextBox);
		filterPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
				
		pager.setDisplay(table);
		pager.addStyleName("centered-hortz");
		/*
		if(curator != null && curator.isAuthorised(entity)) {
			Label tip = new Label("Use Shift key to select/unselect consecutive rows");
			tip.addStyleName("dialog-message");
			tip.addStyleName("info-icon-left");
			vertPanel.add(tip);
		} else {
			Label label = new Label("Insufficient privileges to approve/reject items");
			label.addStyleName("dialog-message");
			label.addStyleName("info-icon-left");
			vertPanel.add(label);
		}
		*/
		
		if(curator != null) {
			vertPanel.add(errorLabel);
		}
		vertPanel.add(filterPanel);
		vertPanel.add(table);
		vertPanel.add(pager);
				
		if(curator != null && curator.isAuthorised(entity)) {
			Label comments = new Label("Comments:");
			HorizontalPanel buttonsHPanel = new HorizontalPanel();
			
			buttonsHPanel.add(approveButton);
			buttonsHPanel.add(rejectButton);
			buttonsHPanel.add(closeButton);
			
			comments.addStyleName("dialog-label");
			buttonsHPanel.addStyleName("dialog-buttons");
			buttonsHPanel.addStyleName("centered-hortz");
			
			vertPanel.add(comments);
			vertPanel.add(commentsField);
			commentsField.getElement().getParentElement().addClassName("text-area-right-padded");
						
			vertPanel.add(buttonsHPanel);
		} else {
			vertPanel.add(closeButton);
			vertPanel.setCellHorizontalAlignment(closeButton, HasHorizontalAlignment.ALIGN_CENTER);
		}
		initWidget(vertPanel);
	}
	
	private void checkStatusChanges(Set<T> entities) {
		boolean sort = false;
		for(T entity : entities) {
			if(entity.getStatus().equals(Status.PENDING)) {
				addAprroveRejectColumns(entity);
				dataProvider.getList().add(entity);
				sort = true;
			} else { 
				fireUpdateEvent(entity);
			}
		}
				
		if(sort) {
			ColumnSortEvent.fire(table, table.getColumnSortList());
		}
	}
	
	private void addAprroveRejectColumns(Collection<T> entities) {
		for(T entity : entities) {
			addAprroveRejectColumns(entity);
		}
	}
	
	private void addAprroveRejectColumns(T entity) {
		for(CuratorAction action : entity.getCuratorActions()) {
			switch(action.getAction()) {
			case APPROVE:
				addApproveColumn();
				break;
			case REJECT:
				addRejectColumn();
				break;
			}
		}
	}
	
	private void addApproveColumn() {
		int rejectColumnIndex = -1;
		for(int i = 0; i < table.getColumnCount(); i++) {
			Column<T, ?> column = table.getColumn(i);
			if(column instanceof ApproveRejectComposite<?>.ApprovedByColumn) {
				return;
			}
			if(column instanceof ApproveRejectComposite<?>.RejectedByColumn) {
				rejectColumnIndex = i;
			}
		}
		
		if(rejectColumnIndex >= 0) {
			table.insertColumn(rejectColumnIndex, new ApprovedByColumn(), 
					"Approved By");
		} else if(curator != null) {
			table.insertColumn(table.getColumnCount() - 2, new ApprovedByColumn(), 
					"Approved By");
		} else {
			table.addColumn(new ApprovedByColumn(), "Approved By");
		}
	}
	
	private void addRejectColumn() {
		for(int i = 0; i < table.getColumnCount(); i++) {
			Column<T, ?> column = table.getColumn(i);
			if(column instanceof ApproveRejectComposite<?>.RejectedByColumn) {
				return;
			}
		}
		
		if(curator != null) {
			table.insertColumn(table.getColumnCount() - 2, 
					new RejectedByColumn(), "Rejected By");
		} else {
			table.addColumn(new RejectedByColumn(), "Rejected By");
		}
	}
}
