package com.smallaswater.npc.variable;

import cn.nukkit.Player;
import com.smallaswater.npc.data.RsNpcConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author LT_Name
 */
public abstract class BaseVariableV2 {

    private final ConcurrentHashMap<String, String> variable = new ConcurrentHashMap<>();

    public final void update(Player player, RsNpcConfig rsNpcConfig) {
        this.variable.clear();
        this.onUpdate(player, rsNpcConfig);
    }

    public abstract void onUpdate(Player player, RsNpcConfig rsNpcConfig);

    /**
     * Add a variable
     *
     * @param key the original content
     * @param value the replacement content
     */
    protected final void addVariable(String key, String value){
        this.variable.put(key, value);
    }

    public final String stringReplace(String inString) {
        for (Map.Entry<String, String> entry : this.variable.entrySet()) {
            inString = inString.replace(entry.getKey(), entry.getValue());
        }

        return inString;
    }

}
