package com.apophenic.main;

import com.sun.javafx.scene.control.skin.VirtualFlow.ArrayLinkedList;

/**
 * A custom implementation of {@link ArrayLinkedList<Resource>}
 */
public class ResourceLinkedList extends ArrayLinkedList<Resource>
{
    /**
     * Searches for a {@link Resource} based on its ID
     * @param o  {@code int} resource's ID
     * @return  {@code Resource} matching this ID
     */
    @Override
    public int indexOf (Object o)
    {
        for (Resource res : this)
        {
            if (res.getResourceID() == (int) o)
            {
                return super.indexOf(res);
            }
        }
        return -1;
    }

    @Override
    public Resource get(int index)
    {
        return (index < this.size()) ? super.get(index) : null;
    }
}
