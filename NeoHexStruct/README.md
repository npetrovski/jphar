## Hex Editor Neo's Structure Viewer declaration file for PHAR type


Hex Editor Neo is a binary files editing software utility for Windows. It's rich and handy set of features will help all software and hardware developers working with ASCII, hex, decimal, float, double and binary data. Numerous powerful features, such as Pattern Coloring, Structure Viewer, Bookmarks as well as highly-optimized commands can be utilized to make file editing task quick and simple.

Visit http://www.hhdsoftware.com/hex-editor to retrieve a copy of the software.


### How to install / configure

1. Copy both **phar.h** and **phar.js** files inside **%NEOHEX_INSTALL%\Sample Structures** folder
2. Open Hex Editor and in *Structure Library* add new structure file "phar.h" (a new structure PHAR_FILE should appear)
3. Open a ".phar" file (could be any .phar file) - this step is immportant otherwise you won't be able to bind the new scheme
4. In *Structure Viewer* bind the new structure - from Type dropdown select "phar.h" and specify the name of the bind
5. In *Structure Viewer* hit "Save Scheme..." and give a name of the new scheme (example. "PHAR")
6. In *Structure Viewer* hit "Associations..", for "RegEx" enter ".+?\\.phar$" (w/o quotes), for Associated scheme select "PHAR" and hit "Add"

### PHAR file structure
```
=========================================|
X    STUB                                |
=========================================|
4    Length of manifest in bytes         |
-----------------------------------------|
4    Number of files in the Phar         |
-----------------------------------------|
2    API version of the Phar manifest    |
-----------------------------------------|
4    Global Phar bitmapped flags         |
-----------------------------------------|
4    Length of Phar alias                |
-----------------------------------------|       M
X    Phar alias                          |
-----------------------------------------|       A
4    Length of Phar metadata             |
-----------------------------------------|       N
X    Serialized Phar Meta-data           |
==========================================       I
4    Filename length                     |
-----------------------------------------|       F
X    Filename                            |
-----------------------------------------|       E
4    Un-compressed file size in bytes    |
-----------------------------------------|       S
4    Unix timestamp of file              |
-----------------------------------------|       T
4    Compressed file size                |      
-----------------------------------------|
4    CRC32 of un-compressed file content |
-----------------------------------------|
4    Bit-mapped File-specific flags      |
-----------------------------------------|
4    Serialized File Meta-data length    |
-----------------------------------------|
X    Serialized File Meta-data           |
=========================================|
    CONTENTS
------------------------------------------

```
