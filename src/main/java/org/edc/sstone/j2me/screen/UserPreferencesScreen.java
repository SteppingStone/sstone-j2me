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
import org.edc.sstone.Constants;
import org.edc.sstone.event.Observer;
import org.edc.sstone.il8n.MessageSource;
import org.edc.sstone.j2me.audio.AudioPlayer;
import org.edc.sstone.j2me.core.Registry;
import org.edc.sstone.j2me.device.BacklightControl;
import org.edc.sstone.j2me.font.FontFactory;
import org.edc.sstone.j2me.font.IFont;
import org.edc.sstone.j2me.store.ValueSourceObserver;
import org.edc.sstone.j2me.ui.KeyCode;
import org.edc.sstone.j2me.ui.component.ComponentSelectionPanel;
import org.edc.sstone.j2me.ui.component.Gauge;
import org.edc.sstone.j2me.ui.component.HSelect;
import org.edc.sstone.j2me.ui.component.Option;
import org.edc.sstone.j2me.ui.screen.ErrorMessageScreen;
import org.edc.sstone.j2me.ui.style.ComponentStyle;
import org.edc.sstone.j2me.ui.style.Style;
import org.edc.sstone.log.Log;
import org.edc.sstone.store.ValueSource;
import org.edc.sstone.util.StdLib;

/**
 * @author Greg Orlowski
 */
public class UserPreferencesScreen extends AppScreen {

    public UserPreferencesScreen() {
        this(new ComponentStyle(Registry.getManager().getTheme()));
        ComponentStyle screenStyle = (ComponentStyle) getStyle();
        IFont font = Registry.getManager().getFontFactory().getFont(getStyle().getFontStyle());
        screenStyle.setPadding((short) (font.getHeight() / 4));
    }

    protected UserPreferencesScreen(Style style) {
        super(style, new CustomSelectionPanel());
        String title = getTitle();
        if (title == null || title.trim().length() == 0) {
            setTitle(getManager().getMessageSource().getString("user.preferences.screen.title"));
        }
        addComponents();
    }

    protected ValueSourceObserver observer(String[] title, int recordId) {
        return new ValueSourceObserver(StdLib.join(title, " "), recordId, getUserPreferences());
    }

    protected void reset(int selectedComponentIdx) {
        UserPreferencesScreen prefsScreen = new UserPreferencesScreen();
        ((CustomSelectionPanel) prefsScreen.componentPanel).selectComponent(selectedComponentIdx);

        for (int i = 0; i < menuButtons.length; i++) {
            prefsScreen.menuButtons[i] = menuButtons[i];
        }
        prefsScreen.show();
    }

    protected void keyPressed(KeyCode keyCode) {
        // because the super class loads a UserPreferencesScreen when we press 0, we should
        // disable the NUM0 hotkey so we cannot navigate to a new user prefs screen while we are
        // already on one.
        if (keyCode != KeyCode.NUM0) {
            super.keyPressed(keyCode);
        }
    }

