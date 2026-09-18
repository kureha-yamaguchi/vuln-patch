package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final Object bad = new Object() {
            @Override
            public String toString() {
                return null;
            }
        };

        anchorShiftedSliceChar(bad);
        anchorShiftedSliceString(bad);

        int prefixCount = data.consumeInt(1, 3);
        int tailCount = data.consumeInt(0, 3);

        Object[] arr = new Object[prefixCount + 1 + tailCount];
        for (int i = 0; i < prefixCount; i++) {
            String s = data.consumeAsciiString(8);
            if (s == null || s.length() == 0) {
                s = "p" + i;
            }
            arr[i] = s;
        }
        arr[prefixCount] = bad;
        for (int i = 0; i < tailCount; i++) {
            int choice = data.consumeInt(0, 3);
            if (choice == 0) {
                arr[prefixCount + 1 + i] = null;
            } else if (choice == 1) {
                arr[prefixCount + 1 + i] = data.consumeAsciiString(6);
            } else if (choice == 2) {
                arr[prefixCount + 1 + i] = Long.valueOf(data.consumeInt(-1000, 1000));
            } else {
                String s = data.consumeString(6);
                arr[prefixCount + 1 + i] = s;
            }
        }

        int start = prefixCount;
        int end = arr.length;

        char sepChar = (char) (data.consumeByte() & 0x7f);
        if (sepChar == 0) {
            sepChar = '/';
        }

        String sepString;
        if (data.consumeBoolean()) {
            sepString = "/";
        } else {
            sepString = data.consumeAsciiString(3);
            if (sepString == null) {
                sepString = "";
            }
        }

        exploreShiftedSliceChar(arr, sepChar, start, end);
        exploreShiftedSliceString(arr, sepString, start, end);

        String empty = StringUtils.join(new Object[0], sepString, 0, 0);
        if (!empty.equals(StringUtils.trimToEmpty(null))
                || !empty.equals(StringUtils.stripToEmpty(null))
                || !empty.equals(StringUtils.left("x", -1))
                || !empty.equals(StringUtils.substring("x", 1, 1))) {
            throw new RuntimeException("[oracle:empty-constant-agreement-extra] metamorphic violation: EMPTY-backed APIs disagree");
        }
    }

    private static void anchorShiftedSliceChar(Object bad) {
        Object[] arr = new Object[] { "x", bad };
        exploreShiftedSliceChar(arr, '/', 1, 2);
    }

    private static void anchorShiftedSliceString(Object bad) {
        Object[] arr = new Object[] { "x", bad };
        exploreShiftedSliceString(arr, "/", 1, 2);
    }

    private static void exploreShiftedSliceChar(Object[] arr, char sep, int start, int end) {
        String full = null;
        String prefix = null;
        String slice = null;
        Throwable sliceFailure = null;

        try {
            full = StringUtils.join(arr, sep, 0, end);
            prefix = StringUtils.join(arr, sep, 0, start);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        try {
            slice = StringUtils.join(arr, sep, start, end);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                sliceFailure = t;
            } else {
                return;
            }
        }

        try {
            String sepString = String.valueOf(sep);
            String suffix = StringUtils.substring(full, StringUtils.length(prefix) + StringUtils.length(sepString));

            if (sliceFailure != null) {
                throw new RuntimeException(
                        "[oracle:shifted-slice-char] metamorphic violation: valid shifted slice threw while equivalent full join succeeded inputStart="
                                + start + " inputEnd=" + end + " prefix=" + prefix + " full=" + full + " sep=" + sep,
                        sliceFailure);
            }

            /* Contract used: join(array, separator, start, end) concatenates exactly the elements in [start,end),
             * inserting the separator between adjacent selected elements. Therefore when start > 0 and end > start,
             * the full join over [0,end) must equal the join over [0,start) + separator + the join over [start,end).
             * A throw-deleting or overfit patch can make the crashing shifted slice silently wrong even if exact seeds pass;
             * comparing the shifted slice against the suffix extracted from the successful full join catches that. */
            if (!slice.equals(suffix)) {
                throw new RuntimeException(
                        "[oracle:shifted-slice-char] metamorphic violation: shifted slice must equal suffix of full join arrLen="
                                + arr.length + " start=" + start + " end=" + end + " sep=" + sep + " slice=" + slice
                                + " suffix=" + suffix + " full=" + full + " prefix=" + prefix);
            }

            int sepPos = StringUtils.lastIndexOf(full, sep);
            if (sepPos >= 0) {
                String tail = StringUtils.substring(full, sepPos + 1);
                if (!StringUtils.endsWith(full, String.valueOf(sep) + tail)) {
                    throw new RuntimeException(
                            "[oracle:tail-end-char] metamorphic violation: full join must end with its last separator and tail full="
                                    + full + " sep=" + sep + " tail=" + tail);
                }
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            throw t;
        }
    }

    private static void exploreShiftedSliceString(Object[] arr, String sep, int start, int end) {
        String full = null;
        String prefix = null;
        String slice = null;
        Throwable sliceFailure = null;

        try {
            full = StringUtils.join(arr, sep, 0, end);
            prefix = StringUtils.join(arr, sep, 0, start);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        try {
            slice = StringUtils.join(arr, sep, start, end);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                sliceFailure = t;
            } else {
                return;
            }
        }

        try {
            String effectiveSep = sep == null ? StringUtils.EMPTY : sep;
            String suffix = StringUtils.substring(full, StringUtils.length(prefix) + StringUtils.length(effectiveSep));

            if (sliceFailure != null) {
                throw new RuntimeException(
                        "[oracle:shifted-slice-string] metamorphic violation: valid shifted slice threw while equivalent full join succeeded inputStart="
                                + start + " inputEnd=" + end + " prefix=" + prefix + " full=" + full + " sep=" + sep,
                        sliceFailure);
            }

            /* Same documented concatenation guarantee as the char overload, for the String-separator overload.
             * This targets the patched capacity computation with inputs where the bad element is first in the selected
             * slice but not first in the whole array, flipping the patched condition around the boundary startIndex. */
            if (!slice.equals(suffix)) {
                throw new RuntimeException(
                        "[oracle:shifted-slice-string] metamorphic violation: shifted slice must equal suffix of full join arrLen="
                                + arr.length + " start=" + start + " end=" + end + " sep=" + sep + " slice=" + slice
                                + " suffix=" + suffix + " full=" + full + " prefix=" + prefix);
            }

            int sepPos = StringUtils.lastIndexOf(full, effectiveSep);
            if (sepPos >= 0) {
                String tail = StringUtils.substring(full, sepPos + StringUtils.length(effectiveSep));
                if (!StringUtils.endsWith(full, effectiveSep + tail)) {
                    throw new RuntimeException(
                            "[oracle:tail-end-string] metamorphic violation: full join must end with its last separator and tail full="
                                    + full + " sep=" + sep + " tail=" + tail);
                }
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            throw t;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement frame : t.getStackTrace()) {
            if ("org.apache.commons.lang3.StringUtils".equals(frame.getClassName())
                    && ("join".equals(frame.getMethodName())
                            || "length".equals(frame.getMethodName()))) {
                return true;
            }
            if ("org.apache.commons.lang3.CharRange".equals(frame.getClassName())
                    && "toString".equals(frame.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}