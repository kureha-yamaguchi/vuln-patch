package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String primary = data.consumeString(256);
        String ascii = data.consumeAsciiString(256);
        String append = data.consumeString(64);

        String withSpaces = buildVariant(primary, ascii, data.consumeBoolean(), data.consumeBoolean(), data.consumeBoolean());
        String noSpaces = removeSpaces(primary);
        String mostlySpaces = buildMostlySpaces(ascii, data.consumeBoolean());

        String[] strs = new String[] {
            null,
            "",
            primary,
            ascii,
            withSpaces,
            noSpaces,
            mostlySpaces,
            " ",
            "  ",
            "a",
            "a b",
            "abc def ghi",
            " leading",
            "trailing ",
            "mid dle",
            append
        };

        String[] appenders = new String[] {
            null,
            "",
            append,
            ascii,
            primary,
            "...",
            " ",
            "END"
        };

        int any1 = data.consumeInt();
        int any2 = data.consumeInt();
        int any3 = data.consumeInt();
        int any4 = data.consumeInt();

        int[] lowers = new int[] {
            any1,
            any2,
            -1,
            0,
            1,
            2,
            3,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE
        };

        int[] uppers = new int[] {
            any3,
            any4,
            -1,
            0,
            1,
            2,
            3,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE
        };

        for (int i = 0; i < strs.length; i++) {
            String s = strs[i];
            int len = s == null ? 0 : s.length();

            int[] dynamicLowers = new int[] {
                lowers[i % lowers.length],
                len - 2,
                len - 1,
                len,
                len + 1
            };

            int[] dynamicUppers = new int[] {
                uppers[i % uppers.length],
                -1,
                len - 2,
                len - 1,
                len,
                len + 1
            };

            for (int lower : dynamicLowers) {
                for (int upper : dynamicUppers) {
                    for (String a : appenders) {
                        WordUtils.abbreviate(s, lower, upper, a);
                    }
                }
            }
        }

        if (data.remainingBytes() > 0) {
            String tail = data.consumeRemainingAsString();
            WordUtils.abbreviate(tail, any1, any2, append);
            WordUtils.abbreviate(insertSpaces(tail), any3, -1, null);
            WordUtils.abbreviate(removeSpaces(tail), 0, any4, "...");
        }
    }

    private static String buildVariant(String a, String b, boolean prefixSpace, boolean infixSpace, boolean suffixSpace) {
        String left = a == null ? "" : a;
        String right = b == null ? "" : b;
        StringBuilder sb = new StringBuilder();
        if (prefixSpace) {
            sb.append(' ');
        }
        sb.append(left);
        if (infixSpace) {
            sb.append(' ');
        }
        sb.append(right);
        if (suffixSpace) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private static String removeSpaces(String s) {
        if (s == null || s.length() == 0) {
            return s == null ? null : s;
        }
        return s.replace(" ", "");
    }

    private static String insertSpaces(String s) {
        if (s == null || s.length() == 0) {
            return s == null ? null : s;
        }
        StringBuilder sb = new StringBuilder(s.length() * 2);
        for (int i = 0; i < s.length(); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(s.charAt(i));
        }
        return sb.toString();
    }

    private static String buildMostlySpaces(String s, boolean includeContent) {
        StringBuilder sb = new StringBuilder();
        sb.append(' ').append(' ').append(' ');
        if (includeContent && s != null) {
            sb.append(s);
        }
        sb.append(' ').append(' ');
        return sb.toString();
    }
}