    protected void addComponents() {
        MessageSource ms = getManager().getMessageSource();

        String langMappingVal = getManager().getMidletProperty("langMapping");
        if (langMappingVal == null || langMappingVal.trim().length() == 0) {
            langMappingVal = "fr Français en English bm Bambara es Español";
        }

        String defaultLang = getManager().getMidletProperty("lang");
        if (defaultLang == null)
            defaultLang = "fr";
        int defaultLangIdx = 0;

        final String[] langMappingElements = StdLib.split(langMappingVal, ' ');
        final Option[] langOptions = new Option[langMappingElements.length / 2];
        for (int i = 0; i < langOptions.length; i++) {
            langOptions[i] = new Option(i, langMappingElements[(i * 2) + 1]);
            if (defaultLang.equals(langMappingElements[(i * 2)])) {
                defaultLangIdx = i;
            }
        }

        HSelect languageSelector = new HSelect(null, ms.getString("language"), langOptions);
        int langIdx = getValueAsInt(Constants.LANGUAGE_RECORD_ID, defaultLangIdx);
        if (langIdx > langOptions.length - 1) {
            langIdx = 0;
        }
        languageSelector.setValue(langIdx);
        languageSelector.addObserver(observer(languageSelector.title,
                Constants.LANGUAGE_RECORD_ID));
        languageSelector.addObserver(new Observer() {
            public void update(Object value) {
                int i = ((Integer) value).intValue();
                Registry.getManager().setMessageSource(langMappingElements[i * 2]);
                reset(0);
            }
        });

        HSelect fontMagnificationSelector = new HSelect(null, ms.getString("font.magnification.title"), new Option[] {
                new Option(Constants.FONT_SIZE_SMALL, ms.getString("font.magnification.smaller")),
                new Option(Constants.FONT_SIZE_MEDIUM, ms.getString("font.magnification.normal")),
                new Option(Constants.FONT_SIZE_LARGE, ms.getString("font.magnification.larger"))
        });
        fontMagnificationSelector.setValue(getValueAsInt(Constants.FONT_MAGNIFICATION_RECORD_ID,
                Constants.FONT_SIZE_MEDIUM));

        fontMagnificationSelector.addObserver(observer(fontMagnificationSelector.title,
                Constants.FONT_MAGNIFICATION_RECORD_ID));
        fontMagnificationSelector.addObserver(new Observer() {
            public void update(Object value) {
                FontFactory ff = Registry.getManager().getFontFactory();
                // Log.debug("current font height: " +
                // ff.getFont(getStyle().getFontStyle()).getHeight());

                byte magnification = ((Integer) value).byteValue();
                ff.setMagnification(magnification);
                Registry.getManager().getTheme().reinitializeGeometry(ff);
                reset(1);
            }
        });

        Gauge volumeLevelGauge = new Gauge(null, ms.getString("device.volume.level"), 0, 100, 10);
        volumeLevelGauge.setValue(getValueAsInt(Constants.VOLUME_RECORD_ID, 100));
        volumeLevelGauge.addObserver(observer(volumeLevelGauge.title,
                Constants.VOLUME_RECORD_ID));

        HSelect animationPeriodMultiplierChooser = new HSelect(null, ms.getString("animation.speed.title"),
                new Option[] {
                        new Option(15, ms.getString("animation.speed.slower")),
                        new Option(10, ms.getString("animation.speed.normal")),
                        new Option(5, ms.getString("animation.speed.faster"))
                });
        animationPeriodMultiplierChooser.addObserver(
                observer(animationPeriodMultiplierChooser.title, Constants.ANIMATION_SPEED_RECORD_ID));
        animationPeriodMultiplierChooser.setValue(getValueAsInt(Constants.ANIMATION_SPEED_RECORD_ID, 10));

        Option[] screenSaverDelayOptions = new Option[5];

        screenSaverDelayOptions[0] = new Option(0, ms.getString("screensaver.delay.default.name"));
        for (int i = 1; i < screenSaverDelayOptions.length; i++) {
            int n = i * 30;
            screenSaverDelayOptions[i] = new Option(n, n + " " + ms.getString("seconds"));
        }

        HSelect screenSaverDelayChooser = new HSelect(null, ms.getString("device.backlight.delay"),
                screenSaverDelayOptions);
        screenSaverDelayChooser.setValue(getValueAsInt(Constants.SCREENSAVER_DELAY_RECORD_ID, 0));
        screenSaverDelayChooser.addObserver(observer(screenSaverDelayChooser.title,
                Constants.SCREENSAVER_DELAY_RECORD_ID));
        screenSaverDelayChooser.addObserver(new Observer() {
            public void update(Object value) {
                BacklightControl bc = Registry.getManager().getBacklightControl();
                if (bc != null) {
                    int ssavDelay = ((Integer) value).intValue();
                    bc.setKeepAliveSeconds(ssavDelay);
                    bc.stayLit();
                }
            }
        });

        // addVectorIcons();

        addComponent(languageSelector);
        addComponent(fontMagnificationSelector);
        addComponent(volumeLevelGauge);
        addComponent(animationPeriodMultiplierChooser);
        addComponent(screenSaverDelayChooser);
    }

    /*
     * This is just here to give me a convenient place to preview all the vector icons
     */
    // private void addVectorIcons() {
    // VectorIcon[] icons = new VectorIcon[] {
    // new ArrowHeadIcon(Canvas.LEFT),
    // new CancelIcon(),
    // new CheckBoxIcon(),
    // new ExitIcon(),
    // new PauseIcon(),
    // new PlayIcon(),
    // new ReloadIcon()
    // };
    // int iconSize = 20;
    // for (int i = 0; i < icons.length; i++) {
    // addComponent(new VectorIconComponent(icons[i], iconSize, iconSize));
    // }
    // }

    protected int getValueAsInt(int recordId, int defaultValue) {
        Object retObj = getValue(recordId);
        return (retObj instanceof Integer) ? ((Integer) retObj).intValue() : defaultValue;
    }

    protected Object getValue(int recordId) {
        Object ret = null;
        try {
            ret = getUserPreferences().getValue(recordId);
        } catch (CheckedException e) {
            Log.warn("RMS read error", e);
            ErrorMessageScreen.forMessagekey("rms.read.error",
                    Constants.NAVIGATION_DIRECTION_RELOAD_CURR).show();
        }
        return ret;
    }

    protected ValueSource getUserPreferences() {
        return Registry.getManager().getUserPreferences();
    }

    protected void updateAudioVolume() throws CheckedException {
        AudioPlayer audioPlayer = getAudioPlayer();
        if (audioPlayer != null) {
            int volume = getValueAsInt(Constants.VOLUME_RECORD_ID, Constants.NUMBER_NOT_SET);
            if (volume != Constants.NUMBER_NOT_SET)
                audioPlayer.setVolume(volume);
        }
    }

    public void hideNotify() {
        super.hideNotify();
        try {
            updateAudioVolume();
        } catch (CheckedException e) {
            Log.error("error getting user pref records", e);
        }

    }

    private static class CustomSelectionPanel extends ComponentSelectionPanel {

        private void selectComponent(int i) {
            selectedComponentIdx = i;
        }

    }
}
