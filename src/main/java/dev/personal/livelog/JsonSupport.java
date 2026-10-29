package dev.personal.livelog;

import org.jetbrains.annotations.Nullable;

/** Dependency-free JSON detection and pretty printing (kept simple on purpose). */
public final class JsonSupport {
    private JsonSupport() {}

    /**
     * Returns the leading JSON object/array of {@code text} (ignoring leading whitespace) if it is a
     * balanced, string-aware structure; otherwise null.
     */
    public static @Nullable String extractJson(@Nullable String text) {
        if (text == null) return null;
        int start = 0;
        while (start < text.length() && Character.isWhitespace(text.charAt(start))) start++;
        if (start >= text.length()) return null;
        char open = text.charAt(start);
        if (open != '{' && open != '[') return null;
        int depth = 0;
        boolean inString = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (c == '\\') i++;
                else if (c == '"') inString = false;
                continue;
            }
            if (c == '"') inString = true;
            else if (c == '{' || c == '[') depth++;
            else if (c == '}' || c == ']') {
                depth--;
                if (depth == 0) {
                    String candidate = text.substring(start, i + 1);
                    return isStructurallyValid(candidate) ? candidate : null;
                }
                if (depth < 0) return null;
            }
        }
        return null;
    }

    /** Cheap validity check: brackets match in kind and strings terminate. */
    private static boolean isStructurallyValid(String s) {
        java.util.ArrayDeque<Character> stack = new java.util.ArrayDeque<>();
        boolean inString = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inString) {
                if (c == '\\') i++;
                else if (c == '"') inString = false;
                continue;
            }
            switch (c) {
                case '"': inString = true; break;
                case '{': case '[': stack.push(c); break;
                case '}': if (stack.isEmpty() || stack.pop() != '{') return false; break;
                case ']': if (stack.isEmpty() || stack.pop() != '[') return false; break;
                default: break;
            }
        }
        return !inString && stack.isEmpty();
    }

    /** Re-indents JSON with two spaces; input must already be validated. */
    public static String pretty(String json) {
        StringBuilder out = new StringBuilder();
        int indent = 0;
        boolean inString = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (inString) {
                out.append(c);
                if (c == '\\' && i + 1 < json.length()) out.append(json.charAt(++i));
                else if (c == '"') inString = false;
                continue;
            }
            switch (c) {
                case '"': inString = true; out.append(c); break;
                case '{': case '[': {
                    int j = nextNonSpace(json, i + 1);
                    if (j < json.length() && (json.charAt(j) == '}' || json.charAt(j) == ']')) {
                        out.append(c).append(json.charAt(j));
                        i = j;
                    } else {
                        out.append(c);
                        indent++;
                        newline(out, indent);
                    }
                    break;
                }
                case '}': case ']': indent--; newline(out, indent); out.append(c); break;
                case ',': out.append(c); newline(out, indent); break;
                case ':': out.append(": "); break;
                default: if (!Character.isWhitespace(c)) out.append(c);
            }
        }
        return out.toString();
    }

    private static int nextNonSpace(String s, int from) {
        while (from < s.length() && Character.isWhitespace(s.charAt(from))) from++;
        return from;
    }

    private static void newline(StringBuilder sb, int indent) {
        sb.append('\n');
        for (int i = 0; i < indent; i++) sb.append("  ");
    }
}
