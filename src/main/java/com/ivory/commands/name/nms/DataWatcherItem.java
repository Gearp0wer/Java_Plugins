package com.ivory.commands.name.nms;

public class DataWatcherItem
{
    private final DataWatcherObject type;
    private final Object value;

    public DataWatcherItem(DataWatcherObject type, Object value)
    {
        this.type = type;
        this.value = value;
    }

    public static DataWatcherItem fromNMS(Object nmsItem) throws ReflectiveOperationException
    {
        NMSStorage nms = NMSStorage.getInstance();
        if (nms.getMinorVersion() >= 9)
        {
            Object nmsObject = nms.DataWatcherItem_TYPE.get(nmsItem);
            return new DataWatcherItem(
                    new DataWatcherObject(nms.DataWatcherObject_SLOT.getInt(nmsObject),
                            nms.DataWatcherObject_SERIALIZER.get(nmsObject)),
                    nms.DataWatcherItem_VALUE.get(nmsItem));
        }
        return new DataWatcherItem(
                new DataWatcherObject(nms.DataWatcherItem_TYPE.getInt(nmsItem), null),
                nms.DataWatcherItem_VALUE.get(nmsItem));
    }

    public DataWatcherObject getType()
    {
        return type;
    }

    public Object getValue()
    {
        return value;
    }
}
