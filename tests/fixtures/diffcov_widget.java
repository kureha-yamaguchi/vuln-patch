package org.example;

import java.util.List;
import java.util.Map;

public class Widget {

    private int size;
    private String tag;

    public Widget() {
        this(7);
        this.tag = "default";
    }

    public Widget(int size) {
        this.size = size;
        this.tag = "w";
    }

    public int indexOf(Object o) {
        if (o == null) {
            throw new IllegalArgumentException("null argument");
        }
        return -1;
    }

    public int indexOf(String s, int from) {
        if (from < 0) {
            return 0;
        }
        return from + size;
    }

    public double[] scale(double[] xs, int n) {
        if (n <= 0) {
            return new double[0];
        }
        return xs;
    }

    protected abstract static class Base {
        abstract void go();
    }
}
