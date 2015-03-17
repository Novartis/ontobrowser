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

import java.util.Comparator;

import com.google.gwt.user.cellview.client.TextColumn;

public abstract class ComparableTextColumn<T> extends TextColumn<T> implements Comparator<T> {
	public ComparableTextColumn() {
		setSortable(true);
	}
	
	@Override
	public int compare(T obj1, T obj2) {
		String value1 = getValue(obj1);
		String value2 = getValue(obj2);
		if(value1 == value2) {
			return 0;
		}
		
		if(value1 != null) {
			return value2 != null ? value1.compareToIgnoreCase(value2) : 1;
		}
		
		return -1; // value1 is null and value2 is not null
	}

}
