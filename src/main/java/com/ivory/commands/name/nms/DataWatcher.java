package com.ivory.commands.name.nms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataWatcher
{
    private final Map<Integer, DataWatcherItem> dataValues = new HashMap<>();

    public void setValue(DataWatcherObject type, Object value)
    {
        dataValues.put(type.getPosition(), new DataWatcherItem(type, value));
    }

    public void removeValue(int position)
    {
        dataValues.remove(position);
    }

    public DataWatcherItem getItem(int position)
    {
        return dataValues.get(position);
    }

    public Object toNMS() throws ReflectiveOperationException
    {
        NMSStorage nms = NMSStorage.getInstance();
        Object watcher = nms.newDataWatcher.getParameterCount() == 1
                ? nms.newDataWatcher.newInstance(new Object[]{null})
                : nms.newDataWatcher.newInstance();

        for (DataWatcherItem item : dataValues.values())
        {
            Object position = nms.getMinorVersion() >= 9
                    ? nms.newDataWatcherObject.newInstance(item.getType().getPosition(), item.getType().getClassType())
                    : item.getType().getPosition();
            nms.DataWatcher_REGISTER.invoke(watcher, position, item.getValue());
            if (nms.is1_19_3Plus())
            {
                nms.DataWatcher_markDirty.invoke(watcher, position);
            }
        }
        return watcher;
    }

    public static DataWatcher fromNMS(Object nmsWatcher) throws ReflectiveOperationException
    {
        DataWatcher watcher = new DataWatcher();
        List<?> items = (List<?>) nmsWatcher.getClass().getMethod("c").invoke(nmsWatcher);
        if (items != null)
        {
            for (Object item : items)
            {
                DataWatcherItem watcherItem = DataWatcherItem.fromNMS(item);
                watcher.setValue(watcherItem.getType(), watcherItem.getValue());
            }
        }
        return watcher;
    }
}
