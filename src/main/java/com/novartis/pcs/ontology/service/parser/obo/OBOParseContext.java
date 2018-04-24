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
package com.novartis.pcs.ontology.service.parser.obo;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.coode.owlapi.obo.parser.OBOParserHandler;
import org.coode.owlapi.obo.parser.OBOVocabulary;

import com.novartis.pcs.ontology.entity.Curator;
import com.novartis.pcs.ontology.entity.Datasource;
import com.novartis.pcs.ontology.entity.Ontology;
import com.novartis.pcs.ontology.entity.Relationship;
import com.novartis.pcs.ontology.entity.RelationshipType;
import com.novartis.pcs.ontology.entity.Synonym;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.entity.Version;
import com.novartis.pcs.ontology.entity.VersionedEntity;
import com.novartis.pcs.ontology.entity.VersionedEntity.Status;
import com.novartis.pcs.ontology.service.parser.InvalidFormatException;

public class OBOParseContext implements OBOParserHandler {    
    private static final Logger logger = 
    	Logger.getLogger(OBOParseContext.class.getName());
        
    private Map<String, OBOTagHandler> headerTagHandlers = new HashMap<String,OBOTagHandler>();
    private Map<String, OBOTagHandler> termTagHandlers = new HashMap<String,OBOTagHandler>();
    private Map<String, OBOTagHandler> typeDefTagHandlers = new HashMap<String,OBOTagHandler>();   
                    
    private final Ontology ontology;
    private final Curator curator;
    private final Version version;
    private final Map<String,Term> terms = new LinkedHashMap<String,Term>(1973);
    private final Map<String, RelationshipType> relationshipTypes = new LinkedHashMap<String, RelationshipType>(11);
    private final Map<String, Datasource> datasources = new LinkedHashMap<String, Datasource>(13);
    
    private boolean inHeader;
    private OBOVocabulary stanzaType;
    private String currentId;
    
    // Holds synonyms parsed for the current term
    private Set<Synonym> synonyms = new HashSet<Synonym>(73);
    
    // Holds relationships parsed for the current term
    private Set<Relationship> relationships = new HashSet<Relationship>(7);
    
    private Set<String> unsupportedTags = new HashSet<String>();
            
    public OBOParseContext(Ontology ontology, Collection<Term> terms, 
    		Collection<RelationshipType> relationshipTypes, 
    		Collection<Datasource> datasources,
    		Curator curator, Version version) {
        this.ontology = ontology;
        this.curator = curator;
        this.version = version;
        
        for(Term term : terms) {
        	this.terms.put(term.getReferenceId().toUpperCase(), term);
        }
        
        for(RelationshipType relationshipType : relationshipTypes) {
        	this.relationshipTypes.put(
        			relationshipType.getRelationship().toLowerCase(), relationshipType);
        }
        
        for(Datasource datasource : datasources) {
        	this.datasources.put(datasource.getAcronym().toUpperCase(), datasource);
        }
        
        setupTagHandlers();
    }

    public Ontology getOntology() {
        return ontology;
    }
    
    public Curator getCurator() {
		return curator;
	}

	public Version getVersion() {
		return version;
	}

	public Term getTerm(String referenceId) {
        Term term = terms.get(referenceId.toUpperCase());
        if(term == null) {
			term = new Term(ontology, null, referenceId, curator, version);
			term.setStatus(Status.APPROVED);
			term.setApprovedVersion(version);
			terms.put(referenceId.toUpperCase(), term);
		}
		return term;
    }
    
    public Collection<Term> getTerms() {
    	return terms.values();
    }

    public RelationshipType getRelationshipType(String relationship) {
    	RelationshipType relationshipType = relationshipTypes.get(relationship.toLowerCase());
    	if(relationshipType == null) {
			relationshipType = new RelationshipType(relationship, relationship, curator, version);
			relationshipType.setStatus(Status.APPROVED);
			relationshipType.setApprovedVersion(version);
			relationshipTypes.put(relationship.toLowerCase(), relationshipType);
		}
		return relationshipType;  	
    }
    
    public Collection<RelationshipType> getRelationshipTypes() {
    	return relationshipTypes.values();  	
    }
    
