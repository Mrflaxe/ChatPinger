package ru.mrflaxe.chatpinger;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.ComponentConverter;
import com.comphenix.protocol.wrappers.EnumWrappers.ChatType;
import com.comphenix.protocol.wrappers.WrappedChatComponent;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import ru.soknight.lib.configuration.Configuration;

public class ChatPinger extends JavaPlugin {
    
    @Override
    public void onEnable() {
        Configuration config = new Configuration(this, "config.yml");
        config.refresh();
        
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        
        protocolManager.addPacketListener(
        new PacketAdapter(this, PacketType.Play.Server.CHAT) {
            
            // Sending packet from server to player
            @Override
            public void onPacketSending(PacketEvent event) {
                if (event.getPacketType() != PacketType.Play.Server.CHAT) return;
                
                PacketContainer packet = event.getPacket();
                ChatType messageType = packet.getChatTypes().read(0);
                
                if(!messageType.equals(ChatType.CHAT)) return;
                         
                WrappedChatComponent component = packet.getChatComponents().read(0);
                BaseComponent[] components = ComponentConverter.fromWrapper(component);
                TextComponent textComponent = new TextComponent(components);
                String text = textComponent.toLegacyText();
                
                String name = "@" + event.getPlayer().getDisplayName();
                
                if(!text.contains(name)) return;
                
                event.setCancelled(true);
                
                int begin = text.indexOf(name);
                int end = begin + name.length();

                String fragment = text.substring(begin, end);
                BaseComponent[] nameComponent = TextComponent.fromLegacyText(fragment);

                ChatColor color = nameComponent[0].getColor();
                String replacement = config.getColoredString("color", "\u00a7e") + name + color;
                String message = text.replaceFirst(name, replacement);
                
                Player player = event.getPlayer();
                player.sendMessage(message);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
            }
        });
        
        protocolManager.addPacketListener(
                new PacketAdapter(this, PacketType.Play.Client.CHAT) {
                    
                    // Receive packet from player
                    @Override
                    public void onPacketReceiving(PacketEvent event) {
                        if (event.getPacketType() != PacketType.Play.Client.CHAT) return;
                        
                        PacketContainer packet = event.getPacket();
                        String text = packet.getStrings().read(0);
                        
                        if(text.startsWith("/")) return;
                        
                        List<String> names = Bukkit.getOnlinePlayers().stream()
                                .map(Player::getDisplayName)
                                .filter(name -> text.contains(name))
                                .collect(Collectors.toList());
                        
                        if(names.size() == 0) return;
                        
                        String replaced = text;
                        
                        for(String name : names) {
                            String replacement = "@" + name;
                            replaced = replaced.replaceFirst(name, replacement);
                        }
                        
                        packet.getStrings().write(0, replaced);
                        event.setPacket(packet);
                    }
                });
    }
}
