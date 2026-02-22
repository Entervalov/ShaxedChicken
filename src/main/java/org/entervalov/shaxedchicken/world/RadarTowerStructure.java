package org.entervalov.shaxedchicken.world;

import net.minecraft.util.SharedSeedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.gen.feature.template.TemplateManager;

import javax.annotation.Nonnull;

public class RadarTowerStructure extends Structure<NoFeatureConfig> {

    public RadarTowerStructure() {
        super(NoFeatureConfig.CODEC);
    }

    @Override
    @Nonnull
    public GenerationStage.Decoration step() {
        return GenerationStage.Decoration.SURFACE_STRUCTURES;
    }

    @Override
    @Nonnull
    public IStartFactory<NoFeatureConfig> getStartFactory() {
        return Start::new;
    }

    @Override
    protected boolean isFeatureChunk(@Nonnull ChunkGenerator generator, @Nonnull BiomeProvider biomeProvider,
                                     long seed, @Nonnull SharedSeedRandom random,
                                     int chunkX, int chunkZ,
                                     @Nonnull Biome biome, @Nonnull ChunkPos chunkPos,
                                     @Nonnull NoFeatureConfig config) {
        long distSq = (long) chunkX * (long) chunkX + (long) chunkZ * (long) chunkZ;
        return distSq > 64L;
    }

    public static class Start extends StructureStart<NoFeatureConfig> {
        public Start(Structure<NoFeatureConfig> structure, int chunkX, int chunkZ,
                     MutableBoundingBox box, int references, long seed) {
            super(structure, chunkX, chunkZ, box, references, seed);
        }

        @Override
        public void generatePieces(@Nonnull DynamicRegistries registries,
                                   @Nonnull ChunkGenerator generator,
                                   @Nonnull TemplateManager templateManager,
                                   int chunkX, int chunkZ,
                                   @Nonnull Biome biome,
                                   @Nonnull NoFeatureConfig config) {
            int x = chunkX * 16 + 8;
            int z = chunkZ * 16 + 8;
            BlockPos pos = new BlockPos(x, 0, z);

            this.pieces.add(new RadarTowerPiece(pos));
            this.calculateBoundingBox();
        }
    }
}
