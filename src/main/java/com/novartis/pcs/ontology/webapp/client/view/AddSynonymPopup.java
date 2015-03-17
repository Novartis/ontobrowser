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
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.novartis.pcs.ontology.entity.Datasource;
import com.novartis.pcs.ontology.entity.Synonym;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.service.util.DatasourceAcronymComparator;
import com.novartis.pcs.ontology.webapp.client.OntoBrowserServiceAsync;
import com.novartis.pcs.ontology.webapp.client.event.ViewTermEvent;
import com.novartis.pcs.ontology.webapp.client.event.ViewTermHandler;

public class AddSynonymPopup implements OntoBrowserPopup, ViewTermHandler, 
		ClickHandler, KeyPressHandler, KeyUpHandler, 
		ChangeHandler, ValueChangeHandler<String> {
	private static final int MAX_LEN = 256;
	private static final int VIS_LEN = 64;
	
	private final OntoBrowserServiceAsync service;
	private final EventBus eventBus;
	private final DialogBox dialogBox = new DialogBox(false, true);
	private final BusyIndicatorHandler busyIndicator = new WidgetBusyIndicatorHandler(dialogBox.getCaption().asWidget());
	private final TextBox synonymField = new ClipboardAwareTextBox();
	private final ListBox typeDropBox = new ListBox(false);
	private final ListBox sourceDropBox = new ListBox(false);
	private final TextBox referenceIdField = new ClipboardAwareTextBox();
	private final Label synonymError = new Label();
	private final Label typeError = new Label();
	private final Label sourceError = new Label();
	private final Label referenceIdError = new Label();
	private final Button addButton = new Button("Add");
	
	private Term currentTerm;
	
	public AddSynonymPopup(OntoBrowserServiceAsync service,
			EventBus eventBus) {
		this.service = service;
		this.eventBus = eventBus;
		synonymField.setMaxLength(MAX_LEN);
		synonymField.setVisibleLength(VIS_LEN);
		synonymField.addKeyPressHandler(this);
		synonymField.addKeyUpHandler(this);
		synonymField.addValueChangeHandler(this);
		
		for(Synonym.Type type : Synonym.Type.values()) {
			typeDropBox.addItem(type.toString(), type.name());
		}
		typeDropBox.setSelectedIndex(typeDropBox.getItemCount()-1);
		
		sourceDropBox.addChangeHandler(this);
		referenceIdField.setMaxLength(MAX_LEN);
		referenceIdField.setVisibleLength(VIS_LEN);
		referenceIdField.setEnabled(false);
		referenceIdField.addKeyPressHandler(this);
		referenceIdField.addKeyUpHandler(this);
		referenceIdField.addValueChangeHandler(this);
		
		dialogBox.setText("Add Synonym");
		dialogBox.setGlassEnabled(true);
		dialogBox.setAnimationEnabled(true);
		dialogBox.addStyleName("gwt-ModalDialogBox");
				
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
		
		addButton.addClickHandler(this);
		addButton.setEnabled(false);
				
		eventBus.addHandler(ViewTermEvent.TYPE, this);
	}
		
	@Override
	public void show() {
		dialogBox.center();
		synonymField.setFocus(true);
	}

	@Override
	public void onViewTerm(ViewTermEvent event) {
		currentTerm = event.getTerm();
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
		if(addButton.isEnabled() && 
				(synonymField.getValue().trim().length() == 0 ||
				(sourceDropBox.getSelectedIndex() > 0 && referenceIdField.getValue().trim().length() == 0))) {
			addButton.setEnabled(false);
		} else if(!addButton.isEnabled() &&
				synonymField.getValue().trim().length() > 0 && 
				(sourceDropBox.getSelectedIndex() == 0 || referenceIdField.getValue().trim().length() > 0)) {
			addButton.setEnabled(true);
		}
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
		if(addButton.isEnabled() && 
				(synonymField.getValue().trim().length() == 0 ||
				(sourceDropBox.getSelectedIndex() > 0 && referenceIdField.getValue().trim().length() == 0))) {
			addButton.setEnabled(false);
		} else if(!addButton.isEnabled() && 
				synonymField.getValue().trim().length() > 0 && 
				(sourceDropBox.getSelectedIndex() == 0 || referenceIdField.getValue().trim().length() > 0)) {
			addButton.setEnabled(true);
		}		
	}
	
	private void submit() {
		if(currentTerm != null 
				&& synonymField.getValue().trim().length() > 0
				&& (sourceDropBox.getSelectedIndex() == 0 || referenceIdField.getValue().trim().length() > 0)) {
			Synonym.Type type = Synonym.Type.valueOf(
					typeDropBox.getValue(typeDropBox.getSelectedIndex()));
			String source = sourceDropBox.getSelectedIndex() > 0 ?
					sourceDropBox.getValue(sourceDropBox.getSelectedIndex()) : null;
			String refId = source != null ? referenceIdField.getValue() : null;
			addButton.setEnabled(false);
			busyIndicator.busy();
			service.addSynonym(currentTerm.getReferenceId(),
					synonymField.getValue().trim(),
					type,
					source,
					refId,
					new AsyncCallback<Term>() {
				public void onFailure(Throwable caught) {
					GWT.log("Failed to add synonym", caught);
					synonymError.setText(caught.getMessage());
					addButton.setEnabled(true);
					busyIndicator.idle();
				}
	
				public void onSuccess(Term term) {
					eventBus.fireEvent(new ViewTermEvent(term));
					synonymField.setValue(null);
					synonymError.setText(null);
					typeDropBox.setSelectedIndex(typeDropBox.getItemCount()-1);
					sourceDropBox.setSelectedIndex(0);
					referenceIdField.setEnabled(false); 
					referenceIdField.setValue(null);
					busyIndicator.idle();
					dialogBox.hide();
				}
			});
		}
	}
	
	private void addDialogWidgets() {
		VerticalPanel dialogVPanel = new VerticalPanel();
		HorizontalPanel buttonsHPanel = new HorizontalPanel();
		Button cancelButton = new Button("Cancel");
				
		cancelButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				synonymField.setValue(null);
				synonymError.setText(null);
				typeDropBox.setSelectedIndex(typeDropBox.getItemCount()-1);
				sourceDropBox.setSelectedIndex(0);
				referenceIdField.setEnabled(false); 
				referenceIdField.setValue(null);
				addButton.setEnabled(false);
				dialogBox.hide();
			}
		});
		
		buttonsHPanel.add(addButton);
		buttonsHPanel.add(cancelButton);
		buttonsHPanel.addStyleName("dialog-buttons");	
		
		dialogVPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_LEFT);
		dialogVPanel.add(new Label("Synonym:"));
		dialogVPanel.add(synonymField);
		dialogVPanel.add(synonymError);
		dialogVPanel.add(new Label("Type:"));
		dialogVPanel.add(typeDropBox);
		dialogVPanel.add(typeError);
		dialogVPanel.add(new Label("Source:"));
		dialogVPanel.add(sourceDropBox);
		dialogVPanel.add(sourceError);
		dialogVPanel.add(new Label("Reference Id:"));
		dialogVPanel.add(referenceIdField);
		dialogVPanel.add(referenceIdError);
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
