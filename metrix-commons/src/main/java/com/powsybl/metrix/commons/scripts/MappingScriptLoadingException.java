package com.powsybl.metrix.commons.scripts;

import java.util.concurrent.ExecutionException;

/**
 * Created by funfrockmar on 03/11/20.
 */
public class MappingScriptLoadingException extends RuntimeException {

    public MappingScriptLoadingException(ExecutionException exception) {
        super(exception);
    }

    public MappingScriptLoadingException(RuntimeException exception) {
        super(exception);
    }
}
