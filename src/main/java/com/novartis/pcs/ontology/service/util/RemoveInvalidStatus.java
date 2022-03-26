package com.novartis.pcs.ontology.service.util;

import com.novartis.pcs.ontology.entity.VersionedEntity;
import com.novartis.pcs.ontology.entity.VersionedEntity.Status;

import java.util.*;

public class RemoveInvalidStatus {
	private static final EnumSet<Status> valid = EnumSet.of(Status.PENDING, Status.APPROVED);
	
	public static <T extends VersionedEntity> boolean isValid(T entity) {
		return entity != null && valid.contains(entity.getStatus());
	}

	public static <T extends VersionedEntity> boolean removeInvalid(Collection<T> entities) {
		Set<T> invalid = new HashSet<T>();
    	for(T entity : entities) {
    		if(!isValid(entity)) {
    			invalid.add(entity);
    		}
    	}
    	return invalid.isEmpty() ? false : entities.removeAll(invalid);
	}

}
