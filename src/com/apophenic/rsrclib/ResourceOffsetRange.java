package com.apophenic.rsrclib;

public class ResourceOffsetRange
{
    public int StartOffset;
    public int StopOffset;
    public int Length;

    /**
     * Struct-like class representing a byte offset range
     * in the given .rsrc file that holds where the offset begins,
     * ends, and how long it is. Treat this as the equivalent of
     * a {@code Property} that doesn't expose its {@code setter}
     * @param file  The {@link RsrcFile} containing this range
     * @param id  The resource ID to use for the start offset.
     *            The literal next resource will then be used
     *            as the stop offset
     * @param type  The {@link ResourceType} to search for
     */
    public ResourceOffsetRange(RsrcFile file, int id, ResourceType type)
    {
        ResourceLinkedList list = file.getHeaderTable().get(type);
        int index = list.indexOf(id);
        Resource res = list.get(index);

        StartOffset = res.getDataOffset();
        StopOffset = (index < list.size() - 1) ? list.get((index + 1)).getDataOffset() : file.getHeaderOffset();
        Length = StopOffset - StartOffset - 0x04;   // Ignore padding bytes between resources
    }

    public ResourceOffsetRange(RsrcFile file, Resource res)
    {
        this(file, res.getResourceID(), res.getType());
    }

}
