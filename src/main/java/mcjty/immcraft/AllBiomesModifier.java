package mcjty.immcraft;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.world.*;
public record AllBiomesModifier(Holder<PlacedFeature> feature) implements BiomeModifier {
    public static final Codec<AllBiomesModifier> CODEC=PlacedFeature.CODEC.fieldOf("feature").xmap(AllBiomesModifier::new,AllBiomesModifier::feature).codec();
    public void modify(Holder<Biome> biome,Phase phase,ModifiableBiomeInfo.BiomeInfo.Builder builder) { if(phase==Phase.ADD) builder.getGenerationSettings().addFeature(GenerationStep.Decoration.VEGETAL_DECORATION,feature); }
    public Codec<? extends BiomeModifier> codec() { return CODEC; }
}
