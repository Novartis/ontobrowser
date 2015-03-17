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

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

public class DisableableCheckboxCell extends CheckboxCell {
	/**
	 * An html string representation of a disabled input box.
	 */
	private static final SafeHtml INPUT_DISABLED = SafeHtmlUtils.fromSafeConstant("<input type=\"checkbox\" tabindex=\"-1\" disabled/>");
		
	public DisableableCheckboxCell() {
		super();
	}

	public DisableableCheckboxCell(boolean dependsOnSelection,
			boolean handlesSelection) {
		super(dependsOnSelection, handlesSelection);
	}
	
	@Override
	public void render(com.google.gwt.cell.client.Cell.Context context,
			Boolean value, SafeHtmlBuilder sb) {
		if(value == null) {
			sb.append(INPUT_DISABLED);
		} else {
			super.render(context, value, sb);
		}
	}
}
