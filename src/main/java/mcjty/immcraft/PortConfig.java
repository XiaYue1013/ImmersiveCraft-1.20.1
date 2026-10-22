package mcjty.immcraft;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

public final class PortConfig {
    private static final ForgeConfigSpec.Builder B = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.BooleanValue WORLDGEN = B.define("worldgen",true);
    public static final ForgeConfigSpec.BooleanValue ROCK_RECIPE = B.define("rockRecipe",true);
    public static final ForgeConfigSpec.DoubleValue LEAVES = B.defineInRange("leavesDropSticksChance",.1,0,1);
    public static final ForgeConfigSpec.BooleanValue RIGHT_CLICK = B.define("allowRightClickPlacement",false);
    public static final ForgeConfigSpec.BooleanValue AXE = B.define("allowMakingStoneAxe",false);
    public static final ForgeConfigSpec.BooleanValue WORKBENCH = B.define("createWorkbench",false);
    public static final ForgeConfigSpec.BooleanValue DEBUG = B.define("showDebugHandles",false);
    public static final ForgeConfigSpec.DoubleValue RENDER = B.defineInRange("maxRenderDistance",16,1,1e9);
    public static final ForgeConfigSpec.DoubleValue ROCK_DAMAGE = B.defineInRange("rockDamage",2,0,1e6);
    public static final ForgeConfigSpec.DoubleValue FIRE_CHANCE = B.defineInRange("rockStickFireChance",.3,0,1);
    public static final ForgeConfigSpec.IntValue STICK_ATTEMPTS = B.defineInRange("worldgenStickAttemptsPerChunk",30,0,100);
    public static final ForgeConfigSpec.IntValue ROCK_ATTEMPTS = B.defineInRange("worldgenRockAttemptsPerChunk",10,0,100);
    public static final ForgeConfigSpec.BooleanValue FLINT = B.define("flintOnRockMakesFlintAndSteel",true);
    public static final ForgeConfigSpec.BooleanValue TORCH = B.define("lightingFurnaceWithTorchConsumesTorch",false);
    public static final ForgeConfigSpec.BooleanValue RAIN = B.define("doesRainExtinguishTheFurnace",false);
    public static final ForgeConfigSpec.DoubleValue PAGE_VOLUME = B.defineInRange("basePageTurnVolume",1.0,0,100);
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> IGNITION = list("validIgnitionSources", "minecraft:flint_and_steel","minecraft:fire_charge","minecraft:torch");
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CONSUME = list("ignitionSourcesConsumeList", "minecraft:fire_charge","minecraft:torch");
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ROCK_BLOCKS = list("validBlocksForRocks", "minecraft:dirt","minecraft:stone","minecraft:grass_block","minecraft:mycelium");
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> STICK_BLOCKS = list("validBlocksForSticks", "minecraft:dirt","minecraft:grass_block");
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BOOKS = list("books", "minecraft:book=*","minecraft:enchanted_book=*","minecraft:writable_book=*","minecraft:written_book=*","rftools:rftools_manual=dummybook_blue","rftoolscontrol:rftoolscontrol_manual=dummybook_green","rftoolsdim:rftoolsdim_manual=dummybook_green","deepresonance:dr_manual=dummybook_red");
    public static final ForgeConfigSpec SPEC = B.build();
    private static final Map<String,java.util.function.Predicate<BlockState>> GROUND_CACHE=new java.util.concurrent.ConcurrentHashMap<>();
    private static ForgeConfigSpec.ConfigValue<List<? extends String>> list(String key, String... defaults) {
        return B.defineListAllowEmpty(Collections.singletonList(key), List.of(defaults), v -> v instanceof String);
    }
    public static boolean itemIn(ItemStack stack, List<? extends String> ids) {
        return ids.contains(Objects.toString(ForgeRegistries.ITEMS.getKey(stack.getItem())));
    }
    public static boolean book(ItemStack stack) {
        if(stack.getItem() instanceof mcjty.immcraft.api.book.IBook) return true;
        String id=Objects.toString(ForgeRegistries.ITEMS.getKey(stack.getItem()));
        return BOOKS.get().stream().anyMatch(s -> s.startsWith(id+"="));
    }
    public static boolean ground(BlockState state, List<? extends String> selectors) {
        for (String s : selectors) {
            if(GROUND_CACHE.computeIfAbsent(s,PortConfig::terrainPredicate).test(state)) return true;
        }
        return false;
    }
    private static java.util.function.Predicate<BlockState> terrainPredicate(String selector) {
        String s=legacyGround(selector);
        try {
            if(s.startsWith("#")) {
                var tag=TagKey.create(Registries.BLOCK,new ResourceLocation(s.substring(1)));
                return state->state.is(tag); // Resolve tag contents at use time, including datapack reloads.
            }
            var input=new com.mojang.brigadier.StringReader(s);
            var parsed=BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(),input,false);
            if(input.canRead()) return state->false;
            return state->state.is(parsed.blockState().getBlock()) && parsed.properties().entrySet().stream().allMatch(e->state.getValue(e.getKey()).equals(e.getValue()));
        } catch(Exception invalid) { return state->false; }
    }
    /** Translate the vanilla metadata forms supported by the 1.12 terrain configuration. */
    public static String legacyGround(String selector) {
        if(selector.startsWith("#") || selector.contains("[")) return selector;
        String[] split=selector.split("@",2);String id=split[0];int meta=0;
        if(split.length>1) try { meta=Integer.parseInt(split[1]); } catch(NumberFormatException invalid) { return selector; }
        if(id.equals("minecraft:grass")) return "minecraft:grass_block";
        if(id.equals("minecraft:dirt") && meta>=0 && meta<=2) return new String[]{"minecraft:dirt","minecraft:coarse_dirt","minecraft:podzol"}[meta];
        if(id.equals("minecraft:stone") && meta>=0 && meta<=6) return "minecraft:"+new String[]{"stone","granite","polished_granite","diorite","polished_diorite","andesite","polished_andesite"}[meta];
        if(id.equals("minecraft:sand") && meta>=0 && meta<=1) return meta==0?"minecraft:sand":"minecraft:red_sand";
        if(id.equals("minecraft:planks") && meta>=0 && meta<=5) return "minecraft:"+new String[]{"oak","spruce","birch","jungle","acacia","dark_oak"}[meta]+"_planks";
        if(id.equals("minecraft:log") || id.equals("minecraft:log2")) {
            int wood=meta&3;if(id.equals("minecraft:log2") && wood>1) return selector;
            String type=id.equals("minecraft:log2")?new String[]{"acacia","dark_oak"}[wood]:new String[]{"oak","spruce","birch","jungle"}[wood];
            String axis=(meta&12)==4?"x":(meta&12)==8?"z":"y";
            return "minecraft:"+type+((meta&12)==12?"_wood":"_log")+"[axis="+axis+"]";
        }
        return meta==0?id:selector;
    }
}
