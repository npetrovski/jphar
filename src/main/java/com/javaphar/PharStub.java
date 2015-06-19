package com.javaphar;

import java.io.IOException;

/**
 * @see http://www.php.net/manual/en/phar.fileformat.stub.php
 */
public final class PharStub implements PharWritable {

    private static final String DEFAULT_STUB = "<?php\n__HALT_COMPILER(); ?>";

    private String stubCode = "";

    public PharStub() {
        this.stubCode = DEFAULT_STUB;
    }

    public PharStub(final String stubCode) {
        this.stubCode = stubCode;
    }

    @Override
    public void write(final PharOutputStream out) throws IOException {
        if (this.stubCode != null) {
            out.writeString(this.stubCode);
        }
    }
}
