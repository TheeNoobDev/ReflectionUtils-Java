import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

public class ReflectionUtils {
	
	 // Only Tested 1.12.2
	
	public static String getVersion() {
		return Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
	}
	
	public static void sendPacket(Player player, Object packet) {
		try {
			Object handle = player.getClass().getMethod("getHandle").invoke(player);
			Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
			playerConnection.getClass().getMethod("sendPacket", getNMSClass("Packet")).invoke(playerConnection, packet);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static Class<?> getNMSClass(String name) {
		try {
			return Class.forName("net.minecraft.server." + getVersion() + "." + name);
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
			return null;
		}
	}
	
	public static Class<?> getBukkitClass(String clazz) {
		try {
			return Class.forName("org.bukkit.craftbukkit."+ getVersion() + "." +clazz);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static int getReloadCount() {
		try {
			Class<?> clazz = getBukkitClass("CraftServer");
			Object count = clazz.getDeclaredField("reloadCount").get(Bukkit.getServer());
			clazz.getDeclaredField("reloadCount").setAccessible(true);
			return Integer.valueOf(count.toString());
		}catch(Exception e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	public static Integer getPing(Player player) {
		try {
			Object handle = player.getClass().getMethod("getHandle").invoke(player);
			Object ping = handle.getClass().getField("ping").get(handle);
			return Integer.valueOf(ping.toString());
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		return 0;
		
	}
	
	public static void respawn(Player player) {
		try {
			Class<?> respawnClass = getNMSClass("PacketPlayInClientCommand");
			Object enumPerformRespawn = getNMSClass("PacketPlayInClientCommand").getDeclaredClasses()[0].getField("PERFORM_RESPAWN").get(null);
			Object handle = player.getClass().getMethod("getHandle").invoke(player);
			Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
			playerConnection.getClass().getMethod("a", respawnClass).invoke(playerConnection, respawnClass.getConstructors()[1].newInstance(enumPerformRespawn));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static Class<?> getIChatBaseComponentClass() {
		return ReflectionUtils.getNMSClass("IChatBaseComponent");
	}
	
	public static Object stringToIChatBaseComponent(String text) {
		Class<?> chatSerializer = ReflectionUtils.getNMSClass("IChatBaseComponent").getDeclaredClasses()[0];
		Object chatTitle;
		try {
			chatTitle = chatSerializer.getMethod("a", String.class).invoke(chatSerializer, "{\"text\": \"" + text + "\"}");
			return chatTitle;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static void setPlayerList(Player player, List<String> header, List<String> footer) {
		try {
			String head = "";
			String foot = "";
			for(String s : header) {head = head+s+"\\n";}
			for(String s : footer) {foot = foot+s+"\\n";}
			head = head.substring(0, head.length()-3);
			foot = foot.substring(0, foot.length()-3);
			Object packet = ReflectionUtils.getNMSClass("PacketPlayOutPlayerListHeaderFooter").getDeclaredConstructor().newInstance();
	            Field headerField = packet.getClass().getDeclaredField("a");
	            headerField.setAccessible(true);
	            headerField.set(packet, ReflectionUtils.stringToIChatBaseComponent(head));
	       
	            Field footerField = packet.getClass().getDeclaredField("b");
	            footerField.setAccessible(true);
	            footerField.set(packet, ReflectionUtils.stringToIChatBaseComponent(foot));
	        ReflectionUtils.sendPacket(player, packet);
	    } catch (Exception e) {
	            e.printStackTrace();
	    }
	}
	
	public static void updateInventory(Player player) {
		try {
			Object handle = player.getClass().getMethod("getHandle").invoke(player);
			Object human = ReflectionUtils.getBukkitClass("entity.CraftHumanEntity").getMethod("getHandle").invoke((HumanEntity)(player));
			Object activeContainer = human.getClass().getField("activeContainer").get(human);
			handle.getClass().getMethod("updateInventory", ReflectionUtils.getNMSClass("Container")).invoke(handle, activeContainer);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void updateInventoryTitle(Player player, String title, String inventoryItemType) {
		try {
			Object handle = player.getClass().getMethod("getHandle").invoke(player);
			Object human = ReflectionUtils.getBukkitClass("entity.CraftHumanEntity").getMethod("getHandle").invoke((HumanEntity)(player));
			  
			Class<?> packetPlayOutOpenWindow = ReflectionUtils.getNMSClass("PacketPlayOutOpenWindow");
			
		    Object activeContainer = human.getClass().getField("activeContainer").get(human);
			Object windowId = activeContainer.getClass().getField("windowId").get(activeContainer);
			  
			Constructor<?> constructor = packetPlayOutOpenWindow.getDeclaredConstructor(int.class, String.class, ReflectionUtils.getIChatBaseComponentClass(), int.class);
			Object packet = constructor.newInstance(windowId, inventoryItemType, ReflectionUtils.stringToIChatBaseComponent(title), player.getOpenInventory().getTopInventory().getSize());
			ReflectionUtils.sendPacket(player, packet);
			handle.getClass().getMethod("updateInventory", ReflectionUtils.getNMSClass("Container")).invoke(handle, activeContainer);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static SkullMeta getCustomSkullMeta(ItemStack item, String url) {
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        GameProfile profile = new GameProfile(UUID.randomUUID(), null);
        byte[] data = Base64.getEncoder().encode(String.format("{textures:{SKIN:{url:\"%s\"}}}", url).getBytes());
        profile.getProperties().put("textures", new Property("textures", new String(data)));
        try {
        	Field profileField = meta.getClass().getDeclaredField("profile");
        	profileField.setAccessible(true);
        	profileField.set(meta, profile);
        	item.setItemMeta(meta);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return meta;
    }

	public static void sendTitle(Player player, String title, String subtitle, int fadein, int stay, int fadeout) {
		title = ChatColor.translateAlternateColorCodes('&', title);
		subtitle = ChatColor.translateAlternateColorCodes('&', subtitle);
		Class<?> chatSerializer = getNMSClass("IChatBaseComponent").getDeclaredClasses()[0];
		Class<?> chatComponent = getNMSClass("IChatBaseComponent");
		Class<?> packetTitle = getNMSClass("PacketPlayOutTitle");
		try {
			Object enumTitle = getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0].getField("TITLE").get(null);
			Constructor<?> constructorTitle = packetTitle.getDeclaredConstructor(packetTitle.getDeclaredClasses()[0], chatComponent, int.class, int.class, int.class);
			Object chatTitle = chatSerializer.getMethod("a", String.class).invoke(chatSerializer, "{\"text\": \"" + title + "\"}");
			Object packet = constructorTitle.newInstance(enumTitle, chatTitle, fadein, stay, fadeout);
			sendPacket(player, packet);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		try {
			Object enumSubtitle = getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0].getField("SUBTITLE").get(null);
			Constructor<?> constructorSubtitle = packetTitle.getDeclaredConstructor(packetTitle.getDeclaredClasses()[0], chatComponent, int.class, int.class, int.class);
			Object chatSubtitle = chatSerializer.getMethod("a", String.class).invoke(chatSerializer, "{\"text\": \"" + subtitle + "\"}");
			Object packet = constructorSubtitle.newInstance(enumSubtitle, chatSubtitle, fadein, stay, fadeout);
			sendPacket(player, packet);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static void sendActionBar(Player player, String message, byte delay) {
		message = ChatColor.translateAlternateColorCodes('&', message);
		Class<?> chatSerializer = getNMSClass("IChatBaseComponent").getDeclaredClasses()[0];
		Class<?> chatComponent = getNMSClass("IChatBaseComponent");
		Class<?> packetActionbar = getNMSClass("PacketPlayOutChat");
		try {
			Constructor<?> ConstructorActionbar = packetActionbar.getDeclaredConstructor(chatComponent, byte.class);
			Object actionbar = chatSerializer.getMethod("a", String.class).invoke(chatSerializer, "{\"text\": \"" + message + "\"}");
			Object packet = ConstructorActionbar.newInstance(actionbar, (byte) delay);
			sendPacket(player, packet);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}