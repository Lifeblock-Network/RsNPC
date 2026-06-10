package com.smallaswater.npc.variable;

import cn.nukkit.Player;
import com.smallaswater.npc.data.RsNpcConfig;

/**
 * @author lt_name
 */
public class DefaultVariable extends BaseVariableV2 {

    @Override
    public void onUpdate(Player player, RsNpcConfig rsNpcConfig) {
        this.addVariable("\\n", "\n"); //replace the literal \n with a line break
        this.addVariable("\\\n", "\\n"); //replace the literal \\n with the literal \n
        if (rsNpcConfig != null) {
            this.addVariable("%npcName%", rsNpcConfig.getName());
        }
        if (player != null && player.isOnline()) {
            this.addVariable("@p", player.getName());
        }
    }

}
