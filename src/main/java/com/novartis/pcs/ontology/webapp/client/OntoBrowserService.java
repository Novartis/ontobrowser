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
package com.novartis.pcs.ontology.webapp.client;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.novartis.pcs.ontology.entity.ControlledVocabulary;
import com.novartis.pcs.ontology.entity.ControlledVocabularyContext;
import com.novartis.pcs.ontology.entity.ControlledVocabularyDomain;
import com.novartis.pcs.ontology.entity.ControlledVocabularyTerm;
import com.novartis.pcs.ontology.entity.ControlledVocabularyTermLink;
import com.novartis.pcs.ontology.entity.Curator;
import com.novartis.pcs.ontology.entity.CuratorAction;
import com.novartis.pcs.ontology.entity.Datasource;
import com.novartis.pcs.ontology.entity.DuplicateEntityException;
import com.novartis.pcs.ontology.entity.InvalidEntityException;
import com.novartis.pcs.ontology.entity.Relationship;
import com.novartis.pcs.ontology.entity.RelationshipType;
import com.novartis.pcs.ontology.entity.Synonym;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.entity.VersionedEntity;
import com.novartis.pcs.ontology.service.search.result.HTMLSearchResult;
import com.novartis.pcs.ontology.service.search.result.InvalidQuerySyntaxException;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("service")
public interface OntoBrowserService extends RemoteService {
	public Curator loadCurrentCurator();
	public List<Term> loadRootTerms();
	public Term loadTerm(String id);
	public List<Term> loadOntologyTerms(String ontology);
	public String loadSVG(String id);
	public List<RelationshipType> loadAllRelationshipTypes();
	public List<Term> loadLastCreatedTerms(int max);
	
	public List<HTMLSearchResult> search(String pattern, boolean includeSynonyms) 
			throws InvalidQuerySyntaxException;
		
	public Term createChildTerm(String ontologyName, String termName,
			String definition, String url, String comments,
			String relatedTermRefId, String relationshipType,
			String datasoureAcronym, String referenceId,
			List<ControlledVocabularyTerm> synonyms,
			Synonym.Type synonymType)
			throws DuplicateEntityException, InvalidEntityException;
	
	public Term addSynonym(String termRefId, String synonym, Synonym.Type type,
			String source, String referenceId) 
			throws DuplicateEntityException, InvalidEntityException;
	public Term addSynonyms(String termRefId, 
			Collection<ControlledVocabularyTerm> terms, Synonym.Type type)
			throws DuplicateEntityException, InvalidEntityException;
	public Term addRelationship(String termRefId, String relatedTermRefId, String relationship)
			throws DuplicateEntityException, InvalidEntityException;
	
	public List<Datasource> loadPublicDatasources();
	public List<ControlledVocabulary> loadControlledVocabularies();
	public List<ControlledVocabularyTerm> loadControlledVocabularyTerms(ControlledVocabularyDomain domain);
	public List<ControlledVocabularyTerm> loadControlledVocabularyTerms(ControlledVocabularyDomain domain,
			ControlledVocabularyContext conext);
	public List<ControlledVocabularyTerm> loadControlledVocabularyTerms(ControlledVocabularyDomain domain,
			Datasource datasource);
	public List<ControlledVocabularyTerm> loadControlledVocabularyTerms(ControlledVocabularyDomain domain,
			ControlledVocabularyContext conext, Datasource datasource);
	
	public void excludeControlledVocabularyTerms(Set<ControlledVocabularyTerm> terms)
			throws InvalidEntityException;
	
	public List<ControlledVocabularyTermLink> loadControlledVocabularyTermLinks(ControlledVocabularyTerm term);
	
	public List<Term> loadPendingTerms();
	public List<Synonym> loadPendingSynonyms();
	public List<Relationship> loadPendingRelationships();
	
	public <T extends VersionedEntity> Set<T> approve(Set<T> pending, String comments)
			throws InvalidEntityException;
	public <T extends VersionedEntity> Set<T> reject(Set<T> pending, String comments)
			throws InvalidEntityException;
	
	public Term updateTerm(long termId, String definition, 
			String url, String comments) throws InvalidEntityException;
	public Synonym updateSynonym(long synonymId, Synonym.Type type)
			throws InvalidEntityException;
	public Relationship updateRelationship(long relationshipId, 
			String relationship) throws DuplicateEntityException, InvalidEntityException;
	
	public <T extends VersionedEntity> void delete(T entity) throws InvalidEntityException;
	
	public Term obsoleteTerm(long termId, long replacementTermId, String comments)
			throws InvalidEntityException;
	
	public Synonym obsoleteSynonym(long synonymId, long replacementSynonymId, String comments)
			throws InvalidEntityException;
	
	public Relationship obsoleteRelationship(long relationshipId, long replacementRelationshipId, String comments)
			throws InvalidEntityException;
	
	public List<CuratorAction> loadCuratorActions();
	
	public void changePassword(String oldPassword, String newPassword) throws InvalidEntityException;
}
