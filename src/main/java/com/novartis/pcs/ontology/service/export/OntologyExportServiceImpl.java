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
package com.novartis.pcs.ontology.service.export;

import static com.novartis.pcs.ontology.service.export.OntologyExportUtil.createIRI;
import static com.novartis.pcs.ontology.service.export.OntologyExportUtil.escapeOBO;
import static com.novartis.pcs.ontology.service.export.OntologyExportUtil.escapeQuoted;
import static com.novartis.pcs.ontology.service.export.OntologyExportUtil.getRelationshipIRI;
import static com.novartis.pcs.ontology.service.export.OntologyExportUtil.isBuiltIn;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat;
import org.coode.owlapi.turtle.TurtleOntologyFormat;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.novartis.pcs.ontology.dao.DatasourceDAOLocal;
import com.novartis.pcs.ontology.dao.OntologyDAOLocal;
import com.novartis.pcs.ontology.dao.TermDAOLocal;
import com.novartis.pcs.ontology.entity.ControlledVocabularyTerm;
import com.novartis.pcs.ontology.entity.CrossReference;
import com.novartis.pcs.ontology.entity.Datasource;
import com.novartis.pcs.ontology.entity.Ontology;
import com.novartis.pcs.ontology.entity.Relationship;
import com.novartis.pcs.ontology.entity.RelationshipType;
import com.novartis.pcs.ontology.entity.Synonym;
import com.novartis.pcs.ontology.entity.Term;
import com.novartis.pcs.ontology.entity.VersionedEntity.Status;
import com.novartis.pcs.ontology.service.util.TermReferenceIdComparator;

/**
 * Session Bean implementation class OntologyExportServiceImpl
 */
@Stateless
@Local(OntologyExportServiceLocal.class)
@Remote(OntologyExportServiceRemote.class)
public class OntologyExportServiceImpl implements OntologyExportServiceRemote, OntologyExportServiceLocal {
	
	@EJB
	protected OntologyDAOLocal ontologyDAO;
	
	@EJB
	protected TermDAOLocal termDAO;
	
	@EJB
	protected DatasourceDAOLocal datasourceDAO;
	
	@Resource(lookup="java:global/ontobrowser/export/owl/uri")
	private URL baseURL;
	
	private Logger logger = Logger.getLogger(getClass().getName());
	
    /**
     * Default constructor. 
     */
    public OntologyExportServiceImpl() {
    }
    
    @Override
	public void exportOntology(String ontologyName, OutputStream os, 
			OntologyFormat format) throws OntologyNotFoundException {    	
    	exportOntology(ontologyName, os, format, false);
    }
    
    @Override
	public void exportOntology(String ontologyName, OutputStream os, 
			OntologyFormat format, boolean includeNonPublicXrefs) 
    		throws OntologyNotFoundException {
    	Collection<Datasource> datasources = datasourceDAO.loadAll();
    	if(!includeNonPublicXrefs) {
    	  	Collection<Datasource> external = new ArrayList<Datasource>();
	    	for(Datasource datasource : datasources) {
	    		if(datasource.isPubliclyAccessible()) {
	    			external.add(datasource);
	    		}
	    	}
	    	datasources = external;
    	}
    	
    	exportOntology(ontologyName, os, format, datasources);
    }
    
	@Override
	public void exportOntology(String ontologyName, OutputStream os,
			OntologyFormat format, Collection<Datasource> xrefDatasources) 
			throws OntologyNotFoundException {
		
		Ontology ontology = ontologyDAO.loadByName(ontologyName);
		if(ontology == null || ontology.isCodelist()) {
			throw new OntologyNotFoundException("Ontology not found: " + ontologyName, ontologyName);
		}
		
		logger.info("Exporting " + ontology.getName() + " ontology in " + format + " format");
						
		switch(format) {
		case OBO:
			exportAsOBO(ontology, os, xrefDatasources);
			break;
		case RDFXML:
			exportAsOWL(ontology, os, new RDFXMLOntologyFormat(), xrefDatasources);
			break;
		case OWLXML:
			exportAsOWL(ontology, os, new OWLXMLOntologyFormat(), xrefDatasources);
			break;
		case Manchester:
			exportAsOWL(ontology, os, new ManchesterOWLSyntaxOntologyFormat(), xrefDatasources);
			break;
		case Turtle:
			exportAsOWL(ontology, os, new TurtleOntologyFormat(), xrefDatasources);
			break;	
		default:
			throw new IllegalArgumentException("Invalid/Unsupported ontology export format: " + format);
		}
	}

