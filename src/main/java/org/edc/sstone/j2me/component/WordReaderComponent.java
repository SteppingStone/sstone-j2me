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

/**
 * @author Greg Orlowski
 */
public class WordReaderComponent extends SimpleTokenReaderComponent {

    public WordReaderComponent(String text, Alphabet alphabet, Style style, int viewportWidth) {
        this(text, alphabet, style, viewportWidth, null);
    }

    protected WordReaderComponent(String text, Alphabet alphabet, Style style, int viewportWidth, Character sep) {
        super(text, alphabet, style, viewportWidth, sep);
    }

    protected String getResourceParentDir() {
        return "words";
    }

    protected String getResourceString() {
        String ret = alphabet.toLowerCase(getHighlightedTokenString());
        return readNonLetters ? ret : alphabet.stripNonLetters(ret);
    }

}
