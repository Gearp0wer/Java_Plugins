package com.ivory.commands.name;

import com.google.common.base.Optional;
import com.ivory.commands.IvoryCommands;
import com.ivory.commands.name.nms.NMSStorage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

public final class PipelineInjector
{
    private static final String HANDLER_NAME = "ivory_pet_name_filter";
    private final IvoryCommands plugin;
    private final NMSStorage nms;
    private final int petOwnerPosition;

    public PipelineInjector(IvoryCommands plugin) throws ReflectiveOperationException
    {
        this.plugin = plugin;
        this.nms = new NMSStorage();
        NMSStorage.setInstance(nms);
        this.petOwnerPosition = getPetOwnerPosition();
    }

    public void injectAll()
    {
        if (plugin.getConfigManager().isFilterPetTeamPrefix())
        {
            Bukkit.getOnlinePlayers().forEach(this::inject);
        }
    }

    public void unload()
    {
        Bukkit.getOnlinePlayers().forEach(this::uninject);
    }

    public void inject(Player player)
    {
        Channel channel = getChannel(player);
        if (channel != null && channel.pipeline().names().contains("packet_handler"))
        {
            try
            {
                uninject(player);
                channel.pipeline().addBefore("packet_handler", HANDLER_NAME, new PacketHandler());
            }
            catch (Exception exception)
            {
                plugin.getLogger().log(Level.WARNING, "Could not inject pet name filter for "
                        + player.getName(), exception);
            }
        }
    }

    public void uninject(Player player)
    {
        Channel channel = getChannel(player);
        if (channel != null && channel.pipeline().names().contains(HANDLER_NAME))
        {
            channel.pipeline().remove(HANDLER_NAME);
        }
    }

    private int getPetOwnerPosition()
    {
        int version = nms.getMinorVersion();
        return version >= 17 ? 18 : version >= 15 ? 17 : version == 14 ? 16 : version >= 10 ? 14 : 13;
    }

    private Channel getChannel(Player player)
    {
        try
        {
            if (nms.CHANNEL != null)
            {
                Object handle = nms.getHandle.invoke(player);
                Object connection = nms.PLAYER_CONNECTION.get(handle);
                Object networkManager = nms.NETWORK_MANAGER.get(connection);
                return (Channel) nms.CHANNEL.get(networkManager);
            }
        }
        catch (Exception exception)
        {
            plugin.getLogger().log(Level.WARNING, "Could not find player channel for "
                    + player.getName(), exception);
        }
        return null;
    }

    private final class PacketHandler extends ChannelDuplexHandler
    {
        @Override
        public void write(ChannelHandlerContext ctx, Object packet, ChannelPromise promise) throws Exception
        {
            try
            {
                if (nms.is1_19_4Plus() && nms.ClientboundBundlePacket.isInstance(packet))
                {
                    List<Object> validPackets = new ArrayList<>();
                    Iterable<?> packets = (Iterable<?>) nms.ClientboundBundlePacket_packets.get(packet);
                    for (Object subPacket : packets)
                    {
                        if (!nms.PacketPlayOutEntityMetadata.isInstance(subPacket)
                                || !filterMetadata(subPacket))
                        {
                            validPackets.add(subPacket);
                        }
                    }
                    if (validPackets.isEmpty())
                    {
                        return;
                    }
                    packet = nms.newClientboundBundlePacket.newInstance(validPackets);
                }
                else if (nms.PacketPlayOutEntityMetadata.isInstance(packet)
                        && filterMetadata(packet))
                {
                    return;
                }
                super.write(ctx, packet, promise);
            }
            catch (Exception exception)
            {
                super.write(ctx, packet, promise);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private boolean filterMetadata(Object packet) throws ReflectiveOperationException
    {
        Object removedEntry = null;
        List<Object> items = (List<Object>) nms.PacketPlayOutEntityMetadata_LIST.get(packet);
        if (items == null || items.isEmpty())
        {
            return false;
        }

        try
        {
            items.removeIf(Objects::isNull);
            for (Object item : items)
            {
                int slot = nms.DataWatcher$DataValue_POSITION.getInt(item);
                Object value = nms.DataWatcher$DataValue_VALUE.get(item);
                if (slot == petOwnerPosition
                        && (value instanceof java.util.Optional || value instanceof Optional))
                {
                    removedEntry = item;
                    break;
                }
            }
        }
        catch (ConcurrentModificationException exception)
        {
            return filterMetadata(packet);
        }

        if (removedEntry != null)
        {
            items.remove(removedEntry);
            return items.isEmpty();
        }
        return false;
    }
}
