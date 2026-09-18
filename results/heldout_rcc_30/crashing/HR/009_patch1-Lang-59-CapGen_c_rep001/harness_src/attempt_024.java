package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exerciseAnchor();
        exerciseNullTextAnchor();

        int width = data.consumeInt(1, 32);
        int extra = data.consumeInt(1, 32);
        char pad = (char) (data.consumeByte() & 0xff);
        String prefix = data.consumeString(16);
        String source = data.consumeString(width + extra + 8);
        if (source.length() <= width) {
            source = source + repeat('X', width - source.length() + 1);
        }

        runValidExplicitCase(prefix, source, width, pad);
        runValidNullTextCase(prefix, source, width, pad);

        int safeWidth = data.consumeInt(0, 32);
        String safeSource = data.consumeString(32);
        if (safeSource.length() > safeWidth) {
            safeSource = safeSource.substring(0, safeWidth);
        }
        runPostConditionCase(prefix, safeSource, safeWidth, pad);
        runPostConditionNullCase(prefix, safeSource, safeWidth, pad);
    }

    private static void exerciseAnchor() {
        try {
            StrBuilder sb = new StrBuilder(1);
            sb.appendFixedWidthPadRight("foo", 1, '-');
            if (!"f".equals(sb.toString())) {
                throw new RuntimeException("[oracle:anchor-mid] metamorphic violation: exact seed should append first width chars input=foo width=1 actual=" + sb.toString());
            }
        } catch (RuntimeException t) {
            if (isValidation(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void exerciseNullTextAnchor() {
        try {
            StrBuilder sb = new StrBuilder(1);
            sb.setNullText("foo");
            sb.appendFixedWidthPadRight(null, 1, '-');
            if (!"f".equals(sb.toString())) {
                throw new RuntimeException("[oracle:nulltext-mid] metamorphic violation: null input must use getNullText() and append first width chars nullText=foo width=1 actual=" + sb.toString());
            }
        } catch (RuntimeException t) {
            if (isValidation(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void runValidExplicitCase(String prefix, String source, int width, char pad) {
        try {
            StrBuilder sb = new StrBuilder(1);
            sb.append(prefix);
            int start = sb.length();
            sb.appendFixedWidthPadRight(source, width, pad);

            // Contract: appendFixedWidthPadRight appends exactly width chars:
            // either the first width chars of the source or the whole source plus padding.
            // midString observes the newly appended window directly, so a throw-deleting or
            // content-skipping patch still violates this even if no exception is thrown.
            String actualSlice = sb.midString(start, width);
            String expectedSlice = buildExpectedSlice(source, width, pad);
            if (!actualSlice.equals(expectedSlice)) {
                throw new RuntimeException("[oracle:mid-explicit] metamorphic violation: appended window differs from documented fixed-width content source=" + source + " width=" + width + " pad=" + (int) pad + " actual=" + actualSlice + " expected=" + expectedSlice);
            }

            // Sound consistency check: the appended window reported by midString must equal
            // the same window independently obtained from the builder's own full string.
            String viaToString = sb.toString().substring(start, start + width);
            if (!actualSlice.equals(viaToString)) {
                throw new RuntimeException("[oracle:mid-consistency] metamorphic violation: midString disagrees with same window from toString start=" + start + " width=" + width + " mid=" + actualSlice + " fullWindow=" + viaToString);
            }
        } catch (RuntimeException t) {
            if (isValidation(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void runValidNullTextCase(String prefix, String source, int width, char pad) {
        try {
            StrBuilder viaNull = new StrBuilder(1);
            viaNull.append(prefix);
            viaNull.setNullText(source);
            int start = viaNull.length();
            viaNull.appendFixedWidthPadRight(null, width, pad);

            StrBuilder viaExplicit = new StrBuilder(1);
            viaExplicit.append(prefix);
            viaExplicit.appendFixedWidthPadRight(source, width, pad);

            // Contract from implementation: null input uses getNullText(); therefore setting
            // nullText to S and appending null must be equivalent to appending S explicitly.
            String nullSlice = viaNull.midString(start, width);
            String explicitSlice = viaExplicit.midString(start, width);
            if (!nullSlice.equals(explicitSlice)) {
                throw new RuntimeException("[oracle:null-vs-explicit-mid] metamorphic violation: nullText path disagrees with explicit string path source=" + source + " width=" + width + " pad=" + (int) pad + " nullSlice=" + nullSlice + " explicitSlice=" + explicitSlice);
            }

            if (!viaNull.toString().equals(viaExplicit.toString())) {
                throw new RuntimeException("[oracle:null-vs-explicit-full] metamorphic violation: full builders differ for equivalent nullText and explicit calls source=" + source + " width=" + width + " pad=" + (int) pad + " nullBuilder=" + viaNull.toString() + " explicitBuilder=" + viaExplicit.toString());
            }
        } catch (RuntimeException t) {
            if (isValidation(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void runPostConditionCase(String prefix, String source, int width, char pad) {
        try {
            StrBuilder sb = new StrBuilder(1);
            sb.append(prefix);
            int start = sb.length();
            sb.appendFixedWidthPadRight(source, width, pad);

            String actual = sb.midString(start, width);
            String expected = buildExpectedSlice(source, width, pad);
            if (!actual.equals(expected)) {
                throw new RuntimeException("[oracle:boundary-mid] metamorphic violation: non-crashing boundary case has wrong appended window source=" + source + " width=" + width + " pad=" + (int) pad + " actual=" + actual + " expected=" + expected);
            }
        } catch (RuntimeException t) {
            if (isValidation(t)) {
                return;
            }
        }
    }

    private static void runPostConditionNullCase(String prefix, String source, int width, char pad) {
        try {
            StrBuilder sb = new StrBuilder(1);
            sb.append(prefix);
            sb.setNullText(source);
            int start = sb.length();
            sb.appendFixedWidthPadRight(null, width, pad);

            String actual = sb.midString(start, width);
            String expected = buildExpectedSlice(source, width, pad);
            if (!actual.equals(expected)) {
                throw new RuntimeException("[oracle:null-boundary-mid] metamorphic violation: nullText boundary case has wrong appended window source=" + source + " width=" + width + " pad=" + (int) pad + " actual=" + actual + " expected=" + expected);
            }
        } catch (RuntimeException t) {
            if (isValidation(t)) {
                return;
            }
        }
    }

    private static String buildExpectedSlice(String source, int width, char pad) {
        StrBuilder expected = new StrBuilder();
        if (source == null) {
            source = "";
        }
        if (width <= 0) {
            return "";
        }
        if (source.length() >= width) {
            expected.append(source, 0, width);
        } else {
            expected.append(source);
            for (int i = source.length(); i < width; i++) {
                expected.append(pad);
            }
        }
        return expected.toString();
    }

    private static boolean isValidation(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())
                    && "appendFixedWidthPadRight".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static String repeat(char ch, int count) {
        StrBuilder sb = new StrBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(ch);
        }
        return sb.toString();
    }
}