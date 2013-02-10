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

import org.edc.sstone.Constants;
import org.edc.sstone.event.MenuListener;
import org.edc.sstone.j2me.core.Registry;
import org.edc.sstone.j2me.nav.NavigationEventListener;
import org.edc.sstone.j2me.ui.KeyCode;
import org.edc.sstone.j2me.ui.component.ComponentPanel;
import org.edc.sstone.j2me.ui.icon.ExitIcon;
import org.edc.sstone.j2me.ui.menu.MenuButton;
import org.edc.sstone.j2me.ui.menu.MenuItem;
import org.edc.sstone.j2me.ui.screen.ComponentScreen;
import org.edc.sstone.j2me.ui.style.Style;
import org.edc.sstone.nav.ScreenNavigation;

/**
 * TODO: I was going to let the screen itself handle navigation, but it actually seems to make more
 * sense to set up nav events in the screen factory. In that case, this class may be superfluous,
 * and maybe we can just use ComponentScreen...
 * 
 * @author Greg Orlowski
 */
public class AppScreen extends ComponentScreen {

    public AppScreen(Style style, ComponentPanel componentPanel) {
        super(style, componentPanel);
    }

    protected void keyPressed(KeyCode keyCode) {

        ScreenNavigation nav = Registry.getManager().getScreenNavigation();

        if (nav != null && keyCode == KeyCode.ASTERISK && nav.isLogScreenEnabled()) {
            new LogScreen().show();
        } else if (keyCode == KeyCode.NUM0) {
            UserPreferencesScreen prefsScreen = new UserPreferencesScreen();
            MenuListener ml = null;

            if (nav != null) {
                ml = new NavigationEventListener(nav, Constants.NAVIGATION_DIRECTION_RELOAD_CURR);
            } else {
                ml = new ModuleMenuScreenLoadListener();
            }

            prefsScreen.addMenuItem(MenuItem.iconItem(ml, new ExitIcon()), MenuButton.LEFT);
            prefsScreen.show();
        } else {
            super.keyPressed(keyCode);
        }
    }
}
