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

import org.edc.sstone.event.MenuEvent;
import org.edc.sstone.event.MenuListener;
import org.edc.sstone.j2me.core.Registry;
import org.edc.sstone.j2me.ui.component.ComponentContentPanel;
import org.edc.sstone.j2me.ui.component.TextArea;
import org.edc.sstone.j2me.ui.menu.MenuButton;
import org.edc.sstone.j2me.ui.menu.MenuItem;
import org.edc.sstone.log.Log;
import org.edc.sstone.log.LogMessage;
import org.edc.sstone.nav.ScreenNavigation;

/**
 * @author Greg Orlowski
 */
public class LogScreen extends AppScreen {

    public LogScreen() {
        super(null, new ComponentContentPanel());

        final ScreenNavigation nav = Registry.getManager().getScreenNavigation();

        setTitle("Log");
        LogMessage[] messages = Log.getLogMessages();
        int contentWidth = Registry.getManager().getTheme().getContentWidth();

        for (int i = 0; i < messages.length; i++) {
            addComponent(new TextArea(messages[i].toString(), contentWidth));
        }

        addMenuItem(MenuItem.textItem(new MenuListener() {
            public void menuSelected(MenuEvent e) {
                if (nav != null) {
                    nav.showCurrentScreen();
                } else {
                    Registry.getManager().exit();
                }
            }
        }, "OK"), MenuButton.LEFT);
    }

}
