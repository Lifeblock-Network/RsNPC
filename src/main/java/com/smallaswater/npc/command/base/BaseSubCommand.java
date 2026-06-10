package com.smallaswater.npc.command.base;

import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandParameter;
import com.smallaswater.npc.RsNPC;


/**
 * @author SmallasWater
 */
public abstract class BaseSubCommand {

    protected RsNPC rsNPC = RsNPC.getInstance();

    private final String name;

    protected BaseSubCommand(String name) {
        this.name = name.toLowerCase();
    }

    /**
     * @param sender CommandSender
     * @return boolean
     */
    public abstract boolean canUser(CommandSender sender);

    /**
     * Get the name
     * @return string
     */
    public String getName(){
        return name;
    }

    /**
     * Get the aliases
     * @return string[]
     */
    public abstract String[] getAliases();

    /**
     * Command response
     * @param sender the sender      - CommandSender
     * @param args   The arrugements      - String[]
     * @param label  label..
     * @return true if true
     */
    public abstract boolean execute(CommandSender sender, String label, String[] args);

    /**
     * Command parameters.
     * @return  the hint parameters
     * */
    public abstract CommandParameter[] getParameters();

}
