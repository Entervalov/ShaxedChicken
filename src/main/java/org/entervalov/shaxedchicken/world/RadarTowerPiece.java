package org.entervalov.shaxedchicken.world;

import net.minecraft.block.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ChestTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.structure.StructureManager;
import net.minecraft.world.gen.feature.structure.StructurePiece;
import org.entervalov.shaxedchicken.ModBlocks;
import org.entervalov.shaxedchicken.ModEntityTypes;
import org.entervalov.shaxedchicken.ModItems;
import org.entervalov.shaxedchicken.entity.AdvancedDroneEntity;
import org.entervalov.shaxedchicken.entity.DroneType;

import javax.annotation.Nonnull;
import java.util.Random;

/**
 * Радарная вышка — наземная структура.
 * Основание 9x9 с укреплениями, столбы из iron_bars, платформа 7x7 наверху,
 * JammerBlock на вершине, антенные мачты, сундук с лутом, 1-2 дрона.
 */
public class RadarTowerPiece extends StructurePiece {

    private final BlockPos origin;

    private static final int TOWER_HEIGHT = 18;
    private static final int BASE_HALF = 4; // 9x9 base

    public RadarTowerPiece(BlockPos origin) {
        super(ModStructurePieceTypes.RADAR_TOWER_PIECE, 0);
        this.origin = origin;
        // Use a very generous bounding box spanning from bedrock to sky limit
        // so that postProcess is always called regardless of actual surface Y
        this.boundingBox = new MutableBoundingBox(
                origin.getX() - BASE_HALF - 1, 0, origin.getZ() - BASE_HALF - 1,
                origin.getX() + BASE_HALF + 1, 255, origin.getZ() + BASE_HALF + 1
        );
    }

    public RadarTowerPiece(net.minecraft.world.gen.feature.template.TemplateManager manager, CompoundNBT nbt) {
        super(ModStructurePieceTypes.RADAR_TOWER_PIECE, nbt);
        this.origin = new BlockPos(nbt.getInt("OX"), nbt.getInt("OY"), nbt.getInt("OZ"));
    }

    @Override
    protected void addAdditionalSaveData(@Nonnull CompoundNBT nbt) {
        nbt.putInt("OX", origin.getX());
        nbt.putInt("OY", origin.getY());
        nbt.putInt("OZ", origin.getZ());
    }

