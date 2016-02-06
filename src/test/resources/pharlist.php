<?php

function printList($path) {
    $phar = new Phar($path);
    foreach($phar as $file) {
        if ($file->isDir()) {
            printList($file->getPathName());
        } else {
            echo $file->getPath() . '/' . $file->getFilename() . PHP_EOL;
        }
    }
}

printList('php5-image.phar');