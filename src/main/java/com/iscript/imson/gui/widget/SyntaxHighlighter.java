package com.iscript.imson.gui.widget;

import com.iscript.imson.gui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SyntaxHighlighter {
    private SyntaxHighlighter() {}

    private static final int KW_COLOR      = 0xFFFF88FF;
    private static final int API_COLOR     = 0xFF88CCFF;
    private static final int STR_COLOR     = 0xFF88FF88;
    private static final int NUM_COLOR     = 0xFFFFAA66;
    private static final int COMMENT_COLOR = 0xFF666666;
    private static final int DEFAULT_COLOR = Theme.TEXT;
    private static final int OP_COLOR      = 0xFFCCCCCC;

    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "(//.*$)|" +
                    "(/\\*[\\s\\S]*?\\*/)|" +
                    "(\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*')|" +
                    "\\b(function|var|let|const|if|else|for|while|do|switch|case|break|continue|return|new|this|true|false|null|undefined|typeof|instanceof|in|of|async|await|class|extends|import|export|from|try|catch|finally|throw|delete|void|yield|with|debugger)\\b|" +
                    "\\b(api)\\b|" +
                    "(\\b\\d+\\.?\\d*\\b)|" +
                    "([+\\-*/=<>!&|:%]+)|" +
                    "([a-zA-Z_][a-zA-Z0-9_]*)|" +
                    "(\\S)"
    );

    public static void renderLine(GuiGraphics g, Font font, String line, int x, int y, int clipLeft, int clipRight) {
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
            else if (m.group(2) != null) color = COMMENT_COLOR;
            else if (m.group(3) != null) color = STR_COLOR;
            else if (m.group(4) != null) color = KW_COLOR;
            else if (m.group(5) != null) color = API_COLOR;
            else if (m.group(6) != null) color = NUM_COLOR;
            else if (m.group(7) != null) color = OP_COLOR;
            else if (m.group(8) != null) color = DEFAULT_COLOR;
            else if (m.group(9) != null) color = DEFAULT_COLOR;

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
}