package br.com.evolui.healthchecker.exceptions;

import org.apache.http.StatusLine;

public class WebClientStatusException extends Exception {
    private StatusLine status;
    public WebClientStatusException(StatusLine status) {
        super(status.getReasonPhrase());
        this.status = status;
    }

    public StatusLine getStatus() {
        return status;
    }
}
