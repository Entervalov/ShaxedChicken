package org.entervalov.shaxedchicken.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ChestTileEntity;
import net.minecraft.tileentity.MobSpawnerTileEntity;
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
import org.entervalov.shaxedchicken.ModEntityTypes;
import org.entervalov.shaxedchicken.ModItems;
import org.entervalov.shaxedchicken.entity.AdvancedDroneEntity;
import org.entervalov.shaxedchicken.entity.DroneType;

import javax.annotation.Nonnull;
import java.util.Random;

public class AbandonedLabPiece extends StructurePiece {

    private final BlockPos origin;

    private static final int WIDTH = 21;
    private static final int HEIGHT = 9;
    private static final int DEPTH = 21;

    public AbandonedLabPiece(BlockPos origin) {
        super(ModStructurePieceTypes.ABANDONED_LAB_PIECE, 0);
        this.origin = origin;
        this.boundingBox = new MutableBoundingBox(
                origin.getX() - 1, 0, origin.getZ() - 1,
                origin.getX() + WIDTH, 255, origin.getZ() + DEPTH
        );
    }

    public AbandonedLabPiece(net.minecraft.world.gen.feature.template.TemplateManager manager, CompoundNBT nbt) {
        super(ModStructurePieceTypes.ABANDONED_LAB_PIECE, nbt);
        this.origin = new BlockPos(nbt.getInt("OX"), nbt.getInt("OY"), nbt.getInt("OZ"));
    }

    @Override
    protected void addAdditionalSaveData(@Nonnull CompoundNBT nbt) {
        nbt.putInt("OX", origin.getX());
        nbt.putInt("OY", origin.getY());
        nbt.putInt("OZ", origin.getZ());
    }

