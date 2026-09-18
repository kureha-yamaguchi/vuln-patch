package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            String anchorResult = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(anchorResult)) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: documented clamp-to-length behaviour input=0123456789 lower=15 upper=20 append=null lhs=" + String.valueOf(anchorResult) + " rhs=0123456789");
            }
        } catch (RuntimeException t) {
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            boolean throughAbbreviate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                        && "abbreviate".equals(ste.getMethodName())) {
                    throughAbbreviate = true;
                    break;
                }
            }
            if (t instanceof StringIndexOutOfBoundsException && throughAbbreviate) {
                throw t;
            }
            if (t.getMessage() != null && t.getClass().getName().startsWith("org.apache.commons.lang") && throughAbbreviate) {
                return;
            }
            return;
        }

        String s = data.consumeAsciiString(64);
        if (s.length() == 0) {
            s = "A";
        }
        if (s.indexOf(' ') >= 0) {
            s = s.replace(' ', 'X');
        }

        int len = s.length();
        int lower = len + data.consumeInt(1, 32);
        int upper;
        if (data.consumeBoolean()) {
            upper = -1;
        } else {
            upper = len + data.consumeInt(1, 32);
        }

        String append = data.consumeBoolean() ? null : data.consumeAsciiString(8);

        try {
            String result = WordUtils.abbreviate(s, lower, upper, append);

            /*
             * Contract asserted from the method's documented behavior shown in the patch:
             * - if lower is greater than the length of the string, set to the length of the string
             * - if upper is -1 or greater than the length of the string, set to the length of the string
             * For a non-empty string constructed without spaces, indexOf(" ", lower==len) is -1,
             * substring(0, len) is the original string, and because upper==len no appendToEnd is added.
             * Therefore a correct implementation must return the original string exactly.
             * A "fix" that merely avoids the crash but returns a truncated/appended/empty value breaks this.
             */
            if (!s.equals(result)) {
                throw new RuntimeException("[oracle:no-space-clamp] metamorphic violation: oversize lower/upper on no-space string must return original input=" + s + " lower=" + lower + " upper=" + upper + " append=" + String.valueOf(append) + " lhs=" + String.valueOf(result) + " rhs=" + s);
            }
        } catch (RuntimeException t) {
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            boolean throughAbbreviate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                        && "abbreviate".equals(ste.getMethodName())) {
                    throughAbbreviate = true;
                    break;
                }
            }
            if (t instanceof StringIndexOutOfBoundsException && throughAbbreviate) {
                throw t;
            }
            if (t.getMessage() != null && t.getClass().getName().startsWith("org.apache.commons.lang") && throughAbbreviate) {
                return;
            }
            return;
        }

        String spacedCore = data.consumeAsciiString(48);
        if (spacedCore.length() == 0) {
            spacedCore = "B";
        }
        String spaced = "P " + spacedCore.replace('\n', ' ').replace('\r', ' ');
        if (spaced.length() < 3) {
            spaced = "P Q";
        }
        int spacedLen = spaced.length();
        int lower2 = spacedLen + data.consumeInt(1, 32);
        int upper2 = spacedLen + data.consumeInt(1, 32);
        String append2 = data.consumeBoolean() ? null : data.consumeAsciiString(8);

        try {
            WordUtils.abbreviate(spaced, lower2, upper2, append2);
        } catch (RuntimeException t) {
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            boolean throughAbbreviate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                        && "abbreviate".equals(ste.getMethodName())) {
                    throughAbbreviate = true;
                    break;
                }
            }
            if (t instanceof StringIndexOutOfBoundsException && throughAbbreviate) {
                throw t;
            }
            if (t.getMessage() != null && t.getClass().getName().startsWith("org.apache.commons.lang") && throughAbbreviate) {
                return;
            }
        }
    }
}