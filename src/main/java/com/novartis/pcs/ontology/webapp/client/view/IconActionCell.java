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

import com.google.gwt.cell.client.ActionCell;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.ImageResourceRenderer;

public class IconActionCell<C> extends ActionCell<C> {
	private static final ImageResourceRenderer renderer = new ImageResourceRenderer(); 
	private final SafeHtml iconHtml;
	
	public IconActionCell(ImageResource icon, Delegate<C> delegate) {
		super("", delegate);
		this.iconHtml = renderer.render(icon);
	}
	
	@Override
	public void render(Context context, C value, SafeHtmlBuilder sb) {
		if (value != null) {
			sb.append(iconHtml);
		}
	}
}
