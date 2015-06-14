package com.javaphar;

import java.io.IOException;

/**
 * @see http://www.php.net/manual/en/phar.fileformat.stub.php
 */
public final class PharStub implements PharWritable {

    private String stubCode = "";

    public PharStub() {

    }

    public PharStub(final String stubCode) {
        this.stubCode = stubCode;
    }

    @Override
    public void write(final PharOutputStream out) throws IOException {
        //out.writeString("<?php ");
        if (this.stubCode != null) {
            out.writeString(this.stubCode);
        }
        //out.writeString("__HALT_COMPILER(); ?>");
    }

}
