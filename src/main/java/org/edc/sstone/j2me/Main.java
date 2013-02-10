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
package org.edc.sstone.j2me;

import java.util.Vector;

import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDletStateChangeException;

import org.edc.sstone.CheckedException;
import org.edc.sstone.Constants;
import org.edc.sstone.io.ClasspathInputStreamProvider;
import org.edc.sstone.io.InputStreamProvider;
import org.edc.sstone.j2me.audio.AudioPlayer;
import org.edc.sstone.j2me.core.AbstractManagerMIDlet;
import org.edc.sstone.j2me.device.BacklightControl;
import org.edc.sstone.j2me.io.FileConnectionInputStreamProvider;
import org.edc.sstone.j2me.mod.FileModuleLoader;
import org.edc.sstone.j2me.mod.ModuleLoader;
import org.edc.sstone.j2me.screen.LogScreen;
import org.edc.sstone.j2me.screen.ModuleMenuScreen;
import org.edc.sstone.j2me.ui.canvas.SplashScreen;
import org.edc.sstone.j2me.ui.screen.ErrorMessageScreen;
import org.edc.sstone.log.Level;
import org.edc.sstone.log.Log;
import org.edc.sstone.prefs.UserPreferences;
import org.edc.sstone.record.reader.RecordFactory;
import org.edc.sstone.record.reader.model.IntArrayRecord;
import org.edc.sstone.record.reader.model.MenuItemRecord;
import org.edc.sstone.record.reader.model.QuestionRecord;
import org.edc.sstone.record.reader.model.Record;
import org.edc.sstone.record.reader.model.ResourceComponentRecord;
import org.edc.sstone.record.reader.model.ScreenRecord;
import org.edc.sstone.record.reader.model.ScreenSeriesRecord;
import org.edc.sstone.record.reader.model.StyleRecord;
import org.edc.sstone.record.reader.model.TextAreaComponentRecord;
import org.edc.sstone.res.ResourceProvider;
import org.edc.sstone.store.ValueSource;
import org.edc.sstone.util.TimerQueue;
import org.edc.sstone.util.TimerTask;

/**
 * @author Greg Orlowski
 */
public class Main extends AbstractManagerMIDlet {

    private ResourceProvider resourceProvider;
    private UserPreferences userPreferences;

    public Main() {
        super();
        Level level = Level.forName(getMidletProperty("logLevel"));
        if (level == null)
            level = Level.WARN;
        Log.setLevel(level);
    }

    private RecordFactory newRecordFactory() {
        RecordFactory rf = new RecordFactory();
        Record[] recordObjects = new Record[] {
                new StyleRecord(),
                new TextAreaComponentRecord(),
                new ScreenRecord(),
                new ScreenSeriesRecord(),
                new MenuItemRecord(),
                new IntArrayRecord(),
                new ResourceComponentRecord(),
                new QuestionRecord()
        };

        for (int i = 0; i < recordObjects.length; i++)
            rf.registerType(recordObjects[i]);

        return rf;
    }

    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
        // Some cleanup
        AudioPlayer ap = getAudioPlayer();
        if (ap != null) {
            getAudioPlayer().cleanup();
        }

        BacklightControl bc = getBacklightControl();
        if (bc != null) {
            bc.cleanup();
        }
    }

    protected void pauseApp() {
        // TODO implement pauseApp()
    }

    protected void startApp() throws MIDletStateChangeException {
        Display.getDisplay(this).setCurrent(new SplashScreen("/img/ss_logo_128_160.png"));
        String timeoutStr = getMidletProperty("splashScreenTimeout");

        if (timeoutStr != null) {
            try {
                Thread.sleep(Long.parseLong(timeoutStr));
            } catch (Exception e) {
                Log.warn("Error during splash screen timeout: ", e);
            }
        }

        init();
        showMainMenu();
    }

    protected void init() {
        initDisplay();

        /*
         * we have to do init user prefs before backlight controls, font, and theme because user
         * preferences can determine
         */
        initUserPreferences();

        // should we make the language choice a user pref? We could keep a hashtable
        // of language names -> codes, scan the classpath to filter the list by which
        // ones are actually available (so we have the option to exclude some from a build),
        // default

        String lang = getMidletProperty("lang");

        setMessageSource(lang);
        initBacklightControl();

        initFontFactory();
        setTheme(newTheme(getFontFactory()));
    }

    private void initUserPreferences() {
        userPreferences = new UserPreferences();
    }

    protected InputStreamProvider initStreamProvider() {
        if ("ClasspathInputStreamProvider".equals(getMidletProperty("streamProviderType"))) {
            return new ClasspathInputStreamProvider();
        }
        return new FileConnectionInputStreamProvider();
    }

    public void showMainMenu() {
        /*
         * Null out the nav. If we return here from a menu system within a module, we want to null
         * out the navigator before we move on.
         */
        setScreenNavigation(null);

        final InputStreamProvider streamProvider = initStreamProvider();
        final RecordFactory rf = newRecordFactory();
        final ModuleLoader ml = initModuleLoader(streamProvider, rf);

        final Vector updates = ml.listUpdates();
        final TimerQueue queue = new TimerQueue();

        try {
            for (int i = 0; i < updates.size(); i++) {
                if (ml.autoInstallUpdates()) {
                    queue.scheduleOnce(new UnpackUpdateTask(ml, (String) updates.elementAt(i)), 0l);
                } else {
                    // TODO: in >v1.0 support a user prompt before unpacking updates (?)
                }
            }
        } catch (Exception e) {
            ErrorMessageScreen.forMessagekey(new LogScreen(), "error.unpacking.project",
                    new String[] { e.getMessage() },
                    Constants.NAVIGATION_DIRECTION_EXIT);
            queue.cleanup();
        }

        queue.scheduleOnce(new TimerTask() {
            public void run() {
                // TODO Auto-generated method stub
                try {
                    Vector modules = ml.getModules();
                    if (modules.size() > 1) {
                        String showPrefsInMainMenuStr = getMidletProperty("showPreferencesInMainMenu");
                        boolean showPrefsInMainMenu = true;
                        if (showPrefsInMainMenuStr != null) {
                            showPrefsInMainMenu = "true".equals(showPrefsInMainMenuStr.toLowerCase());
                        }
                        ModuleMenuScreen mms = new ModuleMenuScreen(ml, modules, streamProvider, rf,
                                showPrefsInMainMenu);
                        mms.setTitle("Stepping Stone");

                        setScreen(mms);
                    } else if (modules.size() == 1) {
                        // If there is only 1 module found, load it automatically.
                        try {
                            ml.loadModule((MenuItemRecord) modules.firstElement(), streamProvider, rf, false);
                        } catch (CheckedException ce) {
                            handleException(ce);
                        }
                    } else {
                        ErrorMessageScreen.forMessagekey(new LogScreen(), "error.noProjectsFound",
                                Constants.NAVIGATION_DIRECTION_EXIT);
                    }
                } finally {
                    queue.cleanup();
                }
            }
        }, 10l);

    }

    static final class UnpackUpdateTask extends TimerTask {

        final ModuleLoader ml;
        final String updateName;

        UnpackUpdateTask(ModuleLoader ml, String updateName) {
            this.ml = ml;
            this.updateName = updateName;
        }

        public void run() {
            ml.unpackUpdate(updateName);
        }

    }

    // protected void showInitialScreen(ScreenNavigation nav) {
    // nav.showFirstScreen();
    // }

    protected ModuleLoader initModuleLoader(InputStreamProvider streamProvider, RecordFactory rf) {
        String moduleLoaderType = getMidletProperty("moduleLoaderType");

        ModuleLoader ml = ("FileModuleLoader".equals(moduleLoaderType))
                ? new FileModuleLoader(streamProvider, rf, getMidletProperty("fileconn.dir.memorycard"))
                : new ModuleLoader(streamProvider, rf, getMidletProperty("modulePaths"));
        return ml;
    }

    public ResourceProvider getResourceProvider() {
        return resourceProvider;
    }

    public void setResourceProvider(ResourceProvider resourceProvider) {
        this.resourceProvider = resourceProvider;
    }

    public void handleException(Throwable e) {
        Log.warn("Caught exception: ", e);
        // If this is not a checked exception with a known error message,
        // report the exceptions default error message to the user.
        // String errorMessage = e.getMessage();
        String errorKey = "application.error";
        String[] messageArgs = null;

        if (e instanceof CheckedException) {
            CheckedException ce = (CheckedException) e;
            errorKey = ce.getErrorMessageKey();
            messageArgs = ce.messageArgs;
        }
        ErrorMessageScreen.forMessagekey(new LogScreen(), errorKey, messageArgs, Constants.NAVIGATION_DIRECTION_EXIT);
    }

    public Object getUserPreference(int recordId) {
        try {
            return userPreferences.getValue(recordId);
        } catch (CheckedException e) {
            Log.error("Error when retrieving preferences for recordId: " + recordId, e);
        }
        return null;
    }

    public ValueSource getUserPreferences() {
        return userPreferences;
    }

}
