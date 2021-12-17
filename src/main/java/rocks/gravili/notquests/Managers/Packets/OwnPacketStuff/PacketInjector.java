package rocks.gravili.notquests.Managers.Packets.OwnPacketStuff;

import io.netty.channel.Channel;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.NotQuests;

import java.lang.reflect.Field;


public class PacketInjector {
    private final NotQuests main;
    //NMS Mappings
    private final String entityPlayerClass = "level.EntityPlayer";
    private final String PlayerConnectionFieldInEntityPlayer = "b";
    private final String playerConnectionClass = "network.PlayerConnection";
    private final String NetworkManagerClass = "net.minecraft.network.NetworkManager";
    private Field EntityPlayer_playerConnection;
    private Class<?> PlayerConnection;
    private Field PlayerConnection_networkManager;
    private Class<?> NetworkManager;
    private Field channelField;
    private Field packetListenerField;
    private boolean packetStuffEnabled = true; //disabled if there is an error


    public PacketInjector(final NotQuests main) {
        this.main = main;
        initializeNMSStuff();
    }

    public final boolean isPacketStuffEnabled() {
        return packetStuffEnabled;
    }

    public void setPacketStuffEnabled(final boolean packetStuffEnabled) {
        this.packetStuffEnabled = packetStuffEnabled;
        main.getDataManager().getConfiguration().packetMagic = false;
        main.getDataManager().getConfiguration().deletePreviousConversations = false;
    }

    public void initializeNMSStuff() {
        try {
            EntityPlayer_playerConnection = Reflection.getField(Reflection.getNMSClass(entityPlayerClass), PlayerConnectionFieldInEntityPlayer); //adj => EntityPlayer //b => playerConnection https://nms.screamingsandals.org/1.18/net/minecraft/server/network/ServerGamePacketListenerImpl.html

            PlayerConnection = Reflection.getNMSClass(playerConnectionClass);
            PlayerConnection_networkManager = Reflection.getField(PlayerConnection, "a"); //a => Connection (NetworkManager)

            NetworkManager = Reflection.getClass(NetworkManagerClass);
            channelField = Reflection.getField(NetworkManager, "k");
            packetListenerField = Reflection.getField(NetworkManager, "m");
        } catch (Throwable t) {
            if (main.getDataManager().getConfiguration().debug) {
                t.printStackTrace();
            }
            main.getLogManager().warn("Disabling packet stuff because something went wrong...");
            setPacketStuffEnabled(false);
        }
    }

    public void addPlayer(Player player) {
        try {
            Channel ch = getChannel(getNetworkManager(Reflection.getNmsPlayer(player)));
            if (ch != null && ch.pipeline().get("PacketInjector") == null) {
                NQPacketListener h = new NQPacketListener(main, player);
                ch.pipeline().addBefore("packet_handler", "PacketInjector", h);
            }
        } catch (Throwable t) {
            if (main.getDataManager().getConfiguration().debug) {
                t.printStackTrace();
            }
            main.getLogManager().warn("Disabling packet stuff because something went wrong...");
            setPacketStuffEnabled(false);
        }
    }

    public void removePlayer(Player p) {
        try {
            Channel ch = getChannel(getNetworkManager(Reflection.getNmsPlayer(p)));
            if (ch != null && ch.pipeline().get("PacketInjector") != null) {
                ch.pipeline().remove("PacketInjector");
            }
        } catch (Throwable t) {
            if (main.getDataManager().getConfiguration().debug) {
                t.printStackTrace();
            }
            main.getLogManager().warn("Disabling packet stuff because something went wrong...");
            setPacketStuffEnabled(false);
        }
    }

    private Object getNetworkManager(Object ep) {
        return Reflection.getFieldValue(PlayerConnection_networkManager, Reflection.getFieldValue(EntityPlayer_playerConnection, ep));
    }

    private Channel getChannel(Object networkManager) {
        Channel ch = null;
        try {
            ch = Reflection.getFieldValue(channelField, networkManager);
        } catch (Exception e) {
            try {
                ch = Reflection.getFieldValue(packetListenerField, networkManager);
            } catch (Exception e2) {
                if (main.getDataManager().getConfiguration().debug) {
                    e2.printStackTrace();
                }
                main.getLogManager().warn("Disabling packet stuff because something went wrong...");
                setPacketStuffEnabled(false);
            }
        }
        return ch;
    }
}