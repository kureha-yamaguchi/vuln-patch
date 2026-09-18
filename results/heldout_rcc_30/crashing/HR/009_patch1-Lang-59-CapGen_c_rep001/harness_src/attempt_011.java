package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        {
            try {
                StrBuilder sb = new StrBuilder(1);
                sb.appendFixedWidthPadRight("foo", 1, '-');
                String out = sb.toString();
                if (!"f".equals(out)) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:anchor-exact-out] appendFixedWidthPadRight(\"foo\",1,'-') must truncate to width and produce \"f\", got=" + out);
                }
                if (sb.length() != 1) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:anchor-len-out] length must equal requested width 1, got=" + sb.length());
                }
            } catch (Throwable t) {
                boolean inScope = false;
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())
                            && ("appendFixedWidthPadRight".equals(e.getMethodName())
                            || "ensureCapacity".equals(e.getMethodName())
                            || "getNullText".equals(e.getMethodName()))) {
                        inScope = true;
                        break;
                    }
                }
                if (inScope && t instanceof ArrayIndexOutOfBoundsException) {
                    throw (RuntimeException) t;
                }
            }
        }

        String prefix = data.consumeAsciiString(8);
        if (prefix.length() == 0) {
            prefix = "P";
        }
        String s = data.consumeString(16);
        if (s.length() == 0) {
            s = "X";
        }
        int len = s.length();
        int selector = data.consumeInt(0, 4);
        int width;
        if (selector == 0) {
            width = Math.max(1, len - 1);
        } else if (selector == 1) {
            width = len;
        } else if (selector == 2) {
            width = len + 1;
        } else if (selector == 3) {
            width = data.consumeInt(1, len + 4);
        } else {
            width = 1;
        }
        char pad = (char) ('!' + (data.consumeInt(0, 93)));

        {
            try {
                StrBuilder witness = new StrBuilder(1);
                witness.appendFixedWidthPadRight(s, width, pad);
                String expectedAppendedRegion = witness.toString();

                StrBuilder sb = new StrBuilder(1);
                sb.append(prefix);
                int beforeLen = sb.length();
                sb.appendFixedWidthPadRight(s, width, pad);

                String full = sb.toString();

                if (sb.length() != beforeLen + width) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:prefix-growth] appendFixedWidthPadRight must increase length by exactly width; before="
                            + beforeLen + " width=" + width + " after=" + sb.length());
                }

                if (!full.startsWith(prefix)) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:prefix-preserve] appendFixedWidthPadRight appends at the end and must not alter existing prefix; prefix="
                            + prefix + " full=" + full);
                }

                String actualAppendedRegion = full.substring(beforeLen);
                if (!expectedAppendedRegion.equals(actualAppendedRegion)) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:region-isolation] appending to a non-empty builder must add the same width-sized region as appending to an empty builder; input="
                            + s + " width=" + width + " pad=" + (int) pad
                            + " expectedRegion=" + expectedAppendedRegion
                            + " actualRegion=" + actualAppendedRegion
                            + " prefix=" + prefix);
                }

                if (width <= len) {
                    StrBuilder trunc = new StrBuilder(1);
                    trunc.append(s, 0, width);
                    if (!trunc.toString().equals(actualAppendedRegion)) {
                        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                            "[oracle:boundary-truncate] when width<=input length, result must be the first width chars; input="
                                + s + " width=" + width
                                + " expected=" + trunc.toString()
                                + " actual=" + actualAppendedRegion);
                    }
                }
            } catch (Throwable t) {
                boolean inScope = false;
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())
                            && ("appendFixedWidthPadRight".equals(e.getMethodName())
                            || "ensureCapacity".equals(e.getMethodName())
                            || "getNullText".equals(e.getMethodName()))) {
                        inScope = true;
                        break;
                    }
                }
                if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) {
                    throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
                }
                if (inScope && t instanceof ArrayIndexOutOfBoundsException) {
                    throw (RuntimeException) t;
                }
            }
        }

        if (data.consumeBoolean()) {
            try {
                StrBuilder sb = new StrBuilder(1);
                sb.appendFixedWidthPadRight(null, data.consumeInt(-2, 4), pad);
            } catch (Throwable t) {
            }
        }
    }
}