    @Override
    public boolean postProcess(@Nonnull ISeedReader world,
                               @Nonnull StructureManager structureManager,
                               @Nonnull ChunkGenerator generator,
                               @Nonnull Random random,
                               @Nonnull MutableBoundingBox box,
                               @Nonnull ChunkPos chunkPos,
                               @Nonnull BlockPos blockPos) {

        int ox = origin.getX();
        int oy = origin.getY();
        int oz = origin.getZ();
        int rightShift = 2;

        for (int dx = 0; dx < WIDTH; dx++) {
            for (int dy = 0; dy < HEIGHT; dy++) {
                for (int dz = 0; dz < DEPTH; dz++) {
                    BlockPos pos = new BlockPos(ox + dx, oy + dy, oz + dz);

                    boolean isWallX = dx == 0 || dx == WIDTH - 1;
                    boolean isWallZ = dz == 0 || dz == DEPTH - 1;
                    boolean isFloor = dy == 0;
                    boolean isCeiling = dy == HEIGHT - 1;
                    boolean isDivider = dx == (WIDTH / 2) && dz > 0 && dz < DEPTH - 1;

                    if (isFloor) {
                        if ((dx + dz) % 3 == 0) {
                            world.setBlock(pos, Blocks.POLISHED_ANDESITE.defaultBlockState(), 2);
                        } else {
                            world.setBlock(pos, Blocks.STONE_BRICKS.defaultBlockState(), 2);
                        }
                    } else if (isCeiling) {
                        if ((dx + dz) % 5 == 0) {
                            world.setBlock(pos, Blocks.GLOWSTONE.defaultBlockState(), 2);
                        } else if ((dx + dz) % 3 == 0) {
                            world.setBlock(pos, Blocks.SEA_LANTERN.defaultBlockState(), 2);
                        } else {
                            world.setBlock(pos, Blocks.STONE_BRICKS.defaultBlockState(), 2);
                        }
                    } else if (isWallX || isWallZ) {
                        float chance = random.nextFloat();
                        if (chance < 0.20f) {
                            world.setBlock(pos, Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), 2);
                        } else if (chance < 0.25f) {
                            world.setBlock(pos, Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), 2);
                        } else {
                            world.setBlock(pos, Blocks.STONE_BRICKS.defaultBlockState(), 2);
                        }
                    } else if (isDivider) {
                        boolean doorway1 = (dz >= 5 && dz <= 6) && dy >= 1 && dy <= 3;
                        boolean doorway2 = (dz >= 11 && dz <= 12) && dy >= 1 && dy <= 3;
                        if (doorway1 || doorway2) {
                            world.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                        } else {
                            world.setBlock(pos, Blocks.STONE_BRICKS.defaultBlockState(), 2);
                        }
                    } else {
                        world.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        for (int dz = 3; dz <= 6; dz++) {
            setIfInside(world, box, new BlockPos(ox + 2, oy + 1, oz + dz), Blocks.SMOOTH_STONE_SLAB.defaultBlockState());
        }
        for (int dz = 8; dz <= 11; dz++) {
            setIfInside(world, box, new BlockPos(ox + 3, oy + 1, oz + dz), Blocks.SMOOTH_STONE_SLAB.defaultBlockState());
        }

        setIfInside(world, box, new BlockPos(ox + 2, oy + 1, oz + 3), Blocks.BREWING_STAND.defaultBlockState());
        setIfInside(world, box, new BlockPos(ox + 4, oy + 1, oz + 3), Blocks.CRAFTING_TABLE.defaultBlockState());
        setIfInside(world, box, new BlockPos(ox + 1, oy + 1, oz + 13), Blocks.CAULDRON.defaultBlockState());
        setIfInside(world, box, new BlockPos(ox + 3, oy + 1, oz + 13), Blocks.DAMAGED_ANVIL.defaultBlockState());
        setIfInside(world, box, new BlockPos(ox + 5, oy + 1, oz + 13), Blocks.STONECUTTER.defaultBlockState());

        for (int dz = 11; dz <= 13; dz++) {
            setIfInside(world, box, new BlockPos(ox + 1, oy + 1, oz + dz), Blocks.BOOKSHELF.defaultBlockState());
            setIfInside(world, box, new BlockPos(ox + 1, oy + 2, oz + dz), Blocks.BOOKSHELF.defaultBlockState());
        }

        int[][] cobwebPositions = {
                {1, 6, 1}, {6, 6, 1}, {1, 6, 15}, {6, 6, 15},
                {3, 5, 4}, {5, 6, 8}, {2, 4, 11}, {1, 5, 6},
                {4, 5, 13}, {6, 4, 9}
        };
        for (int[] cw : cobwebPositions) {
            BlockPos cwPos = new BlockPos(ox + cw[0], oy + cw[1], oz + cw[2]);
            if (world.getBlockState(cwPos).isAir()) {
                world.setBlock(cwPos, Blocks.COBWEB.defaultBlockState(), 2);
            }
        }

        setIfInside(world, box, new BlockPos(ox + 3, oy + 1, oz + 5), Blocks.GLASS_PANE.defaultBlockState());
        setIfInside(world, box, new BlockPos(ox + 4, oy + 1, oz + 5), Blocks.GLASS_PANE.defaultBlockState());

        BlockPos chest1Pos = new BlockPos(ox + 11 + rightShift, oy + 1, oz + 3);
        world.setBlock(chest1Pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH), 2);
        TileEntity te1 = world.getBlockEntity(chest1Pos);
        if (te1 instanceof ChestTileEntity) {
            fillMainChest((ChestTileEntity) te1, random);
        }

        BlockPos chest2Pos = new BlockPos(ox + 14 + rightShift, oy + 1, oz + 13);
        world.setBlock(chest2Pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST), 2);
        TileEntity te2 = world.getBlockEntity(chest2Pos);
        if (te2 instanceof ChestTileEntity) {
            fillSecondaryChest((ChestTileEntity) te2, random);
        }

        for (int dz = 4; dz <= 16; dz++) {
            BlockPos barrelPos = new BlockPos(ox + 15 + rightShift, oy + 1, oz + dz);
            setIfInside(world, box, barrelPos, Blocks.BARREL.defaultBlockState());
            TileEntity te = world.getBlockEntity(barrelPos);
            if (te instanceof IInventory && random.nextFloat() < 0.35f) {
                fillBarrelLoot((IInventory) te, random);
            }
        }
        for (int dz = 6; dz <= 15; dz++) {
            BlockPos barrelPos = new BlockPos(ox + 15 + rightShift, oy + 2, oz + dz);
            setIfInside(world, box, barrelPos, Blocks.BARREL.defaultBlockState());
            TileEntity te = world.getBlockEntity(barrelPos);
            if (te instanceof IInventory && random.nextFloat() < 0.20f) {
                fillBarrelLoot((IInventory) te, random);
            }
        }

        setIfInside(world, box, new BlockPos(ox + 12 + rightShift, oy + 1, oz + 7), Blocks.BLAST_FURNACE.defaultBlockState());
        setIfInside(world, box, new BlockPos(ox + 11 + rightShift, oy + 1, oz + 10), Blocks.SMITHING_TABLE.defaultBlockState());
        setIfInside(world, box, new BlockPos(ox + 13 + rightShift, oy + 1, oz + 10), Blocks.FLETCHING_TABLE.defaultBlockState());

        setIfInside(world, box, new BlockPos(ox + 12 + rightShift, oy + 1, oz + 13), Blocks.IRON_BLOCK.defaultBlockState());
        setIfInside(world, box, new BlockPos(ox + 12 + rightShift, oy + 2, oz + 13), Blocks.IRON_BARS.defaultBlockState());
        setIfInside(world, box, new BlockPos(ox + 14 + rightShift, oy + 1, oz + 13), Blocks.IRON_BLOCK.defaultBlockState());
        setIfInside(world, box, new BlockPos(ox + 14 + rightShift, oy + 2, oz + 13), Blocks.IRON_BARS.defaultBlockState());

        BlockPos spawnerPos = new BlockPos(ox + 4, oy + 1, oz + 8);
        world.setBlock(spawnerPos, Blocks.SPAWNER.defaultBlockState(), 2);
        TileEntity teSpawner = world.getBlockEntity(spawnerPos);
        if (teSpawner instanceof MobSpawnerTileEntity) {
            EntityType<?> type = ModEntityTypes.DRONE_CHICKEN.get();
            if (type != null && type.getRegistryName() != null) {
                ((MobSpawnerTileEntity) teSpawner).getSpawner().setEntityId(type);
            } else {
                ((MobSpawnerTileEntity) teSpawner).getSpawner().setEntityId(EntityType.ZOMBIE);
            }
        }

        BlockPos[] lanternPositions = {
                new BlockPos(ox + 1, oy + 4, oz + 1),
                new BlockPos(ox + 1, oy + 4, oz + 15),
                new BlockPos(ox + 7, oy + 4, oz + 1),
                new BlockPos(ox + 7, oy + 4, oz + 15),
                new BlockPos(ox + 9 + rightShift, oy + 4, oz + 1),
                new BlockPos(ox + 9 + rightShift, oy + 4, oz + 15),
                new BlockPos(ox + 15 + rightShift, oy + 4, oz + 1),
                new BlockPos(ox + 15 + rightShift, oy + 4, oz + 15)
        };
        for (BlockPos lp : lanternPositions) {
            if (world.getBlockState(lp).isAir()) {
                world.setBlock(lp, Blocks.SOUL_LANTERN.defaultBlockState(), 2);
            }
        }

        setIfInside(world, box, new BlockPos(ox + 4, oy + 3, oz + 1), Blocks.REDSTONE_TORCH.defaultBlockState());
        setIfInside(world, box, new BlockPos(ox + 12 + rightShift, oy + 3, oz + 1), Blocks.REDSTONE_TORCH.defaultBlockState());

        int shaftX = ox + 1;
        int shaftZ = oz + 8;
        int topY = world.getHeight(Heightmap.Type.WORLD_SURFACE_WG, shaftX, shaftZ);

        for (int y = oy + HEIGHT; y <= topY; y++) {
            BlockPos shaftAir = new BlockPos(shaftX, y, shaftZ);
            world.setBlock(shaftAir, Blocks.AIR.defaultBlockState(), 2);

            world.setBlock(shaftAir, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH), 2);

            BlockPos backing = new BlockPos(shaftX, y, shaftZ + 1);
            BlockState backingState = world.getBlockState(backing);
            if (backingState.isAir() || backingState.getMaterial().isReplaceable()) {
                world.setBlock(backing, Blocks.STONE_BRICKS.defaultBlockState(), 2);
            }

            BlockPos sideL = new BlockPos(shaftX - 1, y, shaftZ);
            BlockPos sideR = new BlockPos(shaftX + 1, y, shaftZ);
            if (world.getBlockState(sideL).isAir() || world.getBlockState(sideL).getMaterial().isReplaceable()) {
                world.setBlock(sideL, Blocks.STONE_BRICKS.defaultBlockState(), 2);
            }
            if (world.getBlockState(sideR).isAir() || world.getBlockState(sideR).getMaterial().isReplaceable()) {
                world.setBlock(sideR, Blocks.STONE_BRICKS.defaultBlockState(), 2);
            }
        }

