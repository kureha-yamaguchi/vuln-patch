package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final String anchorPattern = "it''s a {0,lower} 'test'!";

        try {
            Map anchorRegistry = new HashMap();
            new ExtendedMessageFormat(anchorPattern, anchorRegistry);
        } catch (Throwable t) {
            boolean inPatchedMethod = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                        && "appendQuotedString".equals(ste.getMethodName())) {
                    inPatchedMethod = true;
                    break;
                }
            }
            if (t instanceof OutOfMemoryError && inPatchedMethod) {
                throw (OutOfMemoryError) t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
        }

        String pre = data.consumeAsciiString(20).replace("'", "''").replace("{", "(").replace("}", ")");
        String mid = data.consumeAsciiString(20).replace("'", "''").replace("{", "(").replace("}", ")");
        String quoted = data.consumeAsciiString(20).replace("'", "''");
        String post = data.consumeAsciiString(20).replace("'", "''").replace("{", "(").replace("}", ")");
        String arg = data.consumeString(20);

        int variant = data.consumeInt(0, 3);
        String pattern;
        switch (variant) {
            case 0:
                pattern = pre + "''" + mid + " {0} '" + quoted + "'" + post;
                break;
            case 1:
                pattern = "'" + quoted + "' " + pre + "''" + mid + " {0}" + post;
                break;
            case 2:
                pattern = pre + " {0} " + mid + "''" + post + " '" + quoted + "'";
                break;
            default:
                pattern = pre + "''" + mid + " '" + quoted + "' {0} " + post;
                break;
        }

        ExtendedMessageFormat emf;
        String emfFormatted;
        String emfPattern;
        try {
            emf = new ExtendedMessageFormat(pattern);
            emfPattern = emf.toPattern();
            emfFormatted = emf.format(new Object[] { arg });
        } catch (Throwable t) {
            boolean inPatchedMethod = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                        && "appendQuotedString".equals(ste.getMethodName())) {
                    inPatchedMethod = true;
                    break;
                }
            }
            if (t instanceof OutOfMemoryError && inPatchedMethod) {
                throw (OutOfMemoryError) t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            return;
        }

        String mfFormatted;
        String mfPattern;
        try {
            MessageFormat mf = new MessageFormat(pattern);
            mfPattern = mf.toPattern();
            mfFormatted = mf.format(new Object[] { arg });
        } catch (Throwable t) {
            return;
        }

        /*
         * Contract/oracle:
         * ExtendedMessageFormat is a MessageFormat extension. With no custom registry/formats,
         * valid patterns must behave like MessageFormat. A patch that merely skips quote parsing
         * bookkeeping could stop the crash but still produce a different parsed pattern/output.
         */
        if (!emfFormatted.equals(mfFormatted)) {
            throw new RuntimeException(
                    "[oracle:msgfmt-eq] metamorphic violation: ExtendedMessageFormat.format must match MessageFormat.format for patterns without custom formats input="
                            + pattern + " lhs=" + emfFormatted + " rhs=" + mfFormatted);
        }
        if (!emfPattern.equals(mfPattern)) {
            throw new RuntimeException(
                    "[oracle:topattern-eq] metamorphic violation: ExtendedMessageFormat.toPattern must match MessageFormat.toPattern for patterns without custom formats input="
                            + pattern + " lhs=" + emfPattern + " rhs=" + mfPattern);
        }
    }
}