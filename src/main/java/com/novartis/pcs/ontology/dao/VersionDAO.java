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

import java.sql.Timestamp;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import com.novartis.pcs.ontology.entity.InvalidEntityException;
import com.novartis.pcs.ontology.entity.Version;

/**
 * Stateless session bean DAO for Version entity
 */
@Stateless
@Local({VersionDAOLocal.class})
@Remote({VersionDAORemote.class})
public class VersionDAO extends CreatableEntityDAO<Version> 
	implements VersionDAOLocal, VersionDAORemote {
       
    public VersionDAO() {
        super();
    }

	@Override
	public void save(Version version) throws InvalidEntityException {
		if(version.getId() == 0L) {
			version.setPublishedDate(new Timestamp(System.currentTimeMillis()));
		}
				
		super.save(version);
	}
}
