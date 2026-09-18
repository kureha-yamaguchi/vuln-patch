package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            StrBuilder anchor = new StrBuilder(1);
            anchor.appendFixedWidthPadRight("foo", 1, '-');
            if (!"f".equals(anchor.toString())) {
                throw new RuntimeException("[oracle:anchor-exact-post] metamorphic violation: exact seed must truncate to width 1 input=foo width=1 lhs=" + anchor.toString() + " rhs=f");
            }
        } catch (RuntimeException t) {
            boolean root = t instanceof ArrayIndexOutOfBoundsException;
            if (root) {
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.lang.text.StrBuilder".equals(st[i].getClassName())
                            && "appendFixedWidthPadRight".equals(st[i].getMethodName())) {
                        throw t;
                    }
                }
            }
        }

        int initialCapacity = data.consumeInt(1, 4);
        int prefixLen = data.consumeInt(0, 8);
        int aLen = data.consumeInt(1, 8);
        int bLen = data.consumeInt(1, 8);
        int cLen = data.consumeInt(1, 8);

        String prefix = data.consumeAsciiString(prefixLen);
        String a = data.consumeAsciiString(aLen);
        String b = data.consumeAsciiString(bLen);
        String c = data.consumeAsciiString(cLen);
        if (a.length() == 0) a = "A";
        if (b.length() == 0) b = "B";
        if (c.length() == 0) c = "C";

        String carrier = a + " " + b + " " + c;
        int maxWidth = carrier.length();
        int width = data.consumeInt(1, maxWidth);
        char padChar = (char) ('!' + (data.consumeInt(0, 93)));

        boolean useNullObject = data.consumeBoolean();
        boolean setNullText = data.consumeBoolean();
        String nullText = data.consumeAsciiString(12);
        if (nullText.length() == 0) {
            nullText = carrier;
        }
        if (nullText.length() < width) {
            StringBuilder tmp = new StringBuilder(nullText);
            while (tmp.length() < width) {
                tmp.append(carrier);
            }
            nullText = tmp.toString();
        }

        StrBuilder sb = new StrBuilder(initialCapacity);
        sb.append(prefix);

        if (setNullText) {
            sb.setNullText(nullText);
        }

        int beforeLen = sb.length();
        String expectedCarrier = useNullObject ? sb.getNullText() : carrier;
        if (expectedCarrier == null || expectedCarrier.length() < width) {
            return;
        }

        try {
            sb.appendFixedWidthPadRight(useNullObject ? null : carrier, width, padChar);
        } catch (RuntimeException t) {
            boolean validation = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (validation) {
                return;
            }
            boolean root = t instanceof ArrayIndexOutOfBoundsException;
            if (root) {
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.lang.text.StrBuilder".equals(st[i].getClassName())
                            && "appendFixedWidthPadRight".equals(st[i].getMethodName())) {
                        throw t;
                    }
                }
            }
            return;
        }

        String text = sb.toString();

        String expectedAppend = expectedCarrier.substring(0, width);
        String expectedWhole = prefix + expectedAppend;
        if (!expectedWhole.equals(text)) {
            throw new RuntimeException("[oracle:truncate-contract] metamorphic violation: appendFixedWidthPadRight must append exactly the leftmost width characters when source length >= width input=" + expectedCarrier + " width=" + width + " lhs=" + text + " rhs=" + expectedWhole);
        }

        if (sb.length() != beforeLen + width) {
            throw new RuntimeException("[oracle:length-after-append] metamorphic violation: documented fixed-width append must increase size by width input=" + expectedCarrier + " width=" + width + " lhs=" + sb.length() + " rhs=" + (beforeLen + width));
        }

        try {
            StrTokenizer live = sb.asTokenizer();
            StrTokenizer snapshot = new StrTokenizer(text);
            java.util.List liveTokens = live.getTokenList();
            java.util.List snapTokens = snapshot.getTokenList();
            if (!liveTokens.equals(snapTokens)) {
                throw new RuntimeException("[oracle:tokenizer-reparse] metamorphic violation: tokenizing the live builder view must agree with tokenizing its String snapshot input=" + text + " lhs=" + liveTokens + " rhs=" + snapTokens);
            }
        } catch (RuntimeException t) {
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        try {
            StrBuilder rebuilt = new StrBuilder(initialCapacity);
            rebuilt.append(prefix);
            rebuilt.append(text.substring(prefix.length()));
            if (!rebuilt.equals(sb)) {
                throw new RuntimeException("[oracle:rebuild-equals] metamorphic violation: rebuilding from the observable string content must produce an equal StrBuilder input=" + text + " lhs=" + rebuilt.toString() + " rhs=" + sb.toString());
            }
        } catch (RuntimeException t) {
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
        }
    }
}