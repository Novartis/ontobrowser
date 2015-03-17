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

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;

import java.io.IOException;

/**
 * Tokenizes the given token into n-grams of given size(s).
 * <p>
 * This {@link TokenFilter} create n-grams from the beginning edge or ending edge of a input token.
 * </p>
 */
public final class EdgeNGramTokenFilter extends TokenFilter {
	public static final int DEFAULT_MIN_GRAM_SIZE = 2;
	public static final int DEFAULT_MAX_GRAM_SIZE = 255;
		
	private int minGram;
	private int maxGram;
	private char[] curTermBuffer;
	private int curTermLength;
	private int curGramSize;
	private int tokStart;
	private int tokEnd;
	private boolean hasIllegalOffsets;
	private int savePosIncr;
	private boolean isFirstToken = true;

	private final TermAttribute termAtt = addAttribute(TermAttribute.class);
	private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
	private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);

	public EdgeNGramTokenFilter(TokenStream input) {
		super(input);
		this.minGram = DEFAULT_MIN_GRAM_SIZE;
		this.maxGram = DEFAULT_MAX_GRAM_SIZE;
	}

	/**
	 * Creates EdgeNGramTokenFilter that can generate n-grams in the sizes of the given range
	 *
	 * @param input {@link TokenStream} holding the input to be tokenized
	 * @param side the {@link Side} from which to chop off an n-gram
	 * @param minGram the smallest n-gram to generate
	 * @param maxGram the largest n-gram to generate
	 */
	public EdgeNGramTokenFilter(TokenStream input, int minGram, int maxGram) {
		super(input);

		if (minGram < 1) {
			throw new IllegalArgumentException("minGram must be greater than zero");
		}

		if (minGram > maxGram) {
			throw new IllegalArgumentException("minGram must not be greater than maxGram");
		}

		this.minGram = minGram;
		this.maxGram = maxGram;
	}
	
	@Override
	public final boolean incrementToken() throws IOException {
		while (true) {
			if (curTermBuffer == null) {
				if (!input.incrementToken()) {
					return false;
				} else {
					curTermBuffer = (char[]) termAtt.termBuffer().clone();
					curTermLength = termAtt.termLength();
					curGramSize = minGram;
					tokStart = offsetAtt.startOffset();
					tokEnd = offsetAtt.endOffset();
			        // if length by start + end offsets doesn't match the term text then assume
			        // this is a synonym and don't adjust the offsets.
			        hasIllegalOffsets = (tokStart + curTermLength) != tokEnd;
			        savePosIncr = posIncrAtt.getPositionIncrement();
				}
			}
						
			if (curGramSize <= maxGram) {         // if we have hit the end of our n-gram size range, quit
				if (curGramSize <= curTermLength) { // if the remaining input is too short, we can't generate any n-grams
					// grab gramSize chars from front or back
					int start = 0;
					int end = start + curGramSize;
					clearAttributes();
					if (hasIllegalOffsets) {
						offsetAtt.setOffset(tokStart, tokEnd);
					} else {
						offsetAtt.setOffset(tokStart + start, tokStart + end);
					}
					// first ngram gets increment, others don't
					if (curGramSize == minGram) {
						//  Leave the first token position increment at the cleared-attribute value of 1
						if ( ! isFirstToken) {
							posIncrAtt.setPositionIncrement(savePosIncr);
						}
					} else {
						posIncrAtt.setPositionIncrement(0);
					}
					termAtt.setTermBuffer(curTermBuffer, start, curGramSize);
					curGramSize++;
					isFirstToken = false;
					return true;
				}
			}
			
			curTermBuffer = null;
		}
	}

	@Override
	public void reset() throws IOException {
		super.reset();
		curTermBuffer = null;
		isFirstToken = true;
	}
}