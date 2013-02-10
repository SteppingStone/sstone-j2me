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

import org.edc.sstone.CheckedException;
import org.edc.sstone.event.MenuEvent;
import org.edc.sstone.event.MenuListener;
import org.edc.sstone.io.InputStreamProvider;
import org.edc.sstone.j2me.core.Registry;
import org.edc.sstone.j2me.mod.ModuleLoader;
import org.edc.sstone.record.reader.RecordFactory;
import org.edc.sstone.record.reader.model.MenuItemRecord;

/**
 * @author Greg Orlowski
 */
public class ModuleSelectionListener implements MenuListener {

    final InputStreamProvider streamProvider;
    final RecordFactory recordFactory;
    final MenuItemRecord module;
    final ModuleLoader moduleLoader;

    public ModuleSelectionListener(ModuleLoader ml, MenuItemRecord module, InputStreamProvider streamProvider,
            RecordFactory recordFactory) {
        this.streamProvider = streamProvider;
        this.recordFactory = recordFactory;
        this.module = module;
        this.moduleLoader = ml;
    }

    public void menuSelected(MenuEvent e) {
        try {
            moduleLoader.loadModule(module, streamProvider, recordFactory, true);
        } catch (CheckedException ce) {
            Registry.getManager().handleException(ce);
        }
    }

}
