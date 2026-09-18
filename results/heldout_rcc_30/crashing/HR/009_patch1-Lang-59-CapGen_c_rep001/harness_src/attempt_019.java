package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            StrBuilder anchor = new StrBuilder(1);
            anchor.appendFixedWidthPadRight("foo", 1, '-');
            String anchorOut = anchor.toString();
            if (!"f".equals(anchorOut)) {
                throw new RuntimeException("[oracle:anchor-truncate] metamorphic violation: appendFixedWidthPadRight(\"foo\",1,'-') must append exactly the first width characters lhs=" + anchorOut + " rhs=f");
            }
            if (anchor.length() != 1) {
                throw new RuntimeException("[oracle:anchor-length] metamorphic violation: documented fixed-width append must increase length by exactly width length=" + anchor.length());
            }
            char[] anchorSlice = new char[1];
            anchor.getChars(0, 1, anchorSlice, 0);
            if (anchorSlice[0] != 'f') {
                throw new RuntimeException("[oracle:anchor-slice] metamorphic violation: builder slice must agree with toString after fixed-width append slice=" + new String(anchorSlice) + " str=" + anchorOut);
            }
        } catch (RuntimeException t) {
            boolean validation = false;
            for (Class<?> c = t.getClass(); c != null; c = c.getSuperclass()) {
                String n = c.getName();
                if ("java.lang.IllegalArgumentException".equals(n) || "java.lang.NumberFormatException".equals(n)) {
                    validation = true;
                    break;
                }
            }
            if (validation) {
                return;
            }
            boolean root = t instanceof ArrayIndexOutOfBoundsException;
            if (root) {
                StackTraceElement[] st = t.getStackTrace();
                boolean inRegion = false;
                for (int i = 0; i < st.length; i++) {
                    String cn = st[i].getClassName();
                    String mn = st[i].getMethodName();
                    if ("org.apache.commons.lang.text.StrBuilder".equals(cn)
                            && ("appendFixedWidthPadRight".equals(mn) || "ensureCapacity".equals(mn) || "getNullText".equals(mn))) {
                        inRegion = true;
                        break;
                    }
                }
                if (inRegion) {
                    throw new RuntimeException("[oracle:anchor-crash] metamorphic violation: valid width-1 truncation must not crash for input foo/1/-", t);
                }
            }
            return;
        }

        String raw = data.consumeString(32);
        if (raw == null) {
            raw = "";
        }
        int width = data.consumeInt(1, 16);
        StringBuilder materialized = new StringBuilder(raw);
        while (materialized.length() < width) {
            materialized.append((char) ('A' + (materialized.length() % 26)));
        }
        String effective = materialized.toString();
        String prefix = data.consumeAsciiString(8);
        char pad = (char) (data.consumeByte() & 0xff);

        boolean useNullTextPath = data.consumeBoolean();
        boolean useObjectBuilder = data.consumeBoolean();

        int initialCapacity = data.consumeInt(0, Math.max(0, prefix.length()));
        StrBuilder receiver = new StrBuilder(initialCapacity);
        receiver.append(prefix);
        int beforeLen = receiver.length();

        Object obj;
        if (useNullTextPath) {
            receiver.setNullText(effective);
            obj = null;
        } else if (useObjectBuilder) {
            StrBuilder objBuilder = new StrBuilder(Math.max(0, effective.length() - 1));
            objBuilder.append(effective);
            obj = objBuilder;
        } else {
            obj = effective;
        }

        try {
            receiver.ensureCapacity(data.consumeInt(0, beforeLen + width - 1));
            receiver.appendFixedWidthPadRight(obj, width, pad);

            String full = receiver.toString();
            if (receiver.length() != beforeLen + width) {
                throw new RuntimeException("[oracle:length-growth] metamorphic violation: fixed-width append must increase length by exactly width before=" + beforeLen + " width=" + width + " after=" + receiver.length());
            }

            String suffixFromString = full.substring(beforeLen);
            String expected = effective.substring(0, width);

            /* Contract from the method body: when strLen >= width it copies exactly the first width chars,
               then size += width. A throw-deleting or branch-skipping patch could avoid crashing but still
               append the wrong content or leave size/buffer disagreeing. */
            if (!expected.equals(suffixFromString)) {
                throw new RuntimeException("[oracle:truncate-content] metamorphic violation: fixed-width right-pad on long input must truncate to the first width chars input=" + effective + " width=" + width + " lhs=" + suffixFromString + " rhs=" + expected);
            }

            /* Independent consistency check: the same suffix obtained from the object's string view and from
               the builder's slice API must agree for every correct implementation. */
            char[] copied = new char[width];
            receiver.getChars(beforeLen, beforeLen + width, copied, 0);
            String suffixFromSlice = new String(copied);
            if (!suffixFromString.equals(suffixFromSlice)) {
                throw new RuntimeException("[oracle:slice-agree] metamorphic violation: builder slice must agree with toString suffix before=" + beforeLen + " width=" + width + " lhs=" + suffixFromSlice + " rhs=" + suffixFromString);
            }

            /* Shared-state cross-check on buffer/size after the target writes them: minimizing capacity is
               documented to shrink to actual length, so content and exported char count must remain unchanged. */
            int lenBeforeMin = receiver.length();
            String strBeforeMin = receiver.toString();
            receiver.minimizeCapacity();
            if (receiver.length() != lenBeforeMin) {
                throw new RuntimeException("[oracle:mincap-length] metamorphic violation: minimizeCapacity must not change logical length before=" + lenBeforeMin + " after=" + receiver.length());
            }
            if (!strBeforeMin.equals(receiver.toString())) {
                throw new RuntimeException("[oracle:mincap-content] metamorphic violation: minimizeCapacity must preserve content before=" + strBeforeMin + " after=" + receiver.toString());
            }
            if (receiver.toCharArray().length != receiver.length()) {
                throw new RuntimeException("[oracle:chararray-length] metamorphic violation: toCharArray length must equal builder length charArray=" + receiver.toCharArray().length + " length=" + receiver.length());
            }
        } catch (RuntimeException t) {
            boolean validation = false;
            for (Class<?> c = t.getClass(); c != null; c = c.getSuperclass()) {
                String n = c.getName();
                if ("java.lang.IllegalArgumentException".equals(n) || "java.lang.NumberFormatException".equals(n)) {
                    validation = true;
                    break;
                }
            }
            if (validation) {
                return;
            }

            boolean root = t instanceof ArrayIndexOutOfBoundsException;
            if (root) {
                StackTraceElement[] st = t.getStackTrace();
                boolean inRegion = false;
                for (int i = 0; i < st.length; i++) {
                    String cn = st[i].getClassName();
                    String mn = st[i].getMethodName();
                    if ("org.apache.commons.lang.text.StrBuilder".equals(cn)
                            && ("appendFixedWidthPadRight".equals(mn) || "ensureCapacity".equals(mn) || "getNullText".equals(mn))) {
                        inRegion = true;
                        break;
                    }
                }
                if (inRegion) {
                    throw t;
                }
            }

            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }
    }
}