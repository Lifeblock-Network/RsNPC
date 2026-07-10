package com.smallaswater.npc.form;

import org.powernukkitx.Player;
import org.powernukkitx.Server;
import org.powernukkitx.event.EventHandler;
import org.powernukkitx.event.EventPriority;
import org.powernukkitx.event.Listener;
import org.powernukkitx.event.player.PlayerServerSettingsRequestEvent;
import org.powernukkitx.event.server.PacketReceiveEvent;
import org.powernukkitx.form.window.Form;
import org.cloudburstmc.protocol.bedrock.packet.NpcRequestPacket;
import org.cloudburstmc.protocol.bedrock.packet.ServerSettingsResponsePacket;
import com.smallaswater.npc.RsNPC;
import com.smallaswater.npc.form.windows.AdvancedFormWindowDialog;

import java.util.HashMap;
import java.util.Map;

/**
 * Window action listener.
 * Implements AdvancedFormWindow / AdvancedInventory action handling.
 *
 * @author LT_Name
 */
@SuppressWarnings("unused")
public class WindowListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDataPacketReceive(PacketReceiveEvent event) {
        if (event.getPacket() instanceof NpcRequestPacket) {
            if (AdvancedFormWindowDialog.onEvent((NpcRequestPacket) event.getPacket(), event.getPlayer())) {
                event.setCancelled(true);
            }
        }
    }

//    @EventHandler(priority = EventPriority.MONITOR)
//    public void onPlayerFormResponded(PlayerFormRespondedEvent event) {
//        if (SimpleForm.onEvent(event.getWindow(), event.getPlayer())) {
//            return;
//        }
//        if (ModalForm.onEvent(event.getWindow(), event.getPlayer())) {
//            return;
//        }
//        AdvancedFormWindowCustom.onEvent(event.getWindow(), event.getPlayer());
//    }

//    @EventHandler(priority = EventPriority.MONITOR)
//    public void onPlayerSettingsResponded(PlayerSettingsRespondedEvent event) {
//        AdvancedFormWindowCustom.onEvent(event.getWindow(), event.getPlayer());
//    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSettingsRequest(PlayerServerSettingsRequestEvent event) {
        Player player = event.getPlayer();
        HashMap<Integer, Form<?>> map = new HashMap<>(event.getSettings());
        event.setSettings(new HashMap<>());
        //a short delay is required, otherwise the client doesn't display it
        Server.getInstance().getScheduler().scheduleDelayedTask(RsNPC.getInstance(), () -> {
            for (Map.Entry<Integer, Form<?>> entry : map.entrySet()) {
                ServerSettingsResponsePacket pk = new ServerSettingsResponsePacket();
                pk.setFormID(entry.getKey());
                pk.setFormData(entry.getValue().toJson());
                player.sendPacket(pk);
            }
        }, 20);
    }
}
