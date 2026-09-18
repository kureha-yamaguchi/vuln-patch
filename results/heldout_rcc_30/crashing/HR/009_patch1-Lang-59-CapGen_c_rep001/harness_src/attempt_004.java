package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Arrays;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            StrBuilder sb = new StrBuilder(1);
            sb.appendFixedWidthPadRight("foo", 1, '-');
            if (!"f".equals(sb.toString())) {
                throw new RuntimeException("[oracle:anchor-post] metamorphic violation: exact failing-test postcondition input=foo width=1 lhs=" + sb.toString() + " rhs=f");
            }
            char[] a = sb.toCharArray();
            char[] b = sb.getChars((char[]) null);
            if (!Arrays.equals(a, b)) {
                throw new RuntimeException("[oracle:char-copy] metamorphic violation: toCharArray/getChars disagree lhs=" + new String(a) + " rhs=" + new String(b));
            }
            char[] c = new char[sb.length() + 2];
            sb.getChars(0, sb.length(), c, 1);
            char[] d = new char[sb.length()];
            System.arraycopy(c, 1, d, 0, sb.length());
            if (!Arrays.equals(a, d)) {
                throw new RuntimeException("[oracle:char-slice] metamorphic violation: getChars(start,end,...) disagrees lhs=" + new String(a) + " rhs=" + new String(d));
            }
        } catch (RuntimeException t) {
            boolean root = t instanceof ArrayIndexOutOfBoundsException;
            if (root) {
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    String cls = st[i].getClassName();
                    String m = st[i].getMethodName();
                    if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                            && ("appendFixedWidthPadRight".equals(m) || "ensureCapacity".equals(m) || "getNullText".equals(m))) {
                        throw t;
                    }
                }
            }
        }

        int width = data.consumeInt(1, 32);
        char padChar = (char) (data.consumeByte() & 0xff);
        int initialCapacity = data.consumeInt(0, 4);
        String prefix = data.consumeAsciiString(8);
        boolean useNullObject = data.consumeBoolean();

        String source;
        Object obj;
        StrBuilder actual = new StrBuilder(initialCapacity);
        StrBuilder expected = new StrBuilder(initialCapacity);

        actual.append(prefix);
        expected.append(prefix);

        if (useNullObject) {
            String nullText = data.consumeString(48);
            while (nullText.length() < width) {
                nullText = nullText + "N";
            }
            actual.setNullText(nullText);
            expected.setNullText(nullText);
            source = nullText;
            obj = null;
        } else {
            String s = data.consumeString(48);
            while (s.length() < width) {
                s = s + "X";
            }
            source = s;
            obj = s;
        }

        boolean inputValid = source != null && width > 0 && source.length() >= width;

        try {
            actual.appendFixedWidthPadRight(obj, width, padChar);

            if (source.length() >= width) {
                expected.append(source, 0, width);
            } else {
                expected.append(source);
                expected.appendPadding(width - source.length(), padChar);
            }

            String actualString = actual.toString();
            String expectedString = expected.toString();

            /* Contract used:
             * appendFixedWidthPadRight appends a fixed-width field: if the string is longer than width,
             * only the leftmost width characters are appended; otherwise the remainder is padded.
             * A throw-deleting or branch-skipping patch can stop crashing yet still append the wrong content.
             */
            if (!actualString.equals(expectedString)) {
                throw new RuntimeException("[oracle:fixedwidth-content] metamorphic violation: equivalent construction disagrees input=" + source + " width=" + width + " pad=" + (int) padChar + " lhs=" + actualString + " rhs=" + expectedString);
            }

            /* Sound consistency cross-check:
             * toCharArray(), getChars(null), and getChars(start,end,dst,off) are all documented as copies
             * of the builder contents. They must agree for every correct implementation.
             */
            char[] arr1 = actual.toCharArray();
            char[] arr2 = actual.getChars((char[]) null);
            if (!Arrays.equals(arr1, arr2)) {
                throw new RuntimeException("[oracle:char-copy] metamorphic violation: copied content disagrees lhs=" + new String(arr1) + " rhs=" + new String(arr2));
            }

            char[] dest = new char[actual.length() + 3];
            actual.getChars(0, actual.length(), dest, 2);
            char[] arr3 = new char[actual.length()];
            System.arraycopy(dest, 2, arr3, 0, actual.length());
            if (!Arrays.equals(arr1, arr3)) {
                throw new RuntimeException("[oracle:char-slice] metamorphic violation: ranged copy disagrees lhs=" + new String(arr1) + " rhs=" + new String(arr3));
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (IndexOutOfBoundsException e) {
            if (!(e instanceof ArrayIndexOutOfBoundsException) || !inputValid) {
                return;
            }
            StackTraceElement[] st = e.getStackTrace();
            for (int i = 0; i < st.length; i++) {
                String cls = st[i].getClassName();
                String m = st[i].getMethodName();
                if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                        && ("appendFixedWidthPadRight".equals(m) || "ensureCapacity".equals(m) || "getNullText".equals(m))) {
                    throw e;
                }
            }
        } catch (NullPointerException e) {
            return;
        } catch (RuntimeException e) {
            throw e;
        }
    }
}