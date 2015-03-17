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

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.InlineHyperlink;
import com.google.gwt.user.client.ui.impl.HyperlinkImpl;

public class HyperlinkCell extends AbstractCell<InlineHyperlink> {
	private static final HyperlinkImpl impl = GWT.create(HyperlinkImpl.class);
	
	public HyperlinkCell() {
		super("click");
	}

	@Override
	public void render(Context context, InlineHyperlink h, SafeHtmlBuilder sb) {
		if(h != null) {
			sb.append(SafeHtmlUtils.fromTrustedString(h.toString()));
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onBrowserEvent(Context context,
			Element parent, InlineHyperlink value, NativeEvent nativeEvent,
			ValueUpdater<InlineHyperlink> valueUpdater) {
		super.onBrowserEvent(context, parent, value, nativeEvent, valueUpdater);
		Event event = Event.as(nativeEvent);
		if(DOM.eventGetType(event) == Event.ONCLICK && impl.handleAsClick(event)) {
			Element target = Element.as(event.getEventTarget());
			Element child = parent.getFirstChildElement();
			if(child.isOrHasChild(target)) {
				History.newItem(value.getTargetHistoryToken());
				DOM.eventPreventDefault(event);
			}
		}
	}
}
