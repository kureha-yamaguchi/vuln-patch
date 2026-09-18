package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.text.Format;
import java.text.FieldPosition;
import java.text.ParsePosition;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        class LowerCaseFormat extends Format {
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                if (obj != null) {
                    toAppendTo.append(obj.toString().toLowerCase(Locale.ROOT));
                }
                return toAppendTo;
            }

            public Object parseObject(String source, ParsePosition pos) {
                if (source == null) {
                    return null;
                }
                pos.setIndex(source.length());
                return source.toLowerCase(Locale.ROOT);
            }
        }

        class UpperCaseFormat extends Format {
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                if (obj != null) {
                    toAppendTo.append(obj.toString().toUpperCase(Locale.ROOT));
                }
                return toAppendTo;
            }

            public Object parseObject(String source, ParsePosition pos) {
                if (source == null) {
                    return null;
                }
                pos.setIndex(source.length());
                return source.toUpperCase(Locale.ROOT);
            }
        }

        class LowerCaseFormatFactory implements FormatFactory {
            public Format getFormat(String name, String arguments, Locale locale) {
                return new LowerCaseFormat();
            }
        }

        class UpperCaseFormatFactory implements FormatFactory {
            public Format getFormat(String name, String arguments, Locale locale) {
                return new UpperCaseFormat();
            }
        }

        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        {
            String pattern = "it''s a {0,lower} 'test'!";
            ExtendedMessageFormat emf = null;
            String before = null;
            String formatted = null;
            String after = null;
            try {
                emf = new ExtendedMessageFormat(pattern, registry);
                before = emf.toPattern();
                formatted = emf.format(new Object[] { "DUMMY" });
                after = emf.toPattern();
            } catch (Throwable t) {
                boolean rooted = false;
                if (t instanceof OutOfMemoryError) {
                    for (StackTraceElement ste : t.getStackTrace()) {
                        if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                                && ("appendQuotedString".equals(ste.getMethodName())
                                        || "next".equals(ste.getMethodName())
                                        || "append".equals(ste.getMethodName())
                                        || "startsWith".equals(ste.getMethodName()))) {
                            rooted = true;
                            break;
                        }
                        if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                                && ("append".equals(ste.getMethodName())
                                        || "startsWith".equals(ste.getMethodName()))) {
                            rooted = true;
                            break;
                        }
                    }
                }
                if (rooted) {
                    throw (OutOfMemoryError) t;
                }
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    return;
                }
                String cn = t.getClass().getName();
                if (cn.indexOf("Invalid") >= 0 || cn.indexOf("Validation") >= 0) {
                    return;
                }
            }
            if (emf != null) {
                if (!pattern.equals(before)) {
                    throw new RuntimeException("[oracle:anchor-topattern] metamorphic violation: toPattern should preserve the supplied extended pattern input=" + pattern + " got=" + before);
                }
                if (!before.equals(after)) {
                    throw new RuntimeException("[oracle:anchor-state] metamorphic violation: format() is read-only and must not change toPattern input=" + pattern + " before=" + before + " after=" + after);
                }
                if (!"it's a dummy test!".equals(formatted)) {
                    throw new RuntimeException("[oracle:anchor-format] metamorphic violation: valid anchor pattern formatted unexpectedly input=" + pattern + " out=" + formatted);
                }
                try {
                    ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, registry);
                    String formatted2 = emf2.format(new Object[] { "DUMMY" });
                    if (!formatted.equals(formatted2)) {
                        throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: formatting after reparsing toPattern must agree input=" + pattern + " lhs=" + formatted + " rhs=" + formatted2);
                    }
                } catch (Throwable t) {
                    boolean rooted = false;
                    if (t instanceof OutOfMemoryError) {
                        for (StackTraceElement ste : t.getStackTrace()) {
                            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                                    && ("appendQuotedString".equals(ste.getMethodName())
                                            || "next".equals(ste.getMethodName())
                                            || "append".equals(ste.getMethodName())
                                            || "startsWith".equals(ste.getMethodName()))) {
                                rooted = true;
                                break;
                            }
                            if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                                    && ("append".equals(ste.getMethodName())
                                            || "startsWith".equals(ste.getMethodName()))) {
                                rooted = true;
                                break;
                            }
                        }
                    }
                    if (rooted) {
                        throw (OutOfMemoryError) t;
                    }
                }
            }
        }

        String a = data.consumeAsciiString(24);
        String b = data.consumeAsciiString(24);
        String c = data.consumeAsciiString(24);
        String arg = data.consumeAsciiString(24);

        a = a.replace('{', 'a').replace('}', 'b').replace('\'', 'c').replace(',', 'd');
        b = b.replace('{', 'a').replace('}', 'b').replace('\'', 'c').replace(',', 'd');
        c = c.replace('{', 'a').replace('}', 'b').replace('\'', 'c').replace(',', 'd');
        arg = arg.replace('{', 'a').replace('}', 'b').replace('\'', 'c').replace(',', 'd');

        if (a.length() == 0) {
            a = "A";
        }
        if (b.length() == 0) {
            b = "B";
        }
        if (c.length() == 0) {
            c = "C";
        }
        if (arg.length() == 0) {
            arg = "DuMmY";
        }

        String pattern;
        String expected;
        if (data.consumeBoolean()) {
            pattern = "''" + a + " {0,lower} '" + c + "'";
            expected = "'" + a + " " + arg.toLowerCase(Locale.ROOT) + " " + c;
        } else {
            pattern = a + "''" + b + " {0,lower} '" + c + "'";
            expected = a + "'" + b + " " + arg.toLowerCase(Locale.ROOT) + " " + c;
        }

        ExtendedMessageFormat emf = null;
        String before = null;
        String formatted = null;
        String after = null;
        try {
            emf = new ExtendedMessageFormat(pattern, registry);
            before = emf.toPattern();
            formatted = emf.format(new Object[] { arg });
            after = emf.toPattern();
        } catch (Throwable t) {
            boolean rooted = false;
            if (t instanceof OutOfMemoryError) {
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                            && ("appendQuotedString".equals(ste.getMethodName())
                                    || "next".equals(ste.getMethodName())
                                    || "append".equals(ste.getMethodName())
                                    || "startsWith".equals(ste.getMethodName()))) {
                        rooted = true;
                        break;
                    }
                    if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                            && ("append".equals(ste.getMethodName())
                                    || "startsWith".equals(ste.getMethodName()))) {
                        rooted = true;
                        break;
                    }
                }
            }
            if (rooted) {
                throw (OutOfMemoryError) t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            String cn = t.getClass().getName();
            if (cn.indexOf("Invalid") >= 0 || cn.indexOf("Validation") >= 0) {
                return;
            }
            return;
        }

        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:state] metamorphic violation: format() must not mutate toPattern input=" + pattern + " before=" + before + " after=" + after);
        }

        if (!pattern.equals(before)) {
            throw new RuntimeException("[oracle:topattern] metamorphic violation: toPattern should reproduce the valid extended pattern input=" + pattern + " got=" + before);
        }

        if (!expected.equals(formatted)) {
            throw new RuntimeException("[oracle:format] metamorphic violation: valid-by-construction lower/quote pattern formatted incorrectly input=" + pattern + " expected=" + expected + " got=" + formatted);
        }

        try {
            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, registry);
            String formatted2 = emf2.format(new Object[] { arg });
            if (!formatted.equals(formatted2)) {
                throw new RuntimeException("[oracle:roundtrip] metamorphic violation: reparsing toPattern must preserve formatting input=" + pattern + " lhs=" + formatted + " rhs=" + formatted2);
            }
        } catch (Throwable t) {
            boolean rooted = false;
            if (t instanceof OutOfMemoryError) {
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                            && ("appendQuotedString".equals(ste.getMethodName())
                                    || "next".equals(ste.getMethodName())
                                    || "append".equals(ste.getMethodName())
                                    || "startsWith".equals(ste.getMethodName()))) {
                        rooted = true;
                        break;
                    }
                    if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                            && ("append".equals(ste.getMethodName())
                                    || "startsWith".equals(ste.getMethodName()))) {
                        rooted = true;
                        break;
                    }
                }
            }
            if (rooted) {
                throw (OutOfMemoryError) t;
            }
        }
    }
}