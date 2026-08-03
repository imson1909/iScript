package com.iscript.iscript.gui.widget;

import com.iscript.iscript.gui.screen.I18n;
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
import java.util.regex.Pattern;

public class MultiLineEditBox extends AbstractWidget {
    private final Font font;
    private String value = "";
    private int cursorPos = 0;
    private int selectStart = -1;
    private int scrollOffset = 0;
    private int horizontalScrollOffset = 0;
    private final int maxLength;
    private final List<String> lines = new ArrayList<>();
    private Runnable onValueChanged = null;
    private boolean draggingScroll = false;
    private boolean draggingHScroll = false;
    private static final int SCROLLBAR_W = 6;
    private static final int H_SCROLLBAR_H = 6;
    private static final int LINE_H = 10;

    private String searchQuery = "";
    private final List<int[]> searchMatches = new ArrayList<>();
    private int currentMatchIndex = -1;
    private boolean showSearch = false;
    private int searchCursor = 0;
    private boolean searchFocused = false;

    private final List<String> undoStack = new ArrayList<>();
    private final List<Integer> undoCursorStack = new ArrayList<>();
    private final List<Integer> undoSelectStack = new ArrayList<>();
    private int undoIndex = -1;
    private static final int MAX_UNDO = 100;
    private boolean isUndoing = false;

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
        this.horizontalScrollOffset = 0;
        rebuildLines();
        ensureCursorVisible();
        if (!searchQuery.isEmpty()) findAll(searchQuery);
        undoStack.clear();
        undoCursorStack.clear();
        undoSelectStack.clear();
        undoStack.add(this.value);
        undoCursorStack.add(this.cursorPos);
        undoSelectStack.add(this.selectStart);
        undoIndex = 0;
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
        for (String raw : rawLines) {
            lines.add(raw);
        }
        if (lines.isEmpty()) lines.add("");
    }

    private int getMaxLineWidth() {
        int max = 0;
        for (String line : lines) {
            int w = font.width(line);
            if (w > max) max = w;
        }
        return max;
    }

    private void ensureCursorVisible() {
        int visibleLines = Math.max(1, (height - 4) / LINE_H);
        int cursorLine = getLineIndex(cursorPos);
        if (cursorLine < scrollOffset) scrollOffset = cursorLine;
        if (cursorLine >= scrollOffset + visibleLines) scrollOffset = cursorLine - visibleLines + 1;
        int maxScroll = Math.max(0, lines.size() - visibleLines);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        if (scrollOffset < 0) scrollOffset = 0;

        int contentW = width - 8 - SCROLLBAR_W;
        int cursorLineIdx = getLineIndex(cursorPos);
        String line = lines.get(cursorLineIdx);
        int col = getColumnInLine(cursorPos);
        String beforeCursor = line.substring(0, Math.min(col, line.length()));
        int cursorX = font.width(beforeCursor);
        if (cursorX < horizontalScrollOffset) {
            horizontalScrollOffset = Math.max(0, cursorX - 4);
        }
        if (cursorX > horizontalScrollOffset + contentW - 4) {
            horizontalScrollOffset = cursorX - contentW + 4;
        }
        int maxLineW = getMaxLineWidth();
        int maxHScroll = Math.max(0, maxLineW - contentW);
        if (horizontalScrollOffset > maxHScroll) horizontalScrollOffset = maxHScroll;
        if (horizontalScrollOffset < 0) horizontalScrollOffset = 0;
    }

    private long lastClickTime = 0;
    private double lastClickX = 0;
    private double lastClickY = 0;

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

        int contentW = width - 8 - SCROLLBAR_W;
        int maxLineW = getMaxLineWidth();
        boolean needsHScroll = maxLineW > contentW;
        int vTrackH = height - 4 - (needsHScroll ? H_SCROLLBAR_H : 0);

        int scrollBarX = getX() + width - SCROLLBAR_W;
        if (mouseX >= scrollBarX && mouseX <= scrollBarX + SCROLLBAR_W && mouseY >= getY() + 2 && mouseY <= getY() + 2 + vTrackH) {
            draggingScroll = true;
            updateScrollFromMouse(mouseY);
            return true;
        }

        if (needsHScroll) {
            int hTrackX = getX() + 4;
            int hTrackY = getY() + height - H_SCROLLBAR_H - 2;
            int hTrackW = width - 8 - SCROLLBAR_W;
            if (mouseX >= hTrackX && mouseX <= hTrackX + hTrackW && mouseY >= hTrackY && mouseY <= hTrackY + H_SCROLLBAR_H) {
                draggingHScroll = true;
                updateHScrollFromMouse(mouseX);
                return true;
            }
        }

        long now = System.currentTimeMillis();
        boolean isDoubleClick = (now - lastClickTime < 300) &&
                Math.abs(mouseX - lastClickX) < 5 && Math.abs(mouseY - lastClickY) < 5;
        lastClickTime = now;
        lastClickX = mouseX;
        lastClickY = mouseY;

        if (isDoubleClick) {
            selectStart = moveWordLeft(cursorPos);
            cursorPos = moveWordRight(cursorPos);
            ensureCursorVisible();
            return true;
        }

        selectStart = -1;
        updateCursorFromMouse(mouseX, mouseY);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!isMouseOver(mouseX, mouseY) && !(showSearch && mouseY >= getY() - 14 && mouseY <= getY())) return false;
        boolean shift = (org.lwjgl.glfw.GLFW.glfwGetKey(net.minecraft.client.Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS)
                || (org.lwjgl.glfw.GLFW.glfwGetKey(net.minecraft.client.Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS);
        if (shift) {
            int contentW = width - 8 - SCROLLBAR_W;
            int maxLineW = getMaxLineWidth();
            int maxHScroll = Math.max(0, maxLineW - contentW);
            if (delta > 0) horizontalScrollOffset = Math.max(0, horizontalScrollOffset - 30);
            else horizontalScrollOffset = Math.min(horizontalScrollOffset + 30, maxHScroll);
            return true;
        }
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
        if (draggingHScroll) {
            updateHScrollFromMouse(mouseX);
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
        draggingHScroll = false;
        return false;
    }

    private void updateScrollFromMouse(double mouseY) {
        int contentW = width - 8 - SCROLLBAR_W;
        int maxLineW = getMaxLineWidth();
        boolean needsHScroll = maxLineW > contentW;
        int vTrackH = height - 4 - (needsHScroll ? H_SCROLLBAR_H : 0);
        int visibleLines = Math.max(1, (height - 4 - (needsHScroll ? H_SCROLLBAR_H : 0)) / LINE_H);
        int maxScroll = Math.max(0, lines.size() - visibleLines);
        if (maxScroll <= 0) { scrollOffset = 0; return; }
        int trackY = getY() + 2;
        int thumbH = Math.max(10, (visibleLines * vTrackH) / lines.size());
        int trackAvail = vTrackH - thumbH;
        double ratio = (mouseY - trackY) / (double) trackAvail;
        scrollOffset = (int) Math.round(Math.max(0, Math.min(maxScroll, ratio * maxScroll)));
    }

    private void updateHScrollFromMouse(double mouseX) {
        int contentW = width - 8 - SCROLLBAR_W;
        int maxLineW = getMaxLineWidth();
        int maxHScroll = Math.max(0, maxLineW - contentW);
        if (maxHScroll <= 0) { horizontalScrollOffset = 0; return; }
        int hTrackX = getX() + 4;
        int hTrackW = width - 8 - SCROLLBAR_W;
        int hThumbW = Math.max(10, (contentW * hTrackW) / maxLineW);
        int trackAvail = hTrackW - hThumbW;
        double ratio = (mouseX - hTrackX) / (double) trackAvail;
        horizontalScrollOffset = (int) Math.round(Math.max(0, Math.min(maxHScroll, ratio * maxHScroll)));
    }

    private void updateCursorFromMouse(double mouseX, double mouseY) {
        int contentW = width - 8 - SCROLLBAR_W;
        int maxLineW = getMaxLineWidth();
        boolean needsHScroll = maxLineW > contentW;
        int visibleLines = Math.max(1, (height - 4 - (needsHScroll ? H_SCROLLBAR_H : 0)) / LINE_H);

        int relLine = (int) ((mouseY - getY() - 4) / LINE_H);
        int lineIndex = relLine + scrollOffset;
        if (lineIndex < 0) lineIndex = 0;
        if (lineIndex >= lines.size()) lineIndex = lines.size() - 1;
        String line = lines.get(lineIndex);
        int lineStart = getLineStart(lineIndex);
        int col = 0;
        int x = getX() + 4 - horizontalScrollOffset;
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
        if (codePoint == 1 || codePoint == 3 || codePoint == 22 || codePoint == 24 || codePoint == '\t') return false;
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

        if (ctrl && keyCode == GLFW.GLFW_KEY_Z) { undo(); return true; }
        if (ctrl && keyCode == GLFW.GLFW_KEY_Y) { redo(); return true; }
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
            saveState();
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

        if (ctrl && keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            saveState();
            if (selectStart >= 0) {
                deleteSelection();
            } else if (cursorPos > 0) {
                int start = moveWordLeft(cursorPos);
                value = value.substring(0, start) + value.substring(cursorPos);
                cursorPos = start;
                rebuildLines();
                if (onValueChanged != null) onValueChanged.run();
            }
            ensureCursorVisible();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (selectStart >= 0) deleteSelection();
            else if (cursorPos > 0) {
                saveState();
                cursorPos = Math.max(0, Math.min(cursorPos, value.length()));
                value = value.substring(0, cursorPos - 1) + value.substring(cursorPos);
                cursorPos--;
                rebuildLines();
                if (onValueChanged != null) onValueChanged.run();
            }
            ensureCursorVisible();
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_DELETE) {
            saveState();
            if (selectStart >= 0) {
                deleteSelection();
            } else if (cursorPos < value.length()) {
                int end = moveWordRight(cursorPos);
                value = value.substring(0, cursorPos) + value.substring(end);
                rebuildLines();
                if (onValueChanged != null) onValueChanged.run();
            }
            ensureCursorVisible();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (selectStart >= 0) deleteSelection();
            else if (cursorPos < value.length()) {
                saveState();
                cursorPos = Math.max(0, Math.min(cursorPos, value.length()));
                value = value.substring(0, cursorPos) + value.substring(cursorPos + 1);
                rebuildLines();
                if (onValueChanged != null) onValueChanged.run();
            }
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
            saveState();
            deleteSelection();
            for (int i = 0; i < 4; i++) {
                cursorPos = Math.max(0, Math.min(cursorPos, value.length()));
                value = value.substring(0, cursorPos) + ' ' + value.substring(cursorPos);
                cursorPos++;
                rebuildLines();
            }
            if (onValueChanged != null) onValueChanged.run();
            ensureCursorVisible();
            return true;
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

    private void saveState() {
        if (isUndoing) return;
        String current = value;
        int currentCursor = cursorPos;
        int currentSelect = selectStart;
        if (undoIndex >= 0 && undoIndex < undoStack.size()) {
            if (undoStack.get(undoIndex).equals(current) && undoCursorStack.get(undoIndex) == currentCursor) {
                return;
            }
        }
        while (undoStack.size() > undoIndex + 1) {
            undoStack.remove(undoStack.size() - 1);
            undoCursorStack.remove(undoCursorStack.size() - 1);
            undoSelectStack.remove(undoSelectStack.size() - 1);
        }
        undoStack.add(current);
        undoCursorStack.add(currentCursor);
        undoSelectStack.add(currentSelect);
        if (undoStack.size() > MAX_UNDO) {
            undoStack.remove(0);
            undoCursorStack.remove(0);
            undoSelectStack.remove(0);
        } else {
            undoIndex++;
        }
    }

    private void undo() {
        if (undoIndex <= 0) return;
        isUndoing = true;
        undoIndex--;
        value = undoStack.get(undoIndex);
        cursorPos = undoCursorStack.get(undoIndex);
        selectStart = undoSelectStack.get(undoIndex);
        rebuildLines();
        if (onValueChanged != null) onValueChanged.run();
        ensureCursorVisible();
        if (!searchQuery.isEmpty()) findAll(searchQuery);
        isUndoing = false;
    }

    private void redo() {
        if (undoIndex >= undoStack.size() - 1) return;
        isUndoing = true;
        undoIndex++;
        value = undoStack.get(undoIndex);
        cursorPos = undoCursorStack.get(undoIndex);
        selectStart = undoSelectStack.get(undoIndex);
        rebuildLines();
        if (onValueChanged != null) onValueChanged.run();
        ensureCursorVisible();
        if (!searchQuery.isEmpty()) findAll(searchQuery);
        isUndoing = false;
    }

    private void deleteSelection() {
        if (selectStart < 0) return;
        saveState();
        int start = Math.min(selectStart, cursorPos);
        int end = Math.max(selectStart, cursorPos);
        start = Math.max(0, Math.min(start, value.length()));
        end = Math.max(0, Math.min(end, value.length()));
        if (start >= end) { selectStart = -1; return; }
        value = value.substring(0, start) + value.substring(end);
        cursorPos = start;
        selectStart = -1;
        rebuildLines();
        if (onValueChanged != null) onValueChanged.run();
        ensureCursorVisible();
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
        saveState();
        cursorPos = Math.max(0, Math.min(cursorPos, value.length()));
        value = value.substring(0, cursorPos) + c + value.substring(cursorPos);
        cursorPos++;
        rebuildLines();
        if (onValueChanged != null) onValueChanged.run();
        ensureCursorVisible();
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

    public int getScrollOffset() { return scrollOffset; }
    public void setScrollOffset(int v) { scrollOffset = v; }
    public int getHorizontalScrollOffset() { return horizontalScrollOffset; }
    public void setHorizontalScrollOffset(int v) { horizontalScrollOffset = v; }
    public int getCursorPos() { return cursorPos; }
    public void setCursorPos(int v) { cursorPos = v; ensureCursorVisible(); }
    public int getSelectStart() { return selectStart; }
    public void setSelectStart(int v) { selectStart = v; }

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

        int contentW = bgW - 8 - SCROLLBAR_W;
        int maxLineW = getMaxLineWidth();
        boolean needsHScroll = maxLineW > contentW;
        int contentH = bgH - 4 - (needsHScroll ? H_SCROLLBAR_H : 0);
        int visibleLines = Math.max(1, contentH / LINE_H);
        int textBaseX = bgX + 4 - horizontalScrollOffset;
        int clipLeft = bgX + 4;
        int clipRight = bgX + 4 + contentW;

        int scrollBarX = bgX + bgW - SCROLLBAR_W;
        int vTrackY = bgY + 2;
        int vTrackH = bgH - 4 - (needsHScroll ? H_SCROLLBAR_H : 0);
        g.fill(scrollBarX, vTrackY, scrollBarX + SCROLLBAR_W, vTrackY + vTrackH, Theme.BG_INNER);
        g.renderOutline(scrollBarX, vTrackY, SCROLLBAR_W, vTrackH, Theme.BORDER);

        int maxScroll = Math.max(0, lines.size() - visibleLines);
        if (maxScroll > 0) {
            int thumbH = Math.max(10, (visibleLines * vTrackH) / lines.size());
            int trackAvail = vTrackH - thumbH;
            int thumbY = vTrackY + (scrollOffset * trackAvail) / maxScroll;
            g.fill(scrollBarX, thumbY, scrollBarX + SCROLLBAR_W, thumbY + thumbH, Theme.TEXT_DIM);
        }

        if (needsHScroll) {
            int hTrackX = bgX + 4;
            int hTrackY = bgY + bgH - H_SCROLLBAR_H - 2;
            int hTrackW = bgW - 8 - SCROLLBAR_W;
            g.fill(hTrackX, hTrackY, hTrackX + hTrackW, hTrackY + H_SCROLLBAR_H, Theme.BG_INNER);
            g.renderOutline(hTrackX, hTrackY, hTrackW, H_SCROLLBAR_H, Theme.BORDER);
            int hThumbW = Math.max(10, (contentW * hTrackW) / maxLineW);
            int hTrackAvail = hTrackW - hThumbW;
            int maxHScroll = maxLineW - contentW;
            int hThumbX = hTrackX + (horizontalScrollOffset * hTrackAvail) / maxHScroll;
            g.fill(hThumbX, hTrackY, hThumbX + hThumbW, hTrackY + H_SCROLLBAR_H, Theme.TEXT_DIM);
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
                    int x1 = textBaseX + font.width(before);
                    int x2 = x1 + font.width(selected);
                    x1 = Math.max(x1, clipLeft);
                    x2 = Math.min(x2, clipRight);
                    if (x2 > x1) {
                        g.fill(x1, y, x2, y + 9, 0xFF3366AA);
                    }
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
                    int x1 = textBaseX + font.width(before);
                    int x2 = x1 + font.width(match);
                    x1 = Math.max(x1, clipLeft);
                    x2 = Math.min(x2, clipRight);
                    if (x2 > x1) {
                        g.fill(x1, y, x2, y + 9, isCurrent ? 0xFFFFAA00 : 0xFF665522);
                    }
                    idx += searchQuery.length();
                }
            }

            renderHighlightedLine(g, line, textBaseX, y, clipLeft, clipRight);
            y += LINE_H;
        }

        if (isFocused()) {
            int relLine = cursorLine - scrollOffset;
            if (relLine >= 0 && relLine < visibleLines) {
                int col = getColumnInLine(cursorPos);
                String lineText = lines.get(cursorLine);
                int cursorX = textBaseX + font.width(lineText.substring(0, Math.min(col, lineText.length())));
                int cursorY = bgY + 4 + relLine * LINE_H;
                if (cursorX >= clipLeft && cursorX <= clipRight) {
                    g.fill(cursorX, cursorY, cursorX + 1, cursorY + 9, 0xFFFFFFFF);
                }
            }
        }
    }

    private void renderHighlightedLine(GuiGraphics g, String line, int x, int y, int clipLeft, int clipRight) {
        if (line.trim().startsWith("//")) {
            int drawX = Math.max(x, clipLeft);
            if (drawX < clipRight) {
                g.drawString(font, line, x, y, COMMENT_COLOR);
            }
            return;
        }
        Matcher m = TOKEN_PATTERN.matcher(line);
        int lastEnd = 0;
        while (m.find()) {
            if (m.start() > lastEnd) {
                String raw = line.substring(lastEnd, m.start());
                int segX = x + font.width(line.substring(0, lastEnd));
                int segW = font.width(raw);
                int drawX = Math.max(segX, clipLeft);
                int drawEnd = Math.min(segX + segW, clipRight);
                if (drawEnd > drawX) {
                    g.drawString(font, raw, segX, y, DEFAULT_COLOR);
                }
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
            int segX = x + font.width(line.substring(0, m.start()));
            int segW = font.width(token);
            int drawX = Math.max(segX, clipLeft);
            int drawEnd = Math.min(segX + segW, clipRight);
            if (drawEnd > drawX) {
                g.drawString(font, token, segX, y, color);
            }
            lastEnd = m.end();
        }
        if (lastEnd < line.length()) {
            String raw = line.substring(lastEnd);
            int segX = x + font.width(line.substring(0, lastEnd));
            int segW = font.width(raw);
            int drawX = Math.max(segX, clipLeft);
            int drawEnd = Math.min(segX + segW, clipRight);
            if (drawEnd > drawX) {
                g.drawString(font, raw, segX, y, DEFAULT_COLOR);
            }
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
        out.add(NarratedElementType.TITLE, Component.literal(value));
    }
}