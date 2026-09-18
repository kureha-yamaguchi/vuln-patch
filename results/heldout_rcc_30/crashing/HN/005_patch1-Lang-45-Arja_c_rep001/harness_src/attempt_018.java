package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact trigger from the failing test. On the buggy version this must
        // reproduce java.lang.StringIndexOutOfBoundsException from WordUtils.abbreviate.
        try {
            String anchor = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(anchor)) {
                throw new RuntimeException("[oracle:anchor-ret] metamorphic violation: exact regression input must abbreviate to original string input=0123456789 lower=15 upper=20 lhs=" + String.valueOf(anchor) + " rhs=0123456789");
            }
        } catch (RuntimeException t) {
            boolean throughAbbreviate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                        && "abbreviate".equals(ste.getMethodName())) {
                    throughAbbreviate = true;
                    break;
                }
            }
            if (throughAbbreviate && t instanceof StringIndexOutOfBoundsException) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            return;
        }

        // EXPLORE: vary strings and numeric arguments with the same root-cause property:
        // lower beyond the string length, upper at/beyond the string length (or -1).
        String fuzzStr = data.consumeString(64);
        if (fuzzStr == null) {
            fuzzStr = "";
        }
        if (fuzzStr.length() == 0) {
            fuzzStr = "A";
        }
        int len = fuzzStr.length();
        int lower = len + data.consumeInt(1, 32);
        int upper;
        if (data.consumeBoolean()) {
            upper = -1;
        } else if (data.consumeBoolean()) {
            upper = len + data.consumeInt(0, 32);
        } else {
            upper = data.consumeInt(0, lower);
        }
        String append = data.consumeBoolean() ? null : data.consumeAsciiString(8);

        try {
            WordUtils.abbreviate(fuzzStr, lower, upper, append);
        } catch (RuntimeException t) {
            boolean throughAbbreviate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                        && "abbreviate".equals(ste.getMethodName())) {
                    throughAbbreviate = true;
                    break;
                }
            }
            if (throughAbbreviate && t instanceof StringIndexOutOfBoundsException) {
                // Valid by construction: non-null, non-empty string; lower is beyond length;
                // upper is at/beyond length or -1 / <= lower. Per the method's own contract
                // comments, these bounds are normalized rather than rejected.
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
        }

        // MANDATORY ORACLE:
        // Contract visible in the method comments/body: if upper == -1 or > str.length(),
        // set upper to str.length(); if upper < lower, raise it to lower. For a non-empty
        // string with NO spaces and lower > length, StringUtils.indexOf(str, " ", lower)
        // is -1, so a correct implementation must return str.substring(0, str.length()),
        // i.e. the original string, with no appendToEnd added because upper == str.length().
        // A "fix" that merely suppresses the crash or skips the intended normalization would
        // violate this observable result.
        String oracleStr = data.consumeAsciiString(48).replace(" ", "");
        if (oracleStr.length() == 0) {
            oracleStr = "Z";
        }
        int oracleLen = oracleStr.length();
        int oracleLower = oracleLen + data.consumeInt(1, 32);
        int oracleUpper = data.consumeBoolean() ? -1 : oracleLen + data.consumeInt(0, 32);
        String oracleAppend = data.consumeBoolean() ? null : data.consumeAsciiString(8);

        try {
            String got = WordUtils.abbreviate(oracleStr, oracleLower, oracleUpper, oracleAppend);
            if (!oracleStr.equals(got)) {
                throw new RuntimeException("[oracle:no-space-clamp] metamorphic violation: no-space input with bounds beyond length must return original string input=" + oracleStr + " lower=" + oracleLower + " upper=" + oracleUpper + " lhs=" + String.valueOf(got) + " rhs=" + oracleStr);
            }
        } catch (RuntimeException t) {
            boolean throughAbbreviate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                        && "abbreviate".equals(ste.getMethodName())) {
                    throughAbbreviate = true;
                    break;
                }
            }
            if (throughAbbreviate && t instanceof StringIndexOutOfBoundsException) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
        }
    }
}