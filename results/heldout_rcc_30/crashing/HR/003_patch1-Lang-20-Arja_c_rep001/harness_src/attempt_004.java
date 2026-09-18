package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        class Helpers {
            boolean isCleanRejection(Throwable t) {
                return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            }

            boolean hasRelevantFrame(Throwable t) {
                for (StackTraceElement e : t.getStackTrace()) {
                    String cls = e.getClassName();
                    String m = e.getMethodName();
                    if (("org.apache.commons.lang3.StringUtils".equals(cls) && "join".equals(m))
                            || ("org.apache.commons.lang3.StringUtils".equals(cls) && "length".equals(m))
                            || ("org.apache.commons.lang3.CharRange".equals(cls) && "toString".equals(m))) {
                        return true;
                    }
                }
                return false;
            }

            boolean isRootCauseNpe(Throwable t) {
                return t instanceof NullPointerException && hasRelevantFrame(t);
            }
        }
        Helpers h = new Helpers();

        final Object nullToString = new Object() {
            @Override
            public String toString() {
                return null;
            }
        };

        final Object[] anchorNullToStringList = new Object[] { nullToString };

        try {
            if (StringUtils.join((Object[]) null, ',') != null) {
                throw new RuntimeException("[oracle:anchor-null-array] metamorphic violation: join((Object[])null, ',') must return null");
            }
        } catch (RuntimeException t) {
            if (!h.isCleanRejection(t) && !h.isRootCauseNpe(t)) {
                throw t;
            }
        }

        try {
            String anchored = StringUtils.join(anchorNullToStringList, '/', 0, 1);
            if (!"null".equals(anchored)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-value] metamorphic violation: expected \"null\" for single element with null toString, got=" + anchored);
            }
            int reported = StringUtils.length(anchored);
            int independent = StringUtils.length(String.valueOf(nullToString));
            if (reported != independent) {
                throw new RuntimeException("[oracle:anchor-length] consistency violation: reported=" + reported + " independent=" + independent);
            }
        } catch (RuntimeException t) {
            if (h.isCleanRejection(t)) {
                return;
            }
            if (h.isRootCauseNpe(t)) {
                throw new RuntimeException("[oracle:anchor-length] metamorphic violation: valid join input crashed before producing the documented \"null\" text", t);
            }
            throw t;
        }

        int arrayLen = data.consumeInt(1, 8);
        Object[] array = new Object[arrayLen];
        int specialPos = data.consumeInt(0, arrayLen - 1);

        for (int i = 0; i < arrayLen; i++) {
            if (i == specialPos) {
                array[i] = nullToString;
                continue;
            }
            int kind = data.consumeInt(0, 5);
            switch (kind) {
                case 0:
                    array[i] = null;
                    break;
                case 1:
                    array[i] = data.consumeAsciiString(12);
                    break;
                case 2:
                    array[i] = Long.valueOf(data.consumeInt(-1000000, 1000000));
                    break;
                case 3:
                    array[i] = CharRange.is((char) data.consumeInt(32, 126));
                    break;
                case 4:
                    array[i] = CharRange.isNot((char) data.consumeInt(32, 126));
                    break;
                default:
                    char a = (char) data.consumeInt(32, 126);
                    char b = (char) data.consumeInt(32, 126);
                    if (a <= b) {
                        array[i] = CharRange.isIn(a, b);
                    } else {
                        array[i] = CharRange.isIn(b, a);
                    }
                    break;
            }
        }

        int startIndex = specialPos;
        int endIndex = data.consumeInt(startIndex + 1, arrayLen);

        char sepChar = (char) data.consumeInt(1, 126);
        String sepString = data.consumeAsciiString(4);
        if (sepString == null) {
            sepString = "";
        }

        try {
            String out = StringUtils.join(array, sepChar, startIndex, endIndex);

            /* Contract from join's implementation/tests: it appends each non-null array[i] with StringBuilder.append(Object),
               and inserts the separator between items. Therefore the output length must equal the separator contribution plus
               the sum of String.valueOf(element) lengths for each non-null included element. A throw-deleting or element-skipping
               patch can make top-level behavior look non-crashing while this quantity is still wrong. */
            int reportedLen = StringUtils.length(out);
            int independentLen = 0;
            for (int i = startIndex; i < endIndex; i++) {
                if (i > startIndex) {
                    independentLen += 1;
                }
                if (array[i] != null) {
                    independentLen += StringUtils.length(String.valueOf(array[i]));
                }
            }
            if (reportedLen != independentLen) {
                throw new RuntimeException("[oracle:char-join-length] consistency violation: start=" + startIndex + " end=" + endIndex + " reported=" + reportedLen + " independent=" + independentLen + " out=" + out);
            }

            String specialRendered = String.valueOf(array[specialPos]);
            if (specialPos >= startIndex && specialPos < endIndex && specialRendered != null && out.indexOf(specialRendered) < 0) {
                throw new RuntimeException("[oracle:char-special-presence] metamorphic violation: joined output omitted included element text " + specialRendered + " output=" + out);
            }
        } catch (RuntimeException t) {
            if (h.isCleanRejection(t)) {
                return;
            }
            if (h.isRootCauseNpe(t)) {
                throw new RuntimeException("[oracle:char-join-length] metamorphic violation: valid-by-construction join crashed for included element with null toString", t);
            }
            throw t;
        }

        try {
            String out = StringUtils.join(array, sepString, startIndex, endIndex);

            int reportedLen = StringUtils.length(out);
            int independentLen = 0;
            for (int i = startIndex; i < endIndex; i++) {
                if (i > startIndex) {
                    independentLen += StringUtils.length(sepString);
                }
                if (array[i] != null) {
                    independentLen += StringUtils.length(String.valueOf(array[i]));
                }
            }
            if (reportedLen != independentLen) {
                throw new RuntimeException("[oracle:string-join-length] consistency violation: sep=" + sepString + " start=" + startIndex + " end=" + endIndex + " reported=" + reportedLen + " independent=" + independentLen + " out=" + out);
            }

            /* CharRange.toString is a real reachable formatter in the same region list; for any CharRange element that was
               included, joining must preserve exactly the text returned by that element's own toString. */
            for (int i = startIndex; i < endIndex; i++) {
                if (array[i] instanceof CharRange) {
                    String cr = array[i].toString();
                    if (cr != null && out.indexOf(cr) < 0) {
                        throw new RuntimeException("[oracle:charrange-presence] metamorphic violation: output omitted CharRange text " + cr + " output=" + out);
                    }
                }
            }
        } catch (RuntimeException t) {
            if (h.isCleanRejection(t)) {
                return;
            }
            if (h.isRootCauseNpe(t)) {
                throw new RuntimeException("[oracle:string-join-length] metamorphic violation: valid-by-construction string-separator join crashed for included element with null toString", t);
            }
            throw t;
        }
    }
}