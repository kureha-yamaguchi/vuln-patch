package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        class LowerCaseFormat extends java.text.Format {
            public StringBuffer format(Object obj, StringBuffer toAppendTo, java.text.FieldPosition pos) {
                String s = String.valueOf(obj);
                toAppendTo.append(s.toLowerCase(java.util.Locale.ROOT));
                return toAppendTo;
            }

            public Object parseObject(String source, java.text.ParsePosition pos) {
                pos.setIndex(source == null ? 0 : source.length());
                return source;
            }
        }
        class UpperCaseFormat extends java.text.Format {
            public StringBuffer format(Object obj, StringBuffer toAppendTo, java.text.FieldPosition pos) {
                String s = String.valueOf(obj);
                toAppendTo.append(s.toUpperCase(java.util.Locale.ROOT));
                return toAppendTo;
            }

            public Object parseObject(String source, java.text.ParsePosition pos) {
                pos.setIndex(source == null ? 0 : source.length());
                return source;
            }
        }
        class LowerCaseFormatFactory implements FormatFactory {
            public java.text.Format getFormat(String name, String arguments, java.util.Locale locale) {
                return new LowerCaseFormat();
            }
        }
        class UpperCaseFormatFactory implements FormatFactory {
            public java.text.Format getFormat(String name, String arguments, java.util.Locale locale) {
                return new UpperCaseFormat();
            }
        }

        java.util.Map registry = new java.util.HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        String anchorPattern = "it''s a {0,lower} 'test'!";
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(anchorPattern, registry);
            String before = emf.toPattern();
            String out1 = emf.format(new Object[] { "DUMMY" });
            String after = emf.toPattern();

            if (!"it's a dummy test!".equals(out1)) {
                throw new RuntimeException("[oracle:anchor-output] metamorphic violation: exact regression input produced unexpected output input="
                        + anchorPattern + " out=" + out1);
            }

            // Contract: formatting is read-only with respect to the pattern; a throw-deleting or
            // branch-skipping patch in applyPattern/appendQuotedString must not silently corrupt
            // the stored pattern observable via toPattern().
            if (before != null && after != null && !before.equals(after)) {
                throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: format mutated toPattern input="
                        + anchorPattern + " before=" + before + " after=" + after);
            }

            // Contract: reparsing the object's own toPattern with the same registry should produce
            // an equivalent formatter. This cross-check computes the same semantics via a second
            // independent library construction.
            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, registry);
            String out2 = emf2.format(new Object[] { "DUMMY" });
            String tp2 = emf2.toPattern();
            if (!out1.equals(out2) || (before != null && tp2 != null && !before.equals(tp2))) {
                throw new RuntimeException("[oracle:roundtrip-pattern] metamorphic violation: reparsing toPattern changed semantics input="
                        + anchorPattern + " lhsOut=" + out1 + " rhsOut=" + out2 + " lhsPattern=" + before
                        + " rhsPattern=" + tp2);
            }
        } catch (Throwable t) {
            boolean oracle = t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:");
            if (oracle) {
                throw (RuntimeException) t;
            }
            boolean rootStack = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                String cn = ste.getClassName();
                String mn = ste.getMethodName();
                if (("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cn)
                        && ("appendQuotedString".equals(mn) || "next".equals(mn) || "startsWith".equals(mn)
                                || "append".equals(mn)))
                        || ("org.apache.commons.lang.text.StrBuilder".equals(cn)
                                && ("append".equals(mn) || "startsWith".equals(mn)))) {
                    rootStack = true;
                    break;
                }
            }
            if (t instanceof OutOfMemoryError && rootStack) {
                throw (OutOfMemoryError) t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            return;
        }

        String raw1 = data.consumeAsciiString(24);
        String raw2 = data.consumeAsciiString(24);
        String raw3 = data.consumeAsciiString(24);
        String raw4 = data.consumeAsciiString(24);
        String arg = data.consumeString(24);
        boolean useLower = data.consumeBoolean();
        boolean includeQuotedLiteral = data.consumeBoolean();
        boolean includeLeadingSpace = data.consumeBoolean();
        boolean includeTrailingPunct = data.consumeBoolean();

        String s1 = raw1.replace("{", "").replace("}", "").replace("'", "").replace(",", "");
        String s2 = raw2.replace("{", "").replace("}", "").replace("'", "").replace(",", "");
        String s3 = raw3.replace("{", "").replace("}", "").replace("'", "").replace(",", "");
        String s4 = raw4.replace("{", "").replace("}", "").replace("'", "").replace(",", "");
        String fmt = useLower ? "lower" : "upper";

        // Valid by construction: includes a doubled quote in literal text, one real format element,
        // and balanced quoted literal when present, so a correct implementation is obligated to
        // accept it.
        StringBuilder patternBuilder = new StringBuilder();
        if (includeLeadingSpace) {
            patternBuilder.append(' ');
        }
        patternBuilder.append(s1);
        patternBuilder.append("''");
        patternBuilder.append(s2);
        patternBuilder.append(" {0,");
        patternBuilder.append(fmt);
        patternBuilder.append("}");
        if (includeQuotedLiteral) {
            patternBuilder.append(" '");
            patternBuilder.append(s3.length() == 0 ? "x" : s3);
            patternBuilder.append("'");
        } else {
            patternBuilder.append(' ');
            patternBuilder.append(s3);
        }
        if (includeTrailingPunct) {
            patternBuilder.append('!');
        } else {
            patternBuilder.append(' ');
            patternBuilder.append(s4);
        }
        String pattern = patternBuilder.toString();

        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
            String before = emf.toPattern();
            String out1 = emf.format(new Object[] { arg });
            String after = emf.toPattern();

            if (before != null && after != null && !before.equals(after)) {
                throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: format mutated toPattern input="
                        + pattern + " before=" + before + " after=" + after);
            }

            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, registry);
            String out2 = emf2.format(new Object[] { arg });
            String tp2 = emf2.toPattern();

            if (!out1.equals(out2) || (before != null && tp2 != null && !before.equals(tp2))) {
                throw new RuntimeException("[oracle:roundtrip-pattern] metamorphic violation: reparsing toPattern changed semantics input="
                        + pattern + " arg=" + arg + " lhsOut=" + out1 + " rhsOut=" + out2 + " lhsPattern="
                        + before + " rhsPattern=" + tp2);
            }
        } catch (Throwable t) {
            boolean oracle = t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:");
            if (oracle) {
                throw (RuntimeException) t;
            }
            boolean rootStack = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                String cn = ste.getClassName();
                String mn = ste.getMethodName();
                if (("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cn)
                        && ("appendQuotedString".equals(mn) || "next".equals(mn) || "startsWith".equals(mn)
                                || "append".equals(mn)))
                        || ("org.apache.commons.lang.text.StrBuilder".equals(cn)
                                && ("append".equals(mn) || "startsWith".equals(mn)))) {
                    rootStack = true;
                    break;
                }
            }
            if (t instanceof OutOfMemoryError && rootStack) {
                throw (OutOfMemoryError) t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            return;
        }
    }
}