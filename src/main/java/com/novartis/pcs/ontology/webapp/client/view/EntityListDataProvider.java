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

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.Range;
import com.novartis.pcs.ontology.entity.AbstractEntity;

public abstract class EntityListDataProvider<T extends AbstractEntity> extends
		ListDataProvider<T> {
	protected String filter;
	protected List<T> filteredList = new ArrayList<T>();
	protected boolean notFlushing;
	
	public EntityListDataProvider() {
		super(new EntityKeyProvider<T>());
	}
	
	public EntityListDataProvider(
			ProvidesKey<T> keyProvider) {
		super(keyProvider);
	}

	public String getFilter() {
		return filter;
	}

	public void setFilter(String filter) {
		filter = filter != null && filter.trim().length() > 0 ?
				filter.trim().toLowerCase() : null;
		if((this.filter == null || !this.filter.equals(filter))) {
			this.filter = filter;
			
			populateFilteredList();
			
			notFlushing = true;
			for(HasData<T> display : getDataDisplays()) {
				int length = display.getVisibleRange().getLength();
				display.setVisibleRangeAndClearData(new Range(0, length), true);
			}
			notFlushing = false;
		}
	}
	
	@Override
	public void refresh() {
		notFlushing = true;
		super.refresh();
		notFlushing = false;
	}
	
	protected void populateFilteredList() {
		filteredList.clear();
		if(filter != null) {
			for (T entity : getList()) {  
				if(!filter(entity)) {
					filteredList.add(entity);
				} 
			}
		}
	}
	
	protected abstract boolean filter(T entity);
		
	@Override
	protected void onRangeChanged(HasData<T> display) {
		notFlushing = true;
		super.onRangeChanged(display);
		notFlushing = false;
	}

	@Override  
	protected void updateRowData(HasData<T> display, int start, 
			List<T> values) {  
		if (filter == null) {
			updateRowCount(display, getList().size());
			super.updateRowData(display, start, values);
		} else {
			if(!notFlushing) {
				populateFilteredList();
			}
			updateRowCount(display, filteredList.size());
			super.updateRowData(display, 0, filteredList);
		}  
	}
	
	protected void updateRowCount(HasData<T> display, int rowCount) {
		Range range = display.getVisibleRange();
		
		if(display.getRowCount() != rowCount) {
			display.setRowCount(rowCount);
		}
				
		if(range.getStart() >= rowCount 
				|| range.getStart() % range.getLength() > 0) {
			int length = range.getLength();
			int start = length * (rowCount/length);
			if(rowCount > 0 && start == rowCount) {
				start -= length;
			}
			display.setVisibleRange(start, length);
		}
	}
}
