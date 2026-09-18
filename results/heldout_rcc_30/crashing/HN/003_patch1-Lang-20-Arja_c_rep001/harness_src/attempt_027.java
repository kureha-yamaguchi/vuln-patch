package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final String[] ARRAY_LIST = { "foo", "bar", "baz" };
    private static final String[] EMPTY_ARRAY_LIST = {};
    private static final String[] NULL_ARRAY_LIST = { null };
    private static final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
    private static final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };
    private static final char SEPARATOR_CHAR = ';';
    private static final String TEXT_LIST_CHAR = "foo;bar;baz";

    /*
     * This mirrors the test fixture exactly: join must accept an element whose
     * toString() returns null and render it as "null" rather than crash.
     */
    private static final Object[] NULL_TO_STRING_LIST = {
        new Object() {
            @Override
            public String toString() {
                return null;
            }
        }
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchorChecks();
        runSharedEmptyChecks();

        try {
            runExploreChecks(data);
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return;
            }
            throw e;
        } catch (Error e) {
            throw e;
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void runAnchorChecks() {
        checkEquals("anchor-null-array-char", null, StringUtils.join((Object[]) null, ','));
        checkEquals("anchor-array-char", TEXT_LIST_CHAR, StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR));
        checkEquals("anchor-empty-array-char", "", StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR));
        checkEquals("anchor-mixed-array-char", ";;foo", StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR));
        checkEquals("anchor-mixed-type-char", "foo;2", StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR));
        checkEquals("anchor-mixed-array-char-range", "/", StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1));
        checkEquals("anchor-mixed-type-char-range-a", "foo", StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1));
        checkRootCauseOrEquals("anchor-null-to-string-char-range", "null", true, new JoinCall() {
            @Override
            public String call() {
                return StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            }
        });
        checkEquals("anchor-mixed-type-char-range-b", "foo/2", StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2));
        checkEquals("anchor-mixed-type-char-range-c", "2", StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2));
        checkEquals("anchor-mixed-type-char-range-d", "", StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1));

        checkEquals("anchor-null-array", null, StringUtils.join((Object[]) null));
        checkEquals("anchor-empty-varargs", "", StringUtils.join());
        checkEquals("anchor-single-null-vararg", "", StringUtils.join((Object) null));
        checkEquals("anchor-empty-array", "", StringUtils.join(EMPTY_ARRAY_LIST));
        checkEquals("anchor-null-array-list", "", StringUtils.join(NULL_ARRAY_LIST));
        checkRootCauseOrEquals("anchor-null-to-string", "null", true, new JoinCall() {
            @Override
            public String call() {
                return StringUtils.join(NULL_TO_STRING_LIST);
            }
        });
        checkEquals("anchor-abc", "abc", StringUtils.join(new String[] { "a", "b", "c" }));
        checkEquals("anchor-null-a-empty", "a", StringUtils.join(new String[] { null, "a", "" }));
        checkEquals("anchor-mixed-array", "foo", StringUtils.join(MIXED_ARRAY_LIST));
        checkEquals("anchor-mixed-type", "foo2", StringUtils.join(MIXED_TYPE_LIST));
    }

    private static void runSharedEmptyChecks() {
        String empty1 = StringUtils.trimToEmpty(null);
        String empty2 = StringUtils.stripToEmpty(null);
        String empty3 = StringUtils.substring("", 0);
        String empty4 = StringUtils.substring("", 0, 0);
        String empty5 = StringUtils.left("abc", -1);

        if (!"".equals(empty1) || !"".equals(empty2) || !"".equals(empty3) || !"".equals(empty4) || !"".equals(empty5)) {
            throw new RuntimeException("[oracle:empty-shared] metamorphic violation: EMPTY-backed readers disagree input=nulls lhs="
                + safe(empty1) + "," + safe(empty2) + "," + safe(empty3) + "," + safe(empty4) + "," + safe(empty5));
        }
    }

    private static void runExploreChecks(FuzzedDataProvider data) {
        int len = data.consumeInt(1, 6);
        Object[] arr = new Object[len];
        int badIndex = data.consumeInt(0, len - 1);

        for (int i = 0; i < len; i++) {
            if (i == badIndex) {
                arr[i] = NULL_TO_STRING_LIST[0];
            } else {
                int kind = data.consumeInt(0, 4);
                switch (kind) {
                    case 0:
                        arr[i] = null;
                        break;
                    case 1:
                        arr[i] = data.consumeAsciiString(12);
                        break;
                    case 2:
                        arr[i] = Long.valueOf(data.consumeInt(-1000000, 1000000));
                        break;
                    case 3:
                        arr[i] = data.consumeString(12);
                        break;
                    default:
                        arr[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                }
            }
        }

        char sepChar = (char) data.consumeInt(0, 127);
        String sepString = String.valueOf(sepChar);

        final int start = badIndex;
        final int end = badIndex + 1;

        checkRootCauseOrEquals("explore-single-char", "null", true, new JoinCall() {
            @Override
            public String call() {
                return StringUtils.join(arr, sepChar, start, end);
            }
        });

        checkRootCauseOrEquals("explore-single-string", "null", true, new JoinCall() {
            @Override
            public String call() {
                return StringUtils.join(arr, sepString, start, end);
            }
        });

        int rangeStart = data.consumeInt(0, badIndex);
        int rangeEnd = data.consumeInt(badIndex + 1, len);

        String lhs;
        String rhs;
        try {
            lhs = StringUtils.join(arr, sepChar, rangeStart, rangeEnd);
            rhs = StringUtils.join(arr, sepString, rangeStart, rangeEnd);
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return;
            }
            if (isRootCauseInJoin(e) && isValidByConstruction(arr, rangeStart, rangeEnd, badIndex)) {
                throw e;
            }
            return;
        }

        /*
         * Contract/oracle: the char-separator and String-separator overloads are sibling APIs
         * over the same operation; using String.valueOf(sepChar) must produce the same joined text.
         * A patch that merely suppresses the crash or skips appending the first element would break
         * this agreement or the known one-element result below.
         */
        if (!safeEquals(lhs, rhs)) {
            throw new RuntimeException("[oracle:sep-overload] metamorphic violation: char and String separator overloads disagree input=start="
                + rangeStart + ",end=" + rangeEnd + ",sep=" + (int) sepChar + " lhs=" + safe(lhs) + " rhs=" + safe(rhs));
        }

        try {
            String noSep = StringUtils.join(arr, "", rangeStart, rangeEnd);
            String direct = StringUtils.join(slice(arr, rangeStart, rangeEnd));
            if (!safeEquals(noSep, direct)) {
                throw new RuntimeException("[oracle:no-sep-range] metamorphic violation: range join with empty separator must equal join of sliced range input=start="
                    + rangeStart + ",end=" + rangeEnd + " lhs=" + safe(noSep) + " rhs=" + safe(direct));
            }
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return;
            }
            if (isRootCauseInJoin(e) && isValidByConstruction(arr, rangeStart, rangeEnd, badIndex)) {
                throw e;
            }
        }
    }

    private static Object[] slice(Object[] arr, int start, int end) {
        Object[] out = new Object[end - start];
        for (int i = 0; i < out.length; i++) {
            out[i] = arr[start + i];
        }
        return out;
    }

    private static void checkEquals(String id, String expected, String actual) {
        if (!safeEquals(expected, actual)) {
            throw new RuntimeException("[oracle:" + id + "] metamorphic violation: expected=" + safe(expected) + " actual=" + safe(actual));
        }
    }

    private static void checkRootCauseOrEquals(String id, String expected, boolean validByConstruction, JoinCall call) {
        try {
            String actual = call.call();
            checkEquals(id, expected, actual);
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return;
            }
            if (validByConstruction && isRootCauseInJoin(e)) {
                throw e;
            }
            if (e.getMessage() != null && e.getMessage().startsWith("[oracle:")) {
                throw e;
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static boolean isValidByConstruction(Object[] arr, int start, int end, int badIndex) {
        return arr != null && start >= 0 && end >= start && end <= arr.length && start <= badIndex && badIndex < end && arr[badIndex] == NULL_TO_STRING_LIST[0];
    }

    private static boolean isRootCauseInJoin(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement ste = trace[i];
            if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName()) && "join".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String safe(String s) {
        return s == null ? "null" : s;
    }

    private interface JoinCall {
        String call();
    }
}