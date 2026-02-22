package org.entervalov.shaxedchicken.world;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.gen.feature.structure.IStructurePieceType;
import org.entervalov.shaxedchicken.Main;

public class ModStructurePieceTypes {

    public static IStructurePieceType RADAR_TOWER_PIECE;
    public static IStructurePieceType ABANDONED_LAB_PIECE;

    public static void register() {
        RADAR_TOWER_PIECE = Registry.register(
                Registry.STRUCTURE_PIECE,
                new ResourceLocation(Main.MOD_ID, "radar_tower_piece"),
                RadarTowerPiece::new
        );
        ABANDONED_LAB_PIECE = Registry.register(
                Registry.STRUCTURE_PIECE,
                new ResourceLocation(Main.MOD_ID, "abandoned_lab_piece"),
                AbandonedLabPiece::new
        );
    }
}
