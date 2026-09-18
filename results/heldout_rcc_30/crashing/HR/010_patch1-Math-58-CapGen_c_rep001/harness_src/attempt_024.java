package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR = {
        1.1143831578403364E-29,
        4.95281403484594E-28,
        1.1171347211930288E-26,
        1.7044813962636277E-25,
        1.9784716574832164E-24,
        1.8630236407866774E-23,
        1.4820532905097742E-22,
        1.0241963854632831E-21,
        6.275077366673128E-21,
        3.461808994532493E-20,
        1.7407124684715706E-19,
        8.056687953553974E-19,
        3.460193945992071E-18,
        1.3883326374011525E-17,
        5.233894983671116E-17,
        1.8630791465263745E-16,
        6.288759227922111E-16,
        2.0204433920597856E-15,
        6.198768938576155E-15,
        1.821419346860626E-14,
        5.139176445538471E-14,
        1.3956427429045787E-13,
        3.655705706448139E-13,
        9.253753324779779E-13,
        2.267636001476696E-12,
        5.3880460095836855E-12,
        1.2431632654852931E-11
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchorDelegationOracle();

        int mode = data.consumeInt(0, 3);
        switch (mode) {
            case 0:
                runVariantDelegationOracle(data);
                break;
            case 1:
                runSnapshotRebuildOracle(data);
                break;
            case 2:
                runDirectGuessOracle(data);
                break;
            default:
                runVariantDelegationOracle(data);
                runSnapshotRebuildOracle(data);
                break;
        }
    }

    private static void runAnchorDelegationOracle() {
        GaussianFitter publicRoute = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ANCHOR.length; i++) {
            publicRoute.addObservedPoint(i, ANCHOR[i]);
        }

        WeightedObservedPoint[] snapshot = publicRoute.getObservations();
        double[] guess;
        try {
            guess = new GaussianFitter.ParameterGuesser(snapshot).guess();
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }

        GaussianFitter explicitRoute = cloneFromSnapshot(snapshot);

        double[] explicit;
        try {
            explicit = explicitRoute.fit(guess);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }

        try {
            double[] viaPublic = publicRoute.fit();
            if (!sameParameters(viaPublic, explicit)) {
                throw new RuntimeException("[oracle:delegate-wrap] metamorphic violation: fit() must delegate consistently to fit(double[]) on the same observations; public=" + describe(viaPublic) + " explicit=" + describe(explicit));
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throw new RuntimeException("[oracle:delegate-wrap] metamorphic violation: fit() raised root-cause exception on the anchor input while equivalent explicit fit(double[]) succeeded with " + describe(explicit), t);
            }
            if (!isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void runVariantDelegationOracle(FuzzedDataProvider data) {
        int prefix = data.consumeInt(0, 3);
        int suffix = data.consumeInt(0, 3);
        int step = data.consumeInt(1, 3);
        double xShift = data.consumeInt(-20, 20);
        double yScale = Math.pow(2.0, data.consumeInt(-3, 3));
        double floor = Math.pow(10.0, -data.consumeInt(20, 40));

        int n = prefix + ANCHOR.length + suffix;
        GaussianFitter publicRoute = new GaussianFitter(new LevenbergMarquardtOptimizer());
        int x = 0;
        for (int i = 0; i < prefix; i++, x += step) {
            publicRoute.addObservedPoint(x + xShift, floor);
        }
        for (int i = 0; i < ANCHOR.length; i++, x += step) {
            publicRoute.addObservedPoint(x + xShift, ANCHOR[i] * yScale + floor);
        }
        for (int i = 0; i < suffix; i++, x += step) {
            publicRoute.addObservedPoint(x + xShift, floor);
        }
        if (n < 3) {
            return;
        }

        WeightedObservedPoint[] snapshot = publicRoute.getObservations();
        double[] guess;
        try {
            guess = new GaussianFitter.ParameterGuesser(snapshot).guess();
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }

        GaussianFitter explicitRoute = cloneFromSnapshot(snapshot);
        double[] explicit;
        try {
            explicit = explicitRoute.fit(guess);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }

        try {
            double[] viaPublic = publicRoute.fit();
            if (!sameParameters(viaPublic, explicit)) {
                throw new RuntimeException("[oracle:variant-delegate] metamorphic violation: fit() and explicit fit(guess) disagree on equivalent real inputs; public=" + describe(viaPublic) + " explicit=" + describe(explicit));
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throw new RuntimeException("[oracle:variant-delegate] metamorphic violation: public fit() leaked the root-cause exception while explicit fit(guess) succeeded with " + describe(explicit), t);
            }
            if (!isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void runSnapshotRebuildOracle(FuzzedDataProvider data) {
        int n = data.consumeInt(3, 12);
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < n; i++) {
            double x = i + data.consumeInt(-2, 2) * 0.1;
            double y = Math.abs(data.consumeInt(-1000, 1000)) + 1.0;
            fitter.addObservedPoint(x, y);
        }

        WeightedObservedPoint[] a = fitter.getObservations();
        WeightedObservedPoint[] b = fitter.getObservations();
        if (a.length != b.length) {
            throw new RuntimeException("[oracle:getobs-rebuild] metamorphic violation: repeated getObservations() changed length " + a.length + " vs " + b.length);
        }

        GaussianFitter cloneA = cloneFromSnapshot(a);
        GaussianFitter cloneB = cloneFromSnapshot(b);

        double[] guessA;
        double[] guessB;
        try {
            guessA = new GaussianFitter.ParameterGuesser(a).guess();
            guessB = new GaussianFitter.ParameterGuesser(b).guess();
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }

        if (!sameParameters(guessA, guessB)) {
            throw new RuntimeException("[oracle:getobs-rebuild] metamorphic violation: identical observation snapshots produced different guesses " + describe(guessA) + " vs " + describe(guessB));
        }

        try {
            double[] fitA = cloneA.fit(guessA);
            double[] fitB = cloneB.fit(guessB);
            if (!sameParameters(fitA, fitB)) {
                throw new RuntimeException("[oracle:getobs-rebuild] metamorphic violation: rebuilding from repeated observation snapshots changed fitted parameters " + describe(fitA) + " vs " + describe(fitB));
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static void runDirectGuessOracle(FuzzedDataProvider data) {
        int n = data.consumeInt(3, 10);
        WeightedObservedPoint[] pts = new WeightedObservedPoint[n];
        double x = data.consumeInt(-10, 10);
        for (int i = 0; i < n; i++) {
            x += 1.0 + data.consumeInt(0, 2);
            double y = Math.abs(data.consumeInt(-1000, 1000)) + 1.0;
            pts[i] = new WeightedObservedPoint(1.0, x, y);
        }

        try {
            GaussianFitter.ParameterGuesser g = new GaussianFitter.ParameterGuesser(pts);
            double[] p1 = g.guess();
            p1[0] = -12345.0;
            p1[1] = -12345.0;
            p1[2] = -12345.0;
            double[] p2 = g.guess();
            if (p2.length != 3 || p2[0] == -12345.0 || p2[1] == -12345.0 || p2[2] == -12345.0) {
                throw new RuntimeException("[oracle:guess-cache-clone2] metamorphic violation: ParameterGuesser.guess() must return a clone of cached parameters, not expose mutable internal state");
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static GaussianFitter cloneFromSnapshot(WeightedObservedPoint[] snapshot) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < snapshot.length; i++) {
            WeightedObservedPoint p = snapshot[i];
            fitter.addObservedPoint(p.getWeight(), p.getX(), p.getY());
        }
        return fitter;
    }

    private static boolean sameParameters(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            double av = a[i];
            double bv = b[i];
            if (Double.doubleToLongBits(av) == Double.doubleToLongBits(bv)) {
                continue;
            }
            double diff = Math.abs(av - bv);
            double scale = Math.max(1.0, Math.max(Math.abs(av), Math.abs(bv)));
            if (diff > 1e-6 * scale) {
                return false;
            }
        }
        return true;
    }

    private static String describe(double[] v) {
        if (v == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(v[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        Package p = t.getClass().getPackage();
        return p != null && "org.apache.commons.math.exception".equals(p.getName());
    }

    private static boolean isRootCause(Throwable t) {
        if (t == null || !t.getClass().getName().endsWith("NotStrictlyPositiveException")) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            String c = st[i].getClassName();
            String m = st[i].getMethodName();
            if (("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(c) && ("fit".equals(m) || "getObservations".equals(m)))
                || ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(c) && "getObservations".equals(m))
                || ("org.apache.commons.math.optimization.fitting.GaussianFitter$ParameterGuesser".equals(c) && "guess".equals(m))
                || ("org.apache.commons.math.analysis.function.Gaussian$Parametric".equals(c) && "validateParameters".equals(m))) {
                return true;
            }
        }
        return false;
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new RuntimeException(t);
    }
}