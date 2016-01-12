var byteBuffer = "";

function checkStub(c) {
	byteBuffer += String.fromCharCode(c);
	return (/HALT_COMPILER.*\?>.?\n$/.test(byteBuffer));
}

function GetDocumentSize() {
    return document.FileSize;
}
