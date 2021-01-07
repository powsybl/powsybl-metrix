package com.powsybl.metrix.commons.scripts;

import java.util.concurrent.ExecutionException;

/**
 * Created by funfrockmar on 03/11/20.
 */
public class MetrixScriptLoadingException extends RuntimeException {

    public MetrixScriptLoadingException(ExecutionException exception) {
        super(exception);
    }
}
