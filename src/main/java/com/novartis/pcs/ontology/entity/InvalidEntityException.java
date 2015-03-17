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
package com.novartis.pcs.ontology.entity;

import javax.ejb.ApplicationException;

@ApplicationException(rollback=true)
public class InvalidEntityException extends Exception {
	private static final long serialVersionUID = 1L;
	
	protected AbstractEntity entity;
		
	protected InvalidEntityException() {
		super();
	}

	public InvalidEntityException(AbstractEntity entity) {
		super();
		this.entity = entity;
	}

	public InvalidEntityException(AbstractEntity entity, String msg, Throwable t) {
		super(msg, t);
		this.entity = entity;
	}

	public InvalidEntityException(AbstractEntity entity, String msg) {
		super(msg);
		this.entity = entity;
		
	}

	public InvalidEntityException(AbstractEntity entity, Throwable t) {
		super(t);
		this.entity = entity;
	}

	public AbstractEntity getEntity() {
		return entity;
	}
}
