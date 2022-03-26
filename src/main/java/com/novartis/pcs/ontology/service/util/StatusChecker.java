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
package com.novartis.pcs.ontology.service.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import com.novartis.pcs.ontology.entity.InvalidEntityException;
import com.novartis.pcs.ontology.entity.VersionedEntity;
import com.novartis.pcs.ontology.entity.VersionedEntity.Status;

public class StatusChecker {
	private static final EnumSet<Status> valid = EnumSet.of(Status.PENDING, Status.APPROVED);
	
	public static <T extends VersionedEntity> boolean isValid(T entity) {
		return entity != null && valid.contains(entity.getStatus());
	}

	public static <T extends VersionedEntity> void validate(T... entities) throws InvalidEntityException {
		for(T entity : entities) {
			if(!isValid(entity)) {
				throw entity != null ? new InvalidEntityException(entity, 
						entity.getClass().getName() + " has invalid status: " + entity.getStatus())
					: new InvalidEntityException(entity, "Entity not found");
			}
		}
	}
	
	public static <T extends VersionedEntity> Collection<T> valid(Collection<T> entities) {
		Collection<T> valid = new ArrayList<T>();
    	for(T entity : entities) {
    		if(StatusChecker.isValid(entity)) {
    			valid.add(entity);
    		}
    	}
    	return valid;
	}
}
