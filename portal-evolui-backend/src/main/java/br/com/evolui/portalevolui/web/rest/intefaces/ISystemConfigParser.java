package br.com.evolui.portalevolui.web.rest.intefaces;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface ISystemConfigParser {
    ISystemConfigParser parseJson(String json) throws Exception;
    @JsonIgnore
    String getJson() throws Exception;
}
