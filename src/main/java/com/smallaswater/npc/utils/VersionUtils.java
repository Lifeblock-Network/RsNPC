package com.smallaswater.npc.utils;

import org.powernukkitx.plugin.Plugin;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.LinkedList;

/**
 * @author LT_Name
 */
@Slf4j
@SuppressWarnings("unused")
public class VersionUtils {

    /**
     * Check whether the plugin meets the minimum version
     *
     * @param plugin       the plugin
     * @param leastVersion the minimum version
     * @return whether it is greater than or equal to the minimum version
     */
    public static boolean checkMinimumVersion(@NotNull Plugin plugin, @NotNull String leastVersion) {
        return compareVersion(plugin.getDescription().getVersion(), leastVersion) != -1;
    }

    public static int compareVersion(@NotNull String v1, @NotNull String v2) {
        if (v1.equalsIgnoreCase(v2)) {
            return 0;
        }
        try {
            LinkedList<String> version1Array = new LinkedList<>(Arrays.asList(v1.split("[._-]")));
            LinkedList<String> version2Array = new LinkedList<>(Arrays.asList(v2.split("[._-]")));

            LinkedList<String> version1Suffix = new LinkedList<>();
            LinkedList<String> version2Suffix = new LinkedList<>();

            int v1index = version1Array.size() - 1;
            while (v1index != 0) {
                try {
                    Long.parseLong(version1Array.getLast());
                } catch (Exception e) {
                    version1Suffix.addLast(version1Array.pollLast());
                }
                v1index--;
            }
            int v2index = version2Array.size() - 1;
            while (v2index != 0) {
                try {
                    Long.parseLong(version2Array.getLast());
                } catch (Exception e) {
                    version2Suffix.addLast(version2Array.pollLast());
                }
                v2index--;
            }

            int index = 0;
            int minLen = Math.min(version1Array.size(), version2Array.size());
            long diff = 0;

            while (index < minLen
                    && (diff = Long.parseLong(version1Array.get(index))
                    - Long.parseLong(version2Array.get(index))) == 0) {
                index++;
            }
            if (diff == 0) {
                if (!version1Suffix.equals(version2Suffix)) {
                    return -1;
                }

                for (int i = index; i < version1Array.size(); i++) {
                    if (Long.parseLong(version1Array.get(i)) > 0) {
                        return 1;
                    }
                }

                for (int i = index; i < version2Array.size(); i++) {
                    if (Long.parseLong(version2Array.get(i)) > 0) {
                        return -1;
                    }
                }
                return 0;
            } else {
                return diff > 0 ? 1 : -1;
            }
        } catch (Exception e) {
            log.error("VersionUtils#compareVersion()", e);
        }
        return -1;
    }

}
