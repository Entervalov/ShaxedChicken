package org.entervalov.shaxedchicken.container;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import org.entervalov.shaxedchicken.ModContainers;
import org.entervalov.shaxedchicken.block.JammerTileEntity;
import org.entervalov.shaxedchicken.item.JammerModuleItem;
import org.entervalov.shaxedchicken.item.JammerModuleType;

import javax.annotation.Nonnull;

public class JammerContainer extends Container {

    private final JammerTileEntity jammer;

    public JammerContainer(int id, PlayerInventory playerInventory, PacketBuffer data) {
        this(id, playerInventory, getTileEntity(playerInventory, data));
    }

    public JammerContainer(int id, PlayerInventory playerInventory, JammerTileEntity jammer) {
        super(ModContainers.JAMMER.get(), id);
        this.jammer = jammer;

        int startX = 62;
        int y = 20;
        for (int i = 0; i < 3; i++) {
            this.addSlot(new ModuleSlot(jammer, i, startX + i * 18, y));
        }

        int invStartY = 128;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, invStartY + row * 18));
            }
        }

        int hotbarY = invStartY + 58;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, hotbarY));
        }
    }

    private static JammerTileEntity getTileEntity(PlayerInventory playerInventory, PacketBuffer data) {
        TileEntity te = playerInventory.player.level.getBlockEntity(data.readBlockPos());
        if (te instanceof JammerTileEntity) {
            return (JammerTileEntity) te;
        }
        throw new IllegalStateException("JammerTileEntity not found");
    }

    public JammerTileEntity getJammer() {
        return jammer;
    }

    @Override
    public boolean stillValid(@Nonnull PlayerEntity player) {
        return jammer != null && jammer.stillValid(player);
    }

    @Nonnull
    @Override
    public ItemStack quickMoveStack(@Nonnull PlayerEntity player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();

            int moduleSlots = 3;
            if (index < moduleSlots) {
                if (!this.moveItemStackTo(stack, moduleSlots, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (stack.getItem() instanceof JammerModuleItem) {
                    if (!this.moveItemStackTo(stack, 0, moduleSlots, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    int invStart = moduleSlots;
                    int invEnd = invStart + 27;
                    int hotbarStart = invEnd;
                    int hotbarEnd = hotbarStart + 9;

                    if (index < invEnd) {
                        if (!this.moveItemStackTo(stack, hotbarStart, hotbarEnd, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (index < hotbarEnd) {
                        if (!this.moveItemStackTo(stack, invStart, invEnd, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    private static class ModuleSlot extends Slot {
        private final JammerTileEntity jammer;

        public ModuleSlot(JammerTileEntity jammer, int index, int x, int y) {
            super(jammer, index, x, y);
            this.jammer = jammer;
        }

        @Override
        public boolean mayPlace(@Nonnull ItemStack stack) {
            if (!(stack.getItem() instanceof JammerModuleItem)) return false;
            JammerModuleType type = ((JammerModuleItem) stack.getItem()).getType();
            return !jammer.hasModuleType(type);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}