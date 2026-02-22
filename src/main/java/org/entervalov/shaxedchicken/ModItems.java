package org.entervalov.shaxedchicken;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.entervalov.shaxedchicken.item.HackingConsoleItem;
import org.entervalov.shaxedchicken.item.JammerModuleItem;
import org.entervalov.shaxedchicken.item.JammerModuleType;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Main.MOD_ID);

    public static final RegistryObject<Item> jammer =
            ITEMS.register("jammer",
                    () -> new BlockItem(ModBlocks.JAMMER.get(),
                            new Item.Properties().tab(ItemGroup.TAB_REDSTONE)));

    public static final RegistryObject<Item> hacking_console =
            ITEMS.register("hacking_console",
                    () -> new HackingConsoleItem(
                            new Item.Properties().tab(ItemGroup.TAB_REDSTONE).stacksTo(1)
                    ));

    public static final RegistryObject<Item> jammer_module_basic =
            ITEMS.register("jammer_module_basic",
                    () -> new JammerModuleItem(JammerModuleType.BASIC,
                            new Item.Properties().tab(ItemGroup.TAB_REDSTONE).stacksTo(1)));

    public static final RegistryObject<Item> jammer_module_uncommon =
            ITEMS.register("jammer_module_uncommon",
                    () -> new JammerModuleItem(JammerModuleType.UNCOMMON,
                            new Item.Properties().tab(ItemGroup.TAB_REDSTONE).stacksTo(1)));

    public static final RegistryObject<Item> jammer_module_epic =
            ITEMS.register("jammer_module_epic",
                    () -> new JammerModuleItem(JammerModuleType.EPIC,
                            new Item.Properties().tab(ItemGroup.TAB_REDSTONE).stacksTo(1)));

    public static final RegistryObject<Item> jammer_module_legendary =
            ITEMS.register("jammer_module_legendary",
                    () -> new JammerModuleItem(JammerModuleType.LEGENDARY,
                            new Item.Properties().tab(ItemGroup.TAB_REDSTONE).stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
