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

import org.edc.sstone.CheckedException;
import org.edc.sstone.j2me.audio.AudioPlayer;
import org.edc.sstone.j2me.core.Registry;
import org.edc.sstone.j2me.ui.component.HighlightEnabledTextArea;
import org.edc.sstone.j2me.ui.scroll.ScrollHandler;
import org.edc.sstone.j2me.ui.style.Style;
import org.edc.sstone.text.Alphabet;


/**
 * @author Greg Orlowski
 */
public abstract class SimpleTokenReaderComponent extends HighlightEnabledTextArea {

    /**
     * We need alphabet in all components to be able to convert uppercase/lowercase
     */
    protected final Alphabet alphabet;
    protected boolean playAudio = true;
    protected boolean readNonLetters = false;

    protected SimpleTokenReaderComponent(String text, Alphabet alphabet, Style style, int viewportWidth,
            Character syllableSeparator) {
        super(text, style, viewportWidth, syllableSeparator);
        this.alphabet = alphabet;
    }

    protected String getResourceString() {
        return alphabet.toLowerCase(getHighlightedTokenString());
    }

    protected abstract String getResourceParentDir();

    protected String getAudioResourcePath() {
        String parentDir = getResourceParentDir();
        parentDir = (parentDir != null && parentDir.trim().length() > 0)
                ? parentDir.trim() + '/'
                : "";
        return parentDir + getResourceString();
    }

    /**
     * This gets called after an animation frame is advanced
     */
    protected void frameAdvanced() {
        if (playAudio) {
            AudioPlayer player = Registry.getManager().getAudioPlayer();
            if (player != null) {
                try {
                    player.playAudio(getAudioResourcePath(), true);
                } catch (CheckedException ce) {
                    Registry.getManager().handleException(ce);
                }
            }
        }
    }

    public void advanceFrame(ScrollHandler scrollHandler) {
        super.advanceFrame(scrollHandler);
        frameAdvanced();
    }

    public void setPlayAudio(boolean playAudio) {
        this.playAudio = playAudio;
    }

    public void setReadNonLetters(boolean readNonLetters) {
        this.readNonLetters = readNonLetters;
    }

}