        BlockPos hatch = new BlockPos(shaftX, topY, shaftZ);
        world.setBlock(hatch, Blocks.IRON_TRAPDOOR.defaultBlockState(), 2);

        BlockPos marker1 = new BlockPos(shaftX - 1, topY, shaftZ - 1);
        BlockPos marker2 = new BlockPos(shaftX + 1, topY, shaftZ - 1);
        BlockPos marker3 = new BlockPos(shaftX - 1, topY, shaftZ + 1);
        BlockPos marker4 = new BlockPos(shaftX + 1, topY, shaftZ + 1);
        for (BlockPos m : new BlockPos[]{marker1, marker2, marker3, marker4}) {
            if (world.getBlockState(m).isAir() || world.getBlockState(m).getMaterial().isReplaceable()) {
                world.setBlock(m, Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), 2);
            }
        }

        int droneCount = 2 + random.nextInt(3);
        for (int i = 0; i < droneCount; i++) {
            try {
                AdvancedDroneEntity drone = new AdvancedDroneEntity(ModEntityTypes.DRONE_CHICKEN.get(), world.getLevel());
                DroneType type;
                float roll = random.nextFloat();
                if (roll < 0.08f) {
                    type = DroneType.NUCLEAR;
                } else if (roll < 0.30f) {
                    type = DroneType.HEAVY;
                } else if (roll < 0.50f) {
                    type = DroneType.FIRE;
                } else {
                    type = DroneType.getRandomType(random);
                }

                drone.setDroneType(type);
                drone.moveTo(ox + 4 + random.nextInt(9), oy + 2, oz + 4 + random.nextInt(9), random.nextFloat() * 360, 0);
                world.addFreshEntity(drone);
            } catch (Exception ignored) {
            }
        }

        return true;
    }

    private static void setIfInside(ISeedReader world, MutableBoundingBox box, BlockPos pos, BlockState state) {
        world.setBlock(pos, state, 2);
    }

    private void fillMainChest(ChestTileEntity chest, Random random) {
        putRandomItem(chest, random, new ItemStack(Items.DIAMOND, 1 + random.nextInt(2)));
        putRandomItem(chest, random, new ItemStack(Items.REDSTONE, 6 + random.nextInt(12)));
        putRandomItem(chest, random, new ItemStack(Items.GOLD_INGOT, 2 + random.nextInt(4)));
        putRandomItem(chest, random, new ItemStack(Items.IRON_INGOT, 4 + random.nextInt(7)));
        putRandomItem(chest, random, new ItemStack(Items.EXPERIENCE_BOTTLE, 2 + random.nextInt(5)));

        if (random.nextFloat() < 0.30f && ModItems.hacking_console.get() != null) {
            putRandomItem(chest, random, new ItemStack(ModItems.hacking_console.get()));
        }
        if (random.nextFloat() < 0.25f) {
            putRandomItem(chest, random, new ItemStack(Items.ENDER_PEARL, 1 + random.nextInt(3)));
        }
        if (random.nextFloat() < 0.15f) {
            putRandomItem(chest, random, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));
        }
        if (random.nextFloat() < 0.35f) {
            putRandomItem(chest, random, new ItemStack(Items.TNT, 1 + random.nextInt(3)));
        }
        putRandomItem(chest, random, new ItemStack(Items.GUNPOWDER, 3 + random.nextInt(7)));

        if (random.nextFloat() < 0.35f) {
            putRandomItem(chest, random, new ItemStack(ModItems.jammer_module_basic.get()));
        }
        if (random.nextFloat() < 0.20f) {
            putRandomItem(chest, random, new ItemStack(ModItems.jammer_module_uncommon.get()));
        }
        if (random.nextFloat() < 0.10f) {
            putRandomItem(chest, random, new ItemStack(ModItems.jammer_module_epic.get()));
        }
        if (random.nextFloat() < 0.03f) {
            putRandomItem(chest, random, new ItemStack(ModItems.jammer_module_legendary.get()));
        }
    }

    private void fillSecondaryChest(ChestTileEntity chest, Random random) {
        putRandomItem(chest, random, new ItemStack(Items.IRON_INGOT, 2 + random.nextInt(5)));
        putRandomItem(chest, random, new ItemStack(Items.REDSTONE, 3 + random.nextInt(7)));
        putRandomItem(chest, random, new ItemStack(Items.GUNPOWDER, 2 + random.nextInt(4)));
        putRandomItem(chest, random, new ItemStack(Items.GLASS_PANE, 3 + random.nextInt(6)));
        putRandomItem(chest, random, new ItemStack(Items.BREAD, 4 + random.nextInt(7)));

        if (random.nextFloat() < 0.40f) {
            putRandomItem(chest, random, new ItemStack(Items.GOLDEN_APPLE));
        }
        if (random.nextFloat() < 0.25f) {
            putRandomItem(chest, random, new ItemStack(Items.NAME_TAG));
        }
        if (random.nextFloat() < 0.35f) {
            putRandomItem(chest, random, new ItemStack(Items.ARROW, 6 + random.nextInt(14)));
        }

        if (random.nextFloat() < 0.20f) {
            putRandomItem(chest, random, new ItemStack(ModItems.jammer_module_basic.get()));
        }
        if (random.nextFloat() < 0.10f) {
            putRandomItem(chest, random, new ItemStack(ModItems.jammer_module_uncommon.get()));
        }
    }

    private void fillBarrelLoot(IInventory inv, Random random) {
        putRandomItem(inv, random, new ItemStack(Items.IRON_NUGGET, 6 + random.nextInt(12)));
        if (random.nextFloat() < 0.6f) putRandomItem(inv, random, new ItemStack(Items.REDSTONE, 2 + random.nextInt(5)));
        if (random.nextFloat() < 0.35f) putRandomItem(inv, random, new ItemStack(Items.GUNPOWDER, 1 + random.nextInt(4)));
        if (random.nextFloat() < 0.25f) putRandomItem(inv, random, new ItemStack(Items.IRON_INGOT, 1 + random.nextInt(3)));
        if (random.nextFloat() < 0.2f) putRandomItem(inv, random, new ItemStack(Items.STRING, 2 + random.nextInt(5)));
        if (random.nextFloat() < 0.08f) putRandomItem(inv, random, new ItemStack(ModItems.jammer_module_basic.get()));
        if (random.nextFloat() < 0.03f) putRandomItem(inv, random, new ItemStack(ModItems.jammer_module_uncommon.get()));
    }

    private void putRandomItem(IInventory inv, Random random, ItemStack stack) {
        if (stack.isEmpty()) return;
        int size = inv.getContainerSize();
        for (int i = 0; i < 10; i++) {
            int slot = random.nextInt(size);
            if (inv.getItem(slot).isEmpty()) {
                inv.setItem(slot, stack);
                return;
            }
        }
        inv.setItem(random.nextInt(size), stack);
    }
}


