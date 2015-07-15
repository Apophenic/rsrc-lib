# .Rsrc Lib
--------------
rsrc-lib provides an API for editing and manipulating Mac Resource Files (.rsrc)

### About .rsrc Files
----------------
.rsrc files are Mac Resource Files. Their typical structure consists of multiple types of resources with said resource
 files directly embedded into the .rsrc file. File layout is as follows:
 * The first 16 bytes are the header
 * Bytes 4-8 are the offset the header can be found at
 * The next 256 bytes are usually padding
 * The next section is the majority of the file, containing all embedded resources, each separated by 4 padding bytes
 * The header follows the last resource. So you'll have the final resource file, the 4 padding bytes, then the 16
 header bytes.
 * Then there will be 12-16 unknown bytes separating the first resource type.
 * The resource type will be followed by 8 padding bytes, then will list all resources of that type
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