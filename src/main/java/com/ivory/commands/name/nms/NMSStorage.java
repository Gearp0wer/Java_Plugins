package com.ivory.commands.name.nms;

import io.netty.channel.Channel;
import org.bukkit.Bukkit;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NMSStorage
{
    private static NMSStorage instance;
    private int minorVersion;
    private FunctionWithException<String, Class<?>> classFunction;
    private final boolean is1_19_3Plus = classExists("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket");
    private final boolean is1_19_4Plus;

    public Field PLAYER_CONNECTION;
    public Field NETWORK_MANAGER;
    public Field CHANNEL;
    public final Method getHandle;

    private Class<?> dataWatcher;
    private Class<?> dataWatcherItem;
    public Constructor<?> newDataWatcher;
    public Constructor<?> newDataWatcherObject;
    public Field DataWatcherItem_TYPE;
    public Field DataWatcherItem_VALUE;
    public Field DataWatcherObject_SLOT;
    public Field DataWatcherObject_SERIALIZER;
    public Method DataWatcher_REGISTER;
    protected Class<?> DataWatcher$DataValue;
    public Field DataWatcher$DataValue_POSITION;
    public Field DataWatcher$DataValue_VALUE;
    public Method DataWatcher_markDirty;

    public Class<?> PacketPlayOutSpawnEntityLiving;
    public Field PacketPlayOutSpawnEntityLiving_DATAWATCHER;
    public Class<?> PacketPlayOutEntityMetadata;
    public Field PacketPlayOutEntityMetadata_LIST;
    public Class<?> ClientboundBundlePacket;
    public Constructor<?> newClientboundBundlePacket;
    public Field ClientboundBundlePacket_packets;

    public NMSStorage() throws ReflectiveOperationException
    {
        detectServerVersion();
        getHandle = Class.forName(Bukkit.getServer().getClass().getPackage().getName()
                + ".entity.CraftPlayer").getMethod("getHandle");
        is1_19_4Plus = is1_19_3Plus && (minorVersion > 19
                || !Bukkit.getServer().getClass().getPackage().getName().contains("v1_19_R2"));

        Class<?> entityPlayer = getClass("server.level.ServerPlayer");
        Class<?> playerConnection = getClass("server.network.ServerGamePacketListenerImpl");
        PLAYER_CONNECTION = getFields(entityPlayer, playerConnection).get(0);
        dataWatcher = getClass("network.syncher.SynchedEntityData");
        dataWatcherItem = getClass("network.syncher.SynchedEntityData$DataItem");
        DataWatcherItem_VALUE = getFields(dataWatcherItem, Object.class).get(0);
        PacketPlayOutEntityMetadata = getClass("network.protocol.game.ClientboundSetEntityDataPacket");
        PacketPlayOutEntityMetadata_LIST = getFields(PacketPlayOutEntityMetadata, List.class).get(0);

        Class<?> networkManager = getClass("network.Connection");
        List<Field> networkFields = getFields(playerConnection, networkManager);
        if (networkFields.isEmpty())
        {
            networkFields = getFields(playerConnection.getSuperclass(), networkManager);
        }
        NETWORK_MANAGER = networkFields.get(0);
        CHANNEL = getFields(networkManager, Channel.class).get(0);

        initializeDataWatcher();
        if (minorVersion <= 14)
        {
            PacketPlayOutSpawnEntityLiving = getClass("network.protocol.game.ClientboundAddEntityPacket");
            PacketPlayOutSpawnEntityLiving_DATAWATCHER = getFields(PacketPlayOutSpawnEntityLiving, dataWatcher).get(0);
        }
        if (is1_19_4Plus)
        {
            ClientboundBundlePacket = getClass("network.protocol.game.ClientboundBundlePacket");
            newClientboundBundlePacket = ClientboundBundlePacket.getDeclaredConstructors()[0];
            ClientboundBundlePacket_packets = ClientboundBundlePacket.getSuperclass().getDeclaredFields()[0];
            ClientboundBundlePacket_packets.setAccessible(true);
        }
    }

    private void detectServerVersion()
    {
        classFunction = name -> Class.forName("net.minecraft." + name);
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        String[] parts = packageName.split("\\.");
        minorVersion = parts.length > 3 && parts[3].startsWith("v1_")
                ? Integer.parseInt(parts[3].split("_")[1])
                : Integer.parseInt(Bukkit.getBukkitVersion().split("-")[0].split("\\.")[1]);
    }

    private void initializeDataWatcher() throws ReflectiveOperationException
    {
        Class<?> dataWatcherObject = getClass("network.syncher.EntityDataAccessor");
        Class<?> serializer = getClass("network.syncher.EntityDataSerializer");
        newDataWatcherObject = dataWatcherObject.getConstructor(int.class, serializer);
        DataWatcherItem_TYPE = getFields(dataWatcherItem, dataWatcherObject).get(0);
        DataWatcherObject_SLOT = getFields(dataWatcherObject, int.class).get(0);
        DataWatcherObject_SERIALIZER = getFields(dataWatcherObject, serializer).get(0);
        DataWatcher_REGISTER = getMethod(dataWatcher, new String[]{"set", "register"}, dataWatcherObject, Object.class);
        if (!is1_19_3Plus)
        {
            return;
        }
        DataWatcher$DataValue = getClass("network.syncher.SynchedEntityData$DataValue");
        DataWatcher$DataValue_POSITION = getFields(DataWatcher$DataValue, int.class).get(0);
        DataWatcher$DataValue_VALUE = getFields(DataWatcher$DataValue, Object.class).get(0);
        DataWatcher_markDirty = getMethods(dataWatcher, dataWatcherObject).get(0);
    }

    private Class<?> getClass(String name) throws ClassNotFoundException
    {
        try
        {
            return classFunction.apply(name);
        }
        catch (Exception exception)
        {
            throw new ClassNotFoundException(name, exception);
        }
    }

    private static List<Field> getFields(Class<?> type, Class<?> fieldType)
    {
        List<Field> fields = new ArrayList<>();
        if (type == null)
        {
            return fields;
        }

        for (Field field : type.getDeclaredFields())
        {
            if (field.getType() == fieldType)
            {
                field.setAccessible(true);
                fields.add(field);
            }
        }
        return fields;
    }

    private static Method getMethod(Class<?> type, String[] names, Class<?>... parameterTypes)
            throws NoSuchMethodException
    {
        for (String name : names)
        {
            try
            {
                return type.getMethod(name, parameterTypes);
            }
            catch (NoSuchMethodException ignored)
            {
            }
        }
        throw new NoSuchMethodException(Arrays.toString(names));
    }

    private static List<Method> getMethods(Class<?> type, Class<?>... parameterTypes)
    {
        List<Method> methods = new ArrayList<>();
        for (Method method : type.getDeclaredMethods())
        {
            if (method.getReturnType() == void.class && Modifier.isPublic(method.getModifiers())
                    && Arrays.equals(method.getParameterTypes(), parameterTypes))
            {
                methods.add(method);
            }
        }
        return methods;
    }

    private static boolean classExists(String name)
    {
        try
        {
            Class.forName(name);
            return true;
        }
        catch (ClassNotFoundException ignored)
        {
            return false;
        }
    }

    public static void setInstance(NMSStorage storage)
    {
        instance = storage;
    }

    public static NMSStorage getInstance()
    {
        return instance;
    }

    public int getMinorVersion()
    {
        return minorVersion;
    }

    public boolean is1_19_3Plus()
    {
        return is1_19_3Plus;
    }

    public boolean is1_19_4Plus()
    {
        return is1_19_4Plus;
    }
}