	private void exportAsOBO(Ontology ontology, OutputStream os, 
			Collection<Datasource> xrefDatasources) {
		Collection<Term> terms = termDAO.loadAll(ontology);
		Date now = new Date();
		DateFormat formatter = new SimpleDateFormat("dd:MM:yyyy HH:mm");
		
		if(!(terms instanceof List<?>)) {
			terms = new ArrayList<Term>(terms);
		}
		
		Collections.sort((List<Term>)terms, new TermReferenceIdComparator());
		
		try {
			Writer writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"), 4096);
			
			writer.append("format-version: 1.2\n")
					.append("date: ").append(formatter.format(now)).append("\n")
					.append("auto-generated-by: OntoBrowser Export Service\n")
					.append("\n");
			
			Set<RelationshipType> relationshipTypes = new HashSet<RelationshipType>();
			for(Term term : terms) {
				if(term.getStatus().equals(Status.APPROVED) 
						|| term.getStatus().equals(Status.OBSOLETE)) {
					writer.append("[Term]\n")
						.append("id: ").append(term.getReferenceId()).append("\n")
						.append("name: ").append(escapeOBO(term.getName(), false)).append("\n");
					if(term.getDefinition() != null) {
						writer.append("def: \"").append(escapeQuoted(term.getDefinition())).append("\" [");
						if(term.getUrl() != null) {
							writer.append(term.getUrl());
						}
						for(CrossReference xref : term.getCrossReferences()) {
							if(xref.isDefinitionCrossReference()) {
								if(term.getUrl() != null) {
									writer.append(", ");
								}
								if(xref.getDatasource() != null 
										&& xrefDatasources.contains(xref.getDatasource())) {
									writer.append(escapeOBO(xref.getDatasource().getAcronym()));
									if(xref.getReferenceId() != null) {
										writer.append(":").append(escapeOBO(xref.getReferenceId()));
									}
									
									if(xref.getDescription() != null) {
										writer.append(" \"").append(escapeQuoted(xref.getDescription())).append("\"");
									}
								} else if(xref.getUrl() != null) {
									writer.append(xref.getUrl());
								}
							}
						}
						
						writer.append("]\n");
					}
					
					if(term.getComments() != null) {
						writer.append("comment: ").append(escapeOBO(term.getComments(), false)).append("\n");
					}
					
					for(Synonym synonym : term.getSynonyms()) {
						if(synonym.getStatus().equals(Status.APPROVED)) {
							Datasource datasource = null;
							String referenceId = null;
							String description = null;					
							if(synonym.getControlledVocabularyTerm() != null) {
								ControlledVocabularyTerm ctrldVocabTerm = synonym.getControlledVocabularyTerm();
								datasource = ctrldVocabTerm.getControlledVocabulary().getDatasource();
								referenceId = ctrldVocabTerm.getReferenceId();
								description = synonym.getDescription();
							} else if(synonym.getDatasource() != null) {
								datasource = synonym.getDatasource();
								referenceId = synonym.getReferenceId();
								description = synonym.getDescription();
							}
							
							if(datasource == null || xrefDatasources.contains(datasource)) {
								writer.append("synonym: \"")
									.append(escapeQuoted(synonym.getSynonym())).append("\"")
									.append(" ").append(synonym.getType().name()).append(" [");
								if(datasource != null) {
									writer.append(escapeOBO(datasource.getAcronym()));
									if(referenceId != null) {
										writer.append(":").append(escapeOBO(referenceId));
									}
									
									if(description != null) {
										writer.append(" \"").append(escapeQuoted(description)).append("\"");
									}
								} else if(synonym.getUrl() != null) {
									writer.append(synonym.getUrl());
								}
								writer.append("]\n");
							}
						}
					}
					
					for(CrossReference xref : term.getCrossReferences()) {
						if(!xref.isDefinitionCrossReference() &&
								(xref.getDatasource() == null || xrefDatasources.contains(xref.getDatasource()))) {
							writer.append("xref: ");
							if(xref.getDatasource() != null) {
								writer.append(escapeOBO(xref.getDatasource().getAcronym()));
								if(xref.getReferenceId() != null) {
									writer.append(":").append(escapeOBO(xref.getReferenceId()));
								}
								
								if(xref.getDescription() != null) {
									writer.append(" \"").append(escapeQuoted(xref.getDescription())).append("\"");
								}
							} else if(xref.getUrl() != null) {
								writer.append(xref.getUrl());
							}
							writer.append("\n");
						}
					}
					
					List<Relationship> relationships = new ArrayList<Relationship>(term.getRelationships());
					Collections.sort(relationships, new RelationshipComparator());				
					for(Relationship relationship : relationships) {
						if(relationship.getStatus().equals(Status.APPROVED)) {
							RelationshipType type = relationship.getType();
							relationshipTypes.add(type);
							
							if(relationship.isIntersection()) {
								writer.append("intersection_of: ");
								if(!type.getRelationship().equals("is_a")) {
									writer.append(escapeOBO(type.getRelationship())).append(" ");
								}
							} else if(isBuiltIn(type)) {
								writer.append(type.getRelationship()).append(": ");
							} else {
								writer.append("relationship: ").append(escapeOBO(type.getRelationship())).append(" ");
							}
																	
							writer.append(relationship.getRelatedTerm().getReferenceId())
									.append(" ! ").append(relationship.getRelatedTerm().getName())
									.append("\n");
						}
					}
					
					if(term.getStatus().equals(Status.OBSOLETE)) {
						writer.append("is_obsolete: true\n");
						if(term.getReplacedBy() != null) {
							writer.append("replaced_by: ")
									.append(term.getReplacedBy().getReferenceId())
									.append(" ! ").append(term.getReplacedBy().getName())
									.append("\n");
						}
					}
					
					writer.append("\n");
				}
			}
		
			for(RelationshipType type : relationshipTypes) {
				if(!isBuiltIn(type) && type.getStatus().equals(Status.APPROVED)) {
					RelationshipType inverse = type.getInverseOf();
					RelationshipType transitive = type.getTransitiveOver();
					
					writer.append("[Typedef]\n")
						.append("id: ").append(escapeOBO(type.getRelationship())).append("\n")
						.append("name: ").append(escapeOBO(type.getRelationship().replace('_', ' '))).append("\n");
					
					if(inverse != null) {
						writer.append("inverse_of: ").append(escapeOBO(inverse.getRelationship())).append("\n");
					}
					
					if(transitive != null) {
						writer.append("transitive_over: ").append(escapeOBO(transitive.getRelationship())).append("\n");
					}
					
					if(type.isCyclic()) {
						writer.append("is_cyclic: true\n");
					}
													
					if(type.isSymmetric()) {
						writer.append("is_symmetric: true\n");
					}
													
					if(type.isTransitive()) {
						writer.append("is_transitive: true\n");
					}
													
					writer.append("\n");
				}
			}
			writer.flush();
		} catch(IOException e) {
			logger.log(Level.WARNING, "Failed to export " + ontology.getName() + " in OBO Format" , e);
			throw new RuntimeException(e);
		}
	}
	
