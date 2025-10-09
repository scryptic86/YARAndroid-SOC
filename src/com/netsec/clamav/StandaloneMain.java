package com.netsec.clamav;

import java.io.FileInputStream;
import java.io.File;
import java.util.Properties;

public class StandaloneMain {
    public static void main(String[] args) {
        System.out.println("StandaloneMain starting...");
        System.out.println("Args:");
        for (int i = 0; i < args.length; i++) {
            System.out.println("  [" + i + "] " + args[i]);
        }

        String javaHome = System.getenv("JAVA_HOME");
        String androidSdk = System.getenv("ANDROID_SDK_ROOT");
        if (androidSdk == null) androidSdk = System.getenv("ANDROID_HOME");
        System.out.println("JAVA_HOME=" + javaHome);
        System.out.println("ANDROID_SDK_ROOT=" + androidSdk);

        // If local.properties exists, print sdk.dir
        try {
            File repoLocal = new File("local.properties");
            if (repoLocal.exists()) {
                Properties p = new Properties();
                p.load(new FileInputStream(repoLocal));
                System.out.println("local.properties sdk.dir=" + p.getProperty("sdk.dir"));
            } else {
                System.out.println("local.properties not found in repo root");
            }
        } catch (Exception e) {
            System.out.println("Error reading local.properties: " + e.getMessage());
        }

        System.out.println("Done.");
    }
}
