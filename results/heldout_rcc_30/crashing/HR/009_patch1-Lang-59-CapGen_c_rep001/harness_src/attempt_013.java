package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        try {
            runInsertConsistency(data);
        } catch (RuntimeException t) {
            if (isOracle(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        } catch (Error t) {
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void runAnchor() {
        try {
            StrBuilder sb = new StrBuilder(1);
            sb.appendFixedWidthPadRight("foo", 1, '-');
            String out = sb.toString();
            if (!"f".equals(out)) {
                throw new RuntimeException("[oracle:anchor-out] metamorphic violation: exact trigger output input=foo,width=1,pad=- lhs=" + out + " rhs=f");
            }
        } catch (RuntimeException t) {
            if (isOracle(t)) {
                throw t;
            }
            if (isRootCause(t) || isCleanRejection(t)) {
                return;
            }
        } catch (Error t) {
            if (isRootCause(t)) {
                return;
            }
        }
    }

    private static void runInsertConsistency(FuzzedDataProvider data) {
        String prefix = data.consumeAsciiString(8);
        String source = data.consumeString(24);
        String inserted = data.consumeAsciiString(8);
        String suffix = data.consumeAsciiString(8);
        int width = data.consumeInt(1, 16);
        char pad = (char) (' ' + (data.consumeByte() & 0x3f));
        int initialCapacity = data.consumeInt(1, 8);

        if (source == null) {
            source = "";
        }
        if (source.length() == 0) {
            source = "X";
        }

        boolean useStringBuffer = data.consumeBoolean();
        Object obj = useStringBuffer ? new StringBuffer(source) : source;

        StrBuilder lhs = new StrBuilder(initialCapacity);
        lhs.append(prefix);

        try {
            lhs.appendFixedWidthPadRight(obj, width, pad);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        } catch (Error t) {
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        int fixedRegionStart = prefix.length();
        int insertIndex = fixedRegionStart + data.consumeInt(0, width);

        try {
            lhs.insert(insertIndex, inserted);
            lhs.append(suffix);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        StrBuilder rhs = new StrBuilder(initialCapacity);
        try {
            rhs.append(prefix);
            if (source.length() >= width) {
                rhs.append(source, 0, width);
            } else {
                rhs.append(source);
                rhs.appendPadding(width - source.length(), pad);
            }
            rhs.insert(insertIndex, inserted);
            rhs.append(suffix);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        String lhsString;
        String rhsString;
        try {
            lhsString = lhs.toString();
            rhsString = rhs.toString();
        } catch (RuntimeException t) {
            return;
        }

        /* Contract used:
         * appendFixedWidthPadRight appends exactly width characters: either the leftmost width chars
         * of obj.toString(), or the whole string followed by pad chars. insert then shifts existing
         * content at the chosen index. Therefore building with appendFixedWidthPadRight and then insert
         * must agree with building the same fixed-width segment via the real append(String,int,int) /
         * appendPadding APIs and then performing the same insert.
         * This catches silent wrong-state fixes where the throw disappears but buffer/size content
         * or later mutations disagree.
         */
        if (!lhsString.equals(rhsString) || lhs.length() != rhs.length()) {
            throw new RuntimeException(
                "[oracle:insert-consistency] metamorphic violation: fixed-width+insert disagreement" +
                " source=" + source +
                " objKind=" + (useStringBuffer ? "StringBuffer" : "String") +
                " width=" + width +
                " pad=" + (int) pad +
                " prefix=" + prefix +
                " inserted=" + inserted +
                " suffix=" + suffix +
                " insertIndex=" + insertIndex +
                " lhs=" + lhsString +
                " rhs=" + rhsString +
                " lhsLen=" + lhs.length() +
                " rhsLen=" + rhs.length());
        }
    }

    private static boolean isOracle(Throwable t) {
        return t instanceof RuntimeException
            && t.getMessage() != null
            && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            String cls = st[i].getClassName();
            String m = st[i].getMethodName();
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("appendFixedWidthPadRight".equals(m)
                        || "ensureCapacity".equals(m)
                        || "getNullText".equals(m))) {
                return true;
            }
        }
        return false;
    }
}