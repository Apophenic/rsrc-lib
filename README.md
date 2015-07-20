# .Rsrc Lib
--------------
rsrc-lib provides an API for editing and manipulating Mac Resource Files (.rsrc)

### About .rsrc Files
----------------
.rsrc files are Mac Resource Files. Their typical structure consists of multiple types of resources with said resource
 files directly embedded into the .rsrc file. Here's the file layout:

The first 16 bytes are the header (or footer, really) signature, which is as follows:
 * Bytes 0-3 are always _00 00 01 00_
 * Bytes 4-7 are the offset the actual header can be found at
 * Bytes 8-11 are the header offset - 256
 * Bytes 12-15 are the header's length

As for the rest of the file:
 * The next 256 bytes are padding
 * The next section is the majority of the file, containing all embedded resources, each separated by 4 bytes
 representing the length of the following resource.
 * The header signature directly follows the last resource. It's the exact same as the first 16 bytes.
 * The next 14 bytes aren't clear currently, but typical format is _00 00 00 00 09|0A 00 00 00 1C XX XX 00 ??_,
 where XX is the header's length bytes (Note this only allows two bytes, so it's assumed a header length greater
 than _FF FF_ is invalid').
 * The next 4 bytes are the first resource type, followed by another 4 unknown bytes in the format _XX XX 00 0A|1A_,
 where XX is the number of resources of that resource type.
 * In single resource type rsrc files, the following bytes are each resource entry. For files with multiple types,
 there will be an indeterminate number of bytes before the first resource begins.
 * Each resource entry is 14 bytes, no padding. Format is _3A 99 FF FF 00 03 0D 00 00 00 00 00_, where the first two
 bytes are the resource ID, the next two are stop bytes, and the last 8 are the resource offset location in the
 rsrc file.

### How To Use It
-----------------
Create an ````RsrcFile```` object with your .rsrc file:
~~~ java
new RsrcFile(File file)
~~~
To get information about the resource types and IDs in the file:
~~~ java
rsrc.getResourceByID(int id, ResourceType type)
rsrc.getResourceListByType(ResourceType type)
rsrc.getResourceTypesInFile()
~~~
Where ResourceType is an enum of the data type such as PNG, ICN, etc.

You can extract resources with the following:
~~~ java
rsrc.loadResourceData(int id, ResourceType type)
~~~

And you can replace resources like so:
~~~ java
rsrc.saveResourceData(byte[] data, int id, ResourceType type)
~~~
Where data is the bytes from the file you'd like to place in the rsrc file. Note that resources can only be replaced,
 not added.

To update the .rsrc file after a resource has been replaced, you must explicitly call:
~~~ java
rsrc.saveRsrcFile()
~~~