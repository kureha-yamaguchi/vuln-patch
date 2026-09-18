package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        class LowerCaseFormat extends java.text.Format {
            public StringBuffer format(Object obj, StringBuffer toAppendTo, java.text.FieldPosition pos) {
                String s = String.valueOf(obj);
                toAppendTo.append(s.toLowerCase());
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
                toAppendTo.append(s.toUpperCase());
                return toAppendTo;
            }
            public Object parseObject(String source, java.text.ParsePosition pos) {
                pos.setIndex(source == null ? 0 : source.length());
                return source;
            }
        }
        class LowerCaseFormatFactory implements org.apache.commons.lang.text.FormatFactory {
            public java.text.Format getFormat(String name, String args, java.util.Locale locale) {
                return new LowerCaseFormat();
            }
        }
        class UpperCaseFormatFactory implements org.apache.commons.lang.text.FormatFactory {
            public java.text.Format getFormat(String name, String args, java.util.Locale locale) {
                return new UpperCaseFormat();
            }
        }
        class Runner {
            private String sanitize(String s) {
                if (s == null) {
                    return "";
                }
                return s.replace("'", "").replace("{", "").replace("}", "");
            }

            private boolean stackHasAppendQuotedString(Throwable t) {
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(e.getClassName())
                            && "appendQuotedString".equals(e.getMethodName())) {
                        return true;
                    }
                }
                return false;
            }

            void run(String pattern, String arg, String expected, boolean assertOracle) {
                java.util.Map registry = new java.util.HashMap();
                registry.put("lower", new LowerCaseFormatFactory());
                registry.put("upper", new UpperCaseFormatFactory());
                try {
                    ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

                    String before = emf.toPattern();
                    String r1 = emf.format(new Object[] { arg });
                    String after = emf.toPattern();
                    if (!before.equals(after)) {
                        throw new RuntimeException("[oracle:state] metamorphic violation: toPattern changed across format input="
                                + pattern + " before=" + before + " after=" + after);
                    }

                    String r2 = emf.format(new Object[] { arg });
                    if (!r1.equals(r2)) {
                        throw new RuntimeException("[oracle:det] metamorphic violation: repeated format on same input must be deterministic input="
                                + pattern + " lhs=" + r1 + " rhs=" + r2);
                    }

                    if (assertOracle && expected != null && !expected.equals(r1)) {
                        throw new RuntimeException("[oracle:fmt] metamorphic violation: valid pattern must render escaped quote as a literal apostrophe and quoted text without quote delimiters input="
                                + pattern + " lhs=" + r1 + " rhs=" + expected);
                    }
                } catch (Throwable t) {
                    if (t instanceof RuntimeException) {
                        String m = t.getMessage();
                        if (m != null && m.startsWith("[oracle:")) {
                            throw (RuntimeException) t;
                        }
                    }
                    if (t instanceof OutOfMemoryError && stackHasAppendQuotedString(t)) {
                        throw (OutOfMemoryError) t;
                    }
                    if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                        return;
                    }
                    return;
                }
            }

            void runExplore(FuzzedDataProvider d) {
                String pre = sanitize(d.consumeAsciiString(12));
                String mid = sanitize(d.consumeAsciiString(12));
                String lit = sanitize(d.consumeAsciiString(12));
                String post = sanitize(d.consumeAsciiString(12));
                String arg = sanitize(d.consumeAsciiString(12));

                if (mid.length() == 0) {
                    mid = "x";
                }
                if (lit.length() == 0) {
                    lit = "test";
                }
                if (arg.length() == 0) {
                    arg = "DUMMY";
                }

                int variant = d.consumeInt(0, 3);
                String pattern;
                String expected;
                switch (variant) {
                    case 0:
                        pattern = pre + "''" + mid + " {0,lower} '" + lit + "'" + post;
                        expected = pre + "'" + mid + " " + arg.toLowerCase() + " " + lit + post;
                        break;
                    case 1:
                        pattern = pre + "''" + mid + "{0,lower} '" + lit + "'" + post;
                        expected = pre + "'" + mid + arg.toLowerCase() + " " + lit + post;
                        break;
                    case 2:
                        pattern = pre + "''" + mid + " {0,upper} '" + lit + "'" + post;
                        expected = pre + "'" + mid + " " + arg.toUpperCase() + " " + lit + post;
                        break;
                    default:
                        pattern = pre + "''" + mid + "{0,upper} '" + lit + "'" + post;
                        expected = pre + "'" + mid + arg.toUpperCase() + " " + lit + post;
                        break;
                }

                run(pattern, arg, expected, true);
            }
        }

        Runner runner = new Runner();

        runner.run("it''s a {0,lower} 'test'!", "DUMMY", "it's a dummy test!", true);

        int cases = 1 + data.consumeInt(0, 4);
        for (int i = 0; i < cases; i++) {
            runner.runExplore(data);
        }
    }
}