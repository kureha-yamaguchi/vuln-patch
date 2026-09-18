package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    private static boolean checkCreateNumber(String val) {
        try {
            Object obj = NumberUtils.createNumber(val);
            if (obj == null) {
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void assertOracle(String id, boolean expected, boolean actual, String call) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow("[oracle:" + id + "] semantic mismatch: " + call
                    + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void runLiftedOracles() {
        String[] positives = new String[] {
                "12345",
                "1234.5",
                ".12345",
                "1234E5",
                "1234E+5",
                "1234E-5",
                "123.4E5",
                "-1234",
                "-1234.5",
                "-.12345",
                "-1234E5",
                "0",
                "-0",
                "01234",
                "-01234",
                "0xABC123",
                "0x0",
                "123.4E21D",
                "-221.23F",
                "22338L",
                "2."
        };
        String[] positiveIds = new String[] {
                "isnum-pos-1",
                "isnum-pos-2",
                "isnum-pos-3",
                "isnum-pos-4",
                "isnum-pos-5",
                "isnum-pos-6",
                "isnum-pos-7",
                "isnum-pos-8",
                "isnum-pos-9",
                "isnum-pos-10",
                "isnum-pos-11",
                "isnum-pos-12",
                "isnum-pos-13",
                "isnum-pos-14",
                "isnum-pos-15",
                "isnum-pos-16",
                "isnum-pos-17",
                "isnum-pos-19",
                "isnum-pos-20",
                "isnum-pos-21",
                "isnum-lang-521"
        };
        String[] positiveCreateIds = new String[] {
                "create-pos-1",
                "create-pos-2",
                "create-pos-3",
                "create-pos-4",
                "create-pos-5",
                "create-pos-6",
                "create-pos-7",
                "create-pos-8",
                "create-pos-9",
                "create-pos-10",
                "create-pos-11",
                "create-pos-12",
                "create-pos-13",
                "create-pos-14",
                "create-pos-15",
                "create-pos-16",
                "create-pos-17",
                "create-pos-19",
                "create-pos-20",
                "create-pos-21"
        };

        for (int i = 0; i < positives.length - 1; i++) {
            String val = positives[i];
            assertOracle(positiveIds[i], true, NumberUtils.isNumber(val), "NumberUtils.isNumber(\"" + val + "\")");
            assertOracle(positiveCreateIds[i], true, checkCreateNumber(val), "checkCreateNumber(\"" + val + "\")");
        }
        assertOracle(positiveIds[positives.length - 1], true, NumberUtils.isNumber(positives[positives.length - 1]),
                "NumberUtils.isNumber(\"2.\")");

        String[] negatives = new String[] {
                null,
                "",
                "--2.3",
                ".12.3",
                "-123E",
                "-123E+-212",
                "-123E2.12",
                "0xGF",
                "0xFAE-1",
                ".",
                "-0ABC123",
                "123.4E-D",
                "123.4ED",
                "1234E5l",
                "11a",
                "1a",
                "a",
                "11g",
                "11z",
                "11def",
                "11d11",
                "11 11",
                " 1111",
                "1111 ",
                "1.1L"
        };
        String[] negativeIds = new String[] {
                "isnum-neg-1",
                "isnum-neg-2",
                "isnum-neg-3",
                "isnum-neg-4",
                "isnum-neg-5",
                "isnum-neg-6",
                "isnum-neg-7",
                "isnum-neg-8",
                "isnum-neg-9",
                "isnum-neg-10",
                "isnum-neg-11",
                "isnum-neg-12",
                "isnum-neg-13",
                "isnum-neg-14",
                "isnum-neg-15",
                "isnum-neg-16",
                "isnum-neg-17",
                "isnum-neg-18",
                "isnum-neg-19",
                "isnum-neg-20",
                "isnum-neg-21",
                "isnum-neg-22",
                "isnum-neg-23",
                "isnum-neg-24",
                "isnum-lang-664"
        };
        String[] negativeCreateIds = new String[] {
                "create-neg-1",
                "create-neg-2",
                "create-neg-3",
                "create-neg-4",
                "create-neg-5",
                "create-neg-6",
                "create-neg-7",
                "create-neg-8",
                "create-neg-9",
                "create-neg-10",
                "create-neg-11",
                "create-neg-12",
                "create-neg-13",
                "create-neg-14",
                "create-neg-15",
                "create-neg-16",
                "create-neg-17",
                "create-neg-18",
                "create-neg-19",
                "create-neg-20",
                "create-neg-21",
                "create-neg-22",
                "create-neg-23",
                "create-neg-24"
        };

        for (int i = 0; i < negatives.length - 1; i++) {
            String val = negatives[i];
            String rendered = val == null ? "null" : "\"" + val + "\"";
            assertOracle(negativeIds[i], false, NumberUtils.isNumber(val), "NumberUtils.isNumber(" + rendered + ")");
            assertOracle(negativeCreateIds[i], false, checkCreateNumber(val), "checkCreateNumber(" + rendered + ")");
        }
        assertOracle(negativeIds[negatives.length - 1], false, NumberUtils.isNumber("1.1L"),
                "NumberUtils.isNumber(\"1.1L\")");
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedOracles();

        String arbitrary = data.consumeString(64);
        try {
            NumberUtils.isNumber(arbitrary);
        } catch (Throwable t) {
            return;
        }

        String base;
        try {
            int whole = data.consumeInt(0, 1000000);
            int frac = data.consumeInt(0, 1000000);
            boolean neg = data.consumeBoolean();
            base = (neg ? "-" : "") + whole + "." + frac;
        } catch (Throwable t) {
            return;
        }

        boolean plain;
        try {
            plain = NumberUtils.isNumber(base);
        } catch (Throwable t) {
            return;
        }

        boolean withLong;
        try {
            withLong = NumberUtils.isNumber(base + "L");
        } catch (Throwable t) {
            return;
        }

        // Contract pinned by the trusted LANG-664 test: a valid decimal form is accepted,
        // but the same decimal with an 'L' suffix is invalid. A patch that merely deletes
        // the failing branch or makes it unreachable would still return the wrong boolean here.
        if (!plain) {
            throw new FuzzerSecurityIssueLow("[oracle:decimal-with-long-suffix] semantic mismatch: NumberUtils.isNumber(\""
                    + base + "\") expected=true actual=false");
        }
        if (withLong) {
            throw new FuzzerSecurityIssueLow("[oracle:decimal-with-long-suffix] semantic mismatch: NumberUtils.isNumber(\""
                    + base + "L\") expected=false actual=true");
        }

        boolean ok;
        try {
            ok = NumberUtils.isNumber(base);
        } catch (Throwable t) {
            return;
        }
        if (!ok) {
            throw new FuzzerSecurityIssueLow("[oracle:accepted-decimal-recognized] semantic mismatch: NumberUtils.isNumber(\""
                    + base + "\") expected=true actual=false");
        }

        Number n;
        try {
            n = NumberUtils.createNumber(base);
        } catch (Throwable t) {
            return;
        }

        // Trusted by the original test's repeated coupling of isNumber(val) with checkCreateNumber(val):
        // for accepted plain decimals, createNumber must succeed and return non-null.
        if (n == null) {
            throw new FuzzerSecurityIssueLow("[oracle:accepted-decimal-creatable] semantic mismatch: NumberUtils.createNumber(\""
                    + base + "\") expected non-null actual=null");
        }

        int intVal;
        try {
            intVal = data.consumeInt(-1000000, 1000000);
        } catch (Throwable t) {
            return;
        }
        String canonicalInt = Integer.toString(intVal);
        int parsedInt;
        try {
            parsedInt = NumberUtils.toInt(canonicalInt, intVal + 1);
        } catch (Throwable t) {
            return;
        }

        // Oracle from known answer: the harness chooses intVal first, then builds its canonical decimal string.
        // Parsing that exact canonical string with toInt(String,int) must recover intVal, never the default.
        if (parsedInt != intVal) {
            throw new FuzzerSecurityIssueLow("[oracle:toint-roundtrip] semantic mismatch: NumberUtils.toInt(\""
                    + canonicalInt + "\", " + (intVal + 1) + ") expected=" + intVal + " actual=" + parsedInt);
        }

        String equivalent1 = data.consumeBoolean() ? "1.1L" : "1.1l";
        boolean eqResult;
        try {
            eqResult = NumberUtils.isNumber(equivalent1);
        } catch (Throwable t) {
            return;
        }

        // Equivalent-input relation: the implementation checks both 'l' and 'L' in the same suffix branch,
        // so case must not change the accept/reject result for the same decimal-with-long-suffix form.
        if (eqResult != false) {
            throw new FuzzerSecurityIssueLow("[oracle:long-suffix-case-equivalence] semantic mismatch: NumberUtils.isNumber(\""
                    + equivalent1 + "\") expected=false actual=true");
        }
    }
}