    public Datasource getDatasource(String acronym) {
    	Datasource datasource = datasources.get(acronym.toUpperCase());
    	if(datasource == null) {
			datasource = new Datasource(acronym, acronym, curator);
			datasources.put(acronym.toUpperCase(), datasource);
		}
		return datasource;
    }

    public Collection<Datasource> getDatasources() {
    	return datasources.values();
    }
    
    @Override
    public void startHeader() {
        inHeader = true;
        stanzaType = null;
        currentId = null;
        synonyms.clear();
        relationships.clear();
    }

    @Override
    public void endHeader() {
        inHeader = false;
    }   

    @Override
    public void startFrame(String name) {
    	if(name.equals(OBOVocabulary.TERM.getName())) {
    		stanzaType = OBOVocabulary.TERM;
    	} else if(name.equals(OBOVocabulary.TYPEDEF.getName())) {
    		stanzaType = OBOVocabulary.TYPEDEF;
    	} else {
    		logger.warning("Unsupported OBO stanza type: " + name);
    		stanzaType = null;
    	}
    	currentId = null;
    	synonyms.clear();
    	relationships.clear();
    }

    @Override
    public void endFrame() {
    	if(stanzaType == OBOVocabulary.TERM) {
    		Term term = getCurrentTerm();
    		
    		for(Synonym synonym : term.getSynonyms()) {
    			if(synonym.getControlledVocabularyTerm() == null
    					&& synonym.getStatus().equals(Status.APPROVED)
    					&& !synonyms.contains(synonym)) {
    				synonym.setStatus(Status.OBSOLETE);
    				synonym.setObsoleteVersion(version);
    			}
    		}
    		
    		for(Relationship relationship : term.getRelationships()) {
    			if(relationship.getStatus().equals(Status.APPROVED)
    				&& !relationships.contains(relationship)) {
    				relationship.setStatus(Status.OBSOLETE);
    				relationship.setObsoleteVersion(version);
    			}
    		}
    	}
    	
        stanzaType = null;
        currentId = null;
        synonyms.clear();
        relationships.clear();
    }

	@Override
	@SuppressWarnings("incomplete-switch")
    public void handleTagValue(String tag, String value, 
    		String qualifierBlock, String comment) {
    	OBOTagHandler handler = null;
    	
    	if(inHeader) {
    		handler = headerTagHandlers.get(tag);
    	} else if(stanzaType != null) {
        	if(tag.equals(OBOVocabulary.ID.getName())) {
        		currentId = unescapeTagValue(value);
        	} else if(currentId != null) {
        		switch(stanzaType) {
        		case TERM:
        			handler = termTagHandlers.get(tag);
        			break;
        		case TYPEDEF:
        			handler = typeDefTagHandlers.get(tag);
        			break;	
        		}
        	} else {
        		String msg = "Invalid OBO stanza: id is not defined (or not defined first)";
        		logger.severe(msg);
        		throw new InvalidFormatException(msg);
        	}
        }
    	
    	if(handler != null) {
   			handler.handleTagValue(tag, value, qualifierBlock, comment);
		} else if(!unsupportedTags.contains(tag)) {
			if(stanzaType != null) {
				logger.warning("Unsupported OBO tag " + tag + " for stanza " + stanzaType.getName());
			} else {
				logger.warning("Unsupported OBO tag " + tag);
			}
			unsupportedTags.add(tag);
		}
    }
    
    Term getCurrentTerm() {
        return currentId != null ? getTerm(currentId) : null;
    }   

    RelationshipType getCurrentRelationshipType() {
    	return currentId != null ? getRelationshipType(currentId) : null;
    }
    
    @SuppressWarnings("incomplete-switch")
    VersionedEntity getCurrentEntity() {
    	VersionedEntity entity = null;
    	if(inHeader) {
    		entity = ontology;
    	} else if(stanzaType != null) {
    		switch(stanzaType) {
    		case TERM:
    			entity = getCurrentTerm();
    			break;
    		case TYPEDEF:
    			entity = getCurrentRelationshipType();
    			break;
    		}
        }
    	return entity;
    }
    
