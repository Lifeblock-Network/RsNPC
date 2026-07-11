package com.smallaswater.npc.command.sub;

import org.powernukkitx.Player;
import org.powernukkitx.Server;
import org.powernukkitx.command.CommandSender;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParamType;
import org.powernukkitx.command.data.CommandParameter;
import org.powernukkitx.utils.Config;
import com.smallaswater.npc.command.base.BaseSubCommand;
import com.smallaswater.npc.data.RsNpcConfig;
import com.smallaswater.npc.utils.Utils;

import java.io.File;
import java.util.LinkedHashMap;

/**
 * @author LT_Name
 */
public class CreateSubCommand extends BaseSubCommand {

    public CreateSubCommand(String name) {
        super(name);
    }

    @Override
    public boolean canUser(CommandSender sender) {
        return sender.isPlayer() && sender.hasPermission("RsNPC.admin.create");
    }

    @Override
    public String[] getAliases() {
        return new String[0];
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length > 1) {
            // Supports the subfolder/name form, e.g. /rsnpc create shops/blacksmith
            String name = args[1].trim().replace('\\', '/');
            while (name.startsWith("/")) {
                name = name.substring(1);
            }
            while (name.endsWith("/")) {
                name = name.substring(0, name.length() - 1);
            }
            if (name.isEmpty()) {
                sender.sendMessage(this.rsNPC.getLanguage().translateString("tips.nameRequired"));
                return true;
            }
            // Validate the path: disallow empty path segments and path traversal (..)
            boolean invalidPath = name.contains("//");
            for (String part : name.split("/")) {
                if (part.trim().isEmpty() || "..".equals(part)) {
                    invalidPath = true;
                    break;
                }
            }
            if (invalidPath) {
                sender.sendMessage("§cInvalid NPC name/path: " + name);
                return true;
            }
            if (this.rsNPC.getNpcs().containsKey(name)) {
                sender.sendMessage(this.rsNPC.getLanguage().translateString("tips.npcAlreadyExist", name));
                return true;
            }
            File npcFile = new File(this.rsNPC.getDataFolder() + "/Npcs/" + name + ".yml");
            if (npcFile.getParentFile() != null) {
                npcFile.getParentFile().mkdirs();
            }
            this.rsNPC.saveResource("Npc.yml", "/Npcs/" + name + ".yml", false);
            Config config = new Config(npcFile, Config.YAML);
            // The NPC identifier is the full path, but the default display name (the floating nametag) uses only the last segment for nicer presentation
            String displayName = name.contains("/") ? name.substring(name.lastIndexOf('/') + 1) : name;
            config.set("name", displayName);
            Player player = (Player) sender;
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            map.put("x", player.getX());
            map.put("y", player.getY());
            map.put("z", player.getZ());
            map.put("yaw", Utils.getYaw(player));
            map.put("level", player.getLevel().getName());
            config.set("spawn_point", map);
            config.save();
            RsNpcConfig rsNpcConfig;
            try {
                rsNpcConfig = new RsNpcConfig(name, config);
            } catch (Exception e) {
                sender.sendMessage(this.rsNPC.getLanguage().translateString("tips.npcCreationFailed"));
                this.rsNPC.getLogger().error("Failed to create NPC!", e);
                return true;
            }
            rsNpcConfig.setSourceFile(npcFile);
            this.rsNPC.getNpcs().put(name, rsNpcConfig);
            rsNpcConfig.checkEntity();
            //fix the issue where the NPC doesn't show on first spawn; respawn the entity to work around Nukkit not sending the PlayerListPacket in time
            Server.getInstance().getScheduler().scheduleDelayedTask(this.rsNPC, () -> rsNpcConfig.getEntityRsNpc().close(), 20);
            Server.getInstance().getScheduler().scheduleDelayedTask(this.rsNPC, rsNpcConfig::checkEntity, 40);
            sender.sendMessage(this.rsNPC.getLanguage().translateString("tips.npcCreateSuccess", name));
        } else {
            sender.sendMessage(this.rsNPC.getLanguage().translateString("tips.nameRequired"));
        }
        return true;
    }

    @Override
    public CommandParameter[] getParameters() {
        return new CommandParameter[] { CommandParameter.newType("NPC_Name", CommandParamType.RAW_TEXT) };
    }
}
