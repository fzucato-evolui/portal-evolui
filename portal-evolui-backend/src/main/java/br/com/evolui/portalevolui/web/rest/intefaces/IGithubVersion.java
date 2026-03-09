package br.com.evolui.portalevolui.web.rest.intefaces;

public interface IGithubVersion<T> {
    public T getVersion();

    public void setVersion(T version);
}
