package com.javaphar;

import java.io.IOException;


public interface PharWritable {

    void write(PharOutputStream out) throws IOException;

}