    Synonym addSynonym(String synonym, Synonym.Type type) {
    	Term term = getCurrentTerm();
    	Synonym synonymObj = null;
    	
    	for(Synonym existing : term.getSynonyms()) {
    		if(existing.getControlledVocabularyTerm() == null
    				&& existing.getDatasource() == null
    				&& existing.getUrl() == null
    				&& existing.getSynonym().equals(synonym)) {
   				existing.setType(type);
    			synonymObj = existing;
    			break;
    		}
    	}
    	
    	if(synonymObj == null) {
    		synonymObj = new Synonym(term, synonym, type, curator, version);
    		synonymObj.setStatus(Status.APPROVED);
    		synonymObj.setApprovedVersion(version);
    	} else if(synonymObj.getStatus() != Status.APPROVED) {
			String msg = "Existing synonym for term (" 
				+ term.getReferenceId() 
				+ ") \"" + synonym
				+ "\" is not APPROVED: " + synonymObj.getStatus();
			
			logger.warning(msg);
		}
    	
    	synonyms.add(synonymObj);
    	return synonymObj;
    }
		
	Synonym addSynonym(String synonym, Synonym.Type type, String url) {
    	Term term = getCurrentTerm();
    	Synonym synonymObj = null;
    	
    	for(Synonym existing : term.getSynonyms()) {
    		if(existing.getControlledVocabularyTerm() == null
    				&& existing.getDatasource() == null
    				&& existing.getSynonym().equals(synonym)
    				&& url.equals(existing.getUrl())) {
   				existing.setType(type);
    			synonymObj = existing;
    			break;
    		}
    	}
    	
    	if(synonymObj == null) {
    		synonymObj = new Synonym(term, synonym, type, curator, version);
    		
    		try {
				synonymObj.setUrl(url);
			} catch(IllegalArgumentException e) {
				throw new InvalidFormatException("Invalid OBO synonym xref URL: " + url);
			}
    		    		
    		synonymObj.setStatus(Status.APPROVED);
    		synonymObj.setApprovedVersion(version);
    	} else if(synonymObj.getStatus() != Status.APPROVED) {
			String msg = "Existing synonym for term (" 
				+ term.getReferenceId() 
				+ ") \"" + synonym
				+ "\" is not APPROVED: " + synonymObj.getStatus();
			
			logger.warning(msg);
		}
    	
    	synonyms.add(synonymObj);
    	return synonymObj;
    }
		
	Synonym addSynonym(String synonym, Synonym.Type type, String datasourceAcronym, String refId) {
		Term term = getCurrentTerm();
		Datasource datasource = getDatasource(datasourceAcronym);
		
		Synonym synonymObj = null;  	    	
    	for(Synonym existing : term.getSynonyms()) {
    		if(existing.getControlledVocabularyTerm() == null
    				&& existing.getUrl() == null
    				&& existing.getSynonym().equals(synonym)
    				&& datasource.equals(existing.getDatasource())) {
    			existing.setType(type);
    			existing.setReferenceId(refId);
    			 
    			synonymObj = existing;
    			break;
    		}
    	}
    	
    	if(synonymObj == null) {
    		synonymObj = new Synonym(term, synonym, type, curator, version);
    		synonymObj.setDatasource(datasource);
    		synonymObj.setReferenceId(refId);
    		synonymObj.setStatus(Status.APPROVED);
    		synonymObj.setApprovedVersion(version);
    	} else if(synonymObj.getStatus() != Status.APPROVED) {
			String msg = "Existing synonym for term (" 
				+ term.getReferenceId() 
				+ ") \"" + synonym
				+ "\" is not APPROVED: " + synonymObj.getStatus();
			
			logger.warning(msg);
		}
    	
    	synonyms.add(synonymObj);
    	return synonymObj;
    }
    
