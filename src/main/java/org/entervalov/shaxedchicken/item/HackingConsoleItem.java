package org.entervalov.shaxedchicken.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.entity.projectile.ProjectileHelper;
import org.entervalov.shaxedchicken.entity.AdvancedDroneEntity;
import org.entervalov.shaxedchicken.network.HackCameraPacket;
import org.entervalov.shaxedchicken.network.ModNetwork;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class HackingConsoleItem extends Item {
    private static final int SELECT_RANGE = 40;
    private static final int HACK_RANGE = 30;
    private static final int COOLDOWN_TICKS = 200;
    private static final int MARK_TICKS = 200;

    private static final String NBT_TARGET = "Target";
    private static final String NBT_TARGET_INDEX = "TargetIndex";
    private static final String NBT_LAST_HACK_TIME = "LastHackTime";

    public HackingConsoleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable World world,
                                @Nonnull List<ITextComponent> tooltip, @Nonnull ITooltipFlag flag) {
        tooltip.add(new StringTextComponent(""));
        tooltip.add(new TranslationTextComponent("item.shaxedchicken.hacking_console.tip1").withStyle(TextFormatting.GRAY));
        tooltip.add(new TranslationTextComponent("item.shaxedchicken.hacking_console.tip2").withStyle(TextFormatting.GRAY));
        tooltip.add(new TranslationTextComponent("item.shaxedchicken.hacking_console.tip3").withStyle(TextFormatting.GRAY));
        tooltip.add(new TranslationTextComponent("item.shaxedchicken.hacking_console.tip4").withStyle(TextFormatting.DARK_GRAY));
        tooltip.add(new StringTextComponent(""));
        tooltip.add(new TranslationTextComponent("item.shaxedchicken.hacking_console.range",
                HACK_RANGE).withStyle(TextFormatting.DARK_PURPLE));
    }

    @Override
    public boolean isFoil(@Nonnull ItemStack stack) {
        CompoundNBT tag = stack.getTag();
        return tag != null && tag.hasUUID(NBT_TARGET);
    }

    @Override
    public ActionResult<ItemStack> use(@Nonnull World world, @Nonnull PlayerEntity player, @Nonnull Hand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (world.isClientSide) {
            return ActionResult.pass(stack);
        }

        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;

        if (player.isShiftKeyDown()) {
            selectNextTarget((ServerWorld) world, serverPlayer, stack);
            return ActionResult.success(stack);
        }

        if (isOnCooldown(world, stack)) {
            long remaining = COOLDOWN_TICKS - (world.getGameTime() - stack.getOrCreateTag().getLong(NBT_LAST_HACK_TIME));
            float seconds = remaining / 20f;
            serverPlayer.displayClientMessage(new StringTextComponent(
                    "\u00A7c" + new TranslationTextComponent("item.shaxedchicken.hacking_console.cooldown").getString()
                            + " \u00A77(" + String.format("%.1f", seconds) + " sec)"
            ), true);
            return ActionResult.success(stack);
        }

        AdvancedDroneEntity target = getStoredTarget((ServerWorld) world, stack);
        if (target == null) {
            target = findLookTarget((ServerWorld) world, serverPlayer);
        }
        if (target == null) {
            target = findNearestTarget((ServerWorld) world, serverPlayer);
        }

        if (target == null) {
            serverPlayer.displayClientMessage(new TranslationTextComponent("item.shaxedchicken.hacking_console.no_target")
                    .withStyle(TextFormatting.YELLOW), true);
            return ActionResult.success(stack);
        }

        if (target.isHackedBy(serverPlayer.getUUID())) {
            AdvancedDroneEntity mark = findLookTarget((ServerWorld) world, serverPlayer);
            if (mark != null && mark != target) {
                mark.setMarkedBy(serverPlayer.getUUID(), MARK_TICKS);
                target.setCommandTarget(mark.getId());
                serverPlayer.displayClientMessage(new TranslationTextComponent("item.shaxedchicken.hacking_console.marked")
                        .withStyle(TextFormatting.GREEN), true);
                return ActionResult.success(stack);
            }

            toggleDroneView(serverPlayer, target);
            return ActionResult.success(stack);
        }

        AdvancedDroneEntity.HackResult result = target.startHacking(serverPlayer);
        if (result == AdvancedDroneEntity.HackResult.COOLDOWN) {
            serverPlayer.displayClientMessage(new TranslationTextComponent("item.shaxedchicken.hacking_console.target_busy")
                    .withStyle(TextFormatting.RED), true);
        } else if (result == AdvancedDroneEntity.HackResult.ALREADY_HACKED) {
            serverPlayer.displayClientMessage(new TranslationTextComponent("item.shaxedchicken.hacking_console.already_hacked")
                    .withStyle(TextFormatting.YELLOW), true);
        } else if (result == AdvancedDroneEntity.HackResult.TOO_FAR) {
            serverPlayer.displayClientMessage(new TranslationTextComponent("item.shaxedchicken.hacking_console.too_far")
                    .withStyle(TextFormatting.RED), true);
        } else if (result == AdvancedDroneEntity.HackResult.STARTED) {
            setCooldown(world, stack);
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }

        return ActionResult.success(stack);
    }

    private void selectNextTarget(ServerWorld world, ServerPlayerEntity player, ItemStack stack) {
        List<AdvancedDroneEntity> drones = world.getEntitiesOfClass(
                AdvancedDroneEntity.class,
                new AxisAlignedBB(player.blockPosition()).inflate(SELECT_RANGE),
                Entity::isAlive
        );

        if (drones.isEmpty()) {
            player.displayClientMessage(new TranslationTextComponent("item.shaxedchicken.hacking_console.no_target")
                    .withStyle(TextFormatting.YELLOW), true);
            return;
        }

        drones.sort(Comparator.comparingDouble(player::distanceToSqr));
        CompoundNBT tag = stack.getOrCreateTag();
        int index = tag.getInt(NBT_TARGET_INDEX);
        index = (index + 1) % drones.size();
        tag.putInt(NBT_TARGET_INDEX, index);

        AdvancedDroneEntity selected = drones.get(index);
        tag.putUUID(NBT_TARGET, selected.getUUID());
        selected.setMarkedBy(player.getUUID(), 60);

        ITextComponent name = selected.getName();
        int distance = Math.round(player.distanceTo(selected));
        int hp = Math.round(selected.getHealth());
        int maxHp = Math.round(selected.getMaxHealth());

        world.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK, SoundCategory.PLAYERS, 0.5f, 1.2f);

        player.displayClientMessage(new StringTextComponent(
                "\u00A7b\u25B6 " + name.getString()
                        + " \u00A77| \u00A7c" + hp + "/" + maxHp + " HP"
                        + " \u00A77| \u00A7e" + distance + "m"
        ), true);
    }

    private AdvancedDroneEntity getStoredTarget(ServerWorld world, ItemStack stack) {
        CompoundNBT tag = stack.getTag();
        if (tag == null || !tag.hasUUID(NBT_TARGET)) return null;

        UUID id = tag.getUUID(NBT_TARGET);
        Entity entity = world.getEntity(id);
        if (entity instanceof AdvancedDroneEntity && entity.isAlive()) {
            return (AdvancedDroneEntity) entity;
        }
        return null;
    }

    private AdvancedDroneEntity findNearestTarget(ServerWorld world, ServerPlayerEntity player) {
        List<AdvancedDroneEntity> drones = world.getEntitiesOfClass(
                AdvancedDroneEntity.class,
                new AxisAlignedBB(player.blockPosition()).inflate(HackingConsoleItem.HACK_RANGE),
                Entity::isAlive
        );
        if (drones.isEmpty()) return null;
        drones.sort(Comparator.comparingDouble(player::distanceToSqr));
        return drones.get(0);
    }

    private AdvancedDroneEntity findLookTarget(ServerWorld world, ServerPlayerEntity player) {
        Vector3d eye = player.getEyePosition(1.0F);
        Vector3d look = player.getLookAngle();
        Vector3d end = eye.add(look.scale(HackingConsoleItem.HACK_RANGE));
        AxisAlignedBB box = player.getBoundingBox().expandTowards(look.scale(HackingConsoleItem.HACK_RANGE)).inflate(1.0);

        EntityRayTraceResult result = ProjectileHelper.getEntityHitResult(
                world, player, eye, end, box,
                entity -> entity instanceof AdvancedDroneEntity && entity.isAlive()
        );

        if (result != null && result.getEntity() instanceof AdvancedDroneEntity) {
            return (AdvancedDroneEntity) result.getEntity();
        }
        return null;
    }

    private boolean isOnCooldown(World world, ItemStack stack) {
        CompoundNBT tag = stack.getOrCreateTag();
        long last = tag.getLong(NBT_LAST_HACK_TIME);
        return world.getGameTime() - last < COOLDOWN_TICKS;
    }

    private void setCooldown(World world, ItemStack stack) {
        CompoundNBT tag = stack.getOrCreateTag();
        tag.putLong(NBT_LAST_HACK_TIME, world.getGameTime());
    }

    private void toggleDroneView(ServerPlayerEntity player, AdvancedDroneEntity drone) {
        CompoundNBT root = player.getPersistentData();
        CompoundNBT tag = root.getCompound("shaxedchicken");
        int currentId = tag.getInt("ViewDroneId");
        if (currentId == drone.getId()) {
            tag.putInt("ViewDroneId", -1);
            root.put("shaxedchicken", tag);
            ModNetwork.CHANNEL.send(
                    net.minecraftforge.fml.network.PacketDistributor.PLAYER.with(() -> player),
                    new HackCameraPacket(drone.getId(), false)
            );
        } else {
            tag.putInt("ViewDroneId", drone.getId());
            root.put("shaxedchicken", tag);
            ModNetwork.CHANNEL.send(
                    net.minecraftforge.fml.network.PacketDistributor.PLAYER.with(() -> player),
                    new HackCameraPacket(drone.getId(), true)
            );
        }
    }
}
