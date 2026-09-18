package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            WordUtils.abbreviate("0123456789", 15, 20, null);
        } catch (RuntimeException t) {
            boolean validation = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (!validation && t instanceof StringIndexOutOfBoundsException) {
                for (StackTraceElement ste : t.getStackTrace()) {
                    String cls = ste.getClassName();
                    String m = ste.getMethodName();
                    if (("org.apache.commons.lang.WordUtils".equals(cls) && "abbreviate".equals(m))
                            || ("org.apache.commons.lang.StringUtils".equals(cls)
                                    && ("indexOf".equals(m) || "defaultString".equals(m)))) {
                        throw t;
                    }
                }
            }
        }

        String raw = data.consumeAsciiString(48);
        if (raw == null) {
            raw = "";
        }
        StringBuilder noSpaceBuilder = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c != ' ') {
                noSpaceBuilder.append(c);
            }
        }
        if (noSpaceBuilder.length() == 0) {
            noSpaceBuilder.append('A');
        }
        String noSpace = noSpaceBuilder.toString();
        int len = noSpace.length();
        int lower = len + data.consumeInt(1, 8);
        int upper = data.consumeBoolean() ? -1 : len + data.consumeInt(1, 8);
        String append = data.consumeBoolean() ? null : data.consumeAsciiString(8);

        try {
            String out = WordUtils.abbreviate(noSpace, lower, upper, append);
            if (out == null || out.length() != noSpace.length()
                    || out.charAt(out.length() - 1) != noSpace.charAt(noSpace.length() - 1)) {
                throw new RuntimeException(
                        "[oracle:overshoot-tail-preserved] metamorphic violation: valid overshoot input must preserve the full original string when lower is beyond the end; "
                                + "input=" + noSpace + " lower=" + lower + " upper=" + upper + " out=" + out);
            }
        } catch (RuntimeException t) {
            boolean validation = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (!validation && t instanceof StringIndexOutOfBoundsException) {
                for (StackTraceElement ste : t.getStackTrace()) {
                    String cls = ste.getClassName();
                    String m = ste.getMethodName();
                    if (("org.apache.commons.lang.WordUtils".equals(cls) && "abbreviate".equals(m))
                            || ("org.apache.commons.lang.StringUtils".equals(cls)
                                    && ("indexOf".equals(m) || "defaultString".equals(m)))) {
                        throw t;
                    }
                }
            }
        }

        String prefix = noSpace;
        String suffix1 = data.consumeAsciiString(24).replace(" ", "");
        String suffix2 = data.consumeRemainingAsString().replace(" ", "");
        if (suffix1.length() == 0) {
            suffix1 = "X";
        }
        if (suffix2.length() == 0) {
            suffix2 = "Y";
        }
        String withSpace1 = prefix + " " + suffix1;
        String withSpace2 = prefix + " " + suffix2;
        String append2 = data.consumeBoolean() ? null : data.consumeAsciiString(8);
        int lower2 = prefix.length();
        int upper1 = withSpace1.length() + data.consumeInt(1, 4);
        int upper2 = withSpace2.length() + data.consumeInt(1, 4);

        try {
            String r1 = WordUtils.abbreviate(withSpace1, lower2, upper1, append2);
            String r2 = WordUtils.abbreviate(withSpace2, lower2, upper2, append2);
            String expectedAppend = StringUtils.defaultString(append2);
            String decisivePrefix = withSpace1.substring(0, StringUtils.indexOf(withSpace1, " ", lower2));
            if (!r1.equals(r2) || !r1.equals(decisivePrefix + expectedAppend)) {
                throw new RuntimeException(
                        "[oracle:first-space-suffix-irrelevant] metamorphic violation: when the first space at/after lower is unchanged, changing only the suffix after that decisive space must not change the abbreviation result; "
                                + "s1=" + withSpace1 + " s2=" + withSpace2 + " lower=" + lower2 + " upper1=" + upper1
                                + " upper2=" + upper2 + " append=" + append2 + " r1=" + r1 + " r2=" + r2);
            }
        } catch (RuntimeException t) {
            boolean validation = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            boolean oracle = t.getMessage() != null && t.getMessage().startsWith("[oracle:");
            if (oracle) {
                throw t;
            }
            if (!validation && t instanceof StringIndexOutOfBoundsException) {
                for (StackTraceElement ste : t.getStackTrace()) {
                    String cls = ste.getClassName();
                    String m = ste.getMethodName();
                    if (("org.apache.commons.lang.WordUtils".equals(cls) && "abbreviate".equals(m))
                            || ("org.apache.commons.lang.StringUtils".equals(cls)
                                    && ("indexOf".equals(m) || "defaultString".equals(m)))) {
                        throw t;
                    }
                }
            }
        }
    }
}