//
//   Library for editing Mac Resource Files (.rsrc)
//
//   https://github.com/Apophenic
//
//   Copyright (c) 2015 Justin Dayer (jdayer9@gmail.com)
//
//   Permission is hereby granted, free of charge, to any person obtaining a copy
//   of this software and associated documentation files (the "Software"), to deal
//   in the Software without restriction, including without limitation the rights
//   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//   copies of the Software, and to permit persons to whom the Software is
//   furnished to do so, subject to the following conditions:
//
//   The above copyright notice and this permission notice shall be included in
//   all copies or substantial portions of the Software.
//
//   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//   THE SOFTWARE.

package com.apophenic.rsrclib;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The [typical] structure of .rsrc files:
 * <li>-The first 16 bytes are the header
 * <li>-Bytes 4-8 are the offset the header can be found at
 * <li>-The next 256 bytes are usually padding
 * <li>-The next section is the majority of the file, containing
 * all embedded resources, each separated by 4 padding bytes
 * <li>-The header follows the last resource. So you'll have the final
 * resource file, the 4 padding bytes, then the 16 header bytes.
 * <li>-Then there will be 12-16 unknown bytes separating the first
 * resource type.
 * <li>-The resource type will be followed by 8 padding bytes, then
 * will list all resources of that type. See {@link Resource}
 * for how each resource entry is structured.
 */
public class RsrcFile
{
    /** .rsrc file */
    private File _file;

    /** _file bytes */
    private byte[] _data;

    /** byte offset where the header begins */
    private int _headerOffset;

    /** maintains resources types, resources, and their offsets as defined in the file's header */
    private LinkedHashMap<ResourceType, ResourceLinkedList> _headerTable = new LinkedHashMap<>();

    /** used to traverse bytes */
    private int _pointer = -1;

    /**
     * Creates a new {@link RsrcFile} object from the given {@code File}
     * @param _file  Mac resource file (.rsrc) to create {@code RsrcFile} from
     */
    public RsrcFile(File _file)
    {
        this._file = _file;

        init();
    }

    private void init()
    {
        try
        {
            readRsrcFile();
            buildHeaderTable();
        }
        catch (IOException e)
        {
            Logger.getAnonymousLogger().log(Level.SEVERE, "Failed to read rsrc file");
        }
    }

    public RsrcFile(String filepath)
    {
        this(new File(filepath));
    }

    /** Reads .rsrc file into a byte[] */
    private void readRsrcFile() throws IOException
    {
        int length = getFileSizeInBytes();

        _data = new byte[length];

        FileInputStream fis;
        fis = new FileInputStream(_file);
        fis.read(_data);
        fis.close();
    }

    /**
     * Reads bytes 4-8 for the header offset location,
     * then reads the header while storing resources by type
     */
    private void buildHeaderTable()
    {
        byte[] headerBytes = Arrays.copyOfRange(_data, 0x04, 0x08); // Bytes 4-8 are header offset
        _headerOffset = ((headerBytes[0] & 0xFF) << 24) | ((headerBytes[1] & 0xFF) << 16) |
                        ((headerBytes[2] & 0xFF) << 8) | (headerBytes[3] & 0xFF);

        _pointer = _headerOffset + 0x1E;    // Place pointer at first resource type

        while (_pointer < _data.length)
        {
            ResourceType type = ResourceType.getValue(Arrays.copyOfRange(_data, _pointer, _pointer + 0x04));

            // Set pointer to first resource by searching for stop bytes, then decrement back to ID start
            _pointer = findMatchingOffsetStart(new byte[]{(byte) 0xFF, (byte) 0xFF}, _pointer) - 0x02;

            ResourceLinkedList resourceList = new ResourceLinkedList();

            while (_pointer < _data.length) // TODO Also include: look for type stop bytes
            {
                // Every Resource entry in the header table is 14 bytes long
                resourceList.addLast(new Resource(Arrays.copyOfRange(_data, _pointer, _pointer += 0x0C), type));
            }

            _headerTable.put(type, resourceList);
        }
    }

