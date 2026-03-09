package br.com.evolui.portalevolui.shared.json.pattern;

public class JsonViewerPattern {
    public static class Public {
    }

    public static class Admin extends Public {
    }

    public static class Super extends Admin {
    }

    public static class Child {}
    public static class Parent {}
}

