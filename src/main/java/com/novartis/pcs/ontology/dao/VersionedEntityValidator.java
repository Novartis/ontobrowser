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
package com.novartis.pcs.ontology.dao;

import java.util.logging.Logger;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.validation.ValidationException;

import com.novartis.pcs.ontology.entity.ReplaceableEntity;
import com.novartis.pcs.ontology.entity.VersionedEntity;
import com.novartis.pcs.ontology.entity.VersionedEntity.Status;

public class VersionedEntityValidator {
	protected Logger logger = Logger.getLogger(getClass().getName());
	
	@PrePersist
    @PreUpdate
    public void validate(VersionedEntity entity) {
    	switch(entity.getStatus()) {
    	case OBSOLETE:
    		if(entity.getObsoleteVersion() == null) {
    			logger.warning(entity.getClass().getSimpleName() 
        				+ " [id=" + entity.getId() 
        				+ "] has status OBSOLETE but obsolete version is not set");
    			throw new ValidationException("Obsolete Version cannot be null");
    		}
    	case APPROVED:    		
    		if(entity.getApprovedVersion() == null) {
    			logger.warning(entity.getClass().getSimpleName() 
        				+ " [id=" + entity.getId() 
        				+ "] has status APPROVED but approved version is not set");
        		throw new ValidationException("Approved Version cannot be null");
    		}
    	default:
    		if(entity.getCreatedVersion() == null) {
    			throw new ValidationException("Created Version cannot be null");
        	}
    	}
    	
    	if(entity instanceof ReplaceableEntity<?>) {
    		ReplaceableEntity<?> replaceable = (ReplaceableEntity<?>)entity;
    		if(replaceable.getReplacedBy() != null) {
    			if(!entity.getStatus().equals(Status.OBSOLETE)) {
	    			throw new ValidationException("Entity must have status of obsolete if being replaced by another entity");
	    		}
	    		
	    		if(entity.equals(replaceable.getReplacedBy())) {
	    			throw new ValidationException("Entity can't be a replacement for itself");
	    		}
    		}
    	}
    }
}
