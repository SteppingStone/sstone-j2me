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
package org.edc.sstone.j2me.screen;

import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;

import org.edc.sstone.CheckedException;
import org.edc.sstone.event.MenuEvent;
import org.edc.sstone.event.MenuListener;
import org.edc.sstone.j2me.core.Registry;
import org.edc.sstone.j2me.ui.component.ComponentPanel;
import org.edc.sstone.j2me.ui.icon.PauseIcon;
import org.edc.sstone.j2me.ui.icon.PlayIcon;
import org.edc.sstone.j2me.ui.icon.VectorIcon;
import org.edc.sstone.j2me.ui.menu.MenuButton;
import org.edc.sstone.j2me.ui.menu.MenuItem;
import org.edc.sstone.j2me.ui.menu.MenuItemButton;
import org.edc.sstone.j2me.ui.style.Style;


/**
 * @author Greg Orlowski
 */
public class AudioPlayerScreen extends AppScreen implements PlayerListener {

    private String trackName;
    private VectorIcon playIcon = new PlayIcon();
    private VectorIcon pauseIcon = new PauseIcon();

    private boolean isPlaying;
    private boolean trackStarted;

    /**
     * when true, navigate to the next screen when the audio clip finishes
     */
    private boolean autoAdvance = false;
    private long autoAdvanceDelayMs = 0;

    public AudioPlayerScreen(Style style, ComponentPanel componentPanel, boolean autoAdvance, long autoAdvanceDelayMs) {
        super(style, componentPanel);
        this.autoAdvance = autoAdvance;
        this.autoAdvanceDelayMs = autoAdvanceDelayMs;
        addMenuItem(MenuItem.iconItem(playPauseListener(), playIcon), MenuButton.CENTER);
    }

    protected MenuListener playPauseListener() {
        return new MenuListener() {
            public void menuSelected(MenuEvent e) {
                playOrPause();
            }
        };
    }

    /**
     * @return true if the track should start playing as soon as the screen loads. Otherwise, the
     *         user has to click the play button to start playing.
     */
    protected boolean playOnLoad() {
        return true;
    }

    public void showNotify() {
        super.showNotify();
        if (playOnLoad()) {
            new Thread() {
                public void run() {
                    playOrPause();
                }
            }.start();
        }
    }

    protected synchronized void playOrPause() {
        final boolean prefetch = true;
        if (isPlaying) {
            getAudioPlayer().stop();
            getPlayPauseButton().setIcon(playIcon);
            repaint();
            isPlaying = false;
        } else {
            try {
                if (trackStarted) {
                    getAudioPlayer().play(prefetch);
                } else {
                    trackStarted = true;
                    getAudioPlayer().playAudio("tracks/" + trackName, onTrackFinish(), prefetch);
                }
                getPlayPauseButton().setIcon(pauseIcon);
                if (isShown()) {
                    repaint();
                }
                isPlaying = true;
            } catch (CheckedException ce) {
                Registry.getManager().handleException(ce);
            }
        }
    }

    /**
     * Update the state to note that the track has ended and will start from the beginning next time
     * we press play.
     */
    protected synchronized void setTrackEnded() {
        trackStarted = false;
        getPlayPauseButton().setIcon(playIcon);
        repaint();
        isPlaying = false;
    }

    protected PlayerListener onTrackFinish() {
        return this;
    }

    protected MenuItemButton getPlayPauseButton() {
        return (MenuItemButton) menuButtons[MenuButton.CENTER];
    }

    public void setTrackName(String trackName) {
        this.trackName = trackName;
    }

    public void playerUpdate(Player player, String event, Object eventData) {
        if (PlayerListener.END_OF_MEDIA.equals(event)) {
            setTrackEnded();
            getAudioPlayer().cleanup();
            if (autoAdvance) {
                if (autoAdvanceDelayMs <= 0) {
                    Registry.getManager().getScreenNavigation().next();
                } else {
                    new Timer().schedule(new AutoAdvanceTask(), autoAdvanceDelayMs);
                }
            }
        }
    }

    private static class AutoAdvanceTask extends TimerTask {
        public void run() {
            Registry.getManager().getScreenNavigation().next();
        }
    }
}
