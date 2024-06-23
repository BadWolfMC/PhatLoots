package com.codisimus.plugins.phatloots.regions;

import java.util.List;
import org.bukkit.Location;

/**
 *
 * @author Codisimus
 */
public interface RegionHook {

    String getPluginName();
    List<String> getRegionNames(Location loc);
}
