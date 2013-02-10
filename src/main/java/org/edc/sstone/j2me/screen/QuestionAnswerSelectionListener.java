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

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;

import org.edc.sstone.Constants;
import org.edc.sstone.event.MenuEvent;
import org.edc.sstone.event.MenuListener;
import org.edc.sstone.il8n.MessageSource;
import org.edc.sstone.j2me.core.DeviceScreen;
import org.edc.sstone.j2me.core.Registry;
import org.edc.sstone.j2me.nav.NavigationEventListener;
import org.edc.sstone.j2me.ui.component.ComponentContentPanel;
import org.edc.sstone.j2me.ui.component.TextArea;
import org.edc.sstone.j2me.ui.icon.ArrowHeadIcon;
import org.edc.sstone.j2me.ui.menu.MenuButton;
import org.edc.sstone.j2me.ui.menu.MenuItem;
import org.edc.sstone.j2me.ui.style.ComponentStyle;
import org.edc.sstone.j2me.ui.style.Style;
import org.edc.sstone.nav.ScreenNavigation;
import org.edc.sstone.record.reader.model.QuestionRecord;
import org.edc.sstone.ui.model.FixedSpacing;
import org.edc.sstone.ui.model.FontStyle;

/**
 * @author Greg Orlowski
 */
public class QuestionAnswerSelectionListener implements MenuListener {

    private final QuestionRecord questionRecord;
    private final int idx;

    public QuestionAnswerSelectionListener(QuestionRecord questionRecord, int idx) {
        this.questionRecord = questionRecord;
        this.idx = idx;
    }

    public void menuSelected(MenuEvent e) {
        final DeviceScreen currentScreen = Registry.getManager().getScreen();
        final Style currentScreenStyle = currentScreen.getStyle();

        int contentWidth = Registry.getManager().getTheme().getContentWidth();

        MessageSource ms = Registry.getManager().getMessageSource();
        AppScreen answerScreen = new AppScreen(currentScreenStyle, new ComponentContentPanel());

        answerScreen.setTitle(currentScreen.getTitle());
        int correctAnswerIdx = questionRecord.getCorrectAnswerIndex();

        String notice = ms.getString(isCorrectAnswer()
                ? "answer.screen.correct.answer.title"
                : "answer.screen.incorrect.answer.title");

        final FontStyle currFontStyle = currentScreenStyle.getFontStyle();

        final ComponentStyle noticeStyle = new ComponentStyle(currentScreenStyle);
        final FontStyle noticeFontStyle = new FontStyle(currFontStyle.getFace(),
                Constants.FONT_STYLE_BOLD,
                Constants.FONT_SIZE_LARGE);
        noticeStyle.setFontStyle(noticeFontStyle);
        noticeStyle.setAnchor(Graphics.HCENTER | Graphics.TOP);
        noticeStyle.setBackgroundColor(currentScreenStyle.getHighlightColor());

        int largeFontHeight = Registry.getManager().getFontFactory().getFont(noticeFontStyle).getHeight();
        short padding = (short) (largeFontHeight / 4);
        noticeStyle.setPadding(padding);
        // set top/bottom margin to 2xpadding
        noticeStyle.setMargin(new FixedSpacing((short) (padding * 2), (short) 0));
        answerScreen.addComponent(new TextArea(notice, noticeStyle, contentWidth));

        // TODO: add margin between Q&A
        answerScreen.addComponent(new TextArea(questionRecord.question, contentWidth));

        if (!isCorrectAnswer()) {
            ComponentStyle componentStyle = new ComponentStyle(currentScreenStyle);

            FontStyle strikeThroughFont = new FontStyle(currFontStyle.getFace(),
                    (byte) (currFontStyle.getStyle() | Constants.FONT_STYLE_STRIKETHROUGH), currFontStyle.getSize());
            componentStyle.setFontStyle(strikeThroughFont);

            TextArea wrongAnswer = new TextArea((idx + 1) + ") " + questionRecord.answers[idx],
                    componentStyle, contentWidth);
            answerScreen.addComponent(wrongAnswer);
        }

        TextArea correctAnswer = new TextArea((correctAnswerIdx + 1) + ") " + questionRecord.answers[correctAnswerIdx],
                contentWidth);
        answerScreen.addComponent(correctAnswer);

        // Forward + Back buttons
        final ScreenNavigation nav = Registry.getManager().getScreenNavigation();
        answerScreen.addMenuItem(MenuItem.iconItem(new NavigationEventListener(nav,
                Constants.NAVIGATION_DIRECTION_RELOAD_CURR), new ArrowHeadIcon(
                Canvas.LEFT)), MenuButton.LEFT);

        answerScreen.addMenuItem(MenuItem.iconItem(new NavigationEventListener(nav,
                Constants.NAVIGATION_DIRECTION_NEXT), new ArrowHeadIcon(
                Canvas.RIGHT)), MenuButton.RIGHT);

        answerScreen.show();
    }

    private boolean isCorrectAnswer() {
        return questionRecord.getCorrectAnswerIndex() == idx;
    }

}
