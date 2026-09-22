package com.ivory.commands.name.nms;

public class DataWatcherObject
{
    private final int position;
    private final Object classType;

    public DataWatcherObject(int position, Object classType)
    {
        this.position = position;
        this.classType = classType;
    }

    public int getPosition()
    {
        return position;
    }

    public Object getClassType()
    {
        return classType;
    }
}
