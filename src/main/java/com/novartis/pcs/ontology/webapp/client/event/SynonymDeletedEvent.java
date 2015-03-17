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
package com.novartis.pcs.ontology.webapp.client.event;

import com.google.web.bindery.event.shared.Event;
import com.novartis.pcs.ontology.entity.Synonym;


public class SynonymDeletedEvent extends EntityDeletedEvent<Synonym> {
	public static final Type<EntityDeletedHandler<Synonym>> TYPE = new Type<EntityDeletedHandler<Synonym>>();
	
	public SynonymDeletedEvent(Synonym synonym) {
		super(synonym);
	}

	@Override
	public Event.Type<EntityDeletedHandler<Synonym>> getAssociatedType() {
		return TYPE;
	}
}
