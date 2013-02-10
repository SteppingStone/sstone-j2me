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

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import javax.microedition.lcdui.Image;

import org.edc.sstone.Constants;
import org.edc.sstone.event.MenuEvent;
import org.edc.sstone.event.MenuListener;
import org.edc.sstone.io.InputStreamProvider;
import org.edc.sstone.j2me.core.Registry;
import org.edc.sstone.j2me.mod.ModuleLoader;
import org.edc.sstone.j2me.nav.NavigationEventListener;
import org.edc.sstone.j2me.ui.component.ComponentContentPanel;
import org.edc.sstone.j2me.ui.component.ComponentSelectionPanel;
import org.edc.sstone.j2me.ui.component.ImagePanel;
import org.edc.sstone.j2me.ui.component.MenuItemComponent;
import org.edc.sstone.j2me.ui.component.TextArea;
import org.edc.sstone.j2me.ui.icon.ExitIcon;
import org.edc.sstone.j2me.ui.menu.MenuButton;
import org.edc.sstone.j2me.ui.menu.MenuItem;
import org.edc.sstone.log.Log;
import org.edc.sstone.record.reader.RecordFactory;
import org.edc.sstone.record.reader.model.MenuItemRecord;

/**
 * @author Greg Orlowski
 */
public class ModuleMenuScreen extends AppScreen {

    private final InputStreamProvider streamProvider;
    private final RecordFactory recordFactory;
    private final ModuleLoader moduleLoader;

    public ModuleMenuScreen(ModuleLoader moduleLoader, Vector modules, InputStreamProvider streamProvider,
            RecordFactory recordFactory,
            boolean showPreferencesScreen) {
        super(null, new ComponentSelectionPanel());
        this.moduleLoader = moduleLoader;
        this.streamProvider = streamProvider;
        this.recordFactory = recordFactory;
        addComponent(logoImageComponent());
        addModuleSelectionComponents(modules);

        if (showPreferencesScreen) {
            MenuItemComponent prefsMenuItem = new MenuItemComponent(null, getManager().getMessageSource().getString(
                    "user.preferences.screen.title"),
                    new MenuListener() {
                        public void menuSelected(MenuEvent e) {
                            UserPreferencesScreen prefsScreen = new UserPreferencesScreen();
                            prefsScreen.addMenuItem(MenuItem.iconItem(
                                    new ModuleMenuScreenLoadListener(), new ExitIcon()),
                                    MenuButton.LEFT);
                            prefsScreen.show();
                        }
                    }, Registry.getManager().getTheme().getContentWidth());
            addComponent(prefsMenuItem);
        }

        /*
         * Show about in main module menu
         */
        MenuItemComponent aboutMenuItem = new MenuItemComponent(null, getManager().getMessageSource().getString(
                "about.screen.title"),
                new AboutAppMenuListener(), Registry.getManager().getTheme().getContentWidth());
        addComponent(aboutMenuItem);

        addMenuItem(MenuItem.iconItem(
                new NavigationEventListener(null, Constants.NAVIGATION_DIRECTION_EXIT),
                new ExitIcon()), MenuButton.LEFT);
    }

    private static class AboutAppMenuListener implements MenuListener {
        public void menuSelected(MenuEvent e) {
            AppScreen aboutScreen = new AppScreen(null, new ComponentContentPanel());

            aboutScreen.setTitle(Registry.getManager().getMessageSource().getString(
                    "about.screen.title"));

            aboutScreen.addComponent(new TextArea(getAboutText(),
                    Registry.getManager().getTheme(),
                    Registry.getManager().getTheme().getContentWidth()));
            aboutScreen.addMenuItem(MenuItem.iconItem(
                    new ModuleMenuScreenLoadListener(), new ExitIcon()),
                    MenuButton.LEFT);
            aboutScreen.show();
        }

        private String getAboutText() {
            InputStream in = getClass().getResourceAsStream("/README");
            int bytesRead = 0, start = 0, buffLen = 1024;
            byte[] buff = new byte[buffLen];
            StringBuffer sb = new StringBuffer();
            try {
                while ((bytesRead = in.read(buff, start, buffLen - start)) != -1) {
                    sb.append(new String(buff, 0, bytesRead, "UTF-8"));
                }
            } catch (IOException ioe) {
                Log.error("Error reading README", ioe);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                        in = null;
                    } catch (IOException ignoreCloseFailed) {
                    }
                }
            }
            return sb.toString();
        }
    }

    private void addModuleSelectionComponents(Vector modules) {
        for (int i = 0; i < modules.size(); i++) {
            addModuleSelectionItem((MenuItemRecord) modules.elementAt(i));
        }
    }

    private void addModuleSelectionItem(final MenuItemRecord module) {
        addComponent(new MenuItemComponent(module.title,
                new ModuleSelectionListener(moduleLoader, module, streamProvider, recordFactory),
                getWidth()));
    }

    private ImagePanel logoImageComponent() {
        Image image = null;
        String logoImagePath = "/img/ss_logo_48.png";
        try {
            image = Image.createImage(logoImagePath);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage() + "; image: " + logoImagePath);
        }
        return new ImagePanel(image);
    }

}
