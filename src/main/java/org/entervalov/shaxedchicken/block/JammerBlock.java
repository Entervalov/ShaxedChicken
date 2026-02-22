package org.entervalov.shaxedchicken.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkHooks;
import org.entervalov.shaxedchicken.ModTileEntities;
import org.entervalov.shaxedchicken.item.JammerModuleItem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

public class JammerBlock extends Block {

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    // Collision shape for the 3D model
    private static final VoxelShape SHAPE = VoxelShapes.or(
            Block.box(0, 0, 0, 16, 2, 16),
            Block.box(1, 0, 1, 4, 6, 4), Block.box(12, 0, 1, 15, 6, 4),
            Block.box(1, 0, 12, 4, 6, 15), Block.box(12, 0, 12, 15, 6, 15),
            Block.box(2, 2, 2, 14, 10, 14), Block.box(5, 10, 5, 11, 18, 11),
            Block.box(2, 18, 6, 14, 22, 10), Block.box(6, 18, 2, 10, 22, 14),
            Block.box(1, 20, 1, 3, 28, 3), Block.box(13, 20, 1, 15, 28, 3),
            Block.box(1, 20, 13, 3, 28, 15), Block.box(13, 20, 13, 15, 28, 15),
            Block.box(7, 22, 7, 9, 32, 9)
    );

