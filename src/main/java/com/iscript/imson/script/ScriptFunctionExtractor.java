package com.iscript.imson.script;

public class ScriptFunctionExtractor {

    public static String extractFunctionDeclarations(String code) {
        StringBuilder result = new StringBuilder();
        int len = code.length();
        int i = 0;

        while (i < len) {
            while (i < len && Character.isWhitespace(code.charAt(i))) {
                if (code.charAt(i) == '\n') result.append('\n');
                i++;
            }
            if (i >= len) break;

            if (code.startsWith("function", i)) {
                int before = i - 1;
                if (before >= 0 && Character.isJavaIdentifierPart(code.charAt(before))) {
                    i = skipNonFunction(code, i);
                    continue;
                }

                int funcStart = i;
                i += 8;

                while (i < len && Character.isWhitespace(code.charAt(i))) i++;

                if (i >= len || !Character.isJavaIdentifierStart(code.charAt(i))) {
                    i = skipNonFunction(code, funcStart + 1);
                    continue;
                }
                while (i < len && Character.isJavaIdentifierPart(code.charAt(i))) i++;

                while (i < len && Character.isWhitespace(code.charAt(i))) i++;

                if (i >= len || code.charAt(i) != '(') {
                    i = skipNonFunction(code, funcStart + 1);
                    continue;
                }

                i = skipBalanced(code, i, '(', ')');
                if (i == -1) {
                    i = skipNonFunction(code, funcStart + 1);
                    continue;
                }

                while (i < len && Character.isWhitespace(code.charAt(i))) i++;

                if (i >= len || code.charAt(i) != '{') {
                    i = skipNonFunction(code, funcStart + 1);
                    continue;
                }

                i = skipBalanced(code, i, '{', '}');
                if (i == -1) {
                    i = skipNonFunction(code, funcStart + 1);
                    continue;
                }

                result.append(code, funcStart, i).append("\n");
            } else {
                i = skipNonFunction(code, i);
            }
        }

        return result.toString();
    }

    private static int skipNonFunction(String code, int start) {
        int i = start;
        int len = code.length();

        while (i < len) {
            char c = code.charAt(i);

            if (c == '"' || c == '"' || c == '`') {
                i = skipStringLiteral(code, i, c);
            } else if (c == '/' && i + 1 < len) {
                char next = code.charAt(i + 1);
                if (next == '/') {
                    while (i < len && code.charAt(i) != '\n') i++;
                } else if (next == '*') {
                    i += 2;
                    while (i + 1 < len && !(code.charAt(i) == '*' && code.charAt(i + 1) == '/')) i++;
                    i += 2;
                } else {
                    i++;
                }
            } else if (c == 'f' && i + 8 <= len && code.startsWith("function", i)) {
                int before = i - 1;
                if (before < 0 || !Character.isJavaIdentifierPart(code.charAt(before))) {
                    return i;
                }
                i++;
            } else {
                i++;
            }
        }

        return i;
    }

    private static int skipStringLiteral(String code, int start, char quote) {
        int i = start + 1;
        int len = code.length();
        while (i < len) {
            char c = code.charAt(i);
            if (c == quote) return i + 1;
            if (c == '\\') i++;
            i++;
        }
        return i;
    }

    private static int skipBalanced(String code, int start, char open, char close) {
        int i = start + 1;
        int len = code.length();
        int depth = 1;

        while (i < len && depth > 0) {
            char c = code.charAt(i);

            if (c == '"' || c == '"' || c == '`') {
                i = skipStringLiteral(code, i, c);
            } else if (c == '/' && i + 1 < len) {
                char next = code.charAt(i + 1);
                if (next == '/') {
                    while (i < len && code.charAt(i) != '\n') i++;
                } else if (next == '*') {
                    i += 2;
                    while (i + 1 < len && !(code.charAt(i) == '*' && code.charAt(i + 1) == '/')) i++;
                    i += 2;
                } else {
                    if (c == open) depth++;
                    else if (c == close) depth--;
                    i++;
                }
            } else {
                if (c == open) depth++;
                else if (c == close) depth--;
                i++;
            }
        }

        return depth == 0 ? i : -1;
    }
}