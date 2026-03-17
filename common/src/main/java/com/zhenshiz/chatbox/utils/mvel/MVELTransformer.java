package com.zhenshiz.chatbox.utils.mvel;

import java.util.ArrayList;
import java.util.Map;

public class MVELTransformer {

    public static String safeTransform(String s, Map<String, ?> methods, Map<String, ?> properties) {
        String transformed = s;
        boolean[] inQuote = buildInQuoteMap(transformed);

        boolean changed;
        do {
            changed = false;
            var methodNames = new ArrayList<>(methods.keySet());
            methodNames.sort((a,b) -> Integer.compare(b.length(), a.length()));
            for (String name : methodNames) {
                String needle = "." + name;
                int idx = transformed.lastIndexOf(needle);
                while (idx >= 0) {
                    if (idx < inQuote.length && inQuote[idx]) { idx = transformed.lastIndexOf(needle, idx - 1); continue; }
                    int nameStart = idx + 1;
                    int j = nameStart + name.length();
                    while (j < transformed.length() && Character.isWhitespace(transformed.charAt(j))) j++;
                    if (j < transformed.length() && transformed.charAt(j) == '(' && !(j < inQuote.length && inQuote[j])) {
                        int closing = findMatchingClosingParen(transformed, j);
                        if (closing < 0) { idx = transformed.lastIndexOf(needle, idx - 1); continue; }
                        int lhsEnd = idx;
                        int lhsStart = findLhsStart(transformed, lhsEnd - 1);
                        String lhs = transformed.substring(lhsStart, lhsEnd);
                        String args = transformed.substring(j + 1, closing);
                        String repl = "m(\"" + name + "\"," + lhs;
                        if (!args.isBlank()) repl += "," + args;
                        repl += ")";
                        transformed = transformed.substring(0, lhsStart) + repl + transformed.substring(closing + 1);
                        inQuote = buildInQuoteMap(transformed);
                        changed = true;
                        idx = transformed.lastIndexOf(needle, lhsStart - 1);
                    } else {
                        idx = transformed.lastIndexOf(needle, idx - 1);
                    }
                }
            }
            var propNames = new ArrayList<>(properties.keySet());
            propNames.sort((a,b) -> Integer.compare(b.length(), a.length()));
            for (String prop : propNames) {
                String needle = "." + prop;
                int idx = transformed.lastIndexOf(needle);
                while (idx >= 0) {
                    if (idx < inQuote.length && inQuote[idx]) { idx = transformed.lastIndexOf(needle, idx - 1); continue; }
                    int propStart = idx + 1;
                    int propEnd = propStart + prop.length();
                    int j = propEnd;
                    while (j < transformed.length() && Character.isWhitespace(transformed.charAt(j))) j++;
                    if (j < transformed.length() && transformed.charAt(j) == '(' && !(j < inQuote.length && inQuote[j])) { idx = transformed.lastIndexOf(needle, idx - 1); continue; }
                    int lhsEnd = idx;
                    int lhsStart = findLhsStart(transformed, lhsEnd - 1);
                    String lhs = transformed.substring(lhsStart, lhsEnd);
                    String repl = "p(\"" + prop + "\"," + lhs + ")";
                    transformed = transformed.substring(0, lhsStart) + repl + transformed.substring(propEnd);
                    inQuote = buildInQuoteMap(transformed);
                    changed = true;
                    idx = transformed.lastIndexOf(needle, lhsStart - 1);
                }
            }
        } while (changed);
        return transformed;
    }

    private static boolean[] buildInQuoteMap(String s) {
        int n = s.length();
        boolean[] inQuote = new boolean[n];
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\'') {
                inQuote[i] = true;
                i++;
                while (i < n) {
                    char cc = s.charAt(i);
                    inQuote[i] = true;
                    if (cc == '\\') { i++; if (i < n) inQuote[i] = true; i++; continue; }
                    if (cc == c) break;
                    i++;
                }
            }
        }
        return inQuote;
    }

    private static int findLhsStart(String s, int pos) {
        int i = pos;
        int paren = 0;
        int bracket = 0;
        for (; i >= 0; i--) {
            char c = s.charAt(i);
            if (c == ']') { bracket++; continue; }
            if (c == '[') { if (bracket > 0) { bracket--; continue; } continue; }
            if (c == ')') { paren++; continue; }
            if (c == '(') { if (paren > 0) { paren--; continue; } continue; }
            if (paren == 0 && bracket == 0) {
                if (Character.isJavaIdentifierPart(c) || c == '.') continue;
                break;
            }
        }
        return i + 1;
    }

    private static int findMatchingClosingParen(String s, int openIndex) {
        int depth = 0;
        for (int i = openIndex; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') { depth--; if (depth == 0) return i; }
            else if (c == '"' || c == '\'') {
                i++;
                while (i < s.length()) {
                    char cc = s.charAt(i);
                    if (cc == '\\') { i += 2; continue; }
                    if (cc == c) break;
                    i++;
                }
            } else if (c == '[') {
                int brDepth = 1;
                i++;
                while (i < s.length() && brDepth > 0) {
                    char q = s.charAt(i);
                    if (q == '[') brDepth++;
                    else if (q == ']') brDepth--;
                    else if (q == '"' || q == '\'') {
                        i++;
                        while (i < s.length()) {
                            char c2 = s.charAt(i);
                            if (c2 == '\\') { i += 2; continue; }
                            if (c2 == q) break;
                            i++;
                        }
                    }
                    i++;
                }
                i--;
            }
        }
        return -1;
    }
}
