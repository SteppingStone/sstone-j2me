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

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Image;

import org.edc.sstone.Constants;
import org.edc.sstone.event.MenuEvent;
import org.edc.sstone.event.MenuListener;
import org.edc.sstone.j2me.component.LetterReaderComponent;
import org.edc.sstone.j2me.component.SimpleTokenReaderComponent;
import org.edc.sstone.j2me.component.SyllableReaderComponent;
import org.edc.sstone.j2me.component.WordReaderComponent;
import org.edc.sstone.j2me.core.Registry;
import org.edc.sstone.j2me.screen.AppScreen;
import org.edc.sstone.j2me.screen.AudioPlayerScreen;
import org.edc.sstone.j2me.screen.QuestionAnswerSelectionListener;
import org.edc.sstone.j2me.screen.UserPreferencesScreen;
import org.edc.sstone.j2me.ui.component.AnimatedComponentPanel;
import org.edc.sstone.j2me.ui.component.AnimationReplayListener;
import org.edc.sstone.j2me.ui.component.Component;
import org.edc.sstone.j2me.ui.component.ComponentContentPanel;
import org.edc.sstone.j2me.ui.component.ComponentSelectionPanel;
import org.edc.sstone.j2me.ui.component.ImagePanel;
import org.edc.sstone.j2me.ui.component.MenuItemComponent;
import org.edc.sstone.j2me.ui.component.TextArea;
import org.edc.sstone.j2me.ui.icon.ArrowHeadIcon;
import org.edc.sstone.j2me.ui.icon.ExitIcon;
import org.edc.sstone.j2me.ui.icon.ReloadIcon;
import org.edc.sstone.j2me.ui.menu.MenuButton;
import org.edc.sstone.j2me.ui.menu.MenuItem;
import org.edc.sstone.j2me.ui.screen.ComponentScreen;
import org.edc.sstone.j2me.ui.style.ComponentStyle;
import org.edc.sstone.j2me.ui.style.Style;
import org.edc.sstone.record.reader.model.ComponentRecord;
import org.edc.sstone.record.reader.model.MenuItemRecord;
import org.edc.sstone.record.reader.model.QuestionRecord;
import org.edc.sstone.record.reader.model.ResourceComponentRecord;
import org.edc.sstone.record.reader.model.ScreenRecord;
import org.edc.sstone.record.reader.model.StyleRecord;
import org.edc.sstone.record.reader.model.TextAreaComponentRecord;
import org.edc.sstone.text.Alphabet;
import org.edc.sstone.util.StdLib;
/**
 * @author Greg Orlowski
 */
public class ScreenFactory {

    private final Alphabet alphabet;
    private final ScreenNavigator nav;

    public ScreenFactory(ScreenNavigator nav, Alphabet alphabet) {
        this.nav = nav;
        this.alphabet = alphabet;
    }

    /**
     * 
     * NOTE: Do not configure the screen navigation buttons here.
     * 
     * @param screenRecord
     * @return
     */
    public ComponentScreen newScreen(ScreenRecord screenRecord) {
        ComponentScreen ret = null;

        Style style = newStyle(screenRecord.styleRecord);

        switch (screenRecord.subType) {
            case ScreenRecord.SUBTYPE_MENU_SCREEN:
                ret = new AppScreen(style, new ComponentSelectionPanel());
                break;

            case ScreenRecord.SUBTYPE_ANIMATED_SCREEN:
                AnimatedComponentPanel componentPanel = new AnimatedComponentPanel();
                componentPanel.setAllowUserScrolling(false); // we do not want users to be able to
                                                             // scroll
                ret = new AppScreen(style, componentPanel);
                ret.addMenuItem(MenuItem.iconItem(
                        new AnimationReplayListener(componentPanel), new ReloadIcon()),
                        MenuButton.CENTER);
                ret.getMenuButton(MenuButton.CENTER).setEnabled(false);
                break;

            case ScreenRecord.SUBTYPE_AUDIO_SCREEN:
                AudioPlayerScreen aps = new AudioPlayerScreen(style, new ComponentContentPanel(),
                        screenRecord.isAutoAdvance(), screenRecord.getAutoAdvanceDelayMs());
                aps.setTrackName(screenRecord.resourcePath);
                ret = aps;
                break;

            case ScreenRecord.SUBTYPE_QUESTION_SCREEN:
                /*
                 * NOTE: we do NOT want to suppress prev/next buttons on question slides to force
                 * users to answer a question. If we want something like that, we can do it later
                 * with a special screen record subtype
                 */
                ret = new AppScreen(style, new ComponentSelectionPanel());
                break;

            case ScreenRecord.SUBTYPE_USER_PREFERENCES_SCREEN:
                ret = new UserPreferencesScreen();
                break;

            // default is content screen
            case ScreenRecord.SUBTYPE_CONTENT_SCREEN:
            default:
                ret = new AppScreen(style, new ComponentContentPanel());
                break;

        }
        ret.setTitle(screenRecord.title);
        addComponents(ret, screenRecord);
        addNavButtons(ret);
        return ret;
    }

