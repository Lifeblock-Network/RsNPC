package com.smallaswater.npc;

import org.powernukkitx.Player;
import org.powernukkitx.entity.Entity;
import org.powernukkitx.event.EventHandler;
import org.powernukkitx.event.EventPriority;
import org.powernukkitx.event.Listener;
import org.powernukkitx.event.entity.EntityDamageByChildEntityEvent;
import org.powernukkitx.event.entity.EntityDamageByEntityEvent;
import org.powernukkitx.event.entity.EntityDamageEvent;
import org.powernukkitx.event.entity.EntityVehicleEnterEvent;
import org.powernukkitx.event.player.PlayerInteractEntityEvent;
import org.powernukkitx.event.server.PacketSendEvent;
import org.cloudburstmc.protocol.bedrock.data.payload.list.PlayerListAddEntry;
import org.cloudburstmc.protocol.bedrock.data.payload.list.PlayerListEntry;
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket;
import com.smallaswater.npc.data.RsNpcConfig;
import com.smallaswater.npc.dialog.DialogPages;
import com.smallaswater.npc.entitys.EntityRsNPC;
import com.smallaswater.npc.utils.Utils;
import com.smallaswater.npc.variable.VariableManage;

/**
 * @author lt_name
 */
@SuppressWarnings("unused")
public class OnListener implements Listener {

    private final RsNPC rsNPC;

    public OnListener(RsNPC rsNPC) {
        this.rsNPC = rsNPC;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityVehicleEnter(EntityVehicleEnterEvent event) {
        if (event.getEntity() instanceof EntityRsNPC ||
                event.getVehicle() instanceof EntityRsNPC) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof EntityRsNPC) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            EntityRsNPC entityRsNPC = (EntityRsNPC) entity;
            RsNpcConfig config = entityRsNPC.getConfig();
            entityRsNPC.setPauseMoveTick(60);
            Utils.executeCommand(player, config);
            for (String message : config.getMessages()) {
                player.sendMessage(VariableManage.stringReplace(player, message, config));
            }
            if (entityRsNPC.getConfig().isEnabledDialogPages()) {
                DialogPages dialogConfig = this.rsNPC.getDialogManager().getDialogConfig(entityRsNPC.getConfig().getDialogPagesName());
                dialogConfig.getDefaultDialogPage().send(entityRsNPC, player);
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof EntityRsNPC) {
            event.setCancelled(true);
            if (event instanceof EntityDamageByEntityEvent) {
                Entity damage = ((EntityDamageByEntityEvent) event).getDamager();
                if (damage instanceof Player) {
                    Player player = (Player) damage;
                    EntityRsNPC entityRsNpc = (EntityRsNPC) entity;
                    RsNpcConfig rsNpcConfig = entityRsNpc.getConfig();
                    if (!rsNpcConfig.isCanProjectilesTrigger() &&
                            event instanceof EntityDamageByChildEntityEvent) {
                        return;
                    }
                    entityRsNpc.setPauseMoveTick(60);
                    Utils.executeCommand(player, rsNpcConfig);
                    for (String message : rsNpcConfig.getMessages()) {
                        player.sendMessage(VariableManage.stringReplace(player, message, rsNpcConfig));
                    }

                    if (rsNpcConfig.isEnabledDialogPages()) {
                        DialogPages dialogConfig = this.rsNPC.getDialogManager().getDialogConfig(rsNpcConfig.getDialogPagesName());
                        if (dialogConfig != null) {
                            dialogConfig.getDefaultDialogPage().send(entityRsNpc, player);
                        } else {
                            String message = "§cNPC " + rsNpcConfig.getName() + " 配置错误！不存在名为 " + rsNpcConfig.getDialogPagesName() + " 的对话框页面！";
                            this.rsNPC.getLogger().warning(message);
                            if (player.isOp()) {
                                player.sendMessage(message);
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDataPacketSend(PacketSendEvent event) {
        if (event.getPacket() instanceof PlayerListPacket packet) {
            if (Api.isHideCustomSkin(event.getPlayer())) {
                for (PlayerListEntry entry : packet.getEntries()) {
                    if (!(entry instanceof PlayerListAddEntry addEntry)) {
                        continue;
                    }
                    for (RsNpcConfig config : this.rsNPC.getNpcs().values()) {
                        EntityRsNPC entityRsNpc = config.getEntityRsNpc();
                        if (entityRsNpc != null && entityRsNpc.getUniqueId() == addEntry.getUuid()) {
                            addEntry.setSkin(this.rsNPC.getSkinByName("默认皮肤").getSkin());
                            break;
                        }
                    }
                }
            }
        }
    }
}
