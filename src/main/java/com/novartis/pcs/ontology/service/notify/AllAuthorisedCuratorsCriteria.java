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
package com.novartis.pcs.ontology.service.notify;

import java.util.EnumSet;

import com.novartis.pcs.ontology.entity.Curator;
import com.novartis.pcs.ontology.entity.CuratorApprovalWeight.Entity;

public class AllAuthorisedCuratorsCriteria extends DefaultCuratorCriteria {
	protected final EnumSet<Entity> entityTypes;
	
	public AllAuthorisedCuratorsCriteria(EnumSet<Entity> entityTypes) {
		this.entityTypes = entityTypes;
	}
	
	@Override
	public boolean verify(Curator curator) {
		boolean authorised = false; 
		
		for(Entity entityType : entityTypes) {
			authorised = curator.isAuthorised(entityType);
			if(authorised) {
				break;
			}
		}
				
		return super.verify(curator) && authorised;
	}
}
