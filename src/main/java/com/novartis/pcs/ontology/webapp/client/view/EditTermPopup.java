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

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.novartis.pcs.ontology.entity.Curator;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.webapp.client.OntoBrowserServiceAsync;
import com.novartis.pcs.ontology.webapp.client.event.TermUpdatedEvent;
import com.novartis.pcs.ontology.webapp.client.util.UrlValidator;

public class EditTermPopup extends EditPopup implements OntoBrowserEditPopup<Term>, ClickHandler, KeyPressHandler {
	private static final int MAX_LEN = 64;
	
	private final OntoBrowserServiceAsync service;
	private final EventBus eventBus;
	private final DialogBox dialogBox = new DialogBox(false, true);
	private final BusyIndicatorHandler busyIndicator = new WidgetBusyIndicatorHandler(dialogBox.getCaption().asWidget());
	private final TextArea definitionField = new TextArea();
	private final TextBox urlField = new TextBox();
	private final TextArea commentsField = new TextArea();
	
	private final Label definitionError = new Label();
	private final Label urlError = new Label();
	private final Label commentsError = new Label();
	private final Button updateButton = new Button("Update");
	
	private Term term;
		
	public EditTermPopup(OntoBrowserServiceAsync service,
			EventBus eventBus, Curator curator) {
		this.service = service;
		this.eventBus = eventBus;
				
		definitionField.setCharacterWidth(MAX_LEN);
		definitionField.setVisibleLines(3);
		definitionField.addKeyPressHandler(this);
		
		urlField.setVisibleLength(MAX_LEN);
		urlField.addKeyPressHandler(this);
		
		commentsField.setCharacterWidth(MAX_LEN);
		commentsField.setVisibleLines(3);
		commentsField.addKeyPressHandler(this);
		
		dialogBox.setText("Edit Term");
		dialogBox.setGlassEnabled(true);
		dialogBox.setAnimationEnabled(true);
		dialogBox.addStyleName("gwt-ModalDialogBox");
		
		addDialogWidgets();
		updateButton.addClickHandler(this);
	}
		
	@Override
	public void setEntity(Term term) {
		this.term = term;
		dialogBox.setText("Edit Term: " + term.getName());
		definitionField.setValue(term.getDefinition());
		definitionError.setText(null);
		urlField.setValue(term.getUrl());
		urlError.setText(null);
		commentsField.setValue(term.getComments());
	}

	@Override
	public Term getEntity() {
		return term;
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
				
	private void submit() {
		if(term != null) {
			String url = urlField.getValue().trim(); 
			if(url.length() > 0 && !UrlValidator.validate(url, urlError)) {				
				return;
			} else {
				urlError.setText(null);
			}
						
			definitionError.setText(null);
			busyIndicator.busy();
			updateButton.setEnabled(false);
						
			service.updateTerm(term.getId(),
					definitionField.getValue().trim(),
					url,
					commentsField.getValue().trim(),
					new AsyncCallback<Term>() {
				public void onFailure(Throwable caught) {
					GWT.log("Failed to update term", caught);
					definitionError.setText(caught.getMessage());
					updateButton.setEnabled(true);
					busyIndicator.idle();
				}
	
				public void onSuccess(Term term) {															
					eventBus.fireEvent(new TermUpdatedEvent(term));
					busyIndicator.idle();
					dialogBox.hide();
					updateButton.setEnabled(true);
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
				dialogBox.hide();
			}
		});
								
		buttonsHPanel.add(updateButton);
		buttonsHPanel.add(cancelButton);
		buttonsHPanel.addStyleName("dialog-buttons");	
				
		dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_LEFT);
		dialogVPanel.add(new Label("Definition:"));
		dialogVPanel.add(definitionField);
		dialogVPanel.add(definitionError);
		dialogVPanel.add(new Label("URL:"));
		dialogVPanel.add(urlField);
		dialogVPanel.add(urlError);
		dialogVPanel.add(new Label("Comments:"));
		dialogVPanel.add(commentsField);
		dialogVPanel.add(commentsError);
		
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
