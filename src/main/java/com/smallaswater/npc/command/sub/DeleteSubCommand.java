package com.smallaswater.npc.command.sub;

import cn.nukkit.command.CommandSender;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParamType;
import cn.nukkit.command.data.CommandParameter;
import com.smallaswater.npc.command.base.BaseSubCommand;
import com.smallaswater.npc.data.RsNpcConfig;

import java.io.File;

/**
 * @author LT_Name
 */
public class DeleteSubCommand extends BaseSubCommand {

    public DeleteSubCommand(String name) {
        super(name);
    }

    @Override
    public boolean canUser(CommandSender sender) {
        return sender.hasPermission("RsNPC.admin.delete");
    }

    @Override
    public String[] getAliases() {
        return new String[0];
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length > 1) {
            String name = args[1];
            if (!this.rsNPC.getNpcs().containsKey(name)) {
                sender.sendMessage("§c§lNPC " + name + "不存在...");
                return true;
            }
            RsNpcConfig rsNpcConfig = this.rsNPC.getNpcs().get(name);
            if (rsNpcConfig.getEntityRsNpc() != null) {
                rsNpcConfig.getEntityRsNpc().close();
            }
            this.rsNPC.getNpcs().remove(name);
            File npcFile = rsNpcConfig.getSourceFile();
            if (npcFile == null) {
                npcFile = new File(this.rsNPC.getDataFolder() + "/Npcs/" + name + ".yml");
            }
            if (!npcFile.delete()) {
                sender.sendMessage(this.rsNPC.getLanguage().translateString("tips.command.npcRemoveFileFailed", name));
            }else {
                sender.sendMessage(this.rsNPC.getLanguage().translateString("tips.command.npcRemoveSuccess", name));
            }
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