    protected void addNavButtons(ComponentScreen appScreen) {
        if (nav.hasPrevious()) {
            appScreen.addMenuItem(MenuItem.iconItem(
                    new NavigationEventListener(nav, Constants.NAVIGATION_DIRECTION_PREVIOUS), new ArrowHeadIcon(
                            Canvas.LEFT)),
                    MenuButton.LEFT);
        }
        // always add exit button
        appScreen.addMenuItem(MenuItem.iconItem(
                new NavigationEventListener(nav, Constants.NAVIGATION_DIRECTION_UP), new ExitIcon()),
                MenuButton.LEFT);

        // an easy place to see vector icons
        // new NavigationEventListener(nav, Canvas.UP), new PlayIcon()),

        if (nav.hasNext()) {
            appScreen.addMenuItem(MenuItem.iconItem(
                    new NavigationEventListener(nav, Constants.NAVIGATION_DIRECTION_NEXT), new ArrowHeadIcon(
                            Canvas.RIGHT)),
                    MenuButton.RIGHT);
        }
    }

    protected NavigationEventListener navEvent(byte direction) {
        return new NavigationEventListener(nav, direction);
    }

    private void addComponents(ComponentScreen appScreen, ScreenRecord screenRecord) {
        ComponentRecord componentRecord = null;
        for (int i = 0; i < screenRecord.componentRecords.size(); i++) {
            componentRecord = (ComponentRecord) screenRecord.componentRecords.elementAt(i);
            addComponent(appScreen, screenRecord, componentRecord);

            // Component component = newComponent(screenRecord,
            // );
            // if (component != null) {
            // appScreen.addComponent(component);
            // }
        }
    }

    protected void addComponent(ComponentScreen componentScreen, ScreenRecord screenRecord,
            ComponentRecord componentRecord) {
        Component component = null;
        Style componentStyle = null;

        /*
         * If the component record has a null style record, set its style to null and it will just
         * use the screen's style. This is better than instantiating equivalent ComponentStyle
         * instances for every component (unless we need to).
         */
        if (componentRecord.styleRecord != null) {
            componentRecord.styleRecord.setDefaults(screenRecord.styleRecord);
            componentStyle = newStyle(componentRecord.styleRecord);
        }

        switch (componentRecord.getClassUID()) {
            case TextAreaComponentRecord.CLASS_UID:
                component = newTextAreaComponent((TextAreaComponentRecord) componentRecord, componentStyle);
                break;

            case MenuItemRecord.CLASS_UID:
                component = newMenuItemComponent((MenuItemRecord) componentRecord, componentStyle);
                break;

            case ResourceComponentRecord.CLASS_UID:
                component = newResourceComponent((ResourceComponentRecord) componentRecord, componentStyle);
                break;

            // QuestionRecord is a special case. We add a bunch of menu components
            case QuestionRecord.CLASS_UID:
                QuestionRecord qr = (QuestionRecord) componentRecord;
                Component questionText = new TextArea(qr.question, componentStyle, getViewportWidth());
                componentScreen.addComponent(questionText);
                int viewportWidth = getViewportWidth();
                for (int i = 0; i < qr.answers.length; i++) {
                    MenuListener menuListener = new QuestionAnswerSelectionListener(qr, i);
                    MenuItemComponent answerChoice = new MenuItemComponent((i + 1) + ") " + qr.answers[i],
                            menuListener, viewportWidth);
                    componentScreen.addComponent(answerChoice);
                }
                component = null;
                break;
        }

        if (component != null) {
            componentScreen.addComponent(component);
        }
    }

    private Component newResourceComponent(ResourceComponentRecord componentRecord, Style componentStyle) {
        switch (componentRecord.subType) {
            case ResourceComponentRecord.SUBTYPE_IMAGE_PANEL:
                Image image = null;
                InputStream imageIn = null;
                try {
                    imageIn = Registry.getManager().getResourceProvider()
                            .loadImage("panels/" + componentRecord.resourcePath);
                    image = Image.createImage(imageIn);
                    return new ImagePanel(componentStyle, image);
                } catch (IOException e) {
                    // TODO handle image-load IOException
                } finally {
                    if (imageIn != null) {
                        try {
                            imageIn.close();
                        } catch (IOException ignoreCloseFailure) {
                        }
                    }
                }
                break;
        }
        return null;
    }

    // TODO: add icons to MenuItemComponent + show icons here
    private Component newMenuItemComponent(final MenuItemRecord menuItemRecord, Style componentStyle) {
        return new MenuItemComponent(componentStyle, menuItemRecord.title, new MenuListener() {
            public void menuSelected(MenuEvent e) {
                nav.descend(menuItemRecord);
            }
        }, getViewportWidth());
    }

    protected TextArea newTextAreaComponent(TextAreaComponentRecord componentRecord, Style style) {
        SimpleTokenReaderComponent ret = null;
        switch (componentRecord.subType) {
            case TextAreaComponentRecord.SUBTYPE_LETTER_READER:
                ret = new LetterReaderComponent(componentRecord.text, getAlphabet(), style, getViewportWidth());
                break;
            case TextAreaComponentRecord.SUBTYPE_WORD_READER:
                ret = new WordReaderComponent(componentRecord.text, getAlphabet(), style, getViewportWidth());
                break;
            case TextAreaComponentRecord.SUBTYPE_SYLLABLE_READER:
                /*
                 * it is fine to cast byte to char. When we save the record we force
                 * syllableSeparator to be a displayable character with a byte value < 127
                 */
                ret = new SyllableReaderComponent(componentRecord.text, getAlphabet(), style,
                        getViewportWidth(), (char) componentRecord.syllableSeparator);
                break;
            case TextAreaComponentRecord.SUBTYPE_TEXT_AREA:
                return new TextArea(componentRecord.text, style, getViewportWidth());
        }
        if (ret != null) {
            ret.setPlayAudio(!componentRecord.isSuppressAudio());
            ret.setReadNonLetters(componentRecord.isReadNonLetters());
        }
        return ret;
    }

    protected int getViewportWidth() {
        return Registry.getManager().getTheme().getContentWidth();
    }

    protected Style newStyle(StyleRecord styleRecord) {

        if (styleRecord == null || styleRecord.isNull())
            return null;

        ComponentStyle ret = new ComponentStyle(Registry.getManager().getTheme());

        if (StdLib.isSet(styleRecord.backgroundColor))
            ret.setBackgroundColor(styleRecord.backgroundColor);

        if (StdLib.isSet(styleRecord.fontColor))
            ret.setFontColor(styleRecord.fontColor);

        if (StdLib.isSet(styleRecord.highlightColor))
            ret.setHighlightColor(styleRecord.highlightColor);

        if (styleRecord.fontStyle != null) {
            ret.setFontStyle(styleRecord.fontStyle);
        }

        if (StdLib.isSet(styleRecord.lineHeight))
            ret.setLineHeightByte(styleRecord.lineHeight);

        if (styleRecord.margin != null)
            ret.setMargin(styleRecord.margin);

        if (StdLib.isSet(styleRecord.padding))
            ret.setPadding(styleRecord.padding);

        // anchor
        if (StdLib.isSet(styleRecord.textAnchor))
            ret.setTextAnchor(styleRecord.textAnchor);

        if (StdLib.isSet(styleRecord.componentAnchor))
            ret.setAnchor(styleRecord.componentAnchor);

        // animation delay
        if (StdLib.isSet(styleRecord.animationStartDelay))
            ret.setAnimationStartDelay(styleRecord.animationStartDelay);

        if (StdLib.isSet(styleRecord.animationPeriod))
            ret.setAnimationPeriod(styleRecord.animationPeriod);

        return ret;
    }

    private Alphabet getAlphabet() {
        return alphabet;
    }
}
