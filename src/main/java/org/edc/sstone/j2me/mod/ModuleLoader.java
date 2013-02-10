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
package org.edc.sstone.j2me.mod;

import java.util.Vector;

import org.edc.sstone.CheckedException;
import org.edc.sstone.Constants;
import org.edc.sstone.io.InputStreamProvider;
import org.edc.sstone.j2me.audio.AudioPlayer;
import org.edc.sstone.j2me.core.Registry;
import org.edc.sstone.j2me.nav.ScreenNavigator;
import org.edc.sstone.log.Log;
import org.edc.sstone.nav.ModuleManager;
import org.edc.sstone.nav.ScreenNavigation;
import org.edc.sstone.record.reader.RecordFactory;
import org.edc.sstone.record.reader.model.MenuItemRecord;
import org.edc.sstone.util.StringTokenizer;

/**
 * @author Greg Orlowski
 */
public class ModuleLoader {

    final String[] modulePaths;
    protected final InputStreamProvider streamProvider;
    protected final RecordFactory recordFactory;

    /**
     * @param modPaths
     *            a space-separated list of module URLs
     */
    public ModuleLoader(InputStreamProvider streamProvider, RecordFactory rf, String modPaths) {
        this.streamProvider = streamProvider;
        this.recordFactory = rf;

        if (modPaths == null || modPaths.trim().length() == 0) {
            modulePaths = null;
        } else {
            StringTokenizer st = new StringTokenizer(modPaths);
            Vector v = new Vector();

            while (st.hasMoreTokens())
                v.addElement(st.nextToken());
            this.modulePaths = new String[v.size()];
            for (int i = 0; i < modulePaths.length; i++) {
                modulePaths[i] = (String) v.elementAt(i);
            }
        }
    }

    protected ScreenNavigator initScreenNavigator(ModuleManager mm, boolean otherModulesAvailable) {
        return new ScreenNavigator(mm, otherModulesAvailable);
    }

    public void loadModule(MenuItemRecord module, InputStreamProvider streamProvider,
            RecordFactory recordFactory, boolean otherModulesAvailable)
            throws CheckedException {
        String audioType = Registry.getManager().getMidletProperty("audioType");
        ModuleManager mm = new ModuleManager(recordFactory, module.rootUrl, streamProvider, audioType);
        mm.initProperties();

        ScreenNavigation nav = initScreenNavigator(mm, otherModulesAvailable);

        // Both the audio player and the resource provider need to
        // have a reference to the module's resource provider to be able to load
        // audio + image resources from the right place.
        Registry.getManager().setResourceProvider(mm);

        /*
         * TODO: parameterize useStringUrls
         */
        String playAudioFromUrlsStr = Registry.getManager().getMidletProperty("playAudioFromUrls");
        boolean playAudioFromUrls = (playAudioFromUrlsStr != null
                && "true".equals(playAudioFromUrlsStr.toLowerCase()));

        int volume = Constants.NUMBER_NOT_SET;
        Object volumeObj = Registry.getManager().getUserPreference(Constants.VOLUME_RECORD_ID);
        if (volumeObj != null) {
            volume = ((Integer) volumeObj).intValue();
        }

        Registry.getManager().setAudioPlayer(new AudioPlayer(mm, volume, playAudioFromUrls));
        Registry.getManager().setScreenNavigation(nav);

        nav.showCurrentScreen();
    }

    /**
     * Subclasses can call this empty ctor
     * 
     * @param modPaths
     * @param streamProvider
     * @param rf
     */
    public ModuleLoader(InputStreamProvider streamProvider, RecordFactory rf) {
        this(streamProvider, rf, null);
    }

    public Vector getModules() {
        Vector ret = new Vector();
        for (int i = 0; i < modulePaths.length; i++) {
            MenuItemRecord mod;
            try {
                mod = new ModuleManager(recordFactory, modulePaths[i], streamProvider)
                        .getModuleHeaderRecord();
                ret.addElement(mod);
            } catch (CheckedException mer) {
                Log.warn("Error reading module from: " + modulePaths[i], mer);
            }

            // NOTE: We do not call initProperties on ModuleManager. We might have to do that
            // to get the icon. I guess we could also do some by-convention icon thing (like
            // a favicon.ico or favicon.png in the root)

            // TODO: how do we get the icon (?)
        }
        return ret;
    }

    public void unpackUpdate(String path) {
        Log.info("Unpacking update: " + path);
        // do nothing
    }

    public boolean autoInstallUpdates() {
        return true;
    }

    protected boolean deleteArchivesAfterInstall() {
        return true;
    }

    public Vector listUpdates() {
        return new Vector();
    }
}
