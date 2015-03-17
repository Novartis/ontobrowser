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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.novartis.pcs.ontology.entity.ControlledVocabulary;
import com.novartis.pcs.ontology.entity.ControlledVocabularyContext;
import com.novartis.pcs.ontology.entity.ControlledVocabularyDomain;
import com.novartis.pcs.ontology.entity.ControlledVocabularyTerm;
import com.novartis.pcs.ontology.entity.ControlledVocabularyTermLink;
import com.novartis.pcs.ontology.entity.Curator;
import com.novartis.pcs.ontology.entity.CuratorAction;
import com.novartis.pcs.ontology.entity.Datasource;
import com.novartis.pcs.ontology.entity.Relationship;
import com.novartis.pcs.ontology.entity.RelationshipType;
import com.novartis.pcs.ontology.entity.Synonym;
import com.novartis.pcs.ontology.entity.Synonym.Type;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.entity.VersionedEntity;
import com.novartis.pcs.ontology.service.search.result.HTMLSearchResult;

/**
 * The async counterpart of <code>OntoBrowserService</code>.
 */
public interface OntoBrowserServiceAsync {
	void loadRootTerms(AsyncCallback<List<Term>> callback);
	void loadCurrentCurator(AsyncCallback<Curator> callback);
	void loadTerm(String referenceId, AsyncCallback<Term> asyncCallback);
	void loadOntologyTerms(String ontology, AsyncCallback<List<Term>> callback);
	void loadSVG(String referenceId, AsyncCallback<String> asyncCallback);
	void search(String pattern, boolean includeSynonyms, AsyncCallback<List<HTMLSearchResult>> callback);
	void loadAllRelationshipTypes(AsyncCallback<List<RelationshipType>> callback);
	void loadLastCreatedTerms(int max, AsyncCallback<List<Term>> callback);
	void createChildTerm(String ontologyName, String termName,
			String definition, String url, String comments,
			String relatedTermRefId, String relationshipType,
			String datasoureAcronym, String referenceId,
			List<ControlledVocabularyTerm> synonyms, Type synonymType,
			AsyncCallback<Term> callback);
	void addSynonym(String termRefId, String synonym, Type type, String source,
			String referenceId, AsyncCallback<Term> callback);
	void addSynonyms(String termRefId, 
			Collection<ControlledVocabularyTerm> terms,
			Synonym.Type type,
			AsyncCallback<Term> callback);
	void addRelationship(String termRefId, String relatedTermRefId,
			String relationship, AsyncCallback<Term> callback);
	void loadPublicDatasources(AsyncCallback<List<Datasource>> callback);
	void loadControlledVocabularies(
			AsyncCallback<List<ControlledVocabulary>> callback);
	void loadControlledVocabularyTerms(ControlledVocabularyDomain domain,
			AsyncCallback<List<ControlledVocabularyTerm>> callback);
	void loadControlledVocabularyTerms(ControlledVocabularyDomain domain,
			ControlledVocabularyContext conext,
			AsyncCallback<List<ControlledVocabularyTerm>> callback);
	void loadControlledVocabularyTerms(ControlledVocabularyDomain domain,
			Datasource datasource,
			AsyncCallback<List<ControlledVocabularyTerm>> callback);
	void loadControlledVocabularyTerms(ControlledVocabularyDomain domain,
			ControlledVocabularyContext conext, Datasource datasource,
			AsyncCallback<List<ControlledVocabularyTerm>> callback);
	void excludeControlledVocabularyTerms(
			Set<ControlledVocabularyTerm> terms, AsyncCallback<Void> callback);
	void loadControlledVocabularyTermLinks(ControlledVocabularyTerm term,
			AsyncCallback<List<ControlledVocabularyTermLink>> callback);
	void loadPendingTerms(AsyncCallback<List<Term>> callback);
	void loadPendingSynonyms(AsyncCallback<List<Synonym>> callback);
	void loadPendingRelationships(AsyncCallback<List<Relationship>> callback);
	<T extends VersionedEntity> void approve(Set<T> pending, String comments,
			AsyncCallback<Set<T>> callback);
	<T extends VersionedEntity> void reject(Set<T> pending, String comments,
			AsyncCallback<Set<T>> callback);
	void updateTerm(long termId, String definition, String url,
			String comments, AsyncCallback<Term> callback);
	void updateSynonym(long synonymId, Type type,
			AsyncCallback<Synonym> callback);
	void updateRelationship(long relationshipId, String relationship,
			AsyncCallback<Relationship> callback);
	void obsoleteTerm(long termId, long replacementTermId, String comments,
			AsyncCallback<Term> callback);
	void obsoleteSynonym(long synonymId, long replacementSynonymId,
			String comments, AsyncCallback<Synonym> callback);
	void obsoleteRelationship(long relationshipId,
			long replacementRelationshipId, String comments,
			AsyncCallback<Relationship> callback);
	void loadCuratorActions(AsyncCallback<List<CuratorAction>> callback);
	void delete(VersionedEntity entity, AsyncCallback<Void> callback);
	void changePassword(String oldPassword, String newPassword,
			AsyncCallback<Void> callback);
}
