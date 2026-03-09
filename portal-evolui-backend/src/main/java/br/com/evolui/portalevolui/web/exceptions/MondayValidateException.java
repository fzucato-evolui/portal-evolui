package br.com.evolui.portalevolui.web.exceptions;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class MondayValidateException extends Exception{
    private List errors;

    public MondayValidateException(String errorMessage) {
        super(errorMessage);
        this.addError(errorMessage);
    }

    public MondayValidateException(List<String> errors) {
        super(errors != null && !errors.isEmpty() ? String.join("\r\n", errors) : null);
        this.setErrors(errors);
    }

    public MondayValidateException() {

    }

    @Override
    public String getMessage() {
        if (!StringUtils.hasText(super.getMessage())) {
            return errors != null && !errors.isEmpty() ? String.join("\r\n", errors) : null;
        }
        return super.getMessage();
    }

    public List getErrors() {
        return errors;
    }

    public void setErrors(List errors) {
        this.errors = errors;
    }

    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new ArrayList();
        }
        this.errors.add(error);
    }
}