	private void exportAsOWL(Ontology ontology, OutputStream os, OWLOntologyFormat format,
			Collection<Datasource> xrefDatasources) {
		try {
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			OWLDataFactory factory = manager.getOWLDataFactory();
			IRI ontologyIRI = createIRI(baseURL.toURI(), ontology.getName());
	        OWLOntology onto = manager.createOntology(ontologyIRI);
	        
	        OWLAnnotation ontologyLabel = factory.getOWLAnnotation(factory.getRDFSLabel(), 
	        		factory.getOWLLiteral(ontology.getName()));
			manager.applyChange(new AddOntologyAnnotation(onto, ontologyLabel));
			
			if(ontology.getDescription() != null) {
				OWLAnnotation comment = factory.getOWLAnnotation(factory.getRDFSComment(), 
		        		factory.getOWLLiteral(ontology.getDescription()));
				manager.applyChange(new AddOntologyAnnotation(onto, comment));
			}
	        
			Collection<Term> terms = termDAO.loadAll(ontology);
	        Set<RelationshipType> relationshipTypes = new HashSet<RelationshipType>();
			for(Term term : terms) {
				if(term.getStatus().equals(Status.APPROVED) 
						|| term.getStatus().equals(Status.OBSOLETE)) {
					IRI termIRI = createIRI(baseURL.toURI(), ontology.getName(), term.getReferenceId());
					OWLClass termClass = factory.getOWLClass(termIRI);
					
					manager.addAxiom(onto, factory.getOWLDeclarationAxiom(termClass));
					
					manager.addAxiom(onto, factory.getOWLAnnotationAssertionAxiom(factory.getRDFSLabel(),
							termIRI, factory.getOWLLiteral(term.getName())));				
					
					if(term.getComments() != null) {
						OWLLiteral comment = factory.getOWLLiteral(term.getComments());
						OWLAxiom axiom = factory.getOWLAnnotationAssertionAxiom(factory.getRDFSComment(),
								termIRI, comment);
						manager.addAxiom(onto, axiom);
					}
					
					Set<OWLClassExpression> intersectClasses = new HashSet<OWLClassExpression>();
					Set<OWLClassExpression> unionClasses = new HashSet<OWLClassExpression>();
					for(Relationship relationship : term.getRelationships()) {
						if(relationship.getStatus().equals(Status.APPROVED)) {
							RelationshipType type = relationship.getType();
							Term relatedTerm = relationship.getRelatedTerm();
							IRI relatedTermIRI = createIRI(baseURL.toURI(), ontology.getName(), relatedTerm.getReferenceId());
							OWLClass relatedTermClass = factory.getOWLClass(relatedTermIRI);
							
							relationshipTypes.add(type);
							
							if(relationship.isIntersection()) {
								OWLClassExpression intersect = relatedTermClass;
								if(!type.getRelationship().equals("is_a")) {
									IRI relationshipIRI = getRelationshipIRI(type.getRelationship());
									OWLObjectProperty objectProp = factory.getOWLObjectProperty(relationshipIRI);
									OWLObjectSomeValuesFrom someValuesFrom = factory.getOWLObjectSomeValuesFrom(objectProp, relatedTermClass);
									intersect = someValuesFrom;
								}
								intersectClasses.add(intersect);
							} else if(type.getRelationship().equals("is_a")) {
								OWLAxiom axiom = factory.getOWLSubClassOfAxiom(termClass, relatedTermClass);
								manager.addAxiom(onto, axiom);
							} else if(type.getRelationship().equals("union_of")) {			
								unionClasses.add(relatedTermClass);
							} else if(type.getRelationship().equals("disjoint_from")) {
								OWLAxiom axiom = factory.getOWLDisjointClassesAxiom(termClass, relatedTermClass);
								manager.addAxiom(onto, axiom);
							} else {
								IRI relationshipIRI = getRelationshipIRI(type.getRelationship());
								OWLObjectProperty objectProp = factory.getOWLObjectProperty(relationshipIRI);
								OWLObjectSomeValuesFrom someValuesFrom = factory.getOWLObjectSomeValuesFrom(objectProp, relatedTermClass);
								OWLAxiom axiom = factory.getOWLSubClassOfAxiom(termClass, someValuesFrom);
								manager.addAxiom(onto, axiom);
							}
						}
					}
					
					if(!intersectClasses.isEmpty()) {
						OWLObjectIntersectionOf intersection = factory.getOWLObjectIntersectionOf(intersectClasses);		        
				        OWLAxiom axiom = factory.getOWLEquivalentClassesAxiom(termClass, intersection);			        
				        manager.addAxiom(onto, axiom);
					}
					
					if(!unionClasses.isEmpty()) {
						OWLObjectUnionOf unionOf = factory.getOWLObjectUnionOf(unionClasses);
						OWLAxiom axiom = factory.getOWLEquivalentClassesAxiom(termClass, unionOf);
						manager.addAxiom(onto, axiom);
					}
					
					if(term.getStatus().equals(Status.OBSOLETE)) {
						OWLAxiom axiom = factory.getOWLAnnotationAssertionAxiom(factory.getOWLDeprecated(),
								termIRI, factory.getOWLLiteral(true));
						manager.addAxiom(onto, axiom);
					}
				}
			}
		
			for(RelationshipType type : relationshipTypes) {
				if(!isBuiltIn(type) && type.getStatus().equals(Status.APPROVED)) {					
					IRI relationshipIRI = getRelationshipIRI(type.getRelationship());
					OWLObjectProperty objectProp = factory.getOWLObjectProperty(relationshipIRI);
					RelationshipType inverse = type.getInverseOf();
					RelationshipType transitiveOver = type.getTransitiveOver();			
										
					manager.addAxiom(onto, factory.getOWLDeclarationAxiom(objectProp));
					manager.addAxiom(onto, factory.getOWLAnnotationAssertionAxiom(factory.getRDFSLabel(),
							relationshipIRI, factory.getOWLLiteral(type.getRelationship().replace('_', ' '))));
					
					if(inverse != null) {
						IRI inverseIRI = getRelationshipIRI(inverse.getRelationship());
						OWLObjectProperty inverseObjectProperty = factory.getOWLObjectProperty(inverseIRI);
						manager.addAxiom(onto, 
									factory.getOWLInverseObjectPropertiesAxiom(objectProp, inverseObjectProperty));
					}
					
					if(transitiveOver != null) {
						IRI transitiveOverIRI = getRelationshipIRI(transitiveOver.getRelationship());
						OWLObjectProperty transitiveOverObjectProperty = factory.getOWLObjectProperty(transitiveOverIRI);
						List<OWLObjectProperty> chain = Arrays.asList(objectProp, transitiveOverObjectProperty);
						OWLAxiom axiom = factory.getOWLSubPropertyChainOfAxiom(chain, objectProp);						
						manager.addAxiom(onto, axiom);
					}
					
					if(type.isCyclic()) {
						logger.warning("Cyclic relationships are not supported by OWL: " + type.getRelationship());
					}

					/*
					if(type.isReflexive()) {
						manager.addAxiom(ont, factory.getOWLReflexiveObjectPropertyAxiom(objectProp));
					}
					*/
					
					if(type.isSymmetric()) {
						manager.addAxiom(onto, factory.getOWLSymmetricObjectPropertyAxiom(objectProp));
					}
					
					/*
					if(type.isAntiSymmetric()) {
						manager.addAxiom(ont, factory.getOWLASymmetricObjectPropertyAxiom(objectProp));
					}
					*/
					
					if(type.isTransitive()) {
						manager.addAxiom(onto, factory.getOWLTransitiveObjectPropertyAxiom(objectProp));
					}
				}
			}
				        
	        manager.saveOntology(onto, format, os);
		} catch(Exception e) {
			logger.log(Level.WARNING, "Failed to export " + ontology.getName() + " in OWL format" , e);
			throw new RuntimeException(e);
		}
	}
}
