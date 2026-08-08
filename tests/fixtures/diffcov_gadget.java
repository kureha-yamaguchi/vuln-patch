package org.example;

public class Gadget {

    public String render(Map<String, List<Integer>> m, Object... rest) {
        return String.valueOf(m) + rest.length;
    }
}
