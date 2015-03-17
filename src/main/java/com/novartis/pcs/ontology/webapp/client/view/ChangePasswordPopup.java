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
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.novartis.pcs.ontology.entity.InvalidPasswordException;
import com.novartis.pcs.ontology.webapp.client.OntoBrowserServiceAsync;

public class ChangePasswordPopup implements OntoBrowserPopup, ClickHandler, KeyPressHandler {
	private final static String[] LABELS = {
			"Current Password", "New Password", "Confirm Password" 
		};
		
	private final OntoBrowserServiceAsync service;
	private final DialogBox dialogBox = new DialogBox(false, true);
	private final BusyIndicatorHandler busyIndicator = new WidgetBusyIndicatorHandler(dialogBox.getCaption().asWidget());
		
	private final Grid grid = new Grid(LABELS.length, 2);
	private final Label passwordError = new Label();
	private final PasswordTextBox oldPasswordTextBox = new PasswordTextBox();
	private final PasswordTextBox newPasswordTextBox1 = new PasswordTextBox();
	private final PasswordTextBox newPasswordTextBox2 = new PasswordTextBox();
	private final Button updateButton = new Button("Change");
		
	
	public ChangePasswordPopup(OntoBrowserServiceAsync service) {
		this.service = service;
						
		dialogBox.setText("Change Password");
		dialogBox.setGlassEnabled(true);
		dialogBox.setAnimationEnabled(true);
		dialogBox.addStyleName("gwt-ModalDialogBox");
		
		addDialogWidgets();
		
		updateButton.addClickHandler(this);
		
		oldPasswordTextBox.addKeyPressHandler(this);
		newPasswordTextBox1.addKeyPressHandler(this);
		newPasswordTextBox2.addKeyPressHandler(this);	
	}
				
	@Override
	public void show() {
		dialogBox.center();
		oldPasswordTextBox.setFocus(true);
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
		String oldPassword = oldPasswordTextBox.getValue();
		String newPassword = newPasswordTextBox1.getValue();
		String confirmation = newPasswordTextBox2.getValue();
		
		if(oldPassword == null || oldPassword.length() == 0) {
			passwordError.setText("Current password required");
			return;
		}
		
		if(newPassword == null || newPassword.length() == 0) {
			passwordError.setText("New password required");
			return;
		}
		
		if(newPassword.equals(oldPassword)) {
			passwordError.setText("New password is same as current password");
			return;
		}
		
		if(confirmation == null || confirmation.length() == 0) {
			passwordError.setText("Password confirmation required");
			return;
		}
		
		if(newPassword.split("\\s+").length > 1) {
			passwordError.setText("Password contains whitespace");
			return;
		}
						
		if(!newPassword.equals(confirmation)) {
			passwordError.setText("Password confirmation does not match");
			return;
		}
		
		updateButton.setEnabled(false);
		passwordError.setText(null);
		busyIndicator.busy();
			
		service.changePassword(oldPassword, newPassword,
				new AsyncCallback<Void>() {
			public void onFailure(Throwable caught) {
				GWT.log("Failed to change password", caught);
				
				if(caught instanceof InvalidPasswordException) {
					passwordError.setText("Invalid password: " + caught.getMessage());
				} else {
					passwordError.setText(caught.getMessage());
				}
				
				updateButton.setEnabled(true);
				busyIndicator.idle();
			}

			public void onSuccess(Void v) {
				busyIndicator.idle();
				dialogBox.hide();
				updateButton.setEnabled(true);
			}
		});
	}
	
	private void addDialogWidgets() {
		VerticalPanel dialogVPanel = new VerticalPanel();
		HorizontalPanel buttonsHPanel = new HorizontalPanel();
		Button cancelButton = new Button("Cancel");
				
		cancelButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				newPasswordTextBox1.setText(null);
				newPasswordTextBox2.setText(null);
				passwordError.setText(null);
				dialogBox.hide();
			}
		});
		
		grid.setWidget(0, 1, oldPasswordTextBox);
		grid.setWidget(1, 1, newPasswordTextBox1);
		grid.setWidget(2, 1, newPasswordTextBox2);
		grid.addStyleName("gwt-Grid");
		for(int i = 0; i < LABELS.length; i++) {
			grid.setText(i, 0, LABELS[i] + ":");
		}
				
		CellFormatter cellFormatter = grid.getCellFormatter(); 
		for(int row = 0; row < grid.getRowCount(); row++) {
			cellFormatter.addStyleName(row, 0, "dialog-label");
		}
		
		passwordError.addStyleName("dialog-error");
		
		buttonsHPanel.add(updateButton);
		buttonsHPanel.add(cancelButton);
		buttonsHPanel.addStyleName("dialog-buttons");	
		
		dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_LEFT);
		dialogVPanel.add(grid);		
		dialogVPanel.add(passwordError);
		dialogVPanel.add(buttonsHPanel);
		dialogVPanel.setCellHorizontalAlignment(buttonsHPanel, VerticalPanel.ALIGN_CENTER);
		
				
		dialogBox.setWidget(dialogVPanel);
	}	
}
