package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final String anchorPattern = "it''s a {0,lower} 'test'!";
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(anchorPattern, new HashMap());
            try {
                emf.format(new Object[] { "DUMMY" });
            } catch (Throwable t) {
                boolean cleanRejection =
                        t instanceof IllegalArgumentException ||
                        t instanceof NumberFormatException ||
                        t.getClass().getName().indexOf("Validate") >= 0 ||
                        t.getClass().getName().indexOf("Invalid") >= 0;
                if (!cleanRejection) {
                    boolean throughPatchedMethod = false;
                    StackTraceElement[] st = t.getStackTrace();
                    for (int i = 0; i < st.length; i++) {
                        if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(st[i].getClassName())
                                && "appendQuotedString".equals(st[i].getMethodName())) {
                            throughPatchedMethod = true;
                            break;
                        }
                    }
                    if (t instanceof OutOfMemoryError && throughPatchedMethod) {
                        throw (OutOfMemoryError) t;
                    }
                }
            }
        } catch (Throwable t) {
            boolean cleanRejection =
                    t instanceof IllegalArgumentException ||
                    t instanceof NumberFormatException ||
                    t.getClass().getName().indexOf("Validate") >= 0 ||
                    t.getClass().getName().indexOf("Invalid") >= 0;
            if (!cleanRejection) {
                boolean throughPatchedMethod = false;
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(st[i].getClassName())
                            && "appendQuotedString".equals(st[i].getMethodName())) {
                        throughPatchedMethod = true;
                        break;
                    }
                }
                if (t instanceof OutOfMemoryError && throughPatchedMethod) {
                    throw (OutOfMemoryError) t;
                }
            }
        }

        String rawA = data.consumeAsciiString(24);
        String rawB = data.consumeAsciiString(24);
        String rawC = data.consumeAsciiString(24);
        String rawD = data.consumeAsciiString(24);
        String rawArg = data.consumeAsciiString(24);

        StringBuilder a = new StringBuilder();
        for (int i = 0; i < rawA.length(); i++) {
            char ch = rawA.charAt(i);
            if (ch != '\'' && ch != '{' && ch != '}') {
                a.append(ch);
            }
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < rawB.length(); i++) {
            char ch = rawB.charAt(i);
            if (ch != '\'' && ch != '{' && ch != '}') {
                b.append(ch);
            }
        }
        StringBuilder c = new StringBuilder();
        for (int i = 0; i < rawC.length(); i++) {
            char ch = rawC.charAt(i);
            if (ch != '\'' && ch != '{' && ch != '}') {
                c.append(ch);
            }
        }
        StringBuilder d = new StringBuilder();
        for (int i = 0; i < rawD.length(); i++) {
            char ch = rawD.charAt(i);
            if (ch != '\'' && ch != '{' && ch != '}') {
                d.append(ch);
            }
        }

        if (a.length() == 0) {
            a.append('A');
        }
        if (b.length() == 0) {
            b.append('B');
        }
        if (c.length() == 0) {
            c.append('C');
        }
        if (d.length() == 0) {
            d.append('D');
        }

        String arg = rawArg.length() == 0 ? "X" : rawArg;

        String pattern;
        switch (data.consumeInt(0, 3)) {
            case 0:
                pattern = a.toString() + "''" + b.toString() + " {0} '" + c.toString() + "'" + d.toString();
                break;
            case 1:
                pattern = "'" + a.toString() + "' " + b.toString() + "''" + c.toString() + " {0} " + d.toString();
                break;
            case 2:
                pattern = a.toString() + " {0} " + b.toString() + "''" + c.toString() + " '" + d.toString() + "'";
                break;
            default:
                pattern = a.toString() + "''" + b.toString() + " '{0}' " + c.toString() + " {0} " + d.toString();
                break;
        }

        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern);
            MessageFormat mf = new MessageFormat(pattern);

            String before = emf.toPattern();
            String lhs = emf.format(new Object[] { arg });
            String after = emf.toPattern();
            String rhs = mf.format(new Object[] { arg });

            /* Contract/oracle:
               For patterns that use only standard MessageFormat syntax (no custom registry formats),
               ExtendedMessageFormat must behave like MessageFormat, and formatting is read-only:
               toPattern() should not change across format(). A patch that merely skips the escaped-quote
               bookkeeping can avoid the crash but silently produce wrong quoting/parsing results. */
            if (!before.equals(after)) {
                throw new RuntimeException("[oracle:topattern] metamorphic violation: format changed toPattern input=" + pattern + " lhs=" + before + " rhs=" + after);
            }
            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:msgfmt-eq] metamorphic violation: ExtendedMessageFormat must match MessageFormat for standard patterns input=" + pattern + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }

            boolean cleanRejection =
                    t instanceof IllegalArgumentException ||
                    t instanceof NumberFormatException ||
                    t.getClass().getName().indexOf("Validate") >= 0 ||
                    t.getClass().getName().indexOf("Invalid") >= 0;
            if (cleanRejection) {
                return;
            }

            boolean throughPatchedMethod = false;
            StackTraceElement[] st = t.getStackTrace();
            for (int i = 0; i < st.length; i++) {
                if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(st[i].getClassName())
                        && "appendQuotedString".equals(st[i].getMethodName())) {
                    throughPatchedMethod = true;
                    break;
                }
            }

            if (t instanceof OutOfMemoryError && throughPatchedMethod) {
                throw (OutOfMemoryError) t;
            }
        }
    }
}