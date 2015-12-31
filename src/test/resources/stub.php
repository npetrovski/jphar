#!/usr/bin/env php
<?php
spl_autoload_register(function ($class) {
    if (0 === strpos($class, 'Image')) {
        include 'phar://' . __FILE__ . '/' . str_replace('\\', '/', $class) . '.php';
    }
}, true, true);

try {
    Phar::mapPhar(__FILE__);
    //include 'phar://php5-image.phar/startup.php';
} catch (PharException $e) {
    echo $e->getMessage();
    die('Cannot initialize Phar');
}
__HALT_COMPILER(); ?>