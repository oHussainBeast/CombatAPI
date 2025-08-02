package me.hussainbeast.combatapi.util;

import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ActionBarUtil {
    
    private static String nmsVersion;
    private static Class<?> craftPlayerClass;
    private static Class<?> packetPlayOutChatClass;
    private static Class<?> chatComponentTextClass;
    private static Class<?> iChatBaseComponentClass;
    private static Class<?> packetClass;
    private static Constructor<?> chatComponentTextConstructor;
    private static Constructor<?> packetPlayOutChatConstructor;
    private static Method getHandleMethod;
    private static Field playerConnectionField;
    private static Method sendPacketMethod;
    private static boolean reflectionInitialized = false;
    
    static {
        try {
            nmsVersion = org.bukkit.Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            initializeReflection();
        } catch (Exception e) {
            nmsVersion = "unknown";
            reflectionInitialized = false;
        }
    }
    
    private static void initializeReflection() {
        try {
            craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + nmsVersion + ".entity.CraftPlayer");
            packetPlayOutChatClass = Class.forName("net.minecraft.server." + nmsVersion + ".PacketPlayOutChat");
            chatComponentTextClass = Class.forName("net.minecraft.server." + nmsVersion + ".ChatComponentText");
            iChatBaseComponentClass = Class.forName("net.minecraft.server." + nmsVersion + ".IChatBaseComponent");
            packetClass = Class.forName("net.minecraft.server." + nmsVersion + ".Packet");
            
            chatComponentTextConstructor = chatComponentTextClass.getConstructor(String.class);
            packetPlayOutChatConstructor = packetPlayOutChatClass.getConstructor(iChatBaseComponentClass, byte.class);
            getHandleMethod = craftPlayerClass.getMethod("getHandle");
            
            Class<?> entityPlayerClass = Class.forName("net.minecraft.server." + nmsVersion + ".EntityPlayer");
            playerConnectionField = entityPlayerClass.getField("playerConnection");
            
            Class<?> playerConnectionClass = Class.forName("net.minecraft.server." + nmsVersion + ".PlayerConnection");
            sendPacketMethod = playerConnectionClass.getMethod("sendPacket", packetClass);
            
            reflectionInitialized = true;
        } catch (Exception e) {
            reflectionInitialized = false;
        }
    }
    
    public static void sendActionBar(Player player, String message) {
        if (player == null || message == null) {
            return;
        }
        
        if (!reflectionInitialized) {
            player.sendMessage(message);
            return;
        }
        
        try {
            Object craftPlayer = craftPlayerClass.cast(player);
            Object chatComponentText = chatComponentTextConstructor.newInstance(message);
            Object packetPlayOutChat = packetPlayOutChatConstructor.newInstance(chatComponentText, (byte) 2);
            
            Object entityPlayer = getHandleMethod.invoke(craftPlayer);
            Object playerConnection = playerConnectionField.get(entityPlayer);
            
            sendPacketMethod.invoke(playerConnection, packetPlayOutChat);
            
        } catch (Exception e) {
            player.sendMessage(message);
        }
    }
}