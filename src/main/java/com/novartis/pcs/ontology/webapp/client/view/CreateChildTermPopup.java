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

import static com.google.gwt.event.dom.client.KeyCodes.KEY_ENTER;

import java.util.Collections;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.FontWeight;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.VerticalAlign;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.NoSelectionModel;
import com.novartis.pcs.ontology.entity.ControlledVocabularyDomain;
import com.novartis.pcs.ontology.entity.ControlledVocabularyTerm;
import com.novartis.pcs.ontology.entity.Datasource;
import com.novartis.pcs.ontology.entity.RelationshipType;
import com.novartis.pcs.ontology.entity.Synonym;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.service.util.DatasourceAcronymComparator;
import com.novartis.pcs.ontology.webapp.client.OntoBrowserServiceAsync;
import com.novartis.pcs.ontology.webapp.client.event.ViewTermEvent;
import com.novartis.pcs.ontology.webapp.client.event.ViewTermHandler;
import com.novartis.pcs.ontology.webapp.client.util.UrlValidator;
import com.novartis.pcs.ontology.webapp.client.view.CrossRefPopup.NameColumn;
import com.novartis.pcs.ontology.webapp.client.view.CrossRefPopup.SourceColumn;

public class CreateChildTermPopup implements OntoBrowserPopup, ViewTermHandler, 
		ClickHandler, KeyPressHandler, KeyUpHandler, 
		ChangeHandler, ValueChangeHandler<String> {
	private static final int MAX_LEN = 64;
	
	private final OntoBrowserServiceAsync service;
	private final EventBus eventBus;
	private final DialogBox dialogBox = new DialogBox(false, true);
	private final BusyIndicatorHandler busyIndicator = new WidgetBusyIndicatorHandler(dialogBox.getCaption().asWidget());
	private final TextBox nameField = new ClipboardAwareTextBox();
	private final TextArea definitionField = new TextArea();
	private final TextBox urlField = new TextBox();
	private final TextArea commentsField = new TextArea();
	private final ListBox typeDropBox = new ListBox(false);
	private final ListBox sourceDropBox = new ListBox(false);
	private final TextBox referenceIdField = new ClipboardAwareTextBox();
		
	private final CellTable<ControlledVocabularyTerm> synonymTable = 
			new CellTable<ControlledVocabularyTerm>(new EntityKeyProvider<ControlledVocabularyTerm>());
	
	private final Label parentTermLabel = new Label();
	private final Label nameError = new Label();
	private final Label definitionError = new Label();
	private final Label urlError = new Label();
	private final Label commentsError = new Label();
	private final Label sourceError = new Label();
	private final Label referenceIdError = new Label();
	private final Label synonymError = new Label();
	private final Label typeError = new Label();
	private final Button createButton = new Button("Create");
	
	private Term currentTerm;
	private int defaultTypeIndex;
	private ControlledVocabularyTermProvider synonymProvider;
	private List<ControlledVocabularyTerm> currentSynonyms;
	private Synonym.Type synonymType;
	
	public CreateChildTermPopup(OntoBrowserServiceAsync service,
			EventBus eventBus) {
		this.service = service;
		this.eventBus = eventBus;
		
		nameField.setMaxLength(MAX_LEN);
		nameField.setVisibleLength(MAX_LEN);
		nameField.addKeyPressHandler(this);
		nameField.addKeyUpHandler(this);
		nameField.addValueChangeHandler(this);
		
		definitionField.setCharacterWidth(MAX_LEN);
		definitionField.setVisibleLines(2);
		definitionField.addKeyPressHandler(this);
		
		urlField.setVisibleLength(MAX_LEN);
		urlField.addKeyPressHandler(this);
		
		commentsField.setCharacterWidth(MAX_LEN);
		commentsField.setVisibleLines(2);
		commentsField.addKeyPressHandler(this);
		
		sourceDropBox.addChangeHandler(this);
		referenceIdField.setMaxLength(MAX_LEN);
		referenceIdField.setVisibleLength(MAX_LEN);
		referenceIdField.setEnabled(false);
		referenceIdField.addKeyPressHandler(this);
		referenceIdField.addKeyUpHandler(this);
		referenceIdField.addValueChangeHandler(this);
		
		dialogBox.setText("Create Child Term");
		dialogBox.setGlassEnabled(true);
		dialogBox.setAnimationEnabled(true);
		dialogBox.addStyleName("gwt-ModalDialogBox");
		
		addRelationshipTypes();
		addDialogWidgets();
		
		service.loadPublicDatasources(new AsyncCallback<List<Datasource>>() {
			@Override
			public void onFailure(Throwable caught) {
				GWT.log("Failed to load public datasources", caught);
				ErrorView.instance().onUncaughtException(caught);
			}

			@Override
			public void onSuccess(List<Datasource> datasources) {
				Collections.sort(datasources, new DatasourceAcronymComparator());
				sourceDropBox.addItem("", "");
				for(Datasource datasource : datasources) {
					String label = datasource.getAcronym() + " - " + datasource.getName();
					sourceDropBox.addItem(label, datasource.getAcronym());
				}
				sourceDropBox.setSelectedIndex(0);
			}
		});
		
		createButton.addClickHandler(this);
		createButton.setEnabled(false);
		
		eventBus.addHandler(ViewTermEvent.TYPE, this);
	}
	
	@Override
	public void show() {
		if(synonymProvider != null) {
			currentSynonyms = synonymProvider.getTerms();
			synonymType = synonymProvider.getSynonymType();
			if(currentSynonyms != null && !currentSynonyms.isEmpty()) {
				ControlledVocabularyTerm firstSynonym = currentSynonyms.get(0);
				nameField.setValue(firstSynonym.getName().toLowerCase());
				
				
				synonymTable.setRowData(currentSynonyms);
				synonymTable.setRowCount(currentSynonyms.size(), true);
				synonymTable.setVisible(true);
				synonymError.setVisible(true);
				
				if(currentTerm != null) {
					for(ControlledVocabularyTerm synonym : currentSynonyms) {
						ControlledVocabularyDomain domain = synonym.getControlledVocabulary().getDomain();
						if(!domain.getOntologies().contains(currentTerm.getOntology())) {
							synonymError.setText("Cannot map controlled vocabulary term from " 
									+ domain.getName()
									+ " domain to " 
									+ currentTerm.getOntology().getName()
									+ " ontology");
							break;
						}
					}
				}
			} else {
				synonymTable.setVisible(false);
				synonymError.setVisible(false);
			}
		} else {
			synonymTable.setVisible(false);
			synonymError.setVisible(false);
		}
		
		enableCreateButton();
		dialogBox.center();
		nameField.setFocus(true);
		typeDropBox.setSelectedIndex(defaultTypeIndex);
	}
	
	@Override
	public void onViewTerm(ViewTermEvent event) {
		currentTerm = event.getTerm();
		parentTermLabel.setText(currentTerm.getName());
	}
	
	@Override
	public void onClick(ClickEvent event) {
		submit();
	}
	
	@Override
	public void onKeyPress(KeyPressEvent event) {
		int keyCode = event.getCharCode();
		if(keyCode == KEY_ENTER) {
			submit();
		}
	}
	
	@Override
    public void onKeyUp(KeyUpEvent event) {
		enableCreateButton();
    }
	
	@Override
	public void onChange(ChangeEvent event) {
		if(sourceDropBox.getSelectedIndex() > 0) {
			referenceIdField.setEnabled(true);
		} else {
			referenceIdField.setEnabled(false);
			referenceIdField.setValue(null);
		}
	}
		
	@Override
	public void onValueChange(ValueChangeEvent<String> event) {
		enableCreateButton();		
	}
	
	public ControlledVocabularyTermProvider getSynonymProvider() {
		return synonymProvider;
	}

	public void setSynonymProvider(ControlledVocabularyTermProvider synonymProvider) {
		this.synonymProvider = synonymProvider;
	}
	
	private void enableCreateButton() {
		boolean enable = nameField.getValue().trim().length() > 0;
		
		if(enable && sourceDropBox.getSelectedIndex() > 0) {
			enable = referenceIdField.getValue().trim().length() > 0;
		}
		
		if(enable && currentSynonyms != null && currentTerm != null) {
			for(ControlledVocabularyTerm synonym : currentSynonyms) {
				ControlledVocabularyDomain domain = synonym.getControlledVocabulary().getDomain();
						
				if(!domain.getOntologies().contains(currentTerm.getOntology())) {
					enable = false;
					break;
				}
			}
		}
		
		createButton.setEnabled(enable);
	}

	private void submit() {
		if(currentTerm != null 
				&& nameField.getValue().trim().length() > 0
				&& (sourceDropBox.getSelectedIndex() == 0 || referenceIdField.getValue().trim().length() > 0)) {
			String source = sourceDropBox.getSelectedIndex() > 0 ?
					sourceDropBox.getValue(sourceDropBox.getSelectedIndex()) : null;
			String refId = source != null ? referenceIdField.getValue() : null;
			String url = urlField.getValue().trim(); 
			if(url.length() > 0 && !UrlValidator.validate(url, urlError)) {				
				return;
			} else {
				urlError.setText(null);
			}
			
			nameError.setText(null);
			busyIndicator.busy();
			createButton.setEnabled(false);
			service.createChildTerm(currentTerm.getOntology().getName(),
					nameField.getValue().trim(),
					definitionField.getValue().trim(),
					url,
					commentsField.getValue().trim(),
					currentTerm.getReferenceId(),
					typeDropBox.getValue(typeDropBox.getSelectedIndex()),
					source,
					refId,
					currentSynonyms,
					synonymType,
					new AsyncCallback<Term>() {
				public void onFailure(Throwable caught) {
					GWT.log("Failed to create new child term", caught);
					nameError.setText(caught.getMessage());
					enableCreateButton();
					busyIndicator.idle();
				}
	
				public void onSuccess(Term term) {
					History.newItem(term.getReferenceId(), false);
					eventBus.fireEvent(new ViewTermEvent(term));
					nameField.setValue(null);
					nameError.setText(null);
					definitionField.setValue(null);
					urlField.setValue(null);
					urlError.setText(null);
					synonymError.setText(null);
					commentsField.setValue(null);
					sourceDropBox.setSelectedIndex(0);
					referenceIdField.setEnabled(false); 
					referenceIdField.setValue(null);
					
					if(synonymProvider != null && currentSynonyms != null) {
						synonymProvider.removeTerms(currentSynonyms);
					}
					
					busyIndicator.idle();
					dialogBox.hide();
				}
			});
		}
	}
	
	private void addRelationshipTypes() {
		service.loadAllRelationshipTypes(new AsyncCallback<List<RelationshipType>>() {
			public void onFailure(Throwable caught) {
				GWT.log("Failed to load relationship types", caught);
				typeDropBox.clear();
				ErrorView.instance().onUncaughtException(caught);
			}

			public void onSuccess(List<RelationshipType> types) {
				int i = 0;
				
				for(RelationshipType type : types) {
					String label = type.getRelationship().replace('_', ' ');
					String value = type.getRelationship();
					typeDropBox.addItem(label, value);
					if(value.equals("is_a")) {
						defaultTypeIndex = i;
					}
					i++;
				}
				typeDropBox.setSelectedIndex(defaultTypeIndex);
				
			}
		});
	}
	
	private void addDialogWidgets() {
		VerticalPanel dialogVPanel = new VerticalPanel();
		HorizontalPanel buttonsHPanel = new HorizontalPanel();
		HorizontalPanel relshipTypeHPanel = new HorizontalPanel();
		Button cancelButton = new Button("Cancel");
		
		TextColumn<ControlledVocabularyTerm> synonymTypeColumn = 
				new TextColumn<ControlledVocabularyTerm>() {
			@Override
			public String getValue(ControlledVocabularyTerm term) {
				return synonymType != null ? 
						synonymType.toString() : Synonym.Type.RELATED.toString();
			}
		};

		
		cancelButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				nameField.setValue(null);
				nameError.setText(null);
				definitionField.setValue(null);
				urlField.setValue(null);
				urlError.setText(null);
				commentsField.setValue(null);
				sourceDropBox.setSelectedIndex(0);
				referenceIdField.setEnabled(false); 
				referenceIdField.setValue(null);
				synonymError.setText(null);
				createButton.setEnabled(false);
				
				dialogBox.hide();
			}
		});
		
		relshipTypeHPanel.add(typeDropBox);
		relshipTypeHPanel.add(parentTermLabel);
		parentTermLabel.getElement().getStyle().setMarginLeft(12, Unit.PX);
		parentTermLabel.getElement().getStyle().setVerticalAlign(VerticalAlign.MIDDLE);
		parentTermLabel.getElement().getStyle().setFontWeight(FontWeight.BOLD);
				
		buttonsHPanel.add(createButton);
		buttonsHPanel.add(cancelButton);
		buttonsHPanel.addStyleName("dialog-buttons");	
				
		dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_LEFT);
		dialogVPanel.add(new Label("Name:"));
		dialogVPanel.add(nameField);
		dialogVPanel.add(nameError);
		dialogVPanel.add(new Label("Definition:"));
		dialogVPanel.add(definitionField);
		dialogVPanel.add(definitionError);
		dialogVPanel.add(new Label("URL:"));
		dialogVPanel.add(urlField);
		dialogVPanel.add(urlError);
		dialogVPanel.add(new Label("Comments:"));
		dialogVPanel.add(commentsField);
		dialogVPanel.add(commentsError);
		dialogVPanel.add(new Label("Source:"));
		dialogVPanel.add(sourceDropBox);
		dialogVPanel.add(sourceError);
		dialogVPanel.add(new Label("Reference Id:"));
		dialogVPanel.add(referenceIdField);
		dialogVPanel.add(referenceIdError);
		
		synonymTable.setWidth("100%");
		synonymTable.addStyleName("gwt-CellTable");
		synonymTable.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
		synonymTable.setSelectionModel(new NoSelectionModel<ControlledVocabularyTerm>(
				synonymTable.getKeyProvider()));
		
		synonymTable.addColumn(new NameColumn(), "Synonym");
		synonymTable.addColumn(synonymTypeColumn, "Type");
		synonymTable.addColumn(new SourceColumn(), "Source");
		dialogVPanel.add(synonymTable);
		dialogVPanel.add(synonymError);
		
		dialogVPanel.add(new Label("Relationship:"));
		//dialogVPanel.add(typeDropBox);
		dialogVPanel.add(relshipTypeHPanel);
		dialogVPanel.add(typeError);
		
		dialogVPanel.add(buttonsHPanel);
		dialogVPanel.setCellHorizontalAlignment(buttonsHPanel, VerticalPanel.ALIGN_CENTER);
		
		for(Widget widget : dialogVPanel) {
			if(widget instanceof Label) {
				Label label = (Label)widget;
				if(label.getText().length() != 0) {
					label.addStyleName("dialog-label");
				} else {
					label.addStyleName("dialog-error");
				}
			}
		}
		
		dialogBox.setWidget(dialogVPanel);
	}
}
