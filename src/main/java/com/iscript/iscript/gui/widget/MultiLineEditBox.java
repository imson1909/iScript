package com.iscript.iscript.gui.widget;

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
    private final int maxLength;
    private final List<String> lines = new ArrayList<>();

    public MultiLineEditBox(Font font, int x, int y, int width, int height, Component title, Component hint) {
        super(x, y, width, height, title);
        this.font = font;
        this.maxLength = 8192;
    }

    public void setValue(String text) {
        this.value = text.length() > maxLength ? text.substring(0, maxLength) : text;
        this.cursorPos = this.value.length();
        this.selectStart = -1;
        rebuildLines();
    }

    public String getValue() {
        return value;
    }

    private void rebuildLines() {
        lines.clear();
        String[] rawLines = value.split("\n", -1);
        int maxCharsPerLine = Math.max(1, (this.width - 10) / 6);
        for (String raw : rawLines) {
            while (raw.length() > maxCharsPerLine) {
                lines.add(raw.substring(0, maxCharsPerLine));
                raw = raw.substring(maxCharsPerLine);
            }
            lines.add(raw);
        }
        if (lines.isEmpty()) lines.add("");
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (codePoint == 22) return true;
        if (codePoint == 1) return true;
        if (codePoint == 3) return true;
        if (codePoint == 24) return true;
        if (isValidChar(codePoint)) {
            deleteSelection();
            insertChar(codePoint);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;

        if (ctrl && keyCode == GLFW.GLFW_KEY_A) {
            selectStart = 0;
            cursorPos = value.length();
            return true;
        }
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
            for (char c : clipboard.toCharArray()) {
                if (isValidChar(c)) insertChar(c);
            }
            return true;
        }

        if (ctrl && keyCode == GLFW.GLFW_KEY_HOME) {
            if (shift) {
                if (selectStart < 0) selectStart = cursorPos;
            } else {
                selectStart = -1;
            }
            cursorPos = 0;
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_END) {
            if (shift) {
                if (selectStart < 0) selectStart = cursorPos;
            } else {
                selectStart = -1;
            }
            cursorPos = value.length();
            return true;
        }

        if (ctrl && keyCode == GLFW.GLFW_KEY_LEFT) {
            if (shift) {
                if (selectStart < 0) selectStart = cursorPos;
            } else {
                selectStart = -1;
            }
            cursorPos = moveWordLeft(cursorPos);
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_RIGHT) {
            if (shift) {
                if (selectStart < 0) selectStart = cursorPos;
            } else {
                selectStart = -1;
            }
            cursorPos = moveWordRight(cursorPos);
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (selectStart >= 0) {
                deleteSelection();
            } else if (cursorPos > 0) {
                value = value.substring(0, cursorPos - 1) + value.substring(cursorPos);
                cursorPos--;
                rebuildLines();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (selectStart >= 0) {
                deleteSelection();
            } else if (cursorPos < value.length()) {
                value = value.substring(0, cursorPos) + value.substring(cursorPos + 1);
                rebuildLines();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            if (shift) {
                if (selectStart < 0) selectStart = cursorPos;
            } else {
                selectStart = -1;
            }
            if (cursorPos > 0) cursorPos--;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            if (shift) {
                if (selectStart < 0) selectStart = cursorPos;
            } else {
                selectStart = -1;
            }
            if (cursorPos < value.length()) cursorPos++;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            if (shift) {
                if (selectStart < 0) selectStart = cursorPos;
            } else {
                selectStart = -1;
            }
            moveCursorUp();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            if (shift) {
                if (selectStart < 0) selectStart = cursorPos;
            } else {
                selectStart = -1;
            }
            moveCursorDown();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            deleteSelection();
            insertChar('\n');
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            if (shift) {
                if (selectStart < 0) selectStart = cursorPos;
            } else {
                selectStart = -1;
            }
            int lineIndex = getLineIndex(cursorPos);
            cursorPos = getLineStart(lineIndex);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            if (shift) {
                if (selectStart < 0) selectStart = cursorPos;
            } else {
                selectStart = -1;
            }
            int lineIndex = getLineIndex(cursorPos);
            cursorPos = getLineStart(lineIndex) + lines.get(lineIndex).length();
            return true;
        }

        return false;
    }

    private int moveWordLeft(int pos) {
        if (pos <= 0) return 0;
        pos--;
        while (pos > 0 && !isWordChar(value.charAt(pos))) {
            pos--;
        }
        while (pos > 0 && isWordChar(value.charAt(pos - 1))) {
            pos--;
        }
        return pos;
    }

    private int moveWordRight(int pos) {
        if (pos >= value.length()) return value.length();
        while (pos < value.length() && !isWordChar(value.charAt(pos))) {
            pos++;
        }
        while (pos < value.length() && isWordChar(value.charAt(pos))) {
            pos++;
        }
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
    }

    private void moveCursorUp() {
        int lineIndex = getLineIndex(cursorPos);
        if (lineIndex <= 0) {
            cursorPos = 0;
            return;
        }
        int col = getColumnInLine(cursorPos);
        int prevLineStart = getLineStart(lineIndex - 1);
        int prevLineLen = lines.get(lineIndex - 1).length();
        cursorPos = prevLineStart + Math.min(col, prevLineLen);
    }

    private void moveCursorDown() {
        int lineIndex = getLineIndex(cursorPos);
        if (lineIndex >= lines.size() - 1) {
            cursorPos = value.length();
            return;
        }
        int col = getColumnInLine(cursorPos);
        int nextLineStart = getLineStart(lineIndex + 1);
        int nextLineLen = lines.get(lineIndex + 1).length();
        cursorPos = nextLineStart + Math.min(col, nextLineLen);
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
        for (int i = 0; i < lineIndex; i++) {
            count += lines.get(i).length() + 1;
        }
        return count;
    }

    private void insertChar(char c) {
        if (value.length() >= maxLength) return;
        value = value.substring(0, cursorPos) + c + value.substring(cursorPos);
        cursorPos++;
        rebuildLines();
    }

    private boolean isValidChar(char c) {
        return c >= 32 && c != 127 || c == '\n' || c == '\t';
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF000000);
        graphics.renderOutline(getX(), getY(), width, height, isFocused() ? 0xFFFFFFFF : 0xFF888888);

        int lineHeight = 10;
        int visibleLines = Math.max(1, (height - 4) / lineHeight);
        int cursorLine = getLineIndex(cursorPos);

        if (cursorLine < scrollOffset) scrollOffset = cursorLine;
        if (cursorLine >= scrollOffset + visibleLines) scrollOffset = cursorLine - visibleLines + 1;

        if (selectStart >= 0) {
            int selStart = Math.min(selectStart, cursorPos);
            int selEnd = Math.max(selectStart, cursorPos);
            int startLine = getLineIndex(selStart);
            int endLine = getLineIndex(selEnd);

            for (int i = Math.max(startLine, scrollOffset); i <= Math.min(endLine, scrollOffset + visibleLines - 1); i++) {
                int lineStart = getLineStart(i);
                int lineEnd = lineStart + lines.get(i).length();
                int selStartInLine = Math.max(selStart, lineStart);
                int selEndInLine = Math.min(selEnd, lineEnd);

                if (selStartInLine < selEndInLine) {
                    String before = lines.get(i).substring(0, selStartInLine - lineStart);
                    String selected = lines.get(i).substring(selStartInLine - lineStart, selEndInLine - lineStart);
                    int x1 = getX() + 4 + font.width(before);
                    int x2 = x1 + font.width(selected);
                    int y = getY() + 4 + (i - scrollOffset) * lineHeight;
                    graphics.fill(x1, y, x2, y + 9, 0xFF3366AA);
                }
            }
        }

        int y = getY() + 4;
        for (int i = scrollOffset; i < Math.min(lines.size(), scrollOffset + visibleLines); i++) {
            String line = lines.get(i);
            int color = 0xFFE0E0E0;
            String trimmed = line.trim();
            if (trimmed.startsWith("//")) color = 0xFF888888;
            else if (trimmed.startsWith("api.")) color = 0xFF88CCFF;
            else if (trimmed.startsWith("var ") || trimmed.startsWith("let ") || trimmed.startsWith("const ")) color = 0xFFFF8888;
            else if (trimmed.contains("\"") || trimmed.contains("'")) color = 0xFF88FF88;
            else if (trimmed.startsWith("if ") || trimmed.startsWith("for ") || trimmed.startsWith("while ") || trimmed.startsWith("function ")) color = 0xFFFF88FF;
            else if (trimmed.startsWith("return") || trimmed.startsWith("break") || trimmed.startsWith("continue")) color = 0xFFFFAA00;

            graphics.drawString(font, line, getX() + 4, y, color);
            y += lineHeight;
        }

        if (isFocused()) {
            int relLine = cursorLine - scrollOffset;
            if (relLine >= 0 && relLine < visibleLines) {
                int col = getColumnInLine(cursorPos);
                String lineText = lines.get(cursorLine);
                int cursorX = getX() + 4 + font.width(lineText.substring(0, Math.min(col, lineText.length())));
                int cursorY = getY() + 4 + relLine * lineHeight;
                graphics.fill(cursorX, cursorY, cursorX + 1, cursorY + 9, 0xFFFFFFFF);
            }
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, Component.literal(value));
    }
}