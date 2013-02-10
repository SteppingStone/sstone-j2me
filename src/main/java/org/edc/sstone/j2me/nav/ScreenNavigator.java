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
package org.edc.sstone.j2me.nav;

import org.edc.sstone.CheckedException;
import org.edc.sstone.j2me.core.Registry;
import org.edc.sstone.j2me.ui.screen.ComponentScreen;
import org.edc.sstone.nav.ModuleManager;
import org.edc.sstone.nav.ScreenNavigation;
import org.edc.sstone.record.reader.model.MenuItemRecord;
import org.edc.sstone.record.reader.model.ScreenRecord;

/**
 * @author Greg Orlowski
 */
public class ScreenNavigator implements ScreenNavigation {

    protected ScreenFactory factory;
    protected ModuleManager moduleManager;
    private boolean logScreenEnabled;
    private boolean otherModulesAvailable;

    public ScreenNavigator(ModuleManager moduleNav, boolean otherModulesAvailable) {
        this.moduleManager = moduleNav;
        this.factory = new ScreenFactory(this, moduleNav.getAlphabet());
        this.otherModulesAvailable = otherModulesAvailable;

        String enableLogScreen = Registry.getManager().getMidletProperty("enableLogScreen");
        if (enableLogScreen != null && "true".equalsIgnoreCase(enableLogScreen)) {
            logScreenEnabled = true;
        }
    }

    public void showFirstScreen() {
        try {
            showScreen(moduleManager.firstScreen());
        } catch (CheckedException e) {
            showErrorScreen(e);
        }
    }

    public void descend(MenuItemRecord menuItemRecord) {
        try {
            showScreen(moduleManager.descend(menuItemRecord));
        } catch (CheckedException e) {
            showErrorScreen(e);
        }
    }

    public void next() {
        try {
            showScreen(moduleManager.next());
        } catch (CheckedException e) {
            showErrorScreen(e);
        }
    }

    public void showCurrentScreen() {
        try {
            ScreenRecord currentScreen = moduleManager.currentScreen();
            showScreen(currentScreen == null ? moduleManager.firstScreen() : currentScreen);
        } catch (CheckedException e) {
            showErrorScreen(e);
        }
    }

    public void previous() {
        try {
            showScreen(moduleManager.previous());
        } catch (CheckedException e) {
            showErrorScreen(e);
        }
    }

    public boolean hasNext() {
        return moduleManager.hasNext();
    }

    public boolean hasPrevious() {
        return moduleManager.hasPrevious();
    }

    public void up() {
        ScreenRecord screenRecord = moduleManager.up();
        if (screenRecord != null) {
            showScreen(screenRecord);
        } else {
            moduleManager = null;
            factory = null;

            if (otherModulesAvailable) {
                Registry.getManager().showMainMenu();
            } else {
                Registry.getManager().exit();
            }
        }
    }

    protected void showScreen(ScreenRecord screenRecord) {
        ComponentScreen screen = factory.newScreen(screenRecord);
        screen.show();
    }

    protected void showErrorScreen(CheckedException ce) {
        Registry.getManager().handleException(ce);
    }

    public boolean isLogScreenEnabled() {
        return logScreenEnabled;
    }

}
