/*
 * Copyright (c) 2012 EDC
 * 
 * This file is part of Stepping Stone.
 * 
 * Stepping Stone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Stepping Stone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Stepping Stone.  If not, see <http://www.gnu.org/licenses/gpl.txt>.
 */
package org.edc.sstone.j2me.component;

import org.edc.sstone.j2me.ui.style.Style;
import org.edc.sstone.text.Alphabet;
import org.edc.sstone.util.StdLib;
import org.edc.sstone.util.Text;
import org.edc.sstone.util.Text.Token;

/**
 * @author Greg Orlowski
 */
public class SyllableReaderComponent extends WordReaderComponent {

    // this is the index of the syllable within the current token
    private int tokenSyllableIdx = 0;
    private Token syllableToken = null;
    boolean readWord = false; // when this is true, read a word not a syllable

    protected final char syllableSepChar;

    public SyllableReaderComponent(String text, Alphabet alphabet, Style style, int viewportWidth,
            char syllableSeparator) {
        super(text, alphabet, style, viewportWidth, new Character(syllableSeparator));
        this.syllableSepChar = syllableSeparator;
    }

    protected Token getHighlightedToken() {
        if (readWord) {
            return super.getHighlightedToken();
        }
        return syllableToken;
    }

    protected String getAudioResourcePath() {
        if (readWord) {
            return super.getAudioResourcePath();
        }

        String wordTokenString = StdLib.removeChar(super.getHighlightedToken().text, syllableSepChar);
        /*
         * We do not need to add 1 to tokenSyllableIdx because we already incremented it in
         * advanceTokenPointer to effectively point to the next syllable. But since the audio clips
         * are 1-indexed and the token strings are 0-indexed, we're all good.
         */
        return "syllables/"
                + alphabet.stripNonLetters(alphabet.toLowerCase(wordTokenString))
                + '/' + tokenSyllableIdx;
    }

    /*
     * NOTE: advanceTokenPointer() is called before painting and before playing the audio clip. I
     * can set up state for the next paint + read operations here
     */
    protected void advanceTokenPointer() {
        // If we just read a word or we have just entered the slide, we need to advance the pointer
        // to the next (or first) token
        if (tokenIdx == -1 || readWord) {
            tokenIdx++;
        }

        int len = 0;
        Token wordToken = null;
        String tokenStr = null;

        while (len == 0) {
            wordToken = (Text.Token) tokens.elementAt(tokenIdx);
            tokenStr = wordToken.text.trim();
            len = tokenStr.length();

            // skip all empty tokens. We implement line breaks with zero-length tokens on
            // their own line. We never have to read or highlight these, which means
            // we can skip them because all lines always get drawn (token position tracking is
            // just for reading and highlighting)
            if (len == 0) {
                tokenIdx++;
            }
        }

        String[] parts = StdLib.split(tokenStr, syllableSepChar);
        if (tokenStr.length() == 0 || parts.length == 1 || tokenSyllableIdx == parts.length) {
            readWord = true;
            tokenSyllableIdx = 0;
            syllableToken = null;
        } else {
            String syllableTokenStr = parts[tokenSyllableIdx];
            int startPos = wordToken.startPos;
            for (int i = 0; i < tokenSyllableIdx; i++) {
                startPos += parts[i].length();
            }
            int endPos = startPos + syllableTokenStr.length();

            syllableToken = new Token(syllableTokenStr, wordToken.lineIdx,
                    startPos, endPos, getFont().stringWidth(syllableTokenStr));
            readWord = false;
            tokenSyllableIdx++;
        }

    }

    // NOTE: this gets called BEFORE advanceTokenPointer
    protected boolean hasMoreTokens() {

        // if we did not just read a word (at start or we just read a syllable), return true
        if (!readWord && tokens.size() > 0) {
            return true;
        }

        return tokenIdx < (tokens.size() - 1);

        // return true;
        // Log.debug("SyllableReaderComponent.hasMoreTokens() going to return false. Token is: "
        // + getHighlightedToken().text);
        // return false;
    }

    /*
     * When we get a line for display, we have to remove the syllable separators within each word
     * 
     * NOTE: I changed the implementation. we should not need this b/c we strip the syllable
     * separator char from lines when we tokenize +instantiate the component
     */
    // protected String getLine(int idx) {
    // String ret = (String) lines.elementAt(idx);
    // return StdLib.removeChar(ret, syllableSepChar);
    // }

}
