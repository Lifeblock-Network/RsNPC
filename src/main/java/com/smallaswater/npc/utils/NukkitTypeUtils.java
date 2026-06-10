package com.smallaswater.npc.utils;

import cn.nukkit.Server;
import lombok.Getter;

/**
 * @author LT_Name
 */
public class NukkitTypeUtils {

    @Getter
    private static final NukkitType nukkitType;

    static {
        NukkitType nukkitTypeCache;
        switch (Server.getInstance().getCodename().toLowerCase()) {
            case "powernukkitx":
                //PNX2 did not change the version info, so use the presence of a class to tell them apart
                //the PowerNukkitXOnly annotation was removed in PNX2
                try {
                    Class.forName("cn.nukkit.api.PowerNukkitXOnly");
                    nukkitTypeCache = NukkitType.POWER_NUKKIT_X;
                } catch (ClassNotFoundException ignored) {
                    nukkitTypeCache = NukkitType.POWER_NUKKIT_X_2;
                }
                break;
            case "powernukkit":
                nukkitTypeCache = NukkitType.POWER_NUKKIT;
                break;
            case "pm1e":
                nukkitTypeCache = NukkitType.PM1E;
                break;
            case "mot":
                nukkitTypeCache = NukkitType.MOT;
                break;
            default:
                nukkitTypeCache = NukkitType.NUKKITX;
                break;
        }
        nukkitType = nukkitTypeCache;
    }

    public enum NukkitType {

        NUKKITX("NukkitX"),

        POWER_NUKKIT("PowerNukkit"),

        POWER_NUKKIT_X("PowerNukkitX"),

        POWER_NUKKIT_X_2("PowerNukkitX2"),

        PM1E("Nukkit PetteriM1 Edition"),

        MOT("Nukkit MOT");

        @Getter
        private final String showName;

        NukkitType(String showName) {
            this.showName = showName;
        }

    }

}
