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
package com.novartis.pcs.ontology.service.search;

import java.io.Reader;

import org.apache.lucene.analysis.CharTokenizer;
import org.apache.lucene.util.AttributeSource;

public final class TermNameTokenizer extends CharTokenizer {
	public TermNameTokenizer(Reader in) {
		super(in);
	}

	public TermNameTokenizer(AttributeSource source, Reader in) {
		super(source, in);
	}

	public TermNameTokenizer(AttributeFactory factory, Reader in) {
		super(factory, in);
	}

	@Override
	protected boolean isTokenChar(char c) {
		return Character.isLetterOrDigit(c) || c == '\'' || c == '-';
	}

	@Override
	protected char normalize(char c) {
		return Character.toLowerCase(c);
	}
	
	public static boolean isNotTokenChar(char c) {
		return !Character.isLetterOrDigit(c) && c != '\'' && c != '-';
	}
}
