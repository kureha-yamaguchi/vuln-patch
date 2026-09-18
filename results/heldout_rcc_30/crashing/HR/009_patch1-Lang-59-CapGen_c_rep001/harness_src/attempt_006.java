package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            StrBuilder sb = new StrBuilder(1);
            sb.appendFixedWidthPadRight("foo", 1, '-');
            String out = sb.toString();
            if (!"f".equals(out)) {
                throw new RuntimeException("[oracle:anchor-out] metamorphic violation: exact trigger must truncate to width 1 input=foo width=1 lhs=" + out + " rhs=f");
            }
            try {
                String whole = sb.substring(0, sb.length());
                if (!out.equals(whole)) {
                    throw new RuntimeException("[oracle:substr-view] metamorphic violation: substring(0,length) must equal toString lhs=" + out + " rhs=" + whole);
                }
            } catch (RuntimeException ignored) {
            }
            try {
                StringBuilder rebuilt = new StringBuilder();
                for (int i = 0; i < sb.length(); i++) {
                    rebuilt.append(sb.charAt(i));
                }
                String viaChars = rebuilt.toString();
                if (!out.equals(viaChars)) {
                    throw new RuntimeException("[oracle:charat-view] consistency violation: toString must agree with charAt reconstruction lhs=" + out + " rhs=" + viaChars);
                }
            } catch (RuntimeException ignored) {
            }
        } catch (RuntimeException t) {
            boolean root = t instanceof ArrayIndexOutOfBoundsException;
            if (root) {
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    String cn = st[i].getClassName();
                    String mn = st[i].getMethodName();
                    if ("org.apache.commons.lang.text.StrBuilder".equals(cn)
                            && ("appendFixedWidthPadRight".equals(mn) || "ensureCapacity".equals(mn) || "getNullText".equals(mn))) {
                        throw t;
                    }
                }
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
        }

        int initialCapacity = data.consumeInt(0, 3);
        String prefix = data.consumeAsciiString(8);
        int width = data.consumeInt(1, 32);
        char pad = (char) data.consumeInt(32, 126);
        boolean useNullObject = data.consumeBoolean();

        String seed = data.consumeString(32);
        if (seed == null) {
            seed = "";
        }
        StringBuilder srcBuilder = new StringBuilder(seed);
        while (srcBuilder.length() < width) {
            srcBuilder.append((char) ('A' + (srcBuilder.length() % 26)));
        }
        if (srcBuilder.length() == 0) {
            srcBuilder.append('Z');
        }
        String src = srcBuilder.toString();

        try {
            StrBuilder sb = new StrBuilder(initialCapacity);
            if (prefix.length() > 0) {
                sb.append(prefix);
            }
            int oldLen = sb.length();

            if (useNullObject) {
                sb.setNullText(src);
                sb.appendFixedWidthPadRight(null, width, pad);
            } else {
                sb.appendFixedWidthPadRight(src, width, pad);
            }

            String result = sb.toString();

            String expectedAppended;
            if (src.length() >= width) {
                expectedAppended = src.substring(0, width);
            } else {
                StringBuilder tmp = new StringBuilder(src);
                while (tmp.length() < width) {
                    tmp.append(pad);
                }
                expectedAppended = tmp.toString();
            }
            String expected = prefix + expectedAppended;

            if (!expected.equals(result)) {
                throw new RuntimeException("[oracle:fwpr-contract] metamorphic violation: appendFixedWidthPadRight must append exactly width chars, truncating or right-padding input prefix=" + prefix + " src=" + src + " width=" + width + " pad=" + pad + " lhs=" + result + " rhs=" + expected);
            }

            try {
                if (sb.length() != oldLen + width) {
                    throw new RuntimeException("[oracle:size-growth] metamorphic violation: successful appendFixedWidthPadRight must increase length by width old=" + oldLen + " width=" + width + " new=" + sb.length());
                }
            } catch (RuntimeException t) {
                if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                    throw t;
                }
            }

            try {
                String whole = sb.substring(0, sb.length());
                if (!result.equals(whole)) {
                    throw new RuntimeException("[oracle:substr-view] metamorphic violation: substring(0,length) must equal toString after append lhs=" + result + " rhs=" + whole);
                }
            } catch (RuntimeException ignored) {
            }

            try {
                StringBuilder rebuilt = new StringBuilder();
                for (int i = 0; i < sb.length(); i++) {
                    rebuilt.append(sb.charAt(i));
                }
                String viaChars = rebuilt.toString();
                if (!result.equals(viaChars)) {
                    throw new RuntimeException("[oracle:charat-view] consistency violation: toString must agree with charAt reconstruction after append lhs=" + result + " rhs=" + viaChars);
                }
            } catch (RuntimeException ignored) {
            }
        } catch (RuntimeException t) {
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            boolean root = t instanceof ArrayIndexOutOfBoundsException;
            if (root) {
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    String cn = st[i].getClassName();
                    String mn = st[i].getMethodName();
                    if ("org.apache.commons.lang.text.StrBuilder".equals(cn)
                            && ("appendFixedWidthPadRight".equals(mn) || "ensureCapacity".equals(mn) || "getNullText".equals(mn))) {
                        throw t;
                    }
                }
            }
        }
    }
}