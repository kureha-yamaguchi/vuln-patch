package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final FuzzedDataProvider d = data;

        class Gen {
            double nextDouble() {
                switch (d.consumeInt(0, 11)) {
                    case 0:
                        return 0.0d;
                    case 1:
                        return -0.0d;
                    case 2:
                        return 1.0d;
                    case 3:
                        return -1.0d;
                    case 4:
                        return Double.NaN;
                    case 5:
                        return Double.POSITIVE_INFINITY;
                    case 6:
                        return Double.NEGATIVE_INFINITY;
                    case 7:
                        return (double) d.consumeInt();
                    case 8:
                        return ((double) d.consumeInt()) / 3.0d;
                    case 9:
                        return ((double) d.consumeInt()) * ((double) d.consumeInt());
                    case 10:
                        return Math.scalb((double) d.consumeInt(), d.consumeInt(-1074, 1023));
                    default:
                        long bits = (((long) d.consumeInt()) << 32) ^ (((long) d.consumeInt()) & 0xffffffffL);
                        return Double.longBitsToDouble(bits);
                }
            }
        }

        Gen gen = new Gen();

        int n = d.consumeInt(0, 32);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = gen.nextDouble();
        }

        int secondaryLength;
        if (d.consumeBoolean()) {
            secondaryLength = n > 0 ? n - 1 : 0;
        } else {
            secondaryLength = d.consumeInt(0, 32);
        }
        double[] secondary = new double[secondaryLength];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = gen.nextDouble();
        }

        double splitTolerance = gen.nextDouble();

        EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

        if (d.consumeBoolean()) {
            ed.getV();
        }
        if (d.consumeBoolean()) {
            ed.getD();
        }
        if (d.consumeBoolean()) {
            ed.getVT();
        }
        if (d.consumeBoolean()) {
            ed.getDeterminant();
        }

        double[] real = ed.getRealEigenvalues();
        double[] imag = ed.getImagEigenvalues();

        if (real != null) {
            for (int i = 0; i < real.length; i++) {
                if (d.consumeBoolean()) {
                    ed.getRealEigenvalue(i);
                }
                if (d.consumeBoolean()) {
                    ed.getImagEigenvalue(i);
                }
                if (d.consumeBoolean()) {
                    ed.getEigenvector(i);
                }
            }
        }

        if (d.consumeBoolean()) {
            DecompositionSolver solver = ed.getSolver();
            if (d.consumeBoolean()) {
                solver.isNonSingular();
            }
            if (d.consumeBoolean()) {
                int dim = real == null ? 0 : real.length;
                double[] b = new double[dim];
                for (int i = 0; i < dim; i++) {
                    b[i] = gen.nextDouble();
                }
                if (d.consumeBoolean()) {
                    solver.solve(new ArrayRealVector(b, true));
                } else {
                    solver.solve(b);
                }
            }
            if (d.consumeBoolean()) {
                int dim = real == null ? 0 : real.length;
                double[][] rhs = new double[dim][dim == 0 ? 0 : d.consumeInt(0, Math.min(8, dim + 2))];
                for (int i = 0; i < rhs.length; i++) {
                    for (int j = 0; j < rhs[i].length; j++) {
                        rhs[i][j] = gen.nextDouble();
                    }
                }
                solver.solve(new Array2DRowRealMatrix(rhs, false));
            }
            if (d.consumeBoolean()) {
                solver.getInverse();
            }
        }

        if (d.consumeBoolean()) {
            int m = d.consumeInt(0, 12);
            double[][] matrix = new double[m][m];
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < m; j++) {
                    matrix[i][j] = gen.nextDouble();
                }
            }

            if (d.consumeBoolean()) {
                for (int i = 0; i < m; i++) {
                    for (int j = i + 1; j < m; j++) {
                        double v;
                        switch (d.consumeInt(0, 2)) {
                            case 0:
                                v = matrix[i][j];
                                break;
                            case 1:
                                v = matrix[j][i];
                                break;
                            default:
                                v = (matrix[i][j] + matrix[j][i]) / 2.0d;
                                break;
                        }
                        matrix[i][j] = v;
                        matrix[j][i] = v;
                    }
                }
            }

            EigenDecompositionImpl ed2 =
                new EigenDecompositionImpl(new Array2DRowRealMatrix(matrix, false), splitTolerance);

            if (d.consumeBoolean()) {
                ed2.getV();
            }
            if (d.consumeBoolean()) {
                ed2.getD();
            }
            if (d.consumeBoolean()) {
                ed2.getVT();
            }
            if (d.consumeBoolean()) {
                ed2.getDeterminant();
            }

            double[] real2 = ed2.getRealEigenvalues();
            if (real2 != null) {
                for (int i = 0; i < real2.length; i++) {
                    if (d.consumeBoolean()) {
                        ed2.getRealEigenvalue(i);
                    }
                    if (d.consumeBoolean()) {
                        ed2.getImagEigenvalue(i);
                    }
                    if (d.consumeBoolean()) {
                        ed2.getEigenvector(i);
                    }
                }
            }

            if (d.consumeBoolean()) {
                DecompositionSolver solver2 = ed2.getSolver();
                if (d.consumeBoolean()) {
                    solver2.isNonSingular();
                }
                if (d.consumeBoolean()) {
                    int dim = real2 == null ? 0 : real2.length;
                    double[] b2 = new double[dim];
                    for (int i = 0; i < dim; i++) {
                        b2[i] = gen.nextDouble();
                    }
                    solver2.solve(b2);
                }
                if (d.consumeBoolean()) {
                    solver2.getInverse();
                }
            }
        }
    }
}