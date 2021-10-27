package com.shelk.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class Utils {

	 public static String getStringLocation(Location l) {
		    if (l == null)
		      return null;
		    return String.valueOf(l.getWorld().getName()) + ":" + l.getBlockX() + ":" + l.getBlockY() + ":" + l.getBlockZ();
		  }
		  
		  public static Location getLocationString(String s) {
		    if (s == null || s.trim() == "")
		      return null; 
		    String[] parts = s.split(";");
		    if (parts.length == 4) {
		      World w = Bukkit.getServer().getWorld(parts[0]);
		      int x = Integer.parseInt(parts[1]);
		      int y = Integer.parseInt(parts[2]);
		      int z = Integer.parseInt(parts[3]);
		      
		      return new Location(w, x, y, z);
		    } 
		    return null;
		  }
	
}