    Relationship addRelationship(String type, String refId, boolean intersection) {
    	Term term = getCurrentTerm();
		Term relatedTerm = getTerm(refId);
		RelationshipType relType = getRelationshipType(type);
		Relationship relationship = null;
		for(Relationship existing : term.getRelationships()) {
			if(existing.getType().equals(relType)
					&& existing.getRelatedTerm().equals(relatedTerm)) {
				existing.setIntersection(intersection);								
				relationship = existing;
				break;
			}
		}
		
		if(relationship == null) {
			relationship = new Relationship(term, relatedTerm, relType, curator, version);
			relationship.setIntersection(intersection);
			relationship.setStatus(Status.APPROVED);
			relationship.setApprovedVersion(version);
		} else if(!relationship.getStatus().equals(Status.APPROVED)) {
			String msg = "Existing relationship " 
				+ term.getReferenceId() 
				+ " " + type + " "
				+ relatedTerm.getReferenceId()
				+ " is not APPROVED: " + relationship.getStatus();
			
			logger.warning(msg);
		}
		    	
    	relationships.add(relationship);
    	return relationship;
    }

    String unescapeTagValue(String value) {
        String unquoted = value;
        if (value.startsWith("\"") && value.endsWith("\"")) {
            unquoted = value.substring(1, value.length() - 1);
        }
        boolean escaping = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < unquoted.length(); i++) {
            char c = unquoted.charAt(i);
            if(escaping) {
            	switch (c) {
				case '\n':
				case '\r':	
				case 'W':
				case 'n':
				case 't':
					sb.append(' ');				
					break;
				case ':':
				case ',':
				case '"':
				case '\\':
				case '(':
				case ')':	
				case '[':
				case ']':
				case '{':
				case '}':
				case '!':
					sb.append(c);
					break;
				default:
					String msg = "Invalid OBO escape sequence: \\" + c;
	        		logger.severe(msg);
	        		throw new InvalidFormatException(msg);
				}
            	escaping = false;
	            continue;
            }            
            if(c == '\\') {
	            escaping = true;
            } else {
            	sb.append(c);
            }
        }
        return sb.toString().trim();
    }
    
    private void setupTagHandlers() {
    	addTagHandler(headerTagHandlers, new FormatVersionTagHandler(this));
    	addTagHandler(headerTagHandlers, new DataVersionTagHandler(this));
    	addTagHandler(headerTagHandlers, new DateTagHandler(this));
    	addTagHandler(headerTagHandlers, new DefaultNamespaceTagHandler(this));
    	addTagHandler(headerTagHandlers, new RemarkTagHandler(this));
    	
    	addTagHandler(termTagHandlers, new NameTagHandler(this));
    	addTagHandler(termTagHandlers, new DefTagHandler(this));
    	addTagHandler(termTagHandlers, new CommentTagHandler(this));
    	addTagHandler(termTagHandlers, new SynonymTagHandler(this));
    	addTagHandler(termTagHandlers, new XrefTagHandler(this));
    	addTagHandler(termTagHandlers, new BuiltInRelationshipTagHandler(OBOVocabulary.IS_A, this));
    	addTagHandler(termTagHandlers, new IntersectionOfTagHandler(this));
    	addTagHandler(termTagHandlers, new BuiltInRelationshipTagHandler(OBOVocabulary.UNION_OF, this));
    	addTagHandler(termTagHandlers, new BuiltInRelationshipTagHandler(OBOVocabulary.DISJOINT_FROM, this));
    	addTagHandler(termTagHandlers, new RelationshipTagHandler(this));
    	addTagHandler(termTagHandlers, new IsObsoleteTagHandler(this));
    	addTagHandler(termTagHandlers, new ReplacedByTagHandler(this));
    	
    	addTagHandler(typeDefTagHandlers, new DefTagHandler(this));
    	addTagHandler(typeDefTagHandlers, new IsCyclicTagHandler(this));
    	addTagHandler(typeDefTagHandlers, new IsSymmetricTagHandler(this));
    	addTagHandler(typeDefTagHandlers, new IsTransitiveTagHandler(this));
    	addTagHandler(typeDefTagHandlers, new InverseOfTagHandler(this));
    	addTagHandler(typeDefTagHandlers, new TransitiveOverTagHandler(this));
    	addTagHandler(typeDefTagHandlers, new IsObsoleteTagHandler(this));
    	addTagHandler(typeDefTagHandlers, new ReplacedByTagHandler(this));
    	
    	unsupportedTags.add(OBOVocabulary.ID.getName());
    }
    
    private void addTagHandler(Map<String,OBOTagHandler> map, OBOTagHandler handler) {
    	map.put(handler.getTagName().getName(), handler);
    }
}
