package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        java.util.Map registry = new java.util.HashMap();
        registry.put("lower", null);
        registry.put("upper", null);

        String anchorPattern = "it''s a {0,lower} 'test'!";
        try {
            new ExtendedMessageFormat(anchorPattern, registry);
        } catch (Throwable t) {
            boolean cleanRejection =
                    t instanceof IllegalArgumentException ||
                    t instanceof NumberFormatException ||
                    t.getClass().getName().contains("Validation") ||
                    t.getClass().getName().contains("Invalid");
            if (!cleanRejection) {
                boolean throughPatchedMethod = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                            && "appendQuotedString".equals(ste.getMethodName())) {
                        throughPatchedMethod = true;
                        break;
                    }
                }
                if (t instanceof OutOfMemoryError && throughPatchedMethod) {
                    throw (OutOfMemoryError) t;
                }
            }
        }

        String prefix = data.consumeAsciiString(16);
        String middle = data.consumeAsciiString(16);
        String suffix = data.consumeAsciiString(16);
        String quoted = data.consumeAsciiString(12);
        String arg = data.consumeString(24);

        prefix = prefix.replace("{", "").replace("}", "").replace("'", "");
        middle = middle.replace("{", "").replace("}", "").replace("'", "");
        suffix = suffix.replace("{", "").replace("}", "").replace("'", "");
        quoted = quoted.replace("'", "");

        String[] patterns = new String[3];
        patterns[0] = "''" + prefix + " {0} '" + quoted + "' " + suffix;
        patterns[1] = prefix + "''" + middle + " {0} '" + quoted + "' " + suffix;
        patterns[2] = prefix + " {0} '" + quoted + "' " + middle + "''" + suffix;

        for (int i = 0; i < patterns.length; i++) {
            String pattern = patterns[i];
            try {
                ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
                String before = emf.toPattern();

                String lhs;
                try {
                    lhs = emf.format(new Object[] { arg });
                } catch (Throwable t) {
                    boolean cleanRejection =
                            t instanceof IllegalArgumentException ||
                            t instanceof NumberFormatException ||
                            t.getClass().getName().contains("Validation") ||
                            t.getClass().getName().contains("Invalid");
                    if (!cleanRejection) {
                        boolean throughPatchedMethod = false;
                        for (StackTraceElement ste : t.getStackTrace()) {
                            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                                    && "appendQuotedString".equals(ste.getMethodName())) {
                                throughPatchedMethod = true;
                                break;
                            }
                        }
                        if (t instanceof OutOfMemoryError && throughPatchedMethod) {
                            throw (OutOfMemoryError) t;
                        }
                    }
                    continue;
                }

                String rhs;
                try {
                    rhs = new java.text.MessageFormat(pattern).format(new Object[] { arg });
                } catch (Throwable t) {
                    continue;
                }

                if (!lhs.equals(rhs)) {
                    throw new RuntimeException("[oracle:msgfmt-equiv] metamorphic violation: empty-registry ExtendedMessageFormat must match MessageFormat for standard patterns input=" + pattern + " lhs=" + lhs + " rhs=" + rhs);
                }

                try {
                    String reparsed = new ExtendedMessageFormat(before, registry).toPattern();
                    if (!before.equals(reparsed)) {
                        throw new RuntimeException("[oracle:toPattern-idem] metamorphic violation: toPattern should be a stable reparseable representation input=" + pattern + " lhs=" + before + " rhs=" + reparsed);
                    }
                } catch (Throwable t) {
                    if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                        throw (RuntimeException) t;
                    }
                }
            } catch (Throwable t) {
                boolean cleanRejection =
                        t instanceof IllegalArgumentException ||
                        t instanceof NumberFormatException ||
                        t.getClass().getName().contains("Validation") ||
                        t.getClass().getName().contains("Invalid");
                if (cleanRejection) {
                    continue;
                }
                boolean throughPatchedMethod = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                            && "appendQuotedString".equals(ste.getMethodName())) {
                        throughPatchedMethod = true;
                        break;
                    }
                }
                if (t instanceof OutOfMemoryError && throughPatchedMethod) {
                    throw (OutOfMemoryError) t;
                }
                if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                    throw (RuntimeException) t;
                }
            }
        }
    }
}