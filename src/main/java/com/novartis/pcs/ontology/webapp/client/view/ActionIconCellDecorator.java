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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.IconCellDecorator;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safecss.shared.SafeStyles;
import com.google.gwt.safecss.shared.SafeStylesBuilder;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment.VerticalAlignmentConstant;
import com.google.gwt.user.client.ui.impl.HyperlinkImpl;

public class ActionIconCellDecorator<C> extends IconCellDecorator<C> {
	/**
	 * The delegate that will handle events from the cell.
	 *
	 * @param <T> the type that this delegate acts on
	 */
	public static interface Delegate<T> {
		/**
		 * Perform the desired action on the given object.
		 *
		 * @param object the object to be acted upon
		 */
		void execute(T object);
	}
	
	public interface Templates extends SafeHtmlTemplates {
		/**
		 * The wrapper around the image vertically aligned to the bottom.
		 */
		@Template("<div style=\"{0}position:absolute;bottom:0px;line-height:0px;cursor:pointer;\">{1}</div>")
		SafeHtml imageWrapperBottom(SafeStyles styles, SafeHtml image);

		/**
		 * The wrapper around the image vertically aligned to the middle.
		 */
		@Template("<div style=\"{0}position:absolute;top:50%;line-height:0px;cursor:pointer;\">{1}</div>")
		SafeHtml imageWrapperMiddle(SafeStyles styles, SafeHtml image);

		/**
		 * The wrapper around the image vertically aligned to the top.
		 */
		@Template("<div style=\"{0}position:absolute;top:0px;line-height:0px;cursor:pointer;\">{1}</div>")
		SafeHtml imageWrapperTop(SafeStyles styles, SafeHtml image);
	}
	
	private static final Templates templates = GWT.create(Templates.class); 
	private static String direction = LocaleInfo.getCurrentLocale().isRTL()
		      ? "right" : "left";
	private static final HyperlinkImpl impl = GWT.create(HyperlinkImpl.class);
		
	private final Delegate<C> delegate;
	private final SafeHtml iconHtml;

	public ActionIconCellDecorator(ImageResource icon, Cell<C> cell,
			VerticalAlignmentConstant valign, int spacing,
			Delegate<C> delegate) {
		super(icon, cell, valign, spacing);
		this.delegate = delegate;
		this.iconHtml = getImageHtml(icon, valign);
	}

	public ActionIconCellDecorator(ImageResource icon, Cell<C> cell, 
			Delegate<C> delegate) {
		super(icon, cell);
		this.delegate = delegate;
		this.iconHtml = getImageHtml(icon, HasVerticalAlignment.ALIGN_MIDDLE);
	}
		
	@Override
	@SuppressWarnings("deprecation")
	public void onBrowserEvent(Context context, Element parent, C value,
			NativeEvent nativeEvent, ValueUpdater<C> valueUpdater) {
		Event event = Event.as(nativeEvent);
		if(DOM.eventGetType(event) == Event.ONCLICK && impl.handleAsClick(event)) {
			Element target = Element.as(event.getEventTarget());
			// first child is outer div (see IconCellDecorator)
			Element child = parent.getFirstChildElement();
			if(child != null) {
				// next child is div containing icon image
				child = child.getFirstChildElement();
				if(child != null && child.isOrHasChild(target)) {
					delegate.execute(value);
					DOM.eventPreventDefault(event);
				} else {
					super.onBrowserEvent(context, parent, value, nativeEvent, valueUpdater);
				}
			}
		}
	}

	@Override
	public Set<String> getConsumedEvents() {
		Set<String> events = super.getConsumedEvents();
		if(events == null || events.isEmpty()) {
			events = Collections.singleton("click");
		} else {
			events = new LinkedHashSet<String>(events);
			events.add("click");
			events = Collections.unmodifiableSet(events);
		}
		return events;
	}

	@Override
	protected SafeHtml getIconHtml(C value) {
		return iconHtml;
	}
	
	private SafeHtml getImageHtml(ImageResource res, VerticalAlignmentConstant valign) {
		AbstractImagePrototype proto = AbstractImagePrototype.create(res);
		SafeHtml image = SafeHtmlUtils.fromTrustedString(proto.getHTML());

		// Create the wrapper based on the vertical alignment.
		SafeStylesBuilder cssStyles =
				new SafeStylesBuilder().appendTrustedString(direction + ":0px;");
		if (HasVerticalAlignment.ALIGN_TOP == valign) {
			return templates.imageWrapperTop(cssStyles.toSafeStyles(), image);
		} else if (HasVerticalAlignment.ALIGN_BOTTOM == valign) {
			return templates.imageWrapperBottom(cssStyles.toSafeStyles(), image);
		} else {
			int halfHeight = (int) Math.round(res.getHeight() / 2.0);
			cssStyles.appendTrustedString("margin-top:-" + halfHeight + "px;");
			return templates.imageWrapperMiddle(cssStyles.toSafeStyles(), image);
		}
	}
}