    public JammerBlock() {
        super(Properties.of(Material.METAL)
                .strength(4.0F, 10.0F)
                .sound(SoundType.NETHERITE_BLOCK)
                .noOcclusion()
                .lightLevel(state -> state.getValue(POWERED) ? 10 : 0));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(POWERED, false)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(POWERED, FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    @Nonnull
    public VoxelShape getShape(@Nonnull BlockState state, @Nonnull IBlockReader world,
                               @Nonnull BlockPos pos, @Nonnull ISelectionContext context) {
        return SHAPE;
    }

    @Override
    @Nonnull
    public BlockRenderType getRenderShape(@Nonnull BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return ModTileEntities.JAMMER.get().create();
    }

    // === INTERACTION LOGIC (RMB) ===

    @Override
    @Nonnull
    public ActionResultType use(@Nonnull BlockState state, @Nonnull World world, @Nonnull BlockPos pos,
                                @Nonnull PlayerEntity player, @Nonnull Hand hand,
                                @Nonnull BlockRayTraceResult hit) {
        if (!world.isClientSide) {
            TileEntity te = world.getBlockEntity(pos);

            if (te instanceof JammerTileEntity) {
                JammerTileEntity jammer = (JammerTileEntity) te;
                ItemStack held = player.getItemInHand(hand);

                // 1. Module in hand -> try to insert
                if (held.getItem() instanceof JammerModuleItem) {
                    for (int i = 0; i < jammer.getContainerSize(); i++) {
                        if (jammer.getItem(i).isEmpty() && jammer.canPlaceItem(i, held)) {
                            ItemStack toInsert = held.split(1);
                            jammer.setItem(i, toInsert);
                            world.playSound(null, pos, SoundEvents.ANVIL_USE, SoundCategory.BLOCKS, 0.4F, 1.5F);
                            player.displayClientMessage(
                                    new StringTextComponent("\u00a7a\u041c\u043e\u0434\u0443\u043b\u044c \u0443\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d!"), true);
                            return ActionResultType.CONSUME;
                        }
                    }
                    player.displayClientMessage(
                            new StringTextComponent("\u00a7c\u041d\u0435\u0442 \u0441\u0432\u043e\u0431\u043e\u0434\u043d\u044b\u0445 \u0441\u043b\u043e\u0442\u043e\u0432 \u0438\u043b\u0438 \u0442\u0430\u043a\u043e\u0439 \u043c\u043e\u0434\u0443\u043b\u044c \u0443\u0436\u0435 \u0435\u0441\u0442\u044c!"), true);
                    return ActionResultType.CONSUME;
                }

                // 2. SHIFT + RMB (empty hand) -> open module GUI
                if (player.isShiftKeyDown() && held.isEmpty()) {
                    if (player instanceof ServerPlayerEntity) {
                        NetworkHooks.openGui((ServerPlayerEntity) player, jammer, pos);
                    }
                    return ActionResultType.CONSUME;
                }

                // 3. SHIFT + RMB (item in hand) -> cycle mode
                if (player.isShiftKeyDown()) {
                    if (state.getValue(POWERED)) {
                        jammer.cycleMode();
                        JammerTileEntity.JammerMode mode = jammer.getMode();
                        String modeName;
                        switch (mode) {
                            case ALL:
                                modeName = "\u00a7b[\u0412\u0421\u0415 \u0421\u0418\u0421\u0422\u0415\u041c\u042b]";
                                break;
                            case JAMMER:
                                modeName = "\u00a7a[\u0422\u041e\u041b\u042c\u041a\u041e \u0413\u041b\u0423\u0428\u0418\u041b\u041a\u0410]";
                                break;
                            case LASER:
                                modeName = "\u00a7c[\u0422\u041e\u041b\u042c\u041a\u041e \u041b\u0410\u0417\u0415\u0420]";
                                break;
                            default:
                                modeName = "\u00a77[\u041d\u0415\u0418\u0417\u0412\u0415\u0421\u0422\u041d\u041e]";
                                break;
                        }
                        player.displayClientMessage(
                                new StringTextComponent("\u00a7e\u0420\u0435\u0436\u0438\u043c \u043f\u0435\u0440\u0435\u043a\u043b\u044e\u0447\u0435\u043d: " + modeName), true);
                        world.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundCategory.BLOCKS, 0.3F, 2.0F);
                    } else {
                        player.displayClientMessage(
                                new StringTextComponent("\u00a7c\u0421\u043d\u0430\u0447\u0430\u043b\u0430 \u0432\u043a\u043b\u044e\u0447\u0438\u0442\u0435 \u043f\u0438\u0442\u0430\u043d\u0438\u0435!"), true);
                    }
                    return ActionResultType.CONSUME;
                }

                // 4. Normal RMB -> toggle power
                boolean newPowered = !state.getValue(POWERED);
                world.setBlock(pos, state.setValue(POWERED, newPowered), 3);

                float pitch = newPowered ? 1.2F : 0.6F;
                world.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.0F, pitch);

                jammer.setActive(newPowered);

                player.displayClientMessage(
                        new StringTextComponent(newPowered
                                ? "\u00a7a\u00a7l\u041f\u0418\u0422\u0410\u041d\u0418\u0415 \u0412\u041a\u041b\u042e\u0427\u0415\u041d\u041e"
                                : "\u00a7c\u00a7l\u041f\u0418\u0422\u0410\u041d\u0418\u0415 \u041e\u0422\u041a\u041b\u042e\u0427\u0415\u041d\u041e"),
                        true);
            }
        }
        return ActionResultType.sidedSuccess(world.isClientSide);
    }

