package org.entervalov.shaxedchicken.utils;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import org.entervalov.shaxedchicken.Main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class UpdateChecker {

    private static final String FILE_NAME = "shaxed_version.properties";

    private static Boolean hasUpdate = null;

    public static boolean hasNewVersion() {
        if (hasUpdate != null) return hasUpdate;

        File file = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME).toFile();
        Properties props = new Properties();
        String lastVersion = "0.0.0";

        if (file.exists()) {
            try (FileInputStream in = new FileInputStream(file)) {
                props.load(in);
                lastVersion = props.getProperty("last_version", "0.0.0");
            } catch (Exception e) { e.printStackTrace(); }
        }

        String currentVersion = getCurrentVersion();
        hasUpdate = !lastVersion.equals(currentVersion);
        return hasUpdate;
    }

    public static void markAsRead() {
        String currentVersion = getCurrentVersion();

        File file = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME).toFile();
        Properties props = new Properties();
        props.setProperty("last_version", currentVersion);

        try (FileOutputStream out = new FileOutputStream(file)) {
            props.store(out, "ShaxedChicken version tracker");
        } catch (Exception e) {
            e.printStackTrace();
        }

        hasUpdate = false;
    }

    public static String getCurrentVersion() {
        return ModList.get().getModContainerById(Main.MOD_ID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("0.0.0");
    }
}
