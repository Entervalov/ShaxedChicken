package org.entervalov.shaxedchicken.block;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.items.ItemStackHandler;
import org.entervalov.shaxedchicken.ModTileEntities;
import org.entervalov.shaxedchicken.container.JammerContainer;
import org.entervalov.shaxedchicken.entity.AdvancedDroneEntity;
import org.entervalov.shaxedchicken.item.JammerModuleItem;
import org.entervalov.shaxedchicken.item.JammerModuleType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class JammerTileEntity extends TileEntity implements ITickableTileEntity, IInventory, INamedContainerProvider {

    public static final double JAMMER_RADIUS = 30.0;
    public static final double LASER_RANGE = 20.0;

    private static final int BASE_WARMUP_TIME = 60;
    private static final int MAX_TARGETS = 2;

    public enum JammerMode {
        ALL(0), JAMMER(1), LASER(2);
        private final int id;
        JammerMode(int id) { this.id = id; }
        public int getId() { return id; }
        public static JammerMode byId(int id) {
            if (id < 0 || id >= values().length) return ALL;
            return values()[id];
        }
    }

    private boolean active = false;
    private JammerMode mode = JammerMode.ALL;

    private int tickCounter = 0;
    private int targetingTimer = 0;
    private boolean isWeatherBad = false;

    private final List<Integer> targetEntityIds = new ArrayList<>();

    private final ItemStackHandler modules = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            sendUpdate();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (!(stack.getItem() instanceof JammerModuleItem)) return false;
            JammerModuleType type = ((JammerModuleItem) stack.getItem()).getType();
            return !hasModuleType(type);
        }
    };

    public JammerTileEntity() {
        super(ModTileEntities.JAMMER.get());
    }

    @Override
    public void tick() {
        if (level == null) return;

        if (!active) {
            resetSystem();
            return;
        }

        tickCounter++;
        if (level.isClientSide) return;

        if (tickCounter % 20 == 0) checkWeatherConditions();

        if (mode == JammerMode.ALL || mode == JammerMode.LASER) {
            if (tickCounter % 10 == 0) updateTargets();

            if (!targetEntityIds.isEmpty()) {
                int requiredTime = getWarmupTime();

                if (targetingTimer < requiredTime) {
                    if (!isWeatherBad || level.random.nextFloat() > 0.2F) {
                        targetingTimer++;
                    }
                    if (tickCounter % 5 == 0) sendUpdate();
                } else {
                    fireAtTargets();
                }
            } else {
                if (targetingTimer > 0) {
                    targetingTimer -= 2;
                    if (tickCounter % 5 == 0) sendUpdate();
                }
            }
        } else {
            if (!targetEntityIds.isEmpty() || targetingTimer > 0) resetSystem();
        }

        if (mode == JammerMode.ALL || mode == JammerMode.JAMMER) {
            if (tickCounter % 5 == 0) jamNearbyDrones();
        }
    }

    private void resetSystem() {
        if (targetingTimer > 0) targetingTimer = 0;
        if (!targetEntityIds.isEmpty()) {
            targetEntityIds.clear();
            sendUpdate();
        }
    }

    private void checkWeatherConditions() {
        assert level != null;
        boolean isRaining = level.isRaining();
        boolean isThundering = level.isThundering();
        boolean isNight = level.getDayTime() % 24000 > 13000 && level.getDayTime() % 24000 < 23000;
        boolean canSeeSky = level.canSeeSky(worldPosition.above());
        this.isWeatherBad = isRaining || isThundering || isNight || !canSeeSky;
    }

    private void updateTargets() {
        boolean changed = targetEntityIds.removeIf(id -> {
            assert level != null;
            Entity e = level.getEntity(id);
            if (e == null || !e.isAlive()) return true;
            if (e instanceof AdvancedDroneEntity && ((AdvancedDroneEntity) e).isHacked()) return true;
            return e.distanceToSqr(getBlockPos().getX()+0.5, getBlockPos().getY()+0.5, getBlockPos().getZ()+0.5) > getLaserRange() * getLaserRange();
        });

        if (targetEntityIds.size() < getMaxTargets()) {
            AxisAlignedBB scanArea = new AxisAlignedBB(getBlockPos()).inflate(getLaserRange());
            assert level != null;
            List<AdvancedDroneEntity> drones = level.getEntitiesOfClass(AdvancedDroneEntity.class, scanArea, Entity::isAlive);

            for (AdvancedDroneEntity drone : drones) {
                if (targetEntityIds.size() >= getMaxTargets()) break;
                if (targetEntityIds.contains(drone.getId())) continue;
                if (drone.isHacked()) continue;
                if (isWeatherBad && !ignoreWeatherPenalties() && level.random.nextFloat() > 0.75F) continue;

                targetEntityIds.add(drone.getId());
                changed = true;

                if (targetingTimer > 20) {
                    targetingTimer = isWeatherBad ? 10 : 30;
                }
            }
        }
        if (changed) sendUpdate();
    }

    private void fireAtTargets() {
        float damage = getLaserDamage();
        for (Integer id : targetEntityIds) {
            assert level != null;
            Entity target = level.getEntity(id);
            if (target != null && target.isAlive()) {
                target.hurt(DamageSource.MAGIC, damage);
                if (!level.isRaining()) {
                    target.setSecondsOnFire(2);
                }
            }
        }
    }

    private void jamNearbyDrones() {
        double effectiveRadius = getJammerRadius();

        AxisAlignedBB jamArea = new AxisAlignedBB(getBlockPos()).inflate(effectiveRadius);
        assert level != null;
        List<AdvancedDroneEntity> drones = level.getEntitiesOfClass(AdvancedDroneEntity.class, jamArea, Entity::isAlive);

        for (AdvancedDroneEntity drone : drones) {
            drone.setJammed(this.worldPosition, getJammerDurationTicks());

            drone.setAttacking(false);
        }
    }

    public boolean hasModuleType(JammerModuleType type) {
        for (int i = 0; i < modules.getSlots(); i++) {
            ItemStack stack = modules.getStackInSlot(i);
            if (stack.getItem() instanceof JammerModuleItem) {
                JammerModuleType t = ((JammerModuleItem) stack.getItem()).getType();
                if (t == type) return true;
            }
        }
        return false;
    }

    public JammerModuleType getHighestModuleType() {
        JammerModuleType best = null;
        for (int i = 0; i < modules.getSlots(); i++) {
            ItemStack stack = modules.getStackInSlot(i);
            if (stack.getItem() instanceof JammerModuleItem) {
                JammerModuleType type = ((JammerModuleItem) stack.getItem()).getType();
                if (best == null || type.ordinal() > best.ordinal()) {
                    best = type;
                }
            }
        }
        return best;
    }

    private boolean ignoreWeatherPenalties() {
        return hasModuleType(JammerModuleType.LEGENDARY);
    }

    public double getJammerRadius() {
        double radius = JAMMER_RADIUS;
        if (isWeatherBad && !ignoreWeatherPenalties()) radius *= 0.7;
        double mult = 1.0;
        if (hasModuleType(JammerModuleType.BASIC)) mult += 0.10;
        if (hasModuleType(JammerModuleType.LEGENDARY)) mult += 0.25;
        return radius * mult;
    }

    public double getLaserRange() {
        double range = LASER_RANGE;
        double mult = 1.0;
        if (hasModuleType(JammerModuleType.BASIC)) mult += 0.10;
        if (hasModuleType(JammerModuleType.LEGENDARY)) mult += 0.15;
        return range * mult;
    }

    public float getLaserDamage() {
        float base = (isWeatherBad && !ignoreWeatherPenalties()) ? 2.0F : 4.0F;
        if (hasModuleType(JammerModuleType.EPIC)) base *= 1.25F;
        return base;
    }

    public int getWarmupTime() {
        int time = BASE_WARMUP_TIME;
        if (isWeatherBad && !ignoreWeatherPenalties()) time += 40;
        if (hasModuleType(JammerModuleType.EPIC)) time = Math.max(20, (int) (time * 0.75F));
        return time;
    }

    public int getMaxTargets() {
        int max = MAX_TARGETS;
        if (hasModuleType(JammerModuleType.UNCOMMON)) max += 1;
        return max;
    }

    public int getJammerDurationTicks() {
        int base = 20;
        if (hasModuleType(JammerModuleType.BASIC)) base += 2;
        if (hasModuleType(JammerModuleType.EPIC)) base += 5;
        if (hasModuleType(JammerModuleType.LEGENDARY)) base += 8;
        return base;
    }

    public void cycleMode() {
        int nextId = (mode.getId() + 1) % JammerMode.values().length;
        this.mode = JammerMode.byId(nextId);
        this.setChanged();
        sendUpdate();
    }

    public JammerMode getMode() { return mode; }
    public boolean isLaserEnabled() { return active && (mode == JammerMode.ALL || mode == JammerMode.LASER); }
    public boolean isJammerEnabled() { return active && (mode == JammerMode.ALL || mode == JammerMode.JAMMER); }
    public boolean isActive() { return active; }

    public float getChargeProgress() {
        int maxTime = getWarmupTime();
        return Math.min(1.0F, (float) targetingTimer / maxTime);
    }

    public int getTickCounter() { return tickCounter; }
    public List<Integer> getTargetIds() { return targetEntityIds; }
    public boolean isWeatherBad() { return isWeatherBad; }

    public void setActive(boolean active) {
        if (this.active != active) {
            this.active = active;
            this.tickCounter = 0;
            this.targetingTimer = 0;
            this.setChanged();
            sendUpdate();
        }
    }

    @Override
    public void load(@Nonnull BlockState state, @Nonnull CompoundNBT nbt) {
        super.load(state, nbt);
        this.active = nbt.getBoolean("Active");
        this.targetingTimer = nbt.getInt("Timer");
        this.isWeatherBad = nbt.getBoolean("BadWeather");
        if (nbt.contains("Mode")) {
            this.mode = JammerMode.byId(nbt.getInt("Mode"));
        }
        if (nbt.contains("Modules")) {
            modules.deserializeNBT(nbt.getCompound("Modules"));
        }
        this.targetEntityIds.clear();
        for (int id : nbt.getIntArray("Targets")) {
            this.targetEntityIds.add(id);
        }
    }

    @Override
    public CompoundNBT save(@Nonnull CompoundNBT nbt) {
        super.save(nbt);
        nbt.putBoolean("Active", active);
        nbt.putInt("Timer", targetingTimer);
        nbt.putBoolean("BadWeather", isWeatherBad);
        nbt.putInt("Mode", mode.getId());
        nbt.putIntArray("Targets", targetEntityIds.stream().mapToInt(i -> i).toArray());
        nbt.put("Modules", modules.serializeNBT());
        return nbt;
    }

    @Nullable
    @Override
    public SUpdateTileEntityPacket getUpdatePacket() {
        return new SUpdateTileEntityPacket(this.worldPosition, 1, getUpdateTag());
    }

    @Override
    public CompoundNBT getUpdateTag() { return this.save(new CompoundNBT()); }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) { load(getBlockState(), pkt.getTag()); }

    private void sendUpdate() {
        if (level != null && !level.isClientSide) {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(worldPosition).inflate(Math.max(JAMMER_RADIUS, getJammerRadius()));
    }

    @Override
    public int getContainerSize() {
        return modules.getSlots();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < modules.getSlots(); i++) {
            if (!modules.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        return modules.getStackInSlot(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        ItemStack extracted = modules.extractItem(index, count, false);
        if (!extracted.isEmpty()) {
            setChanged();
            sendUpdate();
        }
        return extracted;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack stack = modules.getStackInSlot(index);
        modules.setStackInSlot(index, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (!modules.isItemValid(index, stack)) return;
        modules.setStackInSlot(index, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
        sendUpdate();
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return modules.isItemValid(index, stack);
    }

    @Override
    public boolean stillValid(PlayerEntity player) {
        if (this.level == null || this.level.getBlockEntity(this.worldPosition) != this) return false;
        return player.distanceToSqr(
                this.worldPosition.getX() + 0.5,
                this.worldPosition.getY() + 0.5,
                this.worldPosition.getZ() + 0.5
        ) <= 64.0;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < modules.getSlots(); i++) {
            modules.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    @Override
    public ITextComponent getDisplayName() {
        return new StringTextComponent("EW Jammer");
    }

    @Override
    public Container createMenu(int id, PlayerInventory playerInventory, PlayerEntity player) {
        return new JammerContainer(id, playerInventory, this);
    }
}