    @Override
    public boolean postProcess(@Nonnull ISeedReader world, @Nonnull StructureManager structureManager,
                               @Nonnull ChunkGenerator generator, @Nonnull Random random,
                               @Nonnull MutableBoundingBox box, @Nonnull ChunkPos chunkPos,
                               @Nonnull BlockPos blockPos) {

        int groundY = world.getHeight(Heightmap.Type.WORLD_SURFACE_WG, origin.getX(), origin.getZ());
        if (groundY <= 4 || groundY > 200) return false;

        int baseX = origin.getX();
        int baseZ = origin.getZ();
        int baseY = groundY;

        // === ОСНОВАНИЕ 9x9 — бетонная площадка ===
        for (int dx = -BASE_HALF; dx <= BASE_HALF; dx++) {
            for (int dz = -BASE_HALF; dz <= BASE_HALF; dz++) {
                BlockPos pos = new BlockPos(baseX + dx, baseY, baseZ + dz);
                boolean isEdge = Math.abs(dx) == BASE_HALF || Math.abs(dz) == BASE_HALF;
                boolean isCorner = Math.abs(dx) == BASE_HALF && Math.abs(dz) == BASE_HALF;

                if (isCorner) {
                    world.setBlock(pos, Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 2);
                } else if (isEdge) {
                    world.setBlock(pos, Blocks.STONE_BRICKS.defaultBlockState(), 2);
                } else {
                    world.setBlock(pos, Blocks.IRON_BLOCK.defaultBlockState(), 2);
                }

                // Фундамент — 4 блока вниз
                for (int dy = 1; dy <= 4; dy++) {
                    BlockPos below = pos.below(dy);
                    if (world.getBlockState(below).isAir() || world.getBlockState(below).getMaterial().isReplaceable()) {
                        world.setBlock(below, Blocks.STONE_BRICKS.defaultBlockState(), 2);
                    }
                }
            }
        }

        // === БОРДЮР вокруг основания (stone brick wall) ===
        for (int dx = -BASE_HALF; dx <= BASE_HALF; dx++) {
            for (int dz = -BASE_HALF; dz <= BASE_HALF; dz++) {
                boolean isEdge = Math.abs(dx) == BASE_HALF || Math.abs(dz) == BASE_HALF;
                if (isEdge) {
                    BlockPos wallPos = new BlockPos(baseX + dx, baseY + 1, baseZ + dz);
                    boolean isCorner = Math.abs(dx) == BASE_HALF && Math.abs(dz) == BASE_HALF;
                    if (isCorner) {
                        world.setBlock(wallPos, Blocks.STONE_BRICK_WALL.defaultBlockState(), 2);
                    }
                }
            }
        }

        // === 4 СТОЙКИ по углам (iron_bars) — основные опоры ===
        int[][] corners = {{-3, -3}, {-3, 3}, {3, -3}, {3, 3}};
        for (int[] corner : corners) {
            for (int dy = 1; dy <= TOWER_HEIGHT; dy++) {
                BlockPos pos = new BlockPos(baseX + corner[0], baseY + dy, baseZ + corner[1]);
                world.setBlock(pos, Blocks.IRON_BARS.defaultBlockState(), 2);
            }
        }

        // === ЦЕНТРАЛЬНЫЙ СТОЛБ — усиленная конструкция ===
        for (int dy = 1; dy <= TOWER_HEIGHT; dy++) {
            BlockPos pos = new BlockPos(baseX, baseY + dy, baseZ);
            world.setBlock(pos, Blocks.IRON_BARS.defaultBlockState(), 2);
        }

        // === ГОРИЗОНТАЛЬНЫЕ ПЕРЕМЫЧКИ на высоте 5 и 10 ===
        int[] bracingHeights = {6, 12, 16};
        for (int h : bracingHeights) {
            for (int[] corner : corners) {
                // Перемычка по X
                int stepX = corner[0] > 0 ? -1 : 1;
                for (int dx = corner[0]; dx != 0; dx += stepX) {
                    BlockPos brace = new BlockPos(baseX + dx, baseY + h, baseZ + corner[1]);
                    if (world.getBlockState(brace).isAir()) {
                        world.setBlock(brace, Blocks.IRON_BARS.defaultBlockState(), 2);
                    }
                }
                // Перемычка по Z
                int stepZ = corner[1] > 0 ? -1 : 1;
                for (int dz = corner[1]; dz != 0; dz += stepZ) {
                    BlockPos brace = new BlockPos(baseX + corner[0], baseY + h, baseZ + dz);
                    if (world.getBlockState(brace).isAir()) {
                        world.setBlock(brace, Blocks.IRON_BARS.defaultBlockState(), 2);
                    }
                }
            }
        }

        // === PLATFORM 7x7 TOP ===
        // === MID PLATFORM 3x3 ===
        int midY = baseY + 9;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = new BlockPos(baseX + dx, midY, baseZ + dz);
                world.setBlock(pos, Blocks.IRON_BLOCK.defaultBlockState(), 2);
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (Math.abs(dx) == 1 || Math.abs(dz) == 1) {
                    BlockPos rail = new BlockPos(baseX + dx, midY + 1, baseZ + dz);
                    world.setBlock(rail, Blocks.IRON_BARS.defaultBlockState(), 2);
                }
            }
        }

        int platformY = baseY + TOWER_HEIGHT + 1;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                BlockPos pos = new BlockPos(baseX + dx, platformY, baseZ + dz);
                boolean isEdge = Math.abs(dx) == 3 || Math.abs(dz) == 3;
                if (isEdge) {
                    world.setBlock(pos, Blocks.IRON_BLOCK.defaultBlockState(), 2);
                } else {
                    world.setBlock(pos, Blocks.SMOOTH_STONE.defaultBlockState(), 2);
                    // Пол под плитами
                    world.setBlock(pos.below(), Blocks.IRON_BLOCK.defaultBlockState(), 2);
                }
            }
        }

        // === ПЕРИЛА на платформе ===
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                boolean isPerimeter = Math.abs(dx) == 3 || Math.abs(dz) == 3;
                if (isPerimeter) {
                    BlockPos pos = new BlockPos(baseX + dx, platformY + 1, baseZ + dz);
                    world.setBlock(pos, Blocks.IRON_BARS.defaultBlockState(), 2);
                }
            }
        }

        // === АНТЕННЫЕ МАЧТЫ по углам платформы ===
        int[][] antennaCorners = {{-3, -3}, {-3, 3}, {3, -3}, {3, 3}};
        for (int[] ac : antennaCorners) {
            for (int dy = 2; dy <= 4; dy++) {
                BlockPos antennaPos = new BlockPos(baseX + ac[0], platformY + dy, baseZ + ac[1]);
                world.setBlock(antennaPos, Blocks.END_ROD.defaultBlockState(), 2);
            }
        }

        // === ДЖАММЕР (РЭБ) на центре платформы ===
        BlockPos jammerPos = new BlockPos(baseX, platformY + 1, baseZ);
        try {
            BlockState jammerState = ModBlocks.JAMMER.get().defaultBlockState();
            if (jammerState.hasProperty(net.minecraft.state.properties.BlockStateProperties.POWERED)) {
                jammerState = jammerState.setValue(net.minecraft.state.properties.BlockStateProperties.POWERED, true);
            }
            world.setBlock(jammerPos, jammerState, 2);
        } catch (Exception e) {
            world.setBlock(jammerPos, Blocks.BEACON.defaultBlockState(), 2);
        }

        // === КАБИНА ОПЕРАТОРА вокруг РЭБ ===
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (Math.abs(dx) == 1 || Math.abs(dz) == 1) {
                    BlockPos glass = new BlockPos(baseX + dx, platformY + 1, baseZ + dz);
                    world.setBlock(glass, Blocks.GLASS_PANE.defaultBlockState(), 2);
                }
                BlockPos roof = new BlockPos(baseX + dx, platformY + 2, baseZ + dz);
                world.setBlock(roof, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 2);
            }
        }
        world.setBlock(new BlockPos(baseX, platformY + 3, baseZ), Blocks.IRON_BARS.defaultBlockState(), 2);
        world.setBlock(new BlockPos(baseX, platformY + 4, baseZ), Blocks.LANTERN.defaultBlockState(), 2);

        // === РЕДСТОУН ЛАМПЫ на платформе (индикация работы) ===
        BlockPos lamp1 = new BlockPos(baseX + 2, platformY + 1, baseZ + 2);
        BlockPos lamp2 = new BlockPos(baseX - 2, platformY + 1, baseZ - 2);
        world.setBlock(lamp1, Blocks.REDSTONE_LAMP.defaultBlockState(), 2);
        world.setBlock(lamp2, Blocks.REDSTONE_LAMP.defaultBlockState(), 2);

        // === СУНДУК С ЛУТОМ у основания ===
        BlockPos chestPos = new BlockPos(baseX + 2, baseY + 1, baseZ);
        world.setBlock(chestPos, Blocks.CHEST.defaultBlockState().setValue(
                ChestBlock.FACING, Direction.SOUTH), 2);
        TileEntity te = world.getBlockEntity(chestPos);
        if (te instanceof ChestTileEntity) {
            fillRadarTowerChest((ChestTileEntity) te, random);
        }

        // === ФАКЕЛЫ/ФОНАРИ ===
        // Фонари на земле по углам
        int[][] torchCorners = {{-BASE_HALF, -BASE_HALF}, {-BASE_HALF, BASE_HALF}, {BASE_HALF, -BASE_HALF}, {BASE_HALF, BASE_HALF}};
        for (int[] tc : torchCorners) {
            BlockPos pos = new BlockPos(baseX + tc[0], baseY + 2, baseZ + tc[1]);
            if (world.getBlockState(pos).isAir()) {
                world.setBlock(pos, Blocks.LANTERN.defaultBlockState(), 2);
            }
        }

        // Redstone-торчи для питания ламп
        BlockPos rtorch1 = new BlockPos(baseX + 2, platformY, baseZ + 2);
        BlockPos rtorch2 = new BlockPos(baseX - 2, platformY, baseZ - 2);
        if (world.getBlockState(rtorch1).isAir()) {
            world.setBlock(rtorch1, Blocks.REDSTONE_TORCH.defaultBlockState(), 2);
        }
        if (world.getBlockState(rtorch2).isAir()) {
            world.setBlock(rtorch2, Blocks.REDSTONE_TORCH.defaultBlockState(), 2);
        }

        // === ЛЕСТНИЦА для подъёма ===
        for (int dy = 1; dy <= TOWER_HEIGHT; dy++) {
            BlockPos ladderPos = new BlockPos(baseX - 3, baseY + dy, baseZ);
            // Проверяем, что стена сзади есть (нужна для лестницы)
            BlockPos backWall = new BlockPos(baseX - 4, baseY + dy, baseZ);
            if (world.getBlockState(backWall).isAir() || world.getBlockState(backWall).getMaterial().isReplaceable()) {
                world.setBlock(backWall, Blocks.STONE_BRICKS.defaultBlockState(), 2);
            }
            world.setBlock(ladderPos, Blocks.LADDER.defaultBlockState().setValue(
                    LadderBlock.FACING, Direction.EAST), 2);
        }

        // === ТАБЛИЧКА "DANGER" у основания ===
        BlockPos signPos = new BlockPos(baseX, baseY + 1, baseZ + BASE_HALF + 1);
        if (world.getBlockState(signPos).isAir()) {
            world.setBlock(signPos, Blocks.OAK_SIGN.defaultBlockState(), 2);
        }

        // === СПАВН 1-2 ДРОНА рядом ===
        int droneCount = 1 + random.nextInt(2);
        for (int i = 0; i < droneCount; i++) {
            try {
                AdvancedDroneEntity drone = new AdvancedDroneEntity(ModEntityTypes.DRONE_CHICKEN.get(), world.getLevel());
                DroneType type = random.nextFloat() < 0.4f ? DroneType.SCOUT : DroneType.BASIC;
                drone.setDroneType(type);
                double dx = baseX + (random.nextDouble() - 0.5) * 10;
                double dz = baseZ + (random.nextDouble() - 0.5) * 10;
                drone.moveTo(dx, platformY + 5 + random.nextInt(5), dz, random.nextFloat() * 360, 0);
                world.addFreshEntity(drone);
            } catch (Exception ignored) {}
        }

        return true;
    }

    private void fillRadarTowerChest(ChestTileEntity chest, Random random) {
        chest.setItem(0, new ItemStack(Items.REDSTONE, 4 + random.nextInt(8)));
        chest.setItem(1, new ItemStack(Items.IRON_INGOT, 3 + random.nextInt(6)));
        chest.setItem(2, new ItemStack(Items.GOLD_INGOT, 1 + random.nextInt(3)));

        if (random.nextFloat() < 0.3f) {
            chest.setItem(3, new ItemStack(Items.ENDER_PEARL, 1 + random.nextInt(2)));
        }

        if (random.nextFloat() < 0.15f) {
            try {
                chest.setItem(4, new ItemStack(ModItems.hacking_console.get()));
            } catch (Exception ignored) {}
        }

        if (random.nextFloat() < 0.5f) {
            chest.setItem(5, new ItemStack(Items.DIAMOND, 1));
        }

        chest.setItem(6, new ItemStack(Items.BREAD, 3 + random.nextInt(5)));

        if (random.nextFloat() < 0.25f) {
            chest.setItem(7, new ItemStack(Items.IRON_SWORD));
        }

        // Дополнительный лут
        if (random.nextFloat() < 0.2f) {
            chest.setItem(8, new ItemStack(Items.COMPASS));
        }

        chest.setItem(9, new ItemStack(Items.IRON_NUGGET, 8 + random.nextInt(16)));
    }
}















