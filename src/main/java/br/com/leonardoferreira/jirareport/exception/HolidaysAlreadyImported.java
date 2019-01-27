package br.com.leonardoferreira.jirareport.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class HolidaysAlreadyImported extends RuntimeException {
    private static final long serialVersionUID = 1189381143800680941L;

    public HolidaysAlreadyImported(final Exception e) {
        super(e);
    }

}
