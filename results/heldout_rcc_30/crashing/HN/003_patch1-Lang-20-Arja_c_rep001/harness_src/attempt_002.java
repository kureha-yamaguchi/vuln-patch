package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final Object nullToStringObject = new Object() {
            @Override
            public String toString() {
                return null;
            }
        };

        final String[] ARRAY_LIST = { "foo", "bar", "baz" };
        final String[] EMPTY_ARRAY_LIST = {};
        final String[] NULL_ARRAY_LIST = { null };
        final Object[] NULL_TO_STRING_LIST = { nullToStringObject };
        final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
        final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };
        final char SEPARATOR_CHAR = ';';
        final String TEXT_LIST_CHAR = "foo;bar;baz";

        // ANCHOR: exact trigger/setup from the project tests.
        try {
            String r1 = StringUtils.join((Object[]) null, ',');
            if (r1 != null) {
                throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: expected null for join((Object[])null, ',') input=null lhs=" + r1 + " rhs=null");
            }

            String r2 = StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR);
            if (!TEXT_LIST_CHAR.equals(r2)) {
                throw new RuntimeException("[oracle:anchor-array-char] metamorphic violation: expected TEXT_LIST_CHAR input=" + java.util.Arrays.toString(ARRAY_LIST) + " lhs=" + r2 + " rhs=" + TEXT_LIST_CHAR);
            }

            String r3 = StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR);
            if (!"".equals(r3)) {
                throw new RuntimeException("[oracle:anchor-empty-char] metamorphic violation: expected empty string input=[] lhs=" + r3 + " rhs=");
            }

            String r4 = StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR);
            if (!";;foo".equals(r4)) {
                throw new RuntimeException("[oracle:anchor-mixed-array-char] metamorphic violation: expected ';;foo' input=" + java.util.Arrays.toString(MIXED_ARRAY_LIST) + " lhs=" + r4 + " rhs=;;foo");
            }

            String r5 = StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR);
            if (!"foo;2".equals(r5)) {
                throw new RuntimeException("[oracle:anchor-mixed-type-char] metamorphic violation: expected 'foo;2' input=" + java.util.Arrays.toString(MIXED_TYPE_LIST) + " lhs=" + r5 + " rhs=foo;2");
            }

            String r6 = StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1);
            if (!"/".equals(r6)) {
                throw new RuntimeException("[oracle:anchor-range-char-1] metamorphic violation: expected '/' input=" + java.util.Arrays.toString(MIXED_ARRAY_LIST) + " lhs=" + r6 + " rhs=/");
            }

            String r7 = StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1);
            if (!"foo".equals(r7)) {
                throw new RuntimeException("[oracle:anchor-range-char-2] metamorphic violation: expected 'foo' input=" + java.util.Arrays.toString(MIXED_TYPE_LIST) + " lhs=" + r7 + " rhs=foo");
            }

            String r8 = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(r8)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-char] metamorphic violation: expected 'null' for single element whose toString() returns null input=" + java.util.Arrays.toString(NULL_TO_STRING_LIST) + " lhs=" + r8 + " rhs=null");
            }

            String r9 = StringUtils.join((Object[]) null);
            if (r9 != null) {
                throw new RuntimeException("[oracle:anchor-null-array-object] metamorphic violation: expected null for join((Object[])null) input=null lhs=" + r9 + " rhs=null");
            }

            String r10 = StringUtils.join((Object) null);
            if (!"".equals(r10)) {
                throw new RuntimeException("[oracle:anchor-single-null-object] metamorphic violation: expected empty string for join((Object)null) lhs=" + r10 + " rhs=");
            }

            String r11 = StringUtils.join(EMPTY_ARRAY_LIST);
            if (!"".equals(r11)) {
                throw new RuntimeException("[oracle:anchor-empty-array-object] metamorphic violation: expected empty string lhs=" + r11 + " rhs=");
            }

            String r12 = StringUtils.join(NULL_ARRAY_LIST);
            if (!"".equals(r12)) {
                throw new RuntimeException("[oracle:anchor-null-array-element] metamorphic violation: expected empty string input=[null] lhs=" + r12 + " rhs=");
            }

            String r13 = StringUtils.join(NULL_TO_STRING_LIST);
            if (!"null".equals(r13)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-object] metamorphic violation: expected 'null' for join(NULL_TO_STRING_LIST) lhs=" + r13 + " rhs=null");
            }

            String r14 = StringUtils.join(new String[] { null, "a", "" });
            if (!"a".equals(r14)) {
                throw new RuntimeException("[oracle:anchor-null-leading-object] metamorphic violation: expected 'a' lhs=" + r14 + " rhs=a");
            }
        } catch (RuntimeException t) {
            String msg = t.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                throw t;
            }
            boolean throughJoin = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName()) && "join".equals(ste.getMethodName())) {
                    throughJoin = true;
                    break;
                }
            }
            if ((t instanceof IllegalArgumentException) || (t instanceof NumberFormatException)) {
                return;
            }
            if ((t instanceof NullPointerException) && throughJoin) {
                throw t;
            }
            return;
        }

        int len = data.consumeInt(1, 6);
        Object[] arr = new Object[len];
        for (int i = 0; i < len; i++) {
            int kind = data.consumeInt(0, 4);
            switch (kind) {
                case 0:
                    arr[i] = null;
                    break;
                case 1:
                    arr[i] = data.consumeAsciiString(12);
                    break;
                case 2:
                    arr[i] = Integer.valueOf(data.consumeInt(-1000, 1000));
                    break;
                case 3:
                    arr[i] = Long.valueOf(data.consumeInt(-1000, 1000));
                    break;
                default:
                    arr[i] = data.consumeBoolean() ? "" : data.consumeString(8);
                    break;
            }
        }

        int startIndex = data.consumeInt(0, len - 1);
        int endIndex = data.consumeInt(startIndex + 1, len);
        arr[startIndex] = nullToStringObject;

        char sepChar = (char) data.consumeInt(1, 126);
        String sepString = String.valueOf(sepChar);

        // EXPLORE root cause property: a valid, in-range startIndex whose first joined element is non-null
        // but its toString() returns null. Correct behavior is to produce the text "null" as shown by the tests,
        // not to dereference that null while sizing the StringBuilder.
        try {
            String lhs = StringUtils.join(arr, sepChar, startIndex, endIndex);
            String rhs = StringUtils.join(arr, sepString, startIndex, endIndex);

            // Contract/oracle: these overloads differ only in separator type; for a 1-char separator string,
            // equivalent inputs must yield identical output. A throw-deleting patch that silently drops the first
            // element or mishandles null-toString would break this sibling agreement.
            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:char-vs-string-range] metamorphic violation: equivalent join overloads disagreed input=" + java.util.Arrays.toString(arr) + " start=" + startIndex + " end=" + endIndex + " lhs=" + lhs + " rhs=" + rhs);
            }

            // Contract/oracle from the regression tests: when the joined slice is exactly the single special
            // element, the result must be the literal text \"null\".
            if (endIndex == startIndex + 1) {
                if (!"null".equals(lhs)) {
                    throw new RuntimeException("[oracle:single-null-tostring-range] metamorphic violation: expected 'null' for single element whose toString() returns null input=" + java.util.Arrays.toString(arr) + " start=" + startIndex + " end=" + endIndex + " lhs=" + lhs + " rhs=null");
                }
            }
        } catch (RuntimeException t) {
            String msg = t.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                throw t;
            }
            boolean throughJoin = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName()) && "join".equals(ste.getMethodName())) {
                    throughJoin = true;
                    break;
                }
            }
            if ((t instanceof IllegalArgumentException) || (t instanceof NumberFormatException)) {
                return;
            }
            if ((t instanceof NullPointerException) && throughJoin) {
                throw t;
            }
            return;
        }

        try {
            Object[] full = new Object[len];
            System.arraycopy(arr, 0, full, 0, len);
            full[0] = nullToStringObject;

            String lhs = StringUtils.join(full);
            String rhs = StringUtils.join(full, "", 0, full.length);

            // Contract/oracle: joining an object array with no separator is equivalent to joining the same
            // full range with the empty-string separator. A patch that only avoids the crash by skipping work
            // would violate this observable equality.
            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:object-vs-empty-sep] metamorphic violation: join(array) must equal join(array,\"\",0,len) input=" + java.util.Arrays.toString(full) + " lhs=" + lhs + " rhs=" + rhs);
            }

            if (full.length == 1 && !"null".equals(lhs)) {
                throw new RuntimeException("[oracle:single-null-tostring-object] metamorphic violation: expected 'null' for join(singleObjectWhoseToStringReturnsNull) lhs=" + lhs + " rhs=null");
            }
        } catch (RuntimeException t) {
            String msg = t.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                throw t;
            }
            boolean throughJoin = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName()) && "join".equals(ste.getMethodName())) {
                    throughJoin = true;
                    break;
                }
            }
            if ((t instanceof IllegalArgumentException) || (t instanceof NumberFormatException)) {
                return;
            }
            if ((t instanceof NullPointerException) && throughJoin) {
                throw t;
            }
            return;
        }

        try {
            String emptyFromJoinChar = StringUtils.join(arr, sepChar, 1, 1);
            String emptyFromJoinString = StringUtils.join(arr, sepString, 1, 1);
            String emptyFromTrim = StringUtils.trimToEmpty(null);
            String emptyFromStrip = StringUtils.stripToEmpty(null);
            String emptyFromSubstring = StringUtils.substring("", 0);
            String emptyFromLeft = StringUtils.left("abc", -1);

            // Shared-state agreement on StringUtils.EMPTY:
            // docs say all of these return an empty String for these inputs. If join's empty-range result stops
            // agreeing with the other EMPTY readers/writers, a patch could hide the original throw yet still
            // return the wrong sentinel.
            if (!emptyFromJoinChar.equals(emptyFromTrim)
                    || !emptyFromJoinString.equals(emptyFromStrip)
                    || !emptyFromJoinChar.equals(emptyFromSubstring)
                    || !emptyFromJoinString.equals(emptyFromLeft)) {
                throw new RuntimeException("[oracle:empty-sentinel-agreement] metamorphic violation: EMPTY-returning APIs disagreed lhs1=" + emptyFromJoinChar + " lhs2=" + emptyFromJoinString + " rhs1=" + emptyFromTrim + " rhs2=" + emptyFromStrip + " rhs3=" + emptyFromSubstring + " rhs4=" + emptyFromLeft);
            }
        } catch (RuntimeException t) {
            String msg = t.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                throw t;
            }
            boolean throughJoin = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName()) && "join".equals(ste.getMethodName())) {
                    throughJoin = true;
                    break;
                }
            }
            if ((t instanceof IllegalArgumentException) || (t instanceof NumberFormatException)) {
                return;
            }
            if ((t instanceof NullPointerException) && throughJoin) {
                throw t;
            }
        }
    }
}