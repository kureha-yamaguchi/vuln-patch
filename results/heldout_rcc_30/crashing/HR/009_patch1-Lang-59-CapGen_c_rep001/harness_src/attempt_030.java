package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.io.Reader;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Anchor: exact failing test input from Lang299.
        try {
            StrBuilder anchor = new StrBuilder(1);
            try {
                anchor.appendFixedWidthPadRight("foo", 1, '-');
            } catch (RuntimeException t) {
                boolean rootCause = t instanceof ArrayIndexOutOfBoundsException;
                if (rootCause) {
                    StackTraceElement[] st = t.getStackTrace();
                    for (int i = 0; i < st.length; i++) {
                        if ("org.apache.commons.lang.text.StrBuilder".equals(st[i].getClassName())
                                && "appendFixedWidthPadRight".equals(st[i].getMethodName())) {
                            throw new RuntimeException("[oracle:anchor-flip] valid exact seed crashed: source=foo width=1", t);
                        }
                    }
                }
            }

            String anchorText = anchor.toString();
            if (!"f".equals(anchorText)) {
                throw new RuntimeException("[oracle:anchor-flip] exact seed result mismatch lhs=" + anchorText + " rhs=f");
            }

            try {
                Reader r = anchor.asReader();
                char[] buf = new char[8];
                int n;
                int total = 0;
                StringBuffer seen = new StringBuffer();
                while ((n = r.read(buf)) != -1) {
                    total += n;
                    seen.append(new String(buf, 0, n));
                }
                if (total != anchor.length() || !seen.toString().equals(anchor.toString())) {
                    throw new RuntimeException("[oracle:reader-count] reader/content disagreement total=" + total
                            + " len=" + anchor.length() + " seen=" + seen + " text=" + anchor.toString());
                }
            } catch (Exception ignored) {
            }
        } catch (RuntimeException t) {
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
        }

        String prefix = data.consumeString(16);
        String source = data.consumeString(32);
        if (source.length() == 0) {
            source = "A";
        }
        char pad = (char) (data.consumeByte() & 0xff);

        int len = source.length();
        int choice = data.consumeInt(0, 2);
        int width;
        if (choice == 0) {
            width = Math.max(1, len - 1);
        } else if (choice == 1) {
            width = len;
        } else {
            width = len + 1;
        }

        // Valid-by-construction boundary case around the patched condition.
        // Contract from the method body: for width > 0, it appends exactly width characters:
        // either the first width chars of obj.toString() when strLen >= width,
        // or the full string plus padding otherwise.
        try {
            StrBuilder actual = new StrBuilder(Math.max(1, prefix.length()));
            actual.append(prefix);
            actual.minimizeCapacity();

            boolean validInput = width > 0 && source != null;

            try {
                actual.appendFixedWidthPadRight(source, width, pad);
            } catch (RuntimeException t) {
                boolean rootCause = validInput && (t instanceof ArrayIndexOutOfBoundsException);
                if (rootCause) {
                    StackTraceElement[] st = t.getStackTrace();
                    for (int i = 0; i < st.length; i++) {
                        if ("org.apache.commons.lang.text.StrBuilder".equals(st[i].getClassName())
                                && "appendFixedWidthPadRight".equals(st[i].getMethodName())) {
                            throw new RuntimeException("[oracle:flip-boundary] valid boundary case crashed prefixLen="
                                    + prefix.length() + " sourceLen=" + len + " width=" + width, t);
                        }
                    }
                }
            }

            try {
                StrBuilder expected = new StrBuilder();
                expected.append(prefix);
                if (len >= width) {
                    expected.append(source, 0, width);
                } else {
                    expected.append(source);
                    expected.appendPadding(width - len, pad);
                }

                String lhs = actual.toString();
                String rhs = expected.toString();
                if (!lhs.equals(rhs)) {
                    throw new RuntimeException("[oracle:flip-boundary] equivalent construction mismatch inputLen="
                            + len + " width=" + width + " lhs=" + lhs + " rhs=" + rhs);
                }

                // Independent oracle: the builder's reader must reproduce exactly the builder's own text
                // and exactly builder.length() characters. This would still fail for a throw-deleting patch
                // that silently corrupts size/content bookkeeping.
                Reader r = actual.asReader();
                char[] buf = new char[Math.max(1, actual.length() + 2)];
                int n;
                int total = 0;
                StringBuffer seen = new StringBuffer();
                while ((n = r.read(buf)) != -1) {
                    total += n;
                    seen.append(new String(buf, 0, n));
                }
                if (total != actual.length() || !seen.toString().equals(actual.toString())) {
                    throw new RuntimeException("[oracle:reader-count] reader/content disagreement total=" + total
                            + " len=" + actual.length() + " seen=" + seen + " text=" + actual.toString());
                }
            } catch (IllegalArgumentException ignored) {
            } catch (RuntimeException t) {
                if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                    throw t;
                }
            } catch (Exception ignored) {
            }
        } catch (IllegalArgumentException ignored) {
        } catch (RuntimeException t) {
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
        }

        // getNullText path: null input should behave like the configured nullText string.
        String nullText = data.consumeString(24);
        if (nullText.length() == 0) {
            nullText = "N";
        }
        int nlen = nullText.length();
        int nchoice = data.consumeInt(0, 2);
        int nwidth;
        if (nchoice == 0) {
            nwidth = Math.max(1, nlen - 1);
        } else if (nchoice == 1) {
            nwidth = nlen;
        } else {
            nwidth = nlen + 1;
        }

        try {
            StrBuilder viaNull = new StrBuilder(Math.max(1, prefix.length()));
            viaNull.append(prefix);
            viaNull.setNullText(nullText);
            viaNull.minimizeCapacity();

            StrBuilder viaExplicit = new StrBuilder(Math.max(1, prefix.length()));
            viaExplicit.append(prefix);
            viaExplicit.minimizeCapacity();

            boolean nullCaseCompleted = false;
            try {
                viaNull.appendFixedWidthPadRight(null, nwidth, pad);
                nullCaseCompleted = true;
            } catch (RuntimeException t) {
                boolean rootCause = nwidth > 0 && (t instanceof ArrayIndexOutOfBoundsException);
                if (rootCause) {
                    StackTraceElement[] st = t.getStackTrace();
                    for (int i = 0; i < st.length; i++) {
                        if ("org.apache.commons.lang.text.StrBuilder".equals(st[i].getClassName())
                                && "appendFixedWidthPadRight".equals(st[i].getMethodName())) {
                            throw new RuntimeException("[oracle:nulltext-boundary] valid nullText case crashed nullTextLen="
                                    + nlen + " width=" + nwidth, t);
                        }
                    }
                }
            }

            try {
                viaExplicit.appendFixedWidthPadRight(nullText, nwidth, pad);
            } catch (RuntimeException ignored) {
            }

            if (nullCaseCompleted) {
                String lhs = viaNull.toString();
                String rhs = viaExplicit.toString();
                if (!lhs.equals(rhs)) {
                    throw new RuntimeException("[oracle:nulltext-boundary] null input differs from explicit nullText lhs="
                            + lhs + " rhs=" + rhs + " nullText=" + nullText + " width=" + nwidth);
                }
            }
        } catch (IllegalArgumentException ignored) {
        } catch (RuntimeException t) {
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
        }
    }
}