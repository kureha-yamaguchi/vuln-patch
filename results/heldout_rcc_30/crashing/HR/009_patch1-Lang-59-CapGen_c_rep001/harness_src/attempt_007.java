package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.List;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        String base = data.consumeString(32);
        if (base == null) {
            return;
        }

        if (base.length() == 0) {
            base = "X";
        }

        char padChar = (char) (data.consumeByte() & 0xff);
        boolean useNullObject = data.consumeBoolean();
        boolean useBuilderObject = data.consumeBoolean();

        String effective;
        Object obj;
        String customNullText = null;

        if (useNullObject) {
            customNullText = data.consumeString(32);
            if (customNullText == null || customNullText.length() == 0) {
                customNullText = "N";
            }
            effective = customNullText;
            obj = null;
        } else if (useBuilderObject) {
            StrBuilder src = new StrBuilder(Math.max(1, base.length()));
            src.append(base);
            effective = src.toString();
            obj = src;
        } else {
            effective = base;
            obj = base;
        }

        if (effective.length() == 0) {
            return;
        }

        int width = data.consumeInt(1, effective.length());

        StrBuilder sb = new StrBuilder(1);
        if (customNullText != null) {
            sb.setNullText(customNullText);
        }

        try {
            sb.appendFixedWidthPadRight(obj, width, padChar);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isOracleFailure(t) || isRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }

        // Contract: appendFixedWidthPadRight appends exactly width characters when width > 0.
        // A throw-deleting or truncation-skipping patch can leave internal views inconsistent even if
        // top-level toString() looks plausible. Cross-check a tokenizer view over the builder against
        // an independently tokenized snapshot string; both represent the same contents for any correct
        // implementation.
        try {
            List lhs = sb.asTokenizer().getTokenList();
            List rhs = new StrTokenizer(sb.toString()).getTokenList();
            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:tokenizer-view] metamorphic violation: asTokenizer disagrees with tokenizing toString() input=" + sb.toString() + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable t) {
            if (isOracleFailure(t)) {
                throwUnchecked(t);
            }
        }

        // Equivalent-input relation: this method uses obj.toString() for non-null objects and getNullText()
        // for null. Therefore, for the same builder state and width, passing null with custom nullText must
        // agree with passing that nullText explicitly as a String.
        if (customNullText != null) {
            try {
                StrBuilder viaNull = new StrBuilder(1);
                viaNull.setNullText(customNullText);
                viaNull.appendFixedWidthPadRight(null, width, padChar);

                StrBuilder viaString = new StrBuilder(1);
                viaString.setNullText(customNullText);
                viaString.appendFixedWidthPadRight(customNullText, width, padChar);

                if (!viaNull.toString().equals(viaString.toString())) {
                    throw new RuntimeException("[oracle:nulltext-equiv] metamorphic violation: null object path disagrees with explicit nullText input=" + customNullText + " width=" + width + " lhs=" + viaNull.toString() + " rhs=" + viaString.toString());
                }
            } catch (Throwable t) {
                if (isOracleFailure(t)) {
                    throwUnchecked(t);
                }
            }
        }
    }

    private static void runAnchor() {
        try {
            StrBuilder sb = new StrBuilder(1);
            sb.appendFixedWidthPadRight("foo", 1, '-');
            String out = sb.toString();
            if (!"f".equals(out)) {
                throw new RuntimeException("[oracle:anchor-exact] metamorphic violation: expected=f actual=" + out);
            }
        } catch (Throwable t) {
            if (isRootCause(t) || isOracleFailure(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            String cls = st[i].getClassName();
            String method = st[i].getMethodName();
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("appendFixedWidthPadRight".equals(method)
                        || "ensureCapacity".equals(method)
                        || "getNullText".equals(method))) {
                return true;
            }
        }
        return false;
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }
}