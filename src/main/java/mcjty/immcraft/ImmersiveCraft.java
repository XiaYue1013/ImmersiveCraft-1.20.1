package mcjty.immcraft;

import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.registries.*;
import java.util.*;

@Mod(ImmersiveCraft.ID)
public final class ImmersiveCraft {
    public static final String ID = "immcraft";
    public static final mcjty.immcraft.api.IImmersiveCraft api=new mcjty.immcraft.apiimpl.ImmersiveCraftApi();
    public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    public static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ID);
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, ID);
    public static final DeferredRegister<com.mojang.serialization.Codec<? extends net.minecraftforge.common.world.BiomeModifier>> BIOME_MODIFIERS=DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,ID);
    public static final RegistryObject<com.mojang.serialization.Codec<AllBiomesModifier>> ALL_BIOMES=BIOME_MODIFIERS.register("rubble",()->AllBiomesModifier.CODEC);
    public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> PROJECTILES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    public static final Map<Kind, RegistryObject<ImmersiveBlock>> DEVICES = new EnumMap<>(Kind.class);
    static {
        for (Kind kind : Kind.values()) {
            RegistryObject<ImmersiveBlock> block = BLOCKS.register(kind.id, () -> new ImmersiveBlock(kind));
            DEVICES.put(kind, block);
            if (!kind.display()) ITEMS.register(kind.id, () -> kind == Kind.ROCK ? new RockItem(block.get()) : new BlockItem(block.get(), new Item.Properties()));
        }
    }
    public static final RegistryObject<Item> CHISEL = ITEMS.register("chisel", CraftTool::new);
    public static final RegistryObject<Item> SAW = ITEMS.register("saw", CraftTool::new);
    public static final RegistryObject<Item> DUMMY_BOOK = ITEMS.register("dummybook", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MANUAL = ITEMS.register("manual", mcjty.immcraft.books.ManualItem::new);
    public static final RegistryObject<mcjty.immcraft.blocks.bundle.BundleBlock> BUNDLE = BLOCKS.register("bundle",mcjty.immcraft.blocks.bundle.BundleBlock::new);
    public static final RegistryObject<Item> BUNDLE_ITEM = ITEMS.register("bundle",()->new BlockItem(BUNDLE.get(),new Item.Properties()));
    public static final RegistryObject<BlockEntityType<mcjty.immcraft.blocks.bundle.BundleTE>> BUNDLE_ENTITY = ENTITIES.register("bundle",()->BlockEntityType.Builder.of(mcjty.immcraft.blocks.bundle.BundleTE::new,BUNDLE.get()).build(null));
    public static final RegistryObject<net.minecraft.sounds.SoundEvent> PAGE_TURN = SOUNDS.register("pageturn", () -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(new net.minecraft.resources.ResourceLocation(ID,"pageturn")));
    public static final RegistryObject<BlockEntityType<DeviceEntity>> DEVICE = ENTITIES.register("device", () ->
        BlockEntityType.Builder.of(DeviceEntity::new, DEVICES.values().stream().map(RegistryObject::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> RUBBLE = FEATURES.register("rubble", RubbleFeature::new);
    public static final RegistryObject<net.minecraft.world.entity.EntityType<RockProjectile>> ROCK_ENTITY = PROJECTILES.register("immcraft_rock", () -> net.minecraft.world.entity.EntityType.Builder.<RockProjectile>of(RockProjectile::new,net.minecraft.world.entity.MobCategory.MISC).sized(.25f,.25f).clientTrackingRange(4).updateInterval(5).build("immcraft_rock"));
    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("general", () -> CreativeModeTab.builder()
        .title(Component.literal("Immersive Craft"))
        .icon(() -> new ItemStack(DEVICES.get(Kind.CHEST).get()))
        .displayItems((parameters, out) -> {
            for (Kind k : Kind.values()) if (!k.display() && k != Kind.WORKBENCH_SEC) out.accept(DEVICES.get(k).get());
            out.accept(CHISEL.get()); out.accept(SAW.get());out.accept(MANUAL.get());
        }).build());
    public ImmersiveCraft() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); FEATURES.register(bus); TABS.register(bus); PROJECTILES.register(bus);SOUNDS.register(bus);BIOME_MODIFIERS.register(bus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, PortConfig.SPEC);
        PortNetwork.register();
        net.minecraftforge.common.crafting.CraftingHelper.register(new RockRecipeCondition.Serializer());
    }
}
