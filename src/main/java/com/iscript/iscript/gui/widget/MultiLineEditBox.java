package com.iscript.iscript.gui.widget;

import com.iscript.iscript.gui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import com.iscript.iscript.gui.screen.I18n;
import java.util.regex.Pattern;

public class MultiLineEditBox extends AbstractWidget {
    private final Font font;
    private String value = "";
    private int cursorPos = 0;
    private int selectStart = -1;
    private int scrollOffset = 0;
    private final int maxLength;
    private final List<String> lines = new ArrayList<>();
    private Runnable onValueChanged = null;
    private boolean draggingScroll = false;
    private static final int SCROLLBAR_W = 6;
    private static final int LINE_H = 10;

    private String searchQuery = "";
    private final List<int[]> searchMatches = new ArrayList<>();
    private int currentMatchIndex = -1;
    private boolean showSearch = false;
    private int searchCursor = 0;
    private boolean searchFocused = false;

    private static final int KW_COLOR      = 0xFFFF88FF;
    private static final int API_COLOR     = 0xFF88CCFF;
    private static final int STR_COLOR     = 0xFF88FF88;
    private static final int NUM_COLOR     = 0xFFFFAA66;
    private static final int COMMENT_COLOR = 0xFF666666;
    private static final int DEFAULT_COLOR = Theme.TEXT;
    private static final int OP_COLOR      = 0xFFCCCCCC;

    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "(//.*$)|" +
                    "(\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*')|" +
                    "\\b(function|var|let|const|if|else|for|while|do|switch|case|break|continue|return|new|this|true|false|null|undefined|typeof|instanceof|in|of|async|await|class|extends|import|export|from|try|catch|finally|throw|delete|void|yield|with|debugger)\\b|" +
                    "\\b(api)\\b|" +
                    "(\\b\\d+\\.?\\d*\\b)|" +
                    "([+\\-*/=<>!&|:%]+)|" +
                    "([a-zA-Z_][a-zA-Z0-9_]*)|" +
                    "(\\S)"
    );

    public MultiLineEditBox(Font font, int x, int y, int width, int height, Component title, Component hint) {
        super(x, y, width, height, title);
        this.font = font;
        this.maxLength = 8192;
    }

    public void setOnValueChanged(Runnable callback) {
        this.onValueChanged = callback;
    }

    public void setValue(String text) {
        this.value = text != null ? (text.length() > maxLength ? text.substring(0, maxLength) : text) : "";
        this.cursorPos = this.value.length();
        this.selectStart = -1;
        rebuildLines();
        ensureCursorVisible();
        if (!searchQuery.isEmpty()) findAll(searchQuery);
    }

    public String getValue() {
        return value;
    }

    public void openSearch() {
        showSearch = true;
        searchFocused = true;
        if (selectStart >= 0) {
            int start = Math.min(selectStart, cursorPos);
            int end = Math.max(selectStart, cursorPos);
            searchQuery = value.substring(start, end);
            searchCursor = searchQuery.length();
            findAll(searchQuery);
        } else {
            searchCursor = searchQuery.length();
            if (!searchQuery.isEmpty()) findAll(searchQuery);
        }
    }

    public void closeSearch() {
        showSearch = false;
        searchFocused = false;
        searchMatches.clear();
        currentMatchIndex = -1;
    }

    private void findAll(String query) {
        searchQuery = query;
        searchMatches.clear();
        currentMatchIndex = -1;
        if (query.isEmpty()) return;
        int idx = 0;
        while ((idx = value.toLowerCase().indexOf(query.toLowerCase(), idx)) != -1) {
            searchMatches.add(new int[]{idx, idx + query.length()});
            idx += query.length();
        }
        if (!searchMatches.isEmpty()) {
            currentMatchIndex = 0;
            scrollToMatch(0);
        }
    }

    private void scrollToMatch(int idx) {
        if (idx < 0 || idx >= searchMatches.size()) return;
        int pos = searchMatches.get(idx)[0];
        int line = getLineIndex(pos);
        int visible = Math.max(1, (height - 4) / LINE_H);
        scrollOffset = Math.max(0, line - visible / 2);
        int maxScroll = Math.max(0, lines.size() - visible);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
    }

    public void findNext() {
        if (searchMatches.isEmpty()) return;
        currentMatchIndex = (currentMatchIndex + 1) % searchMatches.size();
        scrollToMatch(currentMatchIndex);
    }

    public void findPrev() {
        if (searchMatches.isEmpty()) return;
        currentMatchIndex = (currentMatchIndex - 1 + searchMatches.size()) % searchMatches.size();
        scrollToMatch(currentMatchIndex);
    }

    private void rebuildLines() {
        lines.clear();
        String[] rawLines = value.split("\n", -1);
        int maxCharsPerLine = Math.max(1, (this.width - 10 - SCROLLBAR_W) / 6);
        for (String raw : rawLines) {
            while (raw.length() > maxCharsPerLine) {
                lines.add(raw.substring(0, maxCharsPerLine));
                raw = raw.substring(maxCharsPerLine);
            }
            lines.add(raw);
        }
        if (lines.isEmpty()) lines.add("");
    }

    private void ensureCursorVisible() {
        int visibleLines = Math.max(1, (height - 4) / LINE_H);
        int cursorLine = getLineIndex(cursorPos);
        if (cursorLine < scrollOffset) scrollOffset = cursorLine;
        if (cursorLine >= scrollOffset + visibleLines) scrollOffset = cursorLine - visibleLines + 1;
        int maxScroll = Math.max(0, lines.size() - visibleLines);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        if (scrollOffset < 0) scrollOffset = 0;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) {
            if (showSearch && mouseY >= getY() - 14 && mouseY <= getY() && mouseX >= getX() && mouseX <= getX() + width) {
                int closeX = getX() + width - 14;
                if (mouseX >= closeX - 2 && mouseX <= closeX + 10) {
                    closeSearch();
                } else {
                    searchFocused = true;
                    setFocused(true);
                }
                return true;
            }
            return false;
        }
        setFocused(true);
        if (showSearch && mouseY > getY()) {
            searchFocused = false;
        }
        int scrollBarX = getX() + width - SCROLLBAR_W;
        if (mouseX >= scrollBarX && mouseX <= scrollBarX + SCROLLBAR_W && mouseY >= getY() + 2 && mouseY <= getY() + height - 2) {
            draggingScroll = true;
            updateScrollFromMouse(mouseY);
            return true;
        }
        selectStart = -1;
        updateCursorFromMouse(mouseX, mouseY);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!isMouseOver(mouseX, mouseY) && !(showSearch && mouseY >= getY() - 14 && mouseY <= getY())) return false;
        int visibleLines = Math.max(1, (height - 4) / LINE_H);
        int maxScroll = Math.max(0, lines.size() - visibleLines);
        if (delta > 0) scrollOffset = Math.max(0, scrollOffset - 3);
        else scrollOffset = Math.min(scrollOffset + 3, maxScroll);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScroll) {
            updateScrollFromMouse(mouseY);
            return true;
        }
        if (!isFocused()) return false;
        if (selectStart < 0) selectStart = cursorPos;
        updateCursorFromMouse(mouseX, mouseY);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScroll = false;
        return false;
    }

    private void updateScrollFromMouse(double mouseY) {
        int visibleLines = Math.max(1, (height - 4) / LINE_H);
        int maxScroll = Math.max(0, lines.size() - visibleLines);
        if (maxScroll <= 0) { scrollOffset = 0; return; }
        int trackY = getY() + 2;
        int trackH = height - 4;
        int thumbH = Math.max(10, (visibleLines * trackH) / lines.size());
        int trackAvail = trackH - thumbH;
        double ratio = (mouseY - trackY) / (double) trackAvail;
        scrollOffset = (int) Math.round(Math.max(0, Math.min(maxScroll, ratio * maxScroll)));
    }

    private void updateCursorFromMouse(double mouseX, double mouseY) {
        int relLine = (int) ((mouseY - getY() - 4) / LINE_H);
        int lineIndex = relLine + scrollOffset;
        if (lineIndex < 0) lineIndex = 0;
        if (lineIndex >= lines.size()) lineIndex = lines.size() - 1;
        String line = lines.get(lineIndex);
        int lineStart = getLineStart(lineIndex);
        int col = 0;
        int x = getX() + 4;
        for (int i = 0; i <= line.length(); i++) {
            int w = font.width(line.substring(0, i));
            if (x + w > mouseX) { col = i; break; }
            col = i;
        }
        cursorPos = lineStart + Math.min(col, line.length());
        ensureCursorVisible();
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        rebuildLines();
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!isFocused()) return false;
        if (showSearch && codePoint == 27) { closeSearch(); return true; }
        if (showSearch && searchFocused) {
            if (codePoint >= 32 && codePoint != 127) {
                searchQuery = searchQuery.substring(0, searchCursor) + codePoint + searchQuery.substring(searchCursor);
                searchCursor++;
                findAll(searchQuery);
                return true;
            }
            return false;
        }
        if (codePoint == 1 || codePoint == 3 || codePoint == 22 || codePoint == 24) return false;
        if (isValidChar(codePoint)) {
            deleteSelection();
            insertChar(codePoint);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFocused()) return false;
        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;

        if (showSearch) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { closeSearch(); return true; }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) { findNext(); return true; }
            if (keyCode == GLFW.GLFW_KEY_UP) { findPrev(); return true; }
            if (keyCode == GLFW.GLFW_KEY_DOWN) { findNext(); return true; }
            if (!shift && keyCode == GLFW.GLFW_KEY_F3) { findNext(); return true; }
            if (shift && keyCode == GLFW.GLFW_KEY_F3) { findPrev(); return true; }
            if (searchFocused) {
                if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                    if (searchCursor > 0) {
                        searchQuery = searchQuery.substring(0, searchCursor - 1) + searchQuery.substring(searchCursor);
                        searchCursor--;
                        findAll(searchQuery);
                    }
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_DELETE) {
                    if (searchCursor < searchQuery.length()) {
                        searchQuery = searchQuery.substring(0, searchCursor) + searchQuery.substring(searchCursor + 1);
                        findAll(searchQuery);
                    }
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_LEFT && searchCursor > 0) { searchCursor--; return true; }
                if (keyCode == GLFW.GLFW_KEY_RIGHT && searchCursor < searchQuery.length()) { searchCursor++; return true; }
                if (keyCode == GLFW.GLFW_KEY_HOME) { searchCursor = 0; return true; }
                if (keyCode == GLFW.GLFW_KEY_END) { searchCursor = searchQuery.length(); return true; }
                return false;
            }
        }

        if (ctrl && keyCode == GLFW.GLFW_KEY_F) { openSearch(); return true; }

        if (ctrl && keyCode == GLFW.GLFW_KEY_A) { selectStart = 0; cursorPos = value.length(); return true; }
        if (ctrl && keyCode == GLFW.GLFW_KEY_C) {
            if (selectStart >= 0) {
                int start = Math.min(selectStart, cursorPos);
                int end = Math.max(selectStart, cursorPos);
                net.minecraft.client.Minecraft.getInstance().keyboardHandler.setClipboard(value.substring(start, end));
            }
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_X) {
            if (selectStart >= 0) {
                int start = Math.min(selectStart, cursorPos);
                int end = Math.max(selectStart, cursorPos);
                net.minecraft.client.Minecraft.getInstance().keyboardHandler.setClipboard(value.substring(start, end));
                deleteSelection();
            }
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_V) {
            deleteSelection();
            String clipboard = net.minecraft.client.Minecraft.getInstance().keyboardHandler.getClipboard();
            for (char c : clipboard.toCharArray()) if (isValidChar(c)) insertChar(c);
            return true;
        }

        if (ctrl && keyCode == GLFW.GLFW_KEY_HOME) {
            if (shift) { if (selectStart < 0) selectStart = cursorPos; } else { selectStart = -1; }
            cursorPos = 0; ensureCursorVisible(); return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_END) {
            if (shift) { if (selectStart < 0) selectStart = cursorPos; } else { selectStart = -1; }
            cursorPos = value.length(); ensureCursorVisible(); return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_LEFT) {
            if (shift) { if (selectStart < 0) selectStart = cursorPos; } else { selectStart = -1; }
            cursorPos = moveWordLeft(cursorPos); ensureCursorVisible(); return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_RIGHT) {
            if (shift) { if (selectStart < 0) selectStart = cursorPos; } else { selectStart = -1; }
            cursorPos = moveWordRight(cursorPos); ensureCursorVisible(); return true;
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (selectStart >= 0) deleteSelection();
            else if (cursorPos > 0) { value = value.substring(0, cursorPos - 1) + value.substring(cursorPos); cursorPos--; rebuildLines(); if (onValueChanged != null) onValueChanged.run(); }
            ensureCursorVisible();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (selectStart >= 0) deleteSelection();
            else if (cursorPos < value.length()) { value = value.substring(0, cursorPos) + value.substring(cursorPos + 1); rebuildLines(); if (onValueChanged != null) onValueChanged.run(); }
            ensureCursorVisible();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            if (shift) { if (selectStart < 0) selectStart = cursorPos; } else { selectStart = -1; }
            if (cursorPos > 0) cursorPos--; ensureCursorVisible(); return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            if (shift) { if (selectStart < 0) selectStart = cursorPos; } else { selectStart = -1; }
            if (cursorPos < value.length()) cursorPos++; ensureCursorVisible(); return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            if (shift) { if (selectStart < 0) selectStart = cursorPos; } else { selectStart = -1; }
            moveCursorUp(); ensureCursorVisible(); return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            if (shift) { if (selectStart < 0) selectStart = cursorPos; } else { selectStart = -1; }
            moveCursorDown(); ensureCursorVisible(); return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            deleteSelection(); insertChar('\n'); ensureCursorVisible(); return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            if (shift) { if (selectStart < 0) selectStart = cursorPos; } else { selectStart = -1; }
            cursorPos = getLineStart(getLineIndex(cursorPos)); ensureCursorVisible(); return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            if (shift) { if (selectStart < 0) selectStart = cursorPos; } else { selectStart = -1; }
            int li = getLineIndex(cursorPos);
            cursorPos = getLineStart(li) + lines.get(li).length(); ensureCursorVisible(); return true;
        }
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            deleteSelection(); insertChar('\t'); ensureCursorVisible(); return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { if (showSearch) { closeSearch(); return true; } setFocused(false); return true; }

        return false;
    }

    private int moveWordLeft(int pos) {
        if (pos <= 0) return 0;
        pos--;
        while (pos > 0 && !isWordChar(value.charAt(pos))) pos--;
        while (pos > 0 && isWordChar(value.charAt(pos - 1))) pos--;
        return pos;
    }

    private int moveWordRight(int pos) {
        if (pos >= value.length()) return value.length();
        while (pos < value.length() && !isWordChar(value.charAt(pos))) pos++;
        while (pos < value.length() && isWordChar(value.charAt(pos))) pos++;
        return pos;
    }

    private boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private void deleteSelection() {
        if (selectStart < 0) return;
        int start = Math.min(selectStart, cursorPos);
        int end = Math.max(selectStart, cursorPos);
        value = value.substring(0, start) + value.substring(end);
        cursorPos = start;
        selectStart = -1;
        rebuildLines();
        if (onValueChanged != null) onValueChanged.run();
    }

    private void moveCursorUp() {
        int li = getLineIndex(cursorPos);
        if (li <= 0) { cursorPos = 0; return; }
        int col = getColumnInLine(cursorPos);
        int prevStart = getLineStart(li - 1);
        int prevLen = lines.get(li - 1).length();
        cursorPos = prevStart + Math.min(col, prevLen);
    }

    private void moveCursorDown() {
        int li = getLineIndex(cursorPos);
        if (li >= lines.size() - 1) { cursorPos = value.length(); return; }
        int col = getColumnInLine(cursorPos);
        int nextStart = getLineStart(li + 1);
        int nextLen = lines.get(li + 1).length();
        cursorPos = nextStart + Math.min(col, nextLen);
    }

    private int getLineIndex(int pos) {
        int count = 0;
        for (int i = 0; i < lines.size(); i++) {
            count += lines.get(i).length() + 1;
            if (count > pos) return i;
        }
        return lines.size() - 1;
    }

    private int getColumnInLine(int pos) {
        int count = 0;
        for (String line : lines) {
            if (count + line.length() >= pos) return pos - count;
            count += line.length() + 1;
        }
        return 0;
    }

    private int getLineStart(int lineIndex) {
        int count = 0;
        for (int i = 0; i < lineIndex; i++) count += lines.get(i).length() + 1;
        return count;
    }

    private void insertChar(char c) {
        if (value.length() >= maxLength) return;
        value = value.substring(0, cursorPos) + c + value.substring(cursorPos);
        cursorPos++;
        rebuildLines();
        if (onValueChanged != null) onValueChanged.run();
    }

    private boolean isValidChar(char c) {
        return (c >= 32 && c != 127) || c == '\n' || c == '\t';
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int bgX = getX();
        int bgY = getY();
        int bgW = width;
        int bgH = height;

        if (showSearch) {
            g.fill(bgX, bgY - 14, bgX + bgW, bgY, 0xFF222233);
            g.renderOutline(bgX, bgY - 14, bgW, 14, searchFocused ? Theme.ACCENT : Theme.BORDER);
            String searchLabel = I18n.s("iscript.script.editor.find") + ": " + searchQuery.substring(0, searchCursor) + "|" + searchQuery.substring(searchCursor);
            g.drawString(font, searchLabel, bgX + 4, bgY - 11, 0xFFFFFFFF);
            String count;
            if (searchMatches.isEmpty()) {
                count = searchQuery.isEmpty() ? "" : " 0/0";
            } else {
                count = " " + (currentMatchIndex + 1) + "/" + searchMatches.size();
            }
            if (!count.isEmpty()) {
                g.drawString(font, count, bgX + bgW - font.width(count) - 18, bgY - 11, Theme.TEXT_DIM);
            }
            String closeLabel = "×";
            int closeX = bgX + bgW - 14;
            boolean closeHovered = mouseX >= closeX - 2 && mouseX <= closeX + 10 && mouseY >= bgY - 14 && mouseY <= bgY;
            g.drawString(font, closeLabel, closeX, bgY - 11, closeHovered ? Theme.ERROR : 0xFFAAAAAA);
        }

        g.fill(bgX, bgY, bgX + bgW, bgY + bgH, Theme.BG_INNER);
        g.renderOutline(bgX, bgY, bgW, bgH, isFocused() ? Theme.ACCENT : Theme.BORDER);

        int visibleLines = Math.max(1, (bgH - 4) / LINE_H);
        int textW = bgW - 4 - SCROLLBAR_W;

        int scrollBarX = bgX + bgW - SCROLLBAR_W;
        int trackY = bgY + 2;
        int trackH = bgH - 4;
        g.fill(scrollBarX, trackY, scrollBarX + SCROLLBAR_W, trackY + trackH, Theme.BG_INNER);
        g.renderOutline(scrollBarX, trackY, SCROLLBAR_W, trackH, Theme.BORDER);

        int maxScroll = Math.max(0, lines.size() - visibleLines);
        if (maxScroll > 0) {
            int thumbH = Math.max(10, (visibleLines * trackH) / lines.size());
            int trackAvail = trackH - thumbH;
            int thumbY = trackY + (scrollOffset * trackAvail) / maxScroll;
            g.fill(scrollBarX, thumbY, scrollBarX + SCROLLBAR_W, thumbY + thumbH, Theme.TEXT_DIM);
        }

        int cursorLine = getLineIndex(cursorPos);

        int y = bgY + 4;
        for (int i = scrollOffset; i < Math.min(lines.size(), scrollOffset + visibleLines); i++) {
            String line = lines.get(i);
            int lineStart = getLineStart(i);
            int lineEnd = lineStart + line.length();

            if (i == cursorLine && isFocused()) {
                g.fill(bgX + 2, y - 1, bgX + bgW - SCROLLBAR_W - 2, y + LINE_H + 1, 0xFF1A1A2E);
            }

            if (selectStart >= 0) {
                int selStart = Math.min(selectStart, cursorPos);
                int selEnd = Math.max(selectStart, cursorPos);
                if (selEnd > lineStart && selStart < lineEnd) {
                    int ss = Math.max(selStart, lineStart) - lineStart;
                    int se = Math.min(selEnd, lineEnd) - lineStart;
                    String before = line.substring(0, Math.max(0, ss));
                    String selected = line.substring(Math.max(0, ss), Math.min(se, line.length()));
                    int x1 = bgX + 4 + font.width(before);
                    int x2 = x1 + font.width(selected);
                    g.fill(x1, y, x2, y + 9, 0xFF3366AA);
                }
            }

            if (!searchQuery.isEmpty()) {
                String lowerLine = line.toLowerCase();
                String lowerQuery = searchQuery.toLowerCase();
                int idx = 0;
                while ((idx = lowerLine.indexOf(lowerQuery, idx)) != -1) {
                    int absStart = lineStart + idx;
                    int absEnd = absStart + searchQuery.length();
                    boolean isCurrent = currentMatchIndex >= 0 && searchMatches.get(currentMatchIndex)[0] == absStart;
                    String before = line.substring(0, idx);
                    String match = line.substring(idx, idx + searchQuery.length());
                    int x1 = bgX + 4 + font.width(before);
                    int x2 = x1 + font.width(match);
                    g.fill(x1, y, x2, y + 9, isCurrent ? 0xFFFFAA00 : 0xFF665522);
                    idx += searchQuery.length();
                }
            }

            renderHighlightedLine(g, line, bgX + 4, y);
            y += LINE_H;
        }

        if (isFocused()) {
            int relLine = cursorLine - scrollOffset;
            if (relLine >= 0 && relLine < visibleLines) {
                int col = getColumnInLine(cursorPos);
                String lineText = lines.get(cursorLine);
                int cursorX = bgX + 4 + font.width(lineText.substring(0, Math.min(col, lineText.length())));
                int cursorY = bgY + 4 + relLine * LINE_H;
                g.fill(cursorX, cursorY, cursorX + 1, cursorY + 9, 0xFFFFFFFF);
            }
        }
    }

    private void renderHighlightedLine(GuiGraphics g, String line, int x, int y) {
        if (line.trim().startsWith("//")) {
            g.drawString(font, line, x, y, COMMENT_COLOR);
            return;
        }
        Matcher m = TOKEN_PATTERN.matcher(line);
        int lastEnd = 0;
        while (m.find()) {
            if (m.start() > lastEnd) {
                g.drawString(font, line.substring(lastEnd, m.start()), x, y, DEFAULT_COLOR);
                x += font.width(line.substring(lastEnd, m.start()));
            }
            int color = DEFAULT_COLOR;
            if (m.group(1) != null) color = COMMENT_COLOR;
            else if (m.group(2) != null) color = STR_COLOR;
            else if (m.group(3) != null) color = KW_COLOR;
            else if (m.group(4) != null) color = API_COLOR;
            else if (m.group(5) != null) color = NUM_COLOR;
            else if (m.group(6) != null) color = OP_COLOR;
            else if (m.group(7) != null) color = DEFAULT_COLOR;
            else if (m.group(8) != null) color = DEFAULT_COLOR;

            String token = m.group();
            g.drawString(font, token, x, y, color);
            x += font.width(token);
            lastEnd = m.end();
        }
        if (lastEnd < line.length()) {
            g.drawString(font, line.substring(lastEnd), x, y, DEFAULT_COLOR);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
        out.add(NarratedElementType.TITLE, Component.literal(value));
    }
}