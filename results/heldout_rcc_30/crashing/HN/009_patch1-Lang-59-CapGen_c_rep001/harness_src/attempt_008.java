package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            StrBuilder anchor = new StrBuilder(1);
            anchor.appendFixedWidthPadRight("foo", 1, '-');

            String anchorString = anchor.toString();
            try {
                anchor.minimizeCapacity();
                String anchorChars = new String(anchor.toCharArray());
                if (!"f".equals(anchorString) || !"f".equals(anchorChars) || anchor.length() != 1) {
                    throw new RuntimeException("[oracle:fwpr-anchor] metamorphic violation: exact failing test contract input=foo,width=1 lhs="
                            + anchorString + " rhs=" + anchorChars);
                }
            } catch (RuntimeException oracleSide) {
                throw oracleSide;
            }
        } catch (RuntimeException t) {
            boolean fromTarget = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                        && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                    fromTarget = true;
                    break;
                }
            }
            if (fromTarget && t instanceof ArrayIndexOutOfBoundsException) {
                throw t;
            }
        }

        int width = data.consumeInt(1, 32);
        int initialCapacity = data.consumeInt(1, 8);
        char padChar = (char) (data.consumeByte() & 0xff);
        String prefix = data.consumeString(8);
        String suffix = data.consumeString(8);
        String base = data.consumeString(40);

        if (base == null) {
            base = "";
        }
        if (base.length() < width) {
            StringBuilder grown = new StringBuilder(base);
            while (grown.length() < width) {
                grown.append('X');
            }
            base = grown.toString();
        }

        try {
            StrBuilder lhs = new StrBuilder(initialCapacity);
            lhs.append(prefix);
            lhs.appendFixedWidthPadRight(base, width, padChar);
            lhs.append(suffix);

            StrBuilder rhs = new StrBuilder(initialCapacity);
            rhs.append(prefix);
            rhs.append(base, 0, width);
            rhs.append(suffix);

            String lhsString;
            String rhsString;
            try {
                lhsString = lhs.toString();
                rhsString = rhs.toString();
            } catch (RuntimeException ignored) {
                return;
            }

            if (!lhsString.equals(rhsString)) {
                throw new RuntimeException("[oracle:fwpr-eq] metamorphic violation: appendFixedWidthPadRight(str,width,pad) must equal append(str,0,width) when str.length()>=width input="
                        + base + " lhs=" + lhsString + " rhs=" + rhsString);
            }

            try {
                lhs.minimizeCapacity();
                String charView = new String(lhs.toCharArray());
                if (!lhsString.equals(charView) || lhs.length() != prefix.length() + width + suffix.length()) {
                    throw new RuntimeException("[oracle:fwpr-state] metamorphic violation: toCharArray/length must agree with appended contents input="
                            + base + " lhs=" + lhsString + " rhs=" + charView);
                }
            } catch (RuntimeException ignored) {
                return;
            }
        } catch (RuntimeException t) {
            boolean fromTarget = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                        && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                    fromTarget = true;
                    break;
                }
            }

            if (fromTarget && t instanceof ArrayIndexOutOfBoundsException) {
                throw t;
            }

            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }

            String name = t.getClass().getName();
            if (name.contains("Invalid") || name.contains("Validation")) {
                return;
            }

            if (name.startsWith("java.lang.IndexOutOfBoundsException")
                    || name.startsWith("java.lang.StringIndexOutOfBoundsException")
                    || name.startsWith("java.lang.NullPointerException")
                    || name.startsWith("java.lang.ClassCastException")
                    || name.startsWith("java.lang.ArithmeticException")) {
                return;
            }

            if (name.startsWith("java.lang.RuntimeException") && t.getMessage() != null
                    && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
        }
    }
}