#include "stddefs.h"

#pragma script("phar.js")


enum SIGNATURE_TYPE<DWORD>
{
	MD5	    = 0x0001,
	SHA1	= 0x0002,
	SHA256  = 0x0004,
	SHA512	= 0x0008
};

enum
{
	COMPRESSION_NONE		= 0x00000000,
	COMPRESSION_ZLIB	    = 0x00001000,
	COMPRESSION_BZIP		= 0x00002000
};


struct Stub {
	struct StubString
	{
		char a1;
		if (checkStub(a1))
		  $break_array(true);
	};
	
	StubString code[*];
};

struct SerializedMetadata {
	DWORD len;
	char data[len];
};

struct EntryManifest {
	DWORD filename_len;
	char Filename[filename_len];
	DWORD uncompressed_size;
	DWORD Timestamp;
	DWORD compressed_size;
	DWORD CRC32;
	
	DWORD flags;
	switch (flags & 0x0000f000) {
		case COMPRESSION_NONE:
			$print("Compression", "NONE");
			break;		
		case COMPRESSION_ZLIB:
			$print("Compression", "ZLIB");
			break;		
		case COMPRESSION_BZIP:
			$print("Compression", "BZIP");
			break;
		default: 
			$print("Compression", "Unknown");
	}
	
	SerializedMetadata Metadata;
};

struct Version {
hidden:
	signed short v1 : 4;
	signed short v2 : 4;
	signed short v3 : 4;
	signed short v4 : 4;
	
visible:
	$print("Version", v1 + "." + v2 + "." + v4);
};

struct Manifest {
	DWORD len;
	DWORD NumberOfFiles;

	Version Version;

	//SHORT Version;
	
	DWORD GlobalBitmappedFlags;
	
	switch (GlobalBitmappedFlags & 0x0000f000) {
		case COMPRESSION_NONE:
			phar_compression = "NONE";
			break;		
		case COMPRESSION_ZLIB:
			phar_compression = "ZLIB";
			break;		
		case COMPRESSION_BZIP:
			phar_compression = "BZIP";
			break;
		default: 
			phar_compression = "Unknown";
	}
	
	DWORD alias_len;
	char Alias[alias_len];
	
	SerializedMetadata Metadata;
	EntryManifest EntryManifest[NumberOfFiles];			
};

struct File {
	var entryManifest = ref(Manifest.EntryManifest[array_index]);
	char data[entryManifest.compressed_size];
};
	
struct Signature {
	char signature[GetDocumentSize()-current_offset-8];
	SIGNATURE_TYPE type;
	char magic[4];
};

public struct PharFile
{
	  
    var phar_compression = "";
	
	Stub 				Stub;
	Manifest 			Manifest;
	File 				Data[Manifest.NumberOfFiles];
	Signature 			Signature;
	
	$print("Phar Compression", phar_compression);
};