    /**
     * Loads the specified resource asset from the .rsrc file
     * @param id  The ID of the resource to load
     * @param type  The {@link ResourceType} of the resource to load
     * @return  {@code byte[]} of data contained within the resource
     */
    public byte[] loadResourceData(int id, ResourceType type)
    {
        ResourceOffsetRange range = new ResourceOffsetRange(this, id, type);

        return Arrays.copyOfRange(_data, range.StartOffset, range.StopOffset - 0x04);   // Ignore padding
    }

    /**
     * Saves the specified resource asset, overwriting the resource's
     * previous data in the given .rsrc file. Note this change will only be made in memory,
     * call {@link #saveRsrcFile} to update the file.
     * If the resource ID and type don't exist, do nothing.
     * @param data  {@code byte[]} data to replace resource with
     * @param id  The ID of the resource to save.
     * @param type  The {@link ResourceType} of the resource being replaced
     */
    public void saveResourceData(byte[] data, int id, ResourceType type)
    {
        ResourceOffsetRange range = new ResourceOffsetRange(this, id, type);
        int difference = data.length - range.Length;

        // Create new array from 3 sections: 1) 0x00 to resource start, 2) new resource data, 3) next resource start
        // to end of .rsrc file
        byte[] newData = new byte[_data.length + difference];
        System.arraycopy(_data, 0x00, newData, 0x00, range.StartOffset);
        System.arraycopy(data, 0x00, newData, range.StartOffset, data.length);
        System.arraycopy(_data, range.StopOffset - 0x04,    // Also copy padding bytes between files
                         newData, range.StartOffset + data.length,
                         _data.length - range.StopOffset);
        _data = newData;

        shiftHeaderOffset(difference);
        updateHeaderTable(range.StartOffset, difference);
    }

    /**
     * Changes header offset to account for changes in
     * resource sizes
     * @param difference  Difference of new .rsrc file size and old file size
     */
    private void shiftHeaderOffset(int difference)
    {
        _headerOffset += difference;

        byte[] headerBytes = new byte[0x0F];

        byte[] offsetBytes = ByteBuffer.allocate(4).putInt(_headerOffset).array();

        // Build 16 header bytes
        System.arraycopy(offsetBytes, 0x00, headerBytes, 0x04, offsetBytes.length);
        offsetBytes[2] -= 0x01;
        System.arraycopy(offsetBytes, 0x00, headerBytes, 0x08, offsetBytes.length);

        // Update header bytes with new offset bytes
        System.arraycopy(headerBytes, 0x00, _data, 0x00, headerBytes.length);
        System.arraycopy(headerBytes, 0x00, _data, _headerOffset, headerBytes.length);
    }

    /**
     * Updates all resource offsets in the header table to
     * account for the replacement resource which will either be
     * larger or smaller in size
     * @param shiftedOffset  The replaced {@link Resource}'s byte offset
     * @param shiftvalue  The difference between the new and old
     *                    resource's size
     */
    private void updateHeaderTable(int shiftedOffset, int shiftvalue)
    {
        _pointer = _headerOffset;

        for (ResourceLinkedList list : _headerTable.values())
        {
            list.forEach(t ->
            {
                if (t.getStartOffset() > shiftedOffset)
                {
                    t.shiftStartOffset(shiftvalue);
                }

                byte[] id = Arrays.copyOf(t.getResourceIDBytes(), 0x04);
                id[2] = id[3] = (byte) 0xFF;

                // Should already be at resource ID, but verify w/ findMatchingOffsetStart, skip 4 bytes to start
                // at resource offset bytes, then copy in new resource offset
                int index = findMatchingOffsetStart(id, _pointer) + 0x04;
                System.arraycopy(t.getStartOffsetBytes(), 0x00, _data, index, 0x08);

                // Skip just read offset bytes to next resource ID
                _pointer = index + 0x08;
            });
        }
    }

    /**
     * Gets the {@link Resource} object representing
     * the supplied resource ID and type, if available.
     * @param id  The resource's ID
     * @param type  The resource's type
     * @return  {@code Resource} object if it exists in the .rsrc file,
     *          null otherwise
     * @see {@link ResourceType}
     */
    public Resource getResourceByID(int id, ResourceType type)
    {
        int index = _headerTable.get(type).indexOf(id);
        return _headerTable.get(type).get(index);
    }

    /**
     * Searches this .rsrc file for the given {@link ResourceType}
     * and returns a list of all resources if present
     * @param type  {@code ResourceType} to search for
     * @return  list of {@link Resource} objects matching the type
     * @see {@link ResourceLinkedList}
     */
    public ResourceLinkedList getResourceListByType(ResourceType type)
    {
        return (_headerTable.containsKey(type)) ? _headerTable.get(type) : new ResourceLinkedList();
    }

    /**
     * Returns a list of all unique {@link ResourceType}'s
     * found in this .rsrc file
     * @return  {@code} Array of {@code ResourceType}s
     */
    public ResourceType[] getResourceTypesInFile()
    {
        return _headerTable.keySet().toArray(new ResourceType[]{});
    }

    /**
     * Knuth-Morris-Pratt Algorithm custom implementation
     * for byte pattern matching. Search will be performed on
     * this {@code RsrcFile} object
     * @param pattern  The byte pattern to search for
     * @param startOffset  The offset to begin searching at
     * @return  The index of the first byte for the first pattern
     *          found matching the pattern sequence
     */
    private int findMatchingOffsetStart(byte[] pattern, int startOffset)
    {
        int j = 0; int matchPoint = -1;
        int[] failure = new int[pattern.length];

        if(_data.length == 0)
        {
            return -1;
        }

        for (int i = startOffset; i < _data.length; i++)
        {
            while (j > 0 && pattern[j] != _data[i])
            {
                j = failure[j - 1];
            }
            if (pattern[j] == _data[i])
            {
                j += 1;
            }
            if (j == pattern.length)
            {
                matchPoint = i - pattern.length + 1;
                break;
            }
        }

        return matchPoint;
    }

    /**
     * Saves this {@code RsrcFile} to disk,
     * overrwriting the old file
     * @param createBackup  If it doesn't already exist, creates a
     *                      backup of the file being used in this
     *                      {@code RsrcFile} object named "*.rsrc.bak"
     */
    public void saveRsrcFile(boolean createBackup) throws IOException
    {
        FileOutputStream fos;
        if(createBackup)
        {
            Files.copy(_file.toPath(), new File(_file + ".bak").toPath());
        }

        fos = new FileOutputStream(_file);
        fos.write(_data);
        fos.close();
    }

    /** Returns the size, in bytes, of the file used to instantiate this {@code RsrcFile} */
    public int getFileSizeInBytes()
    {
        return (int) _file.length();
    }

    /**
     *  Returns the bytes in this {@code RsrcFile} object.
     *  Any new files inserted will be reflected in this {@codebyte[]}
     */
    public byte[] getRsrcData()
    {
        return _data;
    }

    /** Sets the raw data backing this {@code RsrcFile} object */
    public void setRsrcData(byte[] _data)
    {
        this._data = _data;
    }

    /** Returns the current header offset location */
    public int getHeaderOffset()
    {
        return _headerOffset;
    }

    /** Sets the current header offset location */
    public void setHeaderOffset(int _headerOffset)
    {
        this._headerOffset = _headerOffset;
    }

    /** Returns the header table, which maps resource IDs to their offsets in the file */
    public LinkedHashMap<ResourceType, ResourceLinkedList> getHeaderTable()
    {
        return _headerTable;
    }

    /** Sets the header table, which maps resource IDs to their offsets in the file */
    public void setHeaderTable(LinkedHashMap<ResourceType, ResourceLinkedList> _headerTable)
    {
        this._headerTable = _headerTable;
    }

    /** Returns the current offset being read in the file */
    public int getPointerLocation()
    {
        return _pointer;
    }

    /** Sets the current offset being read in the file */
    public void setPointerLocation(int _pointer)
    {
        this._pointer = _pointer;
    }

    /** Returns the .rsrc file backing this {@code RsrcFile} object */
    public File getRsrcFile()
    {
        return _file;
    }

    /** Sets the .rsrc file backing this {@code RsrcFile} and processes it */
    public void loadRsrcFile(File file)
    {
        this._file = file;

        init();
    }
}
