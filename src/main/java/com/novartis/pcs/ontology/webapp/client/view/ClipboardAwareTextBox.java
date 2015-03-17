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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.TextBox;

public class ClipboardAwareTextBox extends TextBox {

	public ClipboardAwareTextBox() {
		super();
		sinkEvents(Event.ONPASTE);
	}

	public ClipboardAwareTextBox(Element element) {
		super(element);
		sinkEvents(Event.ONPASTE);
	}
	
	@Override
    public void onBrowserEvent(Event event) {
        super.onBrowserEvent(event);
        if(DOM.eventGetType(event) == Event.ONPASTE) {
        	Scheduler.get().scheduleDeferred(new ScheduledCommand() {
                @Override
                public void execute() {
                    ValueChangeEvent.fire(ClipboardAwareTextBox.this, getValue());
                }

            });
        }
    }
}