    // Redstone controls power
    @Override
    public void neighborChanged(@Nonnull BlockState state, @Nonnull World world, @Nonnull BlockPos pos,
                                @Nonnull Block block, @Nonnull BlockPos fromPos, boolean isMoving) {
        if (!world.isClientSide) {
            boolean powered = world.hasNeighborSignal(pos);
            if (powered != state.getValue(POWERED)) {
                world.setBlock(pos, state.setValue(POWERED, powered), 3);
                TileEntity te = world.getBlockEntity(pos);
                if (te instanceof JammerTileEntity) {
                    ((JammerTileEntity) te).setActive(powered);
                }
            }
        }
    }

    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull World world, @Nonnull BlockPos pos,
                         @Nonnull BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            TileEntity te = world.getBlockEntity(pos);
            if (te instanceof JammerTileEntity) {
                InventoryHelper.dropContents(world, pos, (JammerTileEntity) te);
                world.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void animateTick(@Nonnull BlockState state, @Nonnull World world,
                            @Nonnull BlockPos pos, @Nonnull Random random) {
        if (state.getValue(POWERED)) {
            if (random.nextInt(3) == 0) {
                world.addParticle(ParticleTypes.ENCHANTED_HIT,
                        pos.getX() + 0.5, pos.getY() + 1.8, pos.getZ() + 0.5,
                        (random.nextDouble() - 0.5) * 0.1, 0.1, (random.nextDouble() - 0.5) * 0.1);
            }
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable IBlockReader world,
                                @Nonnull List<ITextComponent> tooltip, @Nonnull ITooltipFlag flag) {
        tooltip.add(new StringTextComponent("\u00a7b[LASER DEFENSE SYSTEM]"));
        tooltip.add(new StringTextComponent("\u00a77\u0412\u043e\u0435\u043d\u043d\u044b\u0439 \u043a\u043e\u043c\u043f\u043b\u0435\u043a\u0441 \u0420\u042d\u0411 \u00ab\u0411\u0430\u0440\u044c\u0435\u0440\u00bb"));
        tooltip.add(new StringTextComponent(""));

        tooltip.add(new StringTextComponent("\u00a79\u0425\u0430\u0440\u0430\u043a\u0442\u0435\u0440\u0438\u0441\u0442\u0438\u043a\u0438:"));
        tooltip.add(new StringTextComponent(" \u00a77- \u0420\u0430\u0434\u0438\u0443\u0441 \u0433\u043b\u0443\u0448\u0438\u043b\u043a\u0438: \u00a7b30 \u0431\u043b\u043e\u043a\u043e\u0432"));
        tooltip.add(new StringTextComponent(" \u00a77- \u0420\u0430\u0434\u0438\u0443\u0441 \u043b\u0430\u0437\u0435\u0440\u0430: \u00a7c20 \u0431\u043b\u043e\u043a\u043e\u0432"));
        tooltip.add(new StringTextComponent(" \u00a77- \u0412\u0440\u0435\u043c\u044f \u043d\u0430\u0432\u0435\u0434\u0435\u043d\u0438\u044f: \u00a763 \u0441\u0435\u043a \u00a77(\u0411\u0430\u0437\u043e\u0432\u043e\u0435)"));
        tooltip.add(new StringTextComponent(" \u00a77- \u0418\u043c\u0435\u0435\u0442 \u00a7c2-\u0435\u00a77 \u043e\u0434\u043d\u043e\u0432\u0440\u0435\u043c\u0435\u043d\u043d\u044b\u0435 \u0446\u0435\u043b\u0438 \u043d\u0430\u0432\u0435\u0434\u0435\u043d\u0438\u044f"));
        tooltip.add(new StringTextComponent(" "));

        tooltip.add(new StringTextComponent("\u00a74\u041e\u0433\u0440\u0430\u043d\u0438\u0447\u0435\u043d\u0438\u044f:"));
        tooltip.add(new StringTextComponent(" \u00a77- \u041d\u0435 \u0430\u0442\u0430\u043a\u0443\u0435\u0442 \u0434\u0440\u043e\u043d\u044b \u043a\u043b\u0430\u0441\u0441\u043e\u0432:"));
        tooltip.add(new StringTextComponent("   \u00a76\u0422\u0435\u0440\u043c\u043e\u044f\u0434\u0435\u0440\u043d\u044b\u0439 \u00a77\u0438 \u00a7d\u041d\u043e\u0447\u043d\u043e\u0439 \u041e\u0445\u043e\u0442\u043d\u0438\u043a"));
        tooltip.add(new StringTextComponent(" "));

        tooltip.add(new StringTextComponent("\u00a7e\u26a0 \u041e\u0433\u0440\u0430\u043d\u0438\u0447\u0435\u043d\u0438\u044f \u043f\u043e\u0433\u043e\u0434\u043d\u044b\u043c\u0438 \u0443\u0441\u043b\u043e\u0432\u0438\u044f\u043c\u0438:"));
        tooltip.add(new StringTextComponent(" \u00a77\u2022 \u0412 \u00a79\u0434\u043e\u0436\u0434\u044c\u00a77, \u00a78\u0433\u0440\u043e\u0437\u0443 \u00a77\u0438\u043b\u0438 \u00a71\u043d\u043e\u0447\u044c\u044e\u00a77 \u0441\u0438\u0441\u0442\u0435\u043c\u0430 \u0441\u0431\u043e\u0438\u0442:"));
        tooltip.add(new StringTextComponent(" \u00a77\u2022 \u0412\u0440\u0435\u043c\u044f \u043d\u0430\u0432\u0435\u0434\u0435\u043d\u0438\u044f: \u00a7c5 \u0441\u0435\u043a \u00a77(+2 \u0441\u0435\u043a)"));
        tooltip.add(new StringTextComponent(" \u00a77\u2022 \u0423\u0440\u043e\u043d \u043b\u0430\u0437\u0435\u0440\u0430: \u00a7c\u0441\u043d\u0438\u0436\u0435\u043d \u043d\u0430 50%"));
        tooltip.add(new StringTextComponent(" \u00a77\u2022 \u0428\u0430\u043d\u0441 \u043e\u0431\u043d\u0430\u0440\u0443\u0436\u0435\u043d\u0438\u044f: \u00a7c75%"));
        tooltip.add(new StringTextComponent(" "));

        tooltip.add(new StringTextComponent("\u00a7b\u041c\u043e\u0434\u0443\u043b\u0438 \u0420\u042d\u0411:"));
        tooltip.add(new StringTextComponent(" \u00a77- 3 \u0441\u043b\u043e\u0442\u0430, \u0442\u043e\u043b\u044c\u043a\u043e \u0443\u043d\u0438\u043a\u0430\u043b\u044c\u043d\u044b\u0435"));
        tooltip.add(new StringTextComponent(" \u00a77- \u0422\u0438\u043f\u044b: \u00a77Basic\u00a77, \u00a7aUncommon\u00a77, \u00a7dEpic\u00a77, \u00a76Legendary"));
        tooltip.add(new StringTextComponent(" \u00a77- Shift+\u041f\u041a\u041c (\u043f\u0443\u0441\u0442\u0430\u044f \u0440\u0443\u043a\u0430) \u2014 \u043e\u0442\u043a\u0440\u044b\u0442\u044c \u043c\u0435\u043d\u044e \u043c\u043e\u0434\u0443\u043b\u0435\u0439"));
        tooltip.add(new StringTextComponent(" "));

        tooltip.add(new StringTextComponent("\u00a7e\u0423\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0438\u0435:"));
        tooltip.add(new StringTextComponent(" \u00a7e\u041f\u041a\u041c: \u00a77\u0412\u043a\u043b/\u0412\u044b\u043a\u043b \u041f\u0438\u0442\u0430\u043d\u0438\u0435"));
        tooltip.add(new StringTextComponent(" \u00a7eShift+\u041f\u041a\u041c: \u00a77\u0421\u043c\u0435\u043d\u0430 \u0440\u0435\u0436\u0438\u043c\u0430 (\u041b\u0430\u0437\u0435\u0440/\u0413\u043b\u0443\u0448\u0438\u043b\u043a\u0430)"));
        tooltip.add(new StringTextComponent(" "));
        tooltip.add(new StringTextComponent("\u00a7c\u00a7l\u0422\u0420\u0415\u0411\u0423\u0415\u0422 \u041e\u0422\u041a\u0420\u042b\u0422\u041e\u0413\u041e \u041d\u0415\u0411\u0410!"));
    }
}
