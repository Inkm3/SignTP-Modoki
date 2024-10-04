package com.github.inkm3.signtp.Util;

import org.bukkit.util.Vector;

import java.util.Arrays;

public class Util {

    public static String vecToStr(Vector vector) {
        return vector.getBlockX()+","+vector.getBlockY()+","+vector.getBlockZ();
    }

    public static Vector strToVec(String string) {
        try {
            int[] intVec = Arrays.stream(string.split(",", 3)).mapToInt(Integer::parseInt).toArray();
            return new Vector(intVec[0], intVec[1], intVec[2]);
        } catch (NumberFormatException ignore) {
            return null;
        }
    }
}
