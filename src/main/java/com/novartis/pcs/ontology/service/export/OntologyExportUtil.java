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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.semanticweb.owlapi.model.IRI;

import com.novartis.pcs.ontology.entity.RelationshipType;

class OntologyExportUtil {
	private static final URI oboURI = URI.create("http://purl.obolibrary.org/obo");
	private static final Map<String, IRI> relationshipIRIs = new HashMap<String, IRI>();
	
	static {
		relationshipIRIs.put("part_of",createIRI(oboURI, "BFO_0000050"));
		relationshipIRIs.put("has_part",createIRI(oboURI, "BFO_0000051"));
		relationshipIRIs.put("realized_in",createIRI(oboURI, "BFO_0000054"));
		relationshipIRIs.put("realizes",createIRI(oboURI, "BFO_0000055"));
		relationshipIRIs.put("obsolete_preceded_by",createIRI(oboURI, "BFO_0000060"));
		relationshipIRIs.put("preceded_by",createIRI(oboURI, "BFO_0000062"));
		relationshipIRIs.put("precedes",createIRI(oboURI, "BFO_0000063"));
		relationshipIRIs.put("occurs_in",createIRI(oboURI, "BFO_0000066"));
		relationshipIRIs.put("contains_process",createIRI(oboURI, "BFO_0000067"));
		relationshipIRIs.put("inheres_in",createIRI(oboURI, "RO_0000052"));
		relationshipIRIs.put("bearer_of",createIRI(oboURI, "RO_0000053"));
		relationshipIRIs.put("participates_in",createIRI(oboURI, "RO_0000056"));
		relationshipIRIs.put("has_participant",createIRI(oboURI, "RO_0000057"));
		relationshipIRIs.put("in_neural_circuit_with",createIRI(oboURI, "RO_0000300"));
		relationshipIRIs.put("upstream_in_neural_circuit_with",createIRI(oboURI, "RO_0000301"));
		relationshipIRIs.put("downstream_in_neural_circuit_with",createIRI(oboURI, "RO_0000302"));
		relationshipIRIs.put("derives_from",createIRI(oboURI, "RO_0001000"));
		relationshipIRIs.put("derives_into",createIRI(oboURI, "RO_0001001"));
		relationshipIRIs.put("location_of",createIRI(oboURI, "RO_0001015"));
		relationshipIRIs.put("contained_in",createIRI(oboURI, "RO_0001018"));
		relationshipIRIs.put("contains",createIRI(oboURI, "RO_0001019"));
		relationshipIRIs.put("located_in",createIRI(oboURI, "RO_0001025"));
		relationshipIRIs.put("boundary_of",createIRI(oboURI, "RO_0002000"));
		relationshipIRIs.put("aligned_with",createIRI(oboURI, "RO_0002001"));
		relationshipIRIs.put("has_boundary",createIRI(oboURI, "RO_0002002"));
		relationshipIRIs.put("before_or_simultaneous_with",createIRI(oboURI, "RO_0002081"));
		relationshipIRIs.put("simultaneous_with",createIRI(oboURI, "RO_0002082"));
		relationshipIRIs.put("before",createIRI(oboURI, "RO_0002083"));
		relationshipIRIs.put("during_which_ends",createIRI(oboURI, "RO_0002084"));
		relationshipIRIs.put("encompasses",createIRI(oboURI, "RO_0002085"));
		relationshipIRIs.put("ends_after",createIRI(oboURI, "RO_0002086"));
		relationshipIRIs.put("immediately_preceded_by",createIRI(oboURI, "RO_0002087"));
		relationshipIRIs.put("during_which_starts",createIRI(oboURI, "RO_0002088"));
		relationshipIRIs.put("starts_before",createIRI(oboURI, "RO_0002089"));
		relationshipIRIs.put("immediately_precedes",createIRI(oboURI, "RO_0002090"));
		relationshipIRIs.put("starts_during",createIRI(oboURI, "RO_0002091"));
		relationshipIRIs.put("happens_during",createIRI(oboURI, "RO_0002092"));
		relationshipIRIs.put("ends_during",createIRI(oboURI, "RO_0002093"));
		relationshipIRIs.put("has_soma_location",createIRI(oboURI, "RO_0002100"));
		relationshipIRIs.put("fasciculates_with",createIRI(oboURI, "RO_0002101"));
		relationshipIRIs.put("axon_synapses_in",createIRI(oboURI, "RO_0002102"));
		relationshipIRIs.put("synapsed_by",createIRI(oboURI, "RO_0002103"));
		relationshipIRIs.put("has_plasma_membrane_part",createIRI(oboURI, "RO_0002104"));
		relationshipIRIs.put("synapsed_via_type_Ib_bouton_to",createIRI(oboURI, "RO_0002105"));
		relationshipIRIs.put("synapsed_via_type_Is_bouton_to",createIRI(oboURI, "RO_0002106"));
		relationshipIRIs.put("synapsed_via_type_II_bouton_to",createIRI(oboURI, "RO_0002107"));
		relationshipIRIs.put("synapsed_by_via_type_II_bouton",createIRI(oboURI, "RO_0002108"));
		relationshipIRIs.put("synapsed_by_via_type_Ib_bouton",createIRI(oboURI, "RO_0002109"));
		relationshipIRIs.put("has_postsynaptic_terminal_in",createIRI(oboURI, "RO_0002110"));
		relationshipIRIs.put("releases_neurotransmitter",createIRI(oboURI, "RO_0002111"));
		relationshipIRIs.put("synapsed_by_via_type_Is_bouton",createIRI(oboURI, "RO_0002112"));
		relationshipIRIs.put("has_presynaptic_terminal_in",createIRI(oboURI, "RO_0002113"));
		relationshipIRIs.put("synapsed_via_type_III_bouton_to",createIRI(oboURI, "RO_0002114"));
		relationshipIRIs.put("synapsed_by_via_type_III_bouton",createIRI(oboURI, "RO_0002115"));
		relationshipIRIs.put("synapsed_to",createIRI(oboURI, "RO_0002120"));
		relationshipIRIs.put("dendrite_synapsed_in",createIRI(oboURI, "RO_0002121"));
		relationshipIRIs.put("has_synaptic_terminal_in",createIRI(oboURI, "RO_0002130"));
		relationshipIRIs.put("overlaps",createIRI(oboURI, "RO_0002131"));
		relationshipIRIs.put("has_fasciculating_neuron_projection",createIRI(oboURI, "RO_0002132"));
		relationshipIRIs.put("innervates",createIRI(oboURI, "RO_0002134"));
		relationshipIRIs.put("connected_to",createIRI(oboURI, "RO_0002150"));
		relationshipIRIs.put("proper_overlaps",createIRI(oboURI, "RO_0002151"));
		relationshipIRIs.put("derived_by_descent_from",createIRI(oboURI, "RO_0002156"));
		relationshipIRIs.put("has_derived_by_descendant",createIRI(oboURI, "RO_0002157"));
		relationshipIRIs.put("shares_ancestor_with",createIRI(oboURI, "RO_0002158"));
		relationshipIRIs.put("serially_homologous_to",createIRI(oboURI, "RO_0002159"));
		relationshipIRIs.put("only_in_taxon",createIRI(oboURI, "RO_0002160"));
		relationshipIRIs.put("in_taxon",createIRI(oboURI, "RO_0002162"));
		relationshipIRIs.put("spatially_disjoint_from",createIRI(oboURI, "RO_0002163"));
		relationshipIRIs.put("has_component",createIRI(oboURI, "RO_0002180"));
		relationshipIRIs.put("has_phenotype",createIRI(oboURI, "RO_0002200"));
		relationshipIRIs.put("phenotype_of",createIRI(oboURI, "RO_0002201"));
		relationshipIRIs.put("develops_from",createIRI(oboURI, "RO_0002202"));
		relationshipIRIs.put("develops_into",createIRI(oboURI, "RO_0002203"));
		relationshipIRIs.put("gene_product_of",createIRI(oboURI, "RO_0002204"));
		relationshipIRIs.put("has_gene_product",createIRI(oboURI, "RO_0002205"));
		relationshipIRIs.put("expressed_in",createIRI(oboURI, "RO_0002206"));
		relationshipIRIs.put("directly_develops_from",createIRI(oboURI, "RO_0002207"));
		relationshipIRIs.put("directly_develops_into",createIRI(oboURI, "RO_0002210"));
		relationshipIRIs.put("regulates",createIRI(oboURI, "RO_0002211"));
		relationshipIRIs.put("negatively_regulates",createIRI(oboURI, "RO_0002212"));
		relationshipIRIs.put("positively_regulates",createIRI(oboURI, "RO_0002213"));
		relationshipIRIs.put("has_prototype",createIRI(oboURI, "RO_0002214"));
		relationshipIRIs.put("capable_of",createIRI(oboURI, "RO_0002215"));
		relationshipIRIs.put("has_function_in",createIRI(oboURI, "RO_0002216"));
		relationshipIRIs.put("actively_participates_in",createIRI(oboURI, "RO_0002217"));
		relationshipIRIs.put("has_active_participant",createIRI(oboURI, "RO_0002218"));
		relationshipIRIs.put("surrounded_by",createIRI(oboURI, "RO_0002219"));
		relationshipIRIs.put("adjacent_to",createIRI(oboURI, "RO_0002220"));
		relationshipIRIs.put("surrounds",createIRI(oboURI, "RO_0002221"));
		relationshipIRIs.put("temporally_related_to",createIRI(oboURI, "RO_0002222"));
		relationshipIRIs.put("starts",createIRI(oboURI, "RO_0002223"));
		relationshipIRIs.put("starts_with",createIRI(oboURI, "RO_0002224"));
		relationshipIRIs.put("develops_from_part_of",createIRI(oboURI, "RO_0002225"));
		relationshipIRIs.put("develops_in",createIRI(oboURI, "RO_0002226"));
		relationshipIRIs.put("ends",createIRI(oboURI, "RO_0002229"));
		relationshipIRIs.put("ends_with",createIRI(oboURI, "RO_0002230"));
		relationshipIRIs.put("has_start_location",createIRI(oboURI, "RO_0002231"));
		relationshipIRIs.put("has_end_location",createIRI(oboURI, "RO_0002232"));
		relationshipIRIs.put("has_input",createIRI(oboURI, "RO_0002233"));
		relationshipIRIs.put("has_output",createIRI(oboURI, "RO_0002234"));
		relationshipIRIs.put("has_developmental_contribution_from",createIRI(oboURI, "RO_0002254"));
		relationshipIRIs.put("developmentally_contributes_to",createIRI(oboURI, "RO_0002255"));
		relationshipIRIs.put("developmentally_induced_by",createIRI(oboURI, "RO_0002256"));
		relationshipIRIs.put("developmentally_induces",createIRI(oboURI, "RO_0002257"));
		relationshipIRIs.put("developmentally_preceded_by",createIRI(oboURI, "RO_0002258"));
		relationshipIRIs.put("developmentally_replaces",createIRI(oboURI, "RO_0002285"));
		relationshipIRIs.put("developmentally_succeeded_by",createIRI(oboURI, "RO_0002286"));
		relationshipIRIs.put("part_of_developmental_precursor_of",createIRI(oboURI, "RO_0002287"));
		relationshipIRIs.put("ubiquitously_expressed_in",createIRI(oboURI, "RO_0002291"));
		relationshipIRIs.put("expresses",createIRI(oboURI, "RO_0002292"));
		relationshipIRIs.put("ubiquitously_expresses",createIRI(oboURI, "RO_0002293"));
		relationshipIRIs.put("has_habitat",createIRI(oboURI, "RO_0002303"));
		relationshipIRIs.put("variant_of",createIRI(oboURI, "RO_0002312"));
		relationshipIRIs.put("transports_or_maintains_localization_of",createIRI(oboURI, "RO_0002313"));
		relationshipIRIs.put("inheres_in_part_of",createIRI(oboURI, "RO_0002314"));
		relationshipIRIs.put("evolutionarily_related_to",createIRI(oboURI, "RO_0002320"));
		relationshipIRIs.put("ecologically_related_to",createIRI(oboURI, "RO_0002321"));
		relationshipIRIs.put("confers_advantage_in",createIRI(oboURI, "RO_0002322"));
		relationshipIRIs.put("mereotopologically_related_to",createIRI(oboURI, "RO_0002323"));
		relationshipIRIs.put("developmentally_related_to",createIRI(oboURI, "RO_0002324"));
		relationshipIRIs.put("colocalizes_with",createIRI(oboURI, "RO_0002325"));
		relationshipIRIs.put("contributes_to",createIRI(oboURI, "RO_0002326"));
		relationshipIRIs.put("enables",createIRI(oboURI, "RO_0002327"));
		relationshipIRIs.put("functionally_related_to",createIRI(oboURI, "RO_0002328"));
		relationshipIRIs.put("part_of_structure_that_is_capable_of",createIRI(oboURI, "RO_0002329"));
		relationshipIRIs.put("genomically_related_to",createIRI(oboURI, "RO_0002330"));
		relationshipIRIs.put("involved_in",createIRI(oboURI, "RO_0002331"));
		relationshipIRIs.put("regulates_levels_of",createIRI(oboURI, "RO_0002332"));
		relationshipIRIs.put("enabled_by",createIRI(oboURI, "RO_0002333"));
		relationshipIRIs.put("regulated_by",createIRI(oboURI, "RO_0002334"));
		relationshipIRIs.put("negatively_regulated_by",createIRI(oboURI, "RO_0002335"));
		relationshipIRIs.put("positively_regulated_by",createIRI(oboURI, "RO_0002336"));
		relationshipIRIs.put("related_via_localization_to",createIRI(oboURI, "RO_0002337"));
		relationshipIRIs.put("has_target_start_location",createIRI(oboURI, "RO_0002338"));
		relationshipIRIs.put("has_target_end_location",createIRI(oboURI, "RO_0002339"));
		relationshipIRIs.put("imports",createIRI(oboURI, "RO_0002340"));
		relationshipIRIs.put("results_in_transport_along",createIRI(oboURI, "RO_0002341"));
		relationshipIRIs.put("results_in_transport_across",createIRI(oboURI, "RO_0002342"));
		relationshipIRIs.put("results_in_growth_of",createIRI(oboURI, "RO_0002343"));
		relationshipIRIs.put("results_in_transport_to_from_or_in",createIRI(oboURI, "RO_0002344"));
		relationshipIRIs.put("exports",createIRI(oboURI, "RO_0002345"));
		relationshipIRIs.put("results_in_commitment_to",createIRI(oboURI, "RO_0002348"));
		relationshipIRIs.put("results_in_determination_of",createIRI(oboURI, "RO_0002349"));
		relationshipIRIs.put("member_of",createIRI(oboURI, "RO_0002350"));
		relationshipIRIs.put("has_member",createIRI(oboURI, "RO_0002351"));
		relationshipIRIs.put("input_of",createIRI(oboURI, "RO_0002352"));
		relationshipIRIs.put("output_of",createIRI(oboURI, "RO_0002353"));
		relationshipIRIs.put("formed_as_result_of",createIRI(oboURI, "RO_0002354"));
		relationshipIRIs.put("results_in_structural_organization_of",createIRI(oboURI, "RO_0002355"));
		relationshipIRIs.put("results_in_specification_of",createIRI(oboURI, "RO_0002356"));
		relationshipIRIs.put("results_in_developmental_induction_of",createIRI(oboURI, "RO_0002357"));
		relationshipIRIs.put("attached_to",createIRI(oboURI, "RO_0002371"));
		relationshipIRIs.put("has_muscle_origin",createIRI(oboURI, "RO_0002372"));
		relationshipIRIs.put("has_muscle_insertion",createIRI(oboURI, "RO_0002373"));
		relationshipIRIs.put("has_fused_element",createIRI(oboURI, "RO_0002374"));
		relationshipIRIs.put("produces",createIRI(oboURI, "RO_0003000"));
		relationshipIRIs.put("produced_by",createIRI(oboURI, "RO_0003001"));
	}
	
	static String escapeOBO(String s) {
		return escapeOBO(s, true);
	}
	
	static String escapeOBO(String s, boolean strict) {
		StringBuilder buffer = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
			case '\f':
			case '\n':
			case '\r':
				buffer.append("\\n");
				break;
			case '\t':	
				buffer.append("\\t");
				break;
			case '\\':
			case '!':
				buffer.append("\\");
			default:
				if(strict) {
					switch (c) {
					case ':':
					case ',':
					case '"':
					// Although \( and \) are both valid escape sequences,
					// the OBOEdit2 parsers complain with: Unrecognized escape character
					//case '(':
					//case ')':	
					case '[':
					case ']':
					case '{':
					case '}':
						buffer.append("\\");
					}
				}
				buffer.append(c);
			}
		}

		return buffer.toString();
	}
	
	static String escapeQuoted(String s) {
		StringBuilder buffer = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
			case '\f':
			case '\n':
			case '\r':
				buffer.append("\\n");
				break;
			case '\t':	
				buffer.append("\\t");
				break;
			case '"':
			case '\\':
			case '!':
				buffer.append("\\");
			default:
				buffer.append(c);
			}
		}

		return buffer.toString();
	}
	
	static IRI createIRI(URI baseURI, String path) {
		return createIRI(baseURI, path, null);
	}
	
	static IRI createIRI(URI baseURI, String path, String fragment) {
		// Use URI class to escape/encode the path and fragment
		// because the OWLAPI IRI class does not escape/encode
		try {
			String ssp = baseURI.getSchemeSpecificPart();
			ssp += ssp.endsWith("/") ? path : "/" + path;						
			URI uri = new URI(baseURI.getScheme(), ssp, fragment);
			return IRI.create(uri);
		} catch(Exception e) {
			throw new IllegalArgumentException("Illegal URI syntax in path and/or fragment: " 
					+ path + "#" + fragment);
		}
	}
	
	static IRI getRelationshipIRI(String relationship) {
		IRI iri = relationshipIRIs.get(relationship);
		if(iri == null) {
			throw new IllegalArgumentException("Invalid/Unsupported OBO relationship: " + relationship);
		}
		return iri;
	}
}
