package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Anchor: exact regression test from StrBuilderAppendInsertTest.testLang299.
        // Contract asserted: when width > 0 and obj.toString().length() >= width,
        // appendFixedWidthPadRight must append exactly the first width characters.
        try {
            StrBuilder sb = new StrBuilder(1);
            sb.appendFixedWidthPadRight("foo", 1, '-');
            if (!"f".equals(sb.toString())) {
                throw new RuntimeException("[oracle:anchor-truncate] metamorphic violation: exact regression output input=foo width=1 lhs="
                        + sb.toString() + " rhs=f");
            }
            StrBuilder expected = new StrBuilder(1);
            expected.append("foo", 0, 1);
            if (!sb.equals(expected)) {
                throw new RuntimeException("[oracle:anchor-eq] metamorphic violation: exact regression builders differ lhs="
                        + sb.toString() + " rhs=" + expected.toString());
            }
            // Independent consistency check: equal objects must have equal hashCodes.
            if (sb.hashCode() != expected.hashCode()) {
                throw new RuntimeException("[oracle:eq-hash-anchor] consistency violation: equal builders have different hashCodes lhs="
                        + sb.hashCode() + " rhs=" + expected.hashCode());
            }
        } catch (RuntimeException t) {
            StackTraceElement[] st = t.getStackTrace();
            boolean inRegion = false;
            for (int i = 0; i < st.length; i++) {
                String cn = st[i].getClassName();
                String mn = st[i].getMethodName();
                if (("org.apache.commons.lang.text.StrBuilder".equals(cn) && "appendFixedWidthPadRight".equals(mn))
                        || ("org.apache.commons.lang.text.StrBuilder".equals(cn) && "ensureCapacity".equals(mn))
                        || ("org.apache.commons.lang.text.StrBuilder".equals(cn) && "getNullText".equals(mn))) {
                    inRegion = true;
                    break;
                }
            }
            if (t instanceof ArrayIndexOutOfBoundsException && inRegion) {
                throw new RuntimeException("[oracle:valid-crash-anchor] metamorphic violation: valid regression input crashed", t);
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
        }

        String prefix = data.consumeString(8);
        if (prefix == null) {
            prefix = "";
        }
        String s = data.consumeString(24);
        if (s == null || s.length() < 2) {
            s = "AB";
        }
        int width = data.consumeInt(1, s.length() - 1);
        char pad = (char) (data.consumeByte() & 0xff);
        int initialCapacity = data.consumeInt(0, 4);

        // Explore valid-by-construction non-null case with str.length() > width.
        // Metamorphic relation: appendFixedWidthPadRight(s, width, pad) must equal
        // append(s, 0, width) when s.length() >= width.
        try {
            StrBuilder actual = new StrBuilder(initialCapacity);
            actual.append(prefix);
            actual.appendFixedWidthPadRight(s, width, pad);

            StrBuilder expected = new StrBuilder(initialCapacity);
            expected.append(prefix);
            expected.append(s, 0, width);

            if (!actual.equals(expected)) {
                throw new RuntimeException("[oracle:truncate-eq] metamorphic violation: fixed-width right append must match prefix+substring input="
                        + s + " width=" + width + " prefix=" + prefix + " lhs=" + actual.toString() + " rhs=" + expected.toString());
            }
            if (actual.hashCode() != expected.hashCode()) {
                throw new RuntimeException("[oracle:eq-hash] consistency violation: equal builders have different hashCodes input="
                        + s + " width=" + width + " prefix=" + prefix + " lhs=" + actual.hashCode() + " rhs=" + expected.hashCode());
            }

            // Second independent consistency check on the constructed object:
            // toString().length() must equal length(), and toCharArray() length must agree too.
            int reportedLen = actual.length();
            int stringLen = actual.toString().length();
            int arrayLen = actual.toCharArray().length;
            if (reportedLen != stringLen || reportedLen != arrayLen) {
                throw new RuntimeException("[oracle:length-triple] consistency violation: length disagrees with object output reported="
                        + reportedLen + " string=" + stringLen + " array=" + arrayLen);
            }
        } catch (RuntimeException t) {
            StackTraceElement[] st = t.getStackTrace();
            boolean inRegion = false;
            for (int i = 0; i < st.length; i++) {
                String cn = st[i].getClassName();
                String mn = st[i].getMethodName();
                if (("org.apache.commons.lang.text.StrBuilder".equals(cn) && "appendFixedWidthPadRight".equals(mn))
                        || ("org.apache.commons.lang.text.StrBuilder".equals(cn) && "ensureCapacity".equals(mn))
                        || ("org.apache.commons.lang.text.StrBuilder".equals(cn) && "getNullText".equals(mn))) {
                    inRegion = true;
                    break;
                }
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            if (t instanceof ArrayIndexOutOfBoundsException && inRegion) {
                throw new RuntimeException("[oracle:valid-crash-explore] metamorphic violation: valid substring-equivalent input crashed input="
                        + s + " width=" + width + " prefix=" + prefix, t);
            }
        }

        // Explore getNullText path with valid-by-construction non-null nullText.
        // Contract asserted: with obj == null and nullText set to a non-null value,
        // appendFixedWidthPadRight(null, width, pad) must behave as if that nullText string were appended.
        String nullText = data.consumeAsciiString(12);
        if (nullText == null || nullText.length() < 2) {
            nullText = "NULL";
        }
        int nullWidth = data.consumeInt(1, nullText.length() - 1);
        char nullPad = (char) (data.consumeByte() & 0xff);
        String nullPrefix = data.consumeAsciiString(8);
        if (nullPrefix == null) {
            nullPrefix = "";
        }

        try {
            StrBuilder actualNull = new StrBuilder(1);
            actualNull.setNullText(nullText);
            actualNull.append(nullPrefix);
            actualNull.appendFixedWidthPadRight(null, nullWidth, nullPad);

            StrBuilder expectedNull = new StrBuilder(1);
            expectedNull.setNullText(nullText);
            expectedNull.append(nullPrefix);
            expectedNull.append(nullText, 0, nullWidth);

            String reportedNullText = actualNull.getNullText();
            if (!nullText.equals(reportedNullText)) {
                throw new RuntimeException("[oracle:nulltext-state2] consistency violation: getNullText disagrees with configured state lhs="
                        + reportedNullText + " rhs=" + nullText);
            }
            if (!actualNull.equals(expectedNull)) {
                throw new RuntimeException("[oracle:null-truncate-eq] metamorphic violation: null object must use nullText as source input="
                        + nullText + " width=" + nullWidth + " prefix=" + nullPrefix + " lhs=" + actualNull.toString()
                        + " rhs=" + expectedNull.toString());
            }
            if (actualNull.hashCode() != expectedNull.hashCode()) {
                throw new RuntimeException("[oracle:eq-hash-null] consistency violation: equal builders from nullText path have different hashCodes lhs="
                        + actualNull.hashCode() + " rhs=" + expectedNull.hashCode());
            }
        } catch (RuntimeException t) {
            StackTraceElement[] st = t.getStackTrace();
            boolean inRegion = false;
            for (int i = 0; i < st.length; i++) {
                String cn = st[i].getClassName();
                String mn = st[i].getMethodName();
                if (("org.apache.commons.lang.text.StrBuilder".equals(cn) && "appendFixedWidthPadRight".equals(mn))
                        || ("org.apache.commons.lang.text.StrBuilder".equals(cn) && "ensureCapacity".equals(mn))
                        || ("org.apache.commons.lang.text.StrBuilder".equals(cn) && "getNullText".equals(mn))) {
                    inRegion = true;
                    break;
                }
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            if (t instanceof ArrayIndexOutOfBoundsException && inRegion) {
                throw new RuntimeException("[oracle:valid-crash-nulltext] metamorphic violation: valid nullText-backed input crashed nullText="
                        + nullText + " width=" + nullWidth + " prefix=" + nullPrefix, t);
            }
        }
    }
}