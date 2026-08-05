package com.iscript.imson.gui.widget;

import com.iscript.imson.gui.screen.I18n;
import com.iscript.imson.gui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

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
    private final SearchState search = new SearchState();
    private final UndoBuffer undo = new UndoBuffer();
    private int cachedLineIndex = -1;
    private int cachedLineStart = -1;
    private long lastClickTime = 0;
    private double lastClickX = 0;
    private double lastClickY = 0;

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
        if (!search.query.isEmpty()) search.findAll(this.value);
        undo.clear(this.value, this.cursorPos, this.selectStart);
    }

    public String getValue() {
        return value;
    }

    public void openSearch() {
        search.show = true;
        search.focused = true;
        if (selectStart >= 0) {
            int start = Math.min(selectStart, cursorPos);
            int end = Math.max(selectStart, cursorPos);
            search.query = value.substring(start, end);
            search.cursor = search.query.length();
            search.findAll(value);
        } else {
            search.cursor = search.query.length();
            if (!search.query.isEmpty()) search.findAll(value);
        }
    }

    public void closeSearch() {
        search.reset();
    }

    private void rebuildLines() {
        lines.clear();
        String[] rawLines = value.split("\n", -1);
        for (String raw : rawLines) {
            lines.add(raw);
        }
        if (lines.isEmpty()) lines.add("");
        cachedLineIndex = -1;
        cachedLineStart = -1;
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) {
            if (search.show && mouseY >= getY() - 14 && mouseY <= getY() && mouseX >= getX() && mouseX <= getX() + width) {
                int closeX = getX() + width - 14;
                if (mouseX >= closeX - 2 && mouseX <= closeX + 10) {
                    closeSearch();
                } else {
                    search.focused = true;
                    setFocused(true);
                }
                return true;
            }
            return false;
        }
        setFocused(true);
        if (search.show && mouseY > getY()) {
            search.focused = false;
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
        if (!isMouseOver(mouseX, mouseY) && !(search.show && mouseY >= getY() - 14 && mouseY <= getY())) return false;
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
        if (search.show && codePoint == 27) { closeSearch(); return true; }
        if (search.show && search.focused) {
            if (codePoint >= 32 && codePoint != 127) {
                search.query = search.query.substring(0, search.cursor) + codePoint + search.query.substring(search.cursor);
                search.cursor++;
                search.findAll(value);
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

        if (search.show) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { closeSearch(); return true; }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) { search.next(); scrollToMatch(search.currentIndex); return true; }
            if (keyCode == GLFW.GLFW_KEY_UP) { search.prev(); scrollToMatch(search.currentIndex); return true; }
            if (keyCode == GLFW.GLFW_KEY_DOWN) { search.next(); scrollToMatch(search.currentIndex); return true; }
            if (!shift && keyCode == GLFW.GLFW_KEY_F3) { search.next(); scrollToMatch(search.currentIndex); return true; }
            if (shift && keyCode == GLFW.GLFW_KEY_F3) { search.prev(); scrollToMatch(search.currentIndex); return true; }
            if (search.focused) {
                if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                    if (search.cursor > 0) {
                        search.query = search.query.substring(0, search.cursor - 1) + search.query.substring(search.cursor);
                        search.cursor--;
                        search.findAll(value);
                    }
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_DELETE) {
                    if (search.cursor < search.query.length()) {
                        search.query = search.query.substring(0, search.cursor) + search.query.substring(search.cursor + 1);
                        search.findAll(value);
                    }
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_LEFT && search.cursor > 0) { search.cursor--; return true; }
                if (keyCode == GLFW.GLFW_KEY_RIGHT && search.cursor < search.query.length()) { search.cursor++; return true; }
                if (keyCode == GLFW.GLFW_KEY_HOME) { search.cursor = 0; return true; }
                if (keyCode == GLFW.GLFW_KEY_END) { search.cursor = search.query.length(); return true; }
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
            undo.push(value, cursorPos, selectStart);
            deleteSelection();
            String clipboard = net.minecraft.client.Minecraft.getInstance().keyboardHandler.getClipboard();
            for (char c : clipboard.toCharArray()) {
                if (value.length() >= maxLength) break;
                if (isValidChar(c)) insertChar(c);
            }
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
            undo.push(value, cursorPos, selectStart);
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
                undo.push(value, cursorPos, selectStart);
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
            undo.push(value, cursorPos, selectStart);
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
                undo.push(value, cursorPos, selectStart);
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
            if (value.length() + 4 > maxLength) return true;
            undo.push(value, cursorPos, selectStart);
            deleteSelection();
            for (int i = 0; i < 4; i++) {
                if (value.length() >= maxLength) break;
                cursorPos = Math.max(0, Math.min(cursorPos, value.length()));
                value = value.substring(0, cursorPos) + ' ' + value.substring(cursorPos);
                cursorPos++;
                rebuildLines();
            }
            if (onValueChanged != null) onValueChanged.run();
            ensureCursorVisible();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { if (search.show) { closeSearch(); return true; } setFocused(false); return true; }

        return false;
    }

    private void scrollToMatch(int idx) {
        if (idx < 0 || idx >= search.matches.size()) return;
        int pos = search.matches.get(idx)[0];
        int line = getLineIndex(pos);
        int visible = Math.max(1, (height - 4) / LINE_H);
        scrollOffset = Math.max(0, line - visible / 2);
        int maxScroll = Math.max(0, lines.size() - visible);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
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

    private void undo() {
        UndoBuffer.Record r = undo.undo();
        if (r == null) return;
        undo.setUndoing(true);
        value = r.value();
        cursorPos = r.cursor();
        selectStart = r.select();
        rebuildLines();
        if (onValueChanged != null) onValueChanged.run();
        ensureCursorVisible();
        if (!search.query.isEmpty()) search.findAll(value);
        undo.setUndoing(false);
    }

    private void redo() {
        UndoBuffer.Record r = undo.redo();
        if (r == null) return;
        undo.setUndoing(true);
        value = r.value();
        cursorPos = r.cursor();
        selectStart = r.select();
        rebuildLines();
        if (onValueChanged != null) onValueChanged.run();
        ensureCursorVisible();
        if (!search.query.isEmpty()) search.findAll(value);
        undo.setUndoing(false);
    }

    private void deleteSelection() {
        if (selectStart < 0) return;
        undo.push(value, cursorPos, selectStart);
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
        if (cachedLineIndex >= 0 && cachedLineStart >= 0) {
            int lineEnd = cachedLineStart + lines.get(cachedLineIndex).length() + 1;
            if (pos >= cachedLineStart && pos < lineEnd) return cachedLineIndex;
        }
        int count = 0;
        for (int i = 0; i < lines.size(); i++) {
            int lineLen = lines.get(i).length() + 1;
            if (count + lineLen > pos) {
                cachedLineIndex = i;
                cachedLineStart = count;
                return i;
            }
            count += lineLen;
        }
        cachedLineIndex = lines.size() - 1;
        cachedLineStart = count - lines.get(lines.size() - 1).length() - 1;
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
        if (cachedLineIndex == lineIndex && cachedLineStart >= 0) return cachedLineStart;
        int count = 0;
        for (int i = 0; i < lineIndex; i++) count += lines.get(i).length() + 1;
        cachedLineIndex = lineIndex;
        cachedLineStart = count;
        return count;
    }

    private void insertChar(char c) {
        if (value.length() >= maxLength) return;
        undo.push(value, cursorPos, selectStart);
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

        if (search.show) {
            g.fill(bgX, bgY - 14, bgX + bgW, bgY, 0xFF222233);
            g.renderOutline(bgX, bgY - 14, bgW, 14, search.focused ? Theme.ACCENT : Theme.BORDER);
            String searchLabel = I18n.s("iscript.script.editor.find") + ": " + search.query.substring(0, search.cursor) + "|" + search.query.substring(search.cursor);
            g.drawString(font, searchLabel, bgX + 4, bgY - 11, 0xFFFFFFFF);
            String count;
            if (search.matches.isEmpty()) {
                count = search.query.isEmpty() ? "" : " 0/0";
            } else {
                count = " " + (search.currentIndex + 1) + "/" + search.matches.size();
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

            if (!search.query.isEmpty()) {
                String lowerLine = line.toLowerCase();
                String lowerQuery = search.query.toLowerCase();
                int idx = 0;
                while ((idx = lowerLine.indexOf(lowerQuery, idx)) != -1) {
                    int absStart = lineStart + idx;
                    int absEnd = absStart + search.query.length();
                    boolean isCurrent = search.currentIndex >= 0 && search.matches.get(search.currentIndex)[0] == absStart;
                    String before = line.substring(0, idx);
                    String match = line.substring(idx, idx + search.query.length());
                    int x1 = textBaseX + font.width(before);
                    int x2 = x1 + font.width(match);
                    x1 = Math.max(x1, clipLeft);
                    x2 = Math.min(x2, clipRight);
                    if (x2 > x1) {
                        g.fill(x1, y, x2, y + 9, isCurrent ? 0xFFFFAA00 : 0xFF665522);
                    }
                    idx += search.query.length();
                }
            }

            SyntaxHighlighter.renderLine(g, font, line, textBaseX, y, clipLeft, clipRight);
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

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
        out.add(NarratedElementType.TITLE, Component.literal(value));
    }

    private static class UndoBuffer {
        private final List<String> stack = new ArrayList<>();
        private final List<Integer> cursors = new ArrayList<>();
        private final List<Integer> selects = new ArrayList<>();
        private int index = -1;
        private static final int MAX = 100;
        private boolean isUndoing = false;

        void push(String value, int cursor, int select) {
            if (isUndoing) return;
            if (index >= 0 && index < stack.size()
                    && stack.get(index).equals(value)
                    && cursors.get(index) == cursor) return;
            while (stack.size() > index + 1) {
                stack.remove(stack.size() - 1);
                cursors.remove(cursors.size() - 1);
                selects.remove(selects.size() - 1);
            }
            stack.add(value);
            cursors.add(cursor);
            selects.add(select);
            if (stack.size() > MAX) {
                stack.remove(0);
                cursors.remove(0);
                selects.remove(0);
            } else {
                index++;
            }
        }

        boolean canUndo() { return index > 0; }
        boolean canRedo() { return index < stack.size() - 1; }

        Record undo() {
            if (!canUndo()) return null;
            index--;
            return new Record(stack.get(index), cursors.get(index), selects.get(index));
        }

        Record redo() {
            if (!canRedo()) return null;
            index++;
            return new Record(stack.get(index), cursors.get(index), selects.get(index));
        }

        void clear(String value, int cursor, int select) {
            stack.clear(); cursors.clear(); selects.clear();
            stack.add(value); cursors.add(cursor); selects.add(select);
            index = 0;
        }

        record Record(String value, int cursor, int select) {}

        void setUndoing(boolean v) { isUndoing = v; }
    }

    private static class SearchState {
        String query = "";
        final List<int[]> matches = new ArrayList<>();
        int currentIndex = -1;
        boolean show = false;
        int cursor = 0;
        boolean focused = false;

        void findAll(String value) {
            matches.clear(); currentIndex = -1;
            if (query.isEmpty()) return;
            int idx = 0;
            while ((idx = value.toLowerCase().indexOf(query.toLowerCase(), idx)) != -1) {
                matches.add(new int[]{idx, idx + query.length()});
                idx += query.length();
            }
            if (!matches.isEmpty()) { currentIndex = 0; }
        }

        void next() {
            if (matches.isEmpty()) return;
            currentIndex = (currentIndex + 1) % matches.size();
        }

        void prev() {
            if (matches.isEmpty()) return;
            currentIndex = (currentIndex - 1 + matches.size()) % matches.size();
        }

        void reset() {
            query = ""; matches.clear(); currentIndex = -1; show = false; cursor = 0; focused = false;
        }
    }
}