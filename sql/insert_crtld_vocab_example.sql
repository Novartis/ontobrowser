-- Copyright 2015 Novartis Institutes for Biomedical Research
-- 
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
-- 
--     http://www.apache.org/licenses/LICENSE-2.0
-- 
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.

INSERT INTO CTRLD_VOCAB_DOMAIN (
  CTRLD_VOCAB_DOMAIN_ID,
  CTRLD_VOCAB_DOMAIN,
  CREATED_BY
) VALUES (
  PRIMARY_KEY_SEQ.NEXTVAL,
  'Organ',
  (SELECT CURATOR_ID FROM CURATOR WHERE USERNAME='SYSTEM'));

INSERT INTO CTRLD_VOCAB_DOMAIN_ONTOLOGY (
  CTRLD_VOCAB_DOMAIN_ONTOLOGY_ID,
  CTRLD_VOCAB_DOMAIN_ID,
  ONTOLOGY_ID,
  CREATED_BY
) VALUES (
  PRIMARY_KEY_SEQ.NEXTVAL,
  (SELECT CTRLD_VOCAB_DOMAIN_ID FROM CTRLD_VOCAB_DOMAIN WHERE CTRLD_VOCAB_DOMAIN = 'Organ'),
  (SELECT ONTOLOGY_ID FROM ONTOLOGY WHERE ONTOLOGY_NAME = 'Mouse adult gross anatomy'),
  (SELECT CURATOR_ID FROM CURATOR WHERE USERNAME='SYSTEM'));
  
INSERT INTO CTRLD_VOCAB_CONTEXT (
  CTRLD_VOCAB_CONTEXT_ID,
  CTRLD_VOCAB_CONTEXT,
  CREATED_BY
) VALUES (
  PRIMARY_KEY_SEQ.NEXTVAL,
  'Organ Measurement',
  (SELECT CURATOR_ID FROM CURATOR WHERE USERNAME='SYSTEM'));

INSERT INTO CTRLD_VOCAB (
  CTRLD_VOCAB_ID,
  DATASOURCE_ID,
  CTRLD_VOCAB_NAME,
  REFERENCE_ID,
  CTRLD_VOCAB_DOMAIN_ID,
  CTRLD_VOCAB_CONTEXT_ID,
  CREATED_BY)
VALUES (
  PRIMARY_KEY_SEQ.NEXTVAL,
  (SELECT DATASOURCE_ID
    FROM DATASOURCE
    WHERE DATASOURCE_ACRONYM = 'SEND'),
  'Specimen',
  'C77529'
  (SELECT CTRLD_VOCAB_DOMAIN_ID
    FROM CTRLD_VOCAB_DOMAIN 
    WHERE CTRLD_VOCAB_DOMAIN = 'Organ'),
  (SELECT CTRLD_VOCAB_CONTEXT_ID
    FROM CTRLD_VOCAB_CONTEXT 
    WHERE CTRLD_VOCAB_CONTEXT = 'Organ Measurement'),
  (SELECT CURATOR_ID FROM CURATOR WHERE USERNAME='SYSTEM'));
