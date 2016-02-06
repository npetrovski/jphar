<?php

$phar = new Phar('a.phar', FilesystemIterator::CURRENT_AS_FILEINFO | FilesystemIterator::KEY_AS_FILENAME, 'a.phar');

$phar->buildFromDirectory(dirname(__FILE__) . '/Image');
	
	
$phar->setMetadata(array('creator' => 'RAID'));

//$phar->addEmptyDir('Empty Folder');

$phar->compressFiles(Phar::BZ2);

$phar->setStub(file_get_contents(dirname(__FILE__) . DIRECTORY_SEPARATOR. 'stub.php'));