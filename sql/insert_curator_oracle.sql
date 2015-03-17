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

WHENEVER SQLERROR EXIT FAILURE ROLLBACK
WHENEVER OSERROR EXIT FAILURE ROLLBACK
SET SHOW OFF
SET TERMOUT OFF
SET FEEDBACK OFF
SET ECHO OFF
SET VERIFY OFF
INSERT INTO CURATOR (
  CURATOR_ID, 
  USERNAME, 
  IS_ACTIVE, 
  CREATED_BY) 
VALUES (
  PRIMARY_KEY_SEQ.nextval, 
  '&1',
  1, 
  (SELECT CURATOR_ID FROM CURATOR WHERE USERNAME = 'SYSTEM'));

INSERT INTO CURATOR_APPROVAL_WEIGHT (
  CURATOR_APPROVAL_ID,
  CURATOR_ID,
  TABLE_NAME,
  APPROVAL_WEIGHT,
  CREATED_DATE,
  CREATED_BY)
VALUES (
  PRIMARY_KEY_SEQ.NEXTVAL, 
  (SELECT CURATOR_ID FROM CURATOR WHERE USERNAME = '&1'), 
  'TERM',
  1, 
  SYSDATE,
  (SELECT CURATOR_ID FROM CURATOR WHERE USERNAME = 'SYSTEM'));
  
INSERT INTO CURATOR_APPROVAL_WEIGHT (
  CURATOR_APPROVAL_ID,
  CURATOR_ID,
  TABLE_NAME,
  APPROVAL_WEIGHT,
  CREATED_DATE,
  CREATED_BY)
VALUES (
  PRIMARY_KEY_SEQ.NEXTVAL, 
  (SELECT CURATOR_ID FROM CURATOR WHERE USERNAME = '&1'), 
  'TERM_SYNONYM',
  1.0, 
  SYSDATE,
  (select CURATOR_ID from CURATOR where USERNAME = 'SYSTEM'));
  
INSERT INTO CURATOR_APPROVAL_WEIGHT (
  CURATOR_APPROVAL_ID,
  CURATOR_ID,
  TABLE_NAME,
  APPROVAL_WEIGHT,
  CREATED_DATE,
  CREATED_BY)
VALUES (
  PRIMARY_KEY_SEQ.NEXTVAL, 
  (select CURATOR_ID from CURATOR where USERNAME = '&1'), 
  'TERM_RELATIONSHIP',
  1.0, 
  SYSDATE,
  (select CURATOR_ID from CURATOR where USERNAME = 'SYSTEM'));
COMMIT;
EXIT SUCCESS
