package com.skeletonarmy.marrow;

import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class GetConfigValue {
    public static String ConfigFile = Environment.getExternalStorageDirectory() + "/config/Marrow.conf";
    public static String getValue(String key) throws IOException {
        File configFile = new File(ConfigFile);
        Scanner scanner = new Scanner(configFile);
        while (scanner.hasNext()) {
            String line = scanner.nextLine();
           if (line.startsWith("#")) {
               continue;
           }
           if (line.startsWith(key + " =")) {
               scanner.close();
               line = line.substring(line.indexOf("=") + 2);
               return line.replaceAll("\"", "").trim();
           }
        }
        scanner.close();
        return null;
    }
}