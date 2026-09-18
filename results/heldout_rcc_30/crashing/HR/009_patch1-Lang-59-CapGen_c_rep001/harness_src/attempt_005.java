package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Arrays;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            StrBuilder anchor = new StrBuilder(1);
            anchor.appendFixedWidthPadRight("foo", 1, '-');
            StrBuilder anchorExpected = new StrBuilder(1);
            anchorExpected.append("foo", 0, 1);
            /* setLength is documented to add filler of unicode zero when expanding.
             * After appending a fixed-width field of width 1, expanding to length 3 must
             * therefore match the builder constructed from the independently truncated input.
             * A throw-deleting patch or a write-past-width bug leaves stale characters in the
             * buffer, which this relation exposes even if the immediate toString() looked fine.
             */
            anchor.setLength(3);
            anchorExpected.setLength(3);
            if (!Arrays.equals(anchor.toCharArray(), anchorExpected.toCharArray())) {
                throw new RuntimeException("[oracle:setlen-anchor] metamorphic violation: setLength expansion leaked hidden characters lhs="
                        + Arrays.toString(anchor.toCharArray()) + " rhs=" + Arrays.toString(anchorExpected.toCharArray()));
            }
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
        }

        String source = data.consumeString(32);
        if (source != null && source.length() > 0) {
            int width = data.consumeInt(1, source.length());
            char pad = (char) (data.consumeByte() & 0xff);

            try {
                StrBuilder actual = new StrBuilder(1);
                actual.appendFixedWidthPadRight(source, width, pad);

                StrBuilder expected = new StrBuilder(1);
                expected.append(source, 0, width);

                /* appendFixedWidthPadRight(Object,int,char) appends exactly width characters;
                 * if the source is longer, it must append only the leftmost width characters.
                 * Expanding both builders with setLength(source.length()) must therefore yield
                 * identical arrays, because the newly exposed suffix is documented to be unicode
                 * zero filler rather than stale data from an over-copy.
                 */
                actual.setLength(source.length());
                expected.setLength(source.length());

                char[] lhs = actual.toCharArray();
                char[] rhs = expected.toCharArray();
                if (!Arrays.equals(lhs, rhs)) {
                    throw new RuntimeException("[oracle:setlen-fuzz] metamorphic violation: width-truncation boundary sourceLen="
                            + source.length() + " width=" + width + " lhs=" + Arrays.toString(lhs) + " rhs=" + Arrays.toString(rhs));
                }
            } catch (RuntimeException t) {
                if (isRootCause(t) && width > 0 && width <= source.length()) {
                    throw t;
                }
            }
        }

        String nullText = data.consumeAsciiString(32);
        if (nullText != null && nullText.length() > 0) {
            int width = data.consumeInt(1, nullText.length());
            char pad = (char) (data.consumeByte() & 0xff);

            try {
                StrBuilder actual = new StrBuilder(1);
                actual.setNullText(nullText);
                actual.appendFixedWidthPadRight(null, width, pad);

                StrBuilder expected = new StrBuilder(1);
                expected.append(nullText, 0, width);

                /* getNullText() supplies the real string used when obj == null.
                 * For any correct implementation, appending null with non-null nullText is
                 * equivalent to appending that nullText directly, so after the same setLength
                 * expansion both builders must expose the same character array.
                 */
                actual.setLength(nullText.length());
                expected.setLength(nullText.length());

                char[] lhs = actual.toCharArray();
                char[] rhs = expected.toCharArray();
                if (!Arrays.equals(lhs, rhs)) {
                    throw new RuntimeException("[oracle:nulltext-setlen] metamorphic violation: nullText path disagrees getNullText="
                            + actual.getNullText() + " width=" + width + " lhs=" + Arrays.toString(lhs) + " rhs=" + Arrays.toString(rhs));
                }
            } catch (RuntimeException t) {
                if (isRootCause(t) && width > 0 && width <= nullText.length()) {
                    throw t;
                }
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("appendFixedWidthPadRight".equals(method)
                        || "ensureCapacity".equals(method)
                        || "getNullText".equals(method))) {
                return true;
            }
        }
        return false;
    }
}