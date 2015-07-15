package com.apophenic.rsrclib;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class Resource
{
    /** The resource ID, as represented in the .rsrc file */
    private int _id;

    /** The offset location this resource begins in the .rsrc file */
    private int _startOffset;

    /** The data type this resource is */
    private ResourceType _type;

    /**
     * Creates a new {@link Resource} object, representing meta data
     * for resources embedded in .rsrc files
     * @param data  The 14 bytes that represent this resource,
     *              as found in the header portion of the file.
     *              The format is 3A 99 FF FF 00 34 2D 96 00 00 00 00,
     *              where the first two bytes are the ID, the next two are
     *              stop bytes, and the last 8 are the resource's offset.
     * @param type  The {@link ResourceType} of this {@code Resource}
     */
    public Resource(byte[] data, ResourceType type)
    {
        this._type = type;

        int pointer = 0;
        _id = ((data[pointer] & 0xFF) << 8) | (data[pointer+=1] & 0xFF);

        pointer += 0x03;    // Skip stop bytes (0xFF, 0xFF)

        _startOffset = ((data[pointer] & 0xFF) << 56) | ((data[pointer+=1] & 0xFF) << 48) |
                       ((data[pointer+=1] & 0xFF) << 40) | ((data[pointer+=1] & 0xFF) << 32) |
                       ((data[pointer+=1] & 0xFF) << 24) | ((data[pointer+=1] & 0xFF) << 16) |
                       ((data[pointer+=1] & 0xFF) << 8) | (data[pointer+=1] & 0xFF);
    }

    /** Returns the {@code int32} representation of this resource's ID */
    public int getResourceID()
    {
        return _id;
    }

    /** Returns the {@code byte[]} representation of this resource's ID */
    public byte[] getResourceIDBytes()
    {
        return Arrays.copyOfRange(ByteBuffer.allocate(4).putInt(_id).array(), 0x02, 0x04);
    }

    /** Returns the {@code int32} representation of this resource's offset in the file */
    public int getStartOffset()
    {
        return _startOffset;
    }

    /** Returns the {@code byte[]} representation of this resource's offset in the file */
    public byte[] getStartOffsetBytes()
    {
        return ByteBuffer.allocate(8).putInt(_startOffset).array();
    }

    /** Moves where this resource's offset begins in the file */
    public void shiftStartOffset(int shiftValue)
    {
        _startOffset += shiftValue;
    }

    /** Returns this {@code Resource}'s offset location where the data bytes begin */
    public int getDataOffset()
    {
        return _startOffset + 0x104;
    }

    /** Returns the resource's data type */
    public ResourceType getType()
    {
        return _type;
    }
}
