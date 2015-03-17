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

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window.Location;
import com.google.gwt.user.client.rpc.StatusCodeException;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.web.bindery.event.shared.UmbrellaException;

public class ErrorView implements GWT.UncaughtExceptionHandler, ClickHandler {
	private static final ErrorView instance = new ErrorView();
	private final DialogBox dialogBox = new DialogBox(false, true);
	private final Label errorLabel = new Label("");
		
	public ErrorView() {
		VerticalPanel panel = new VerticalPanel();
		Button reloadButton = new Button("Restart");
				
		dialogBox.setText("System Error");
		dialogBox.setGlassEnabled(true);
		dialogBox.setAnimationEnabled(true);
		dialogBox.addStyleName("gwt-ErrorDialogBox");
		
		panel.addStyleName("dialog-vpanel");
		panel.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
				
		errorLabel.addStyleName("dialog-error-icon");
		panel.add(errorLabel);
		
		reloadButton.addClickHandler(this);
		panel.add(reloadButton);
		
		dialogBox.setWidget(panel);
	}

	@Override
	public void onUncaughtException(Throwable e) {
		GWT.log("Unexpected exception", e);
		
		if(e instanceof UmbrellaException) {
			UmbrellaException umbrellaException = (UmbrellaException)e;
			if(umbrellaException.getCause() != null) {
				e = umbrellaException.getCause();
			}
			if(umbrellaException.getCauses() != null) {
				for(Throwable cause : umbrellaException.getCauses()) {
					if(cause instanceof StatusCodeException) {
						e = cause;
						break;
					}
				}
			}
		}
		
		if(e instanceof StatusCodeException) {
			StatusCodeException statusCodeException = (StatusCodeException)e;
			if(statusCodeException.getStatusCode() == 401 
					|| statusCodeException.getStatusCode() == 403) {
				errorLabel.setText("Unauthorized");
			} else {
				errorLabel.setText("HTTP status error: " + statusCodeException.getStatusCode());
			}
		} else {
			errorLabel.setText(e.getMessage());
		}
		dialogBox.center();
	}
	
	public static ErrorView instance() {
		return instance;
	}

	@Override
	public void onClick(ClickEvent event) {
		Location.reload();
	}
}
