package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static Object nullToStringObject() {
        return new Object() {
            @Override
            public String toString() {
                return null;
            }
        };
    }

    private static Object normalObject(final String s) {
        return s;
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] anchor = new Object[] { nullToStringObject() };

        StringUtils.join((Object[]) null, '/');
        StringUtils.join(anchor, '/', 0, 1);

        String fixedExpected = "null";
        String gotString = StringUtils.join(anchor, "/", 0, 1);
        if (!fixedExpected.equals(gotString)) {
            throw new RuntimeException("[oracle:exact-null-tostring-string] metamorphic violation: expected=" + fixedExpected + " got=" + gotString);
        }

        int len = data.consumeInt(1, 6);
        Object[] array = new Object[len];
        int badIndex = data.consumeInt(0, len - 1);
        for (int i = 0; i < len; i++) {
            if (i == badIndex) {
                array[i] = nullToStringObject();
            } else {
                String s = data.consumeString(12);
                if (s == null) {
                    s = "";
                }
                array[i] = normalObject(s);
            }
        }

        int start = badIndex;
        int end = data.consumeInt(start + 1, len);

        char sepChar = (char) (data.consumeByte() & 0x7f);
        String sepString = data.consumeAsciiString(4);
        if (sepString == null) {
            sepString = "";
        }

        String charJoin = StringUtils.join(array, sepChar, start, end);
        String stringJoin = StringUtils.join(array, String.valueOf(sepChar), start, end);

        if (!charJoin.equals(stringJoin)) {
            throw new RuntimeException("[oracle:char-string-slice-agree] metamorphic violation: charJoin=" + charJoin + " stringJoin=" + stringJoin);
        }

        String stringJoin2 = StringUtils.join(array, sepString, start, end);
        String left = StringUtils.left(stringJoin2, StringUtils.length(stringJoin2));
        if (!stringJoin2.equals(left)) {
            throw new RuntimeException("[oracle:left-identity] metamorphic violation: joined=" + stringJoin2 + " left=" + left);
        }

        if (!StringUtils.EMPTY.equals(StringUtils.trimToEmpty(null))) {
            throw new RuntimeException("[oracle:shared-empty-trim] metamorphic violation");
        }
        if (!StringUtils.EMPTY.equals(StringUtils.stripToEmpty(null))) {
            throw new RuntimeException("[oracle:shared-empty-strip] metamorphic violation");
        }
        if (!StringUtils.EMPTY.equals(StringUtils.substring("", 0))) {
            throw new RuntimeException("[oracle:shared-empty-substring] metamorphic violation");
        }
    }
}