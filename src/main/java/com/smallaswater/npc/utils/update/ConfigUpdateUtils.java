package com.smallaswater.npc.utils.update;

import org.powernukkitx.Server;
import org.powernukkitx.plugin.Plugin;
import org.powernukkitx.utils.Config;
import com.smallaswater.npc.RsNPC;
import com.smallaswater.npc.data.RsNpcConfig;
import com.smallaswater.npc.utils.Utils;
import com.smallaswater.npc.utils.VersionUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.File;
import java.net.URLClassLoader;
import java.util.HashMap;

/**
 * @author LT_Name
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConfigUpdateUtils {

    public static void updateConfig() {
        //RsNPC 1.X.X -- RsNpcX 1.X.X does not need updating
        updateRsNpcX1_X_X_To_RsNPC2_0_0();
        updateRsNPC2_0_0_To_RsNPC2_2_3();
    }

    /**
     * Update from RsNPCX 1.X.X to RsNPC 2.0.0
     */
    private static void updateRsNpcX1_X_X_To_RsNPC2_0_0() {
        //disable and delete the RsNPCX plugin
        Plugin rsNPCX = RsNPC.getInstance().getServer().getPluginManager().getPlugin("RsNPCX");
        if (rsNPCX != null) {
            try {
                Class.forName("com.smallaswater.npc.RsNpcX"); //guard against accidentally affecting another plugin with the same name
                File file = Utils.getPluginFile(rsNPCX);
                Server.getInstance().getPluginManager().disablePlugin(rsNPCX);
                ClassLoader classLoader = rsNPCX.getClass().getClassLoader();
                if (classLoader instanceof URLClassLoader) {
                    ((URLClassLoader) classLoader).close();
                }
                if (file != null) {
                    file.delete();
                }
            }catch (Exception ignored) {

            }
        }
        //rename the folder
        File file = new File(RsNPC.getInstance().getServer().getPluginPath() + "/RsNPCX");
        if (file.exists()) {
            if (file.renameTo(RsNPC.getInstance().getDataFolder())) {
                RsNPC.getInstance().getLogger().info("[ConfigUpdateUtils](updateRsNpcX1_X_X_To_RsNPC2_0_0) 配置文件更新成功！");
            }else {
                RsNPC.getInstance().getLogger().error("[ConfigUpdateUtils](updateRsNpcX1_X_X_To_RsNPC2_0_0) RsNPCX文件夹重命名失败，请手动将文件夹重命名为RsNPC");
            }
        }
    }

    /**
     * Update from RsNPC 2.0.0--2.2.2 to RsNPC-2.2.3
     */
    private static void updateRsNPC2_0_0_To_RsNPC2_2_3() {
        updateNpcConfigsInDir(new File(RsNPC.getInstance().getDataFolder() + "/Npcs"));
    }

    /**
     * Recursively update every .yml NPC config under the given directory.
     * Subfolders are traversed; non-yml entries and directories are skipped.
     */
    private static void updateNpcConfigsInDir(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                updateNpcConfigsInDir(file);
                continue;
            }
            if (!file.getName().endsWith(".yml")) {
                continue;
            }
            Config config = new Config(file, Config.YAML);

            if (VersionUtils.compareVersion(config.getString(RsNpcConfig.NPC_CONFIG_VERSION_KEY, "2.0.0"), "2.2.3") >= 0) {
                continue;
            }

            // emoji.interval(seconds) -> emoji.interval
            HashMap<Object, Object> map = config.get("表情动作", new HashMap<>());
            map.put("间隔", map.getOrDefault("间隔(秒)", 10));
            map.remove("间隔(秒)");
            config.set("表情动作", map);

            config.set(RsNpcConfig.NPC_CONFIG_VERSION_KEY, "2.2.3");

            config.save();
        }
    }

}
