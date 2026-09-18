package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.lang3.StringUtils;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        String fuzz = data.consumeString(64);
        NumberUtils.isNumber(fuzz);
        safeCreateNumberProbe(fuzz);

        verifyLiftedIsNumberTest();
        verifyStringUtilsComplement(data);
    }

    private static void verifyLiftedIsNumberTest() {
        String[] positivePairs = new String[] {
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
            "22338L"
        };

        for (int i = 0; i < positivePairs.length; i++) {
            String val = positivePairs[i];
            requireExactBoolean(
                "[oracle:lifted-test-isnumber-pos-" + (i + 1) + "]",
                true,
                NumberUtils.isNumber(val),
                "NumberUtils.isNumber(\"" + val + "\")"
            );
            requireExactBoolean(
                "[oracle:lifted-test-createnumber-pos-" + (i + 1) + "]",
                true,
                checkCreateNumber(val),
                "checkCreateNumber(\"" + val + "\")"
            );
        }

        requireExactBoolean(
            "[oracle:lifted-test-isnumber-neg-1]",
            false,
            NumberUtils.isNumber(null),
            "NumberUtils.isNumber(null)"
        );
        requireExactBoolean(
            "[oracle:lifted-test-createnumber-neg-1]",
            false,
            checkCreateNumber(null),
            "checkCreateNumber(null)"
        );

        String[] negativePairs = new String[] {
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
            "1111 "
        };

        for (int i = 0; i < negativePairs.length; i++) {
            String val = negativePairs[i];
            requireExactBoolean(
                "[oracle:lifted-test-isnumber-neg-" + (i + 2) + "]",
                false,
                NumberUtils.isNumber(val),
                "NumberUtils.isNumber(\"" + val + "\")"
            );
            requireExactBoolean(
                "[oracle:lifted-test-createnumber-neg-" + (i + 2) + "]",
                false,
                checkCreateNumber(val),
                "checkCreateNumber(\"" + val + "\")"
            );
        }

        requireExactBoolean(
            "[oracle:lifted-test-lang-521]",
            true,
            NumberUtils.isNumber("2."),
            "NumberUtils.isNumber(\"2.\")"
        );

        requireExactBoolean(
            "[oracle:lifted-test-lang-664]",
            false,
            NumberUtils.isNumber("1.1L"),
            "NumberUtils.isNumber(\"1.1L\")"
        );
    }

    private static void verifyStringUtilsComplement(FuzzedDataProvider data) {
        String subject;
        switch (data.consumeInt(0, 4)) {
            case 0:
                subject = null;
                break;
            case 1:
                subject = "";
                break;
            case 2:
                subject = " ";
                break;
            case 3:
                subject = data.consumeAsciiString(16);
                break;
            default:
                String s = data.consumeString(16);
                subject = s.length() == 0 ? "x" : s;
                break;
        }

        try {
            boolean empty = StringUtils.isEmpty(subject);
            boolean notEmpty = StringUtils.isNotEmpty(subject);
            /* Docs shown for StringUtils state isEmpty checks null-or-empty and isNotEmpty checks not empty and not null,
               so for every input they are logical complements. A throw-deleting or guard-skipping patch in either function
               could leave one side stale while the other changes; this cross-check remains observable without inventing
               any expected value for arbitrary fuzzed strings. */
            if (empty == notEmpty) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:stringutils-empty-notempty-complement] metamorphic violation: "
                        + "StringUtils.isEmpty(s) must equal !StringUtils.isNotEmpty(s)"
                        + " input=" + quote(subject)
                        + " empty=" + empty
                        + " notEmpty=" + notEmpty
                );
            }
        } catch (RuntimeException e) {
            if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) {
                throw e;
            }
        }
    }

    private static boolean checkCreateNumber(String val) {
        try {
            Object obj = NumberUtils.createNumber(val);
            return obj != null;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void safeCreateNumberProbe(String val) {
        try {
            NumberUtils.createNumber(val);
        } catch (RuntimeException ignored) {
        }
    }

    private static void requireExactBoolean(String oracleId, boolean expected, boolean actual, String expr) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:unnamed-check] " + oracleId + " semantic mismatch: " + expr + " expected=" + expected + " actual=" + actual
            );
        }
    }

    private static String quote(String s) {
        if (s == null) {
            return "null";
        }
        return "\"" + s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t") + "\"";
    }
}