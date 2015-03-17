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
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.impl.HyperlinkImpl;
import com.novartis.pcs.ontology.entity.Ontology;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.webapp.client.OntoBrowserServiceAsync;
import com.novartis.pcs.ontology.webapp.client.event.ViewTermEvent;
import com.novartis.pcs.ontology.webapp.client.event.ViewTermHandler;

public class SVGView extends OntoBrowserView implements ViewTermHandler, ClickHandler {
	private static HyperlinkImpl impl = GWT.create(HyperlinkImpl.class);
	// need scroll panel for DockLayout otherwise exceptions are raised on resize
	private final ScrollPanel panel = new ScrollPanel();	
	private final HTML svgContainer = new HTML();

	private int fixedDocumentSpace;
	
	public SVGView(EventBus eventBus, OntoBrowserServiceAsync service) {
		super(eventBus,service);
				
		if(supportsSVG()) {
			panel.getElement().setId("svgPanel");
			//svgContainer.getElement().getStyle().setOverflow(Overflow.AUTO);
			svgContainer.addClickHandler(this);
			
			panel.add(svgContainer);
			initWidget(panel);
			addStyleName("padded-border");
			eventBus.addHandler(ViewTermEvent.TYPE, this);
		} else {
			Label label = new Label("Your web browser does not support SVG");
			label.addStyleName("centered");
			initWidget(label);
		}
	}

	@Override
	public void onViewTerm(ViewTermEvent event) {
		final Term term = event.getTerm();
		final Ontology ontology = term.getOntology();
						
		if(!ontology.isCodelist()) {			
			service.loadSVG(term.getReferenceId(), new AsyncCallback<String>() {
				public void onFailure(Throwable caught) {
					GWT.log("Failed to load SVG", caught);
					ErrorView.instance().onUncaughtException(caught);
				}
	
				public void onSuccess(String svg) {
					Element element = svgContainer.getElement();
					Element panelElement = panel.getElement();
					panelElement.setScrollLeft(0);
					panelElement.setScrollTop(0);
					element.setInnerHTML(svg);
	
					if(panelElement.getScrollWidth() > element.getClientWidth()
							|| panelElement.getScrollHeight() > element.getClientHeight()) {
						scrollSVG(term.getReferenceId());
					}
				}
			});
		}
	}
	
	@Override
	public void onClick(ClickEvent event) {
		NativeEvent nativeEvent = event.getNativeEvent();
		Element element = Element.as(nativeEvent.getEventTarget());
		String href = null;
		for(Element parent = element; 
				!svgContainer.getElement().equals(parent); 
				parent = parent.getParentElement()) {
			if("a".equalsIgnoreCase(parent.getNodeName())) {
				href = parent.getAttribute("xlink:href");
				break;
			}
		}
		if(href != null && impl.handleAsClick(Event.as(nativeEvent))) {
			int i = href.lastIndexOf('#');
			if(i >= 0) {
				History.newItem(href.substring(i + 1));
				DOM.eventPreventDefault(Event.as(nativeEvent));
			}
		}
	}
		
	private static native boolean supportsSVG() /*-{
		try {
			return $doc.implementation.hasFeature("http://www.w3.org/TR/SVG11/feature#BasicStructure", "1.1");
		} catch(e) {
			return false;
		}
	}-*/;
	
	//attempt to scroll highlighted SVG element into center view
	private static native void scrollSVG(String id) /*-{
		var div = $doc.getElementById("svgPanel");
		var e = $doc.getElementById(id);
		if(e && e.getBBox) {
			var b = e.getBBox();
			var m = e.getScreenCTM().translate(b.x + b.width/2, b.y + b.height/2);
			var x = Math.max(Math.floor(m.e - div.clientWidth/2), 0);
			var y = Math.max(Math.floor(m.f - div.clientHeight/2), 0);
			div.scrollLeft = x;
			div.scrollTop = y;
		}	
	}-*/;
}
