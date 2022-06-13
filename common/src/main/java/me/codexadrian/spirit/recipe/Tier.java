package me.codexadrian.spirit.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.codexadrian.spirit.SpiritRegistry;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public record Tier(ResourceLocation id, String displayName, int requiredSouls, int minSpawnDelay, int maxSpawnDelay, int spawnCount, int spawnRange, int nearbyRange,
                   boolean redstoneControlled, boolean ignoreSpawnConditions, Set<String> blacklist) implements Recipe<Container> {

    public static Codec<Tier> codec(ResourceLocation id) {
        return RecordCodecBuilder.create(instance -> instance.group(
                MapCodec.of(Encoder.empty(), Decoder.unit(() -> id)).forGetter(Tier::id),
                Codec.STRING.fieldOf("displayName").forGetter(Tier::displayName),
                Codec.INT.fieldOf("requiredSouls").forGetter(Tier::requiredSouls),
                Codec.INT.fieldOf("minSpawnDelay").forGetter(Tier::minSpawnDelay),
                Codec.INT.fieldOf("maxSpawnDelay").forGetter(Tier::maxSpawnDelay),
                Codec.INT.fieldOf("spawnCount").forGetter(Tier::spawnCount),
                Codec.INT.fieldOf("spawnRange").forGetter(Tier::spawnRange),
                Codec.INT.fieldOf("nearbyRange").forGetter(Tier::nearbyRange),
                Codec.BOOL.fieldOf("redstoneControlled").orElse(false).forGetter(Tier::redstoneControlled),
                Codec.BOOL.fieldOf("ignoreSpawnConditions").orElse(false).forGetter(Tier::ignoreSpawnConditions),
                createSetCodec(Codec.STRING).orElse(new HashSet<>()).fieldOf("blacklist").forGetter(Tier::blacklist)
        ).apply(instance, Tier::new));
    }

    @Override
    public boolean matches(@NotNull Container container, @NotNull Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(@NotNull Container container) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int i, int j) {
        return true;
    }

    @Override
    public ItemStack getResultItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SpiritRegistry.TIER_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return SpiritRegistry.TIER_RECIPE.get();
    }

    @Nullable
    public static Tier getTier(int souls, String type, Level level, boolean getNextTier) {
        Tier storedTier = null;
        List<Tier> tiers = new ArrayList<>(getTiers(level));
        if(tiers.isEmpty()) return null;
        tiers.sort(Comparator.comparingInt(value -> -value.requiredSouls()));
        if(!getNextTier && souls < tiers.get(tiers.size() - 1).requiredSouls()) return null;
        for (Tier tier : tiers) {
            if(type == null || !tier.blacklist().contains(type)) {
                if(souls < tier.requiredSouls()) storedTier = tier;
                else if (!getNextTier) {
                    storedTier = tier;
                    break;
                } else break;
            }
        }
        return storedTier;
    }
    public static Tier getHighestTier(String type, Level level) {
        Tier storedTier = null;
        for (Tier tier : getTiers(level)) {
            if(type == null || !tier.blacklist().contains(type)) {
                if(storedTier == null || storedTier.requiredSouls() < tier.requiredSouls()) {
                    storedTier = tier;
                }
            }
        }
        return storedTier;
    }

    public static Tier getTier(int souls, String type, Level level) {
        return getTier(souls, type, level, false);
    }

    public static List<Tier> getTiers(Level level) {
        return level.getRecipeManager().getAllRecipesFor(SpiritRegistry.TIER_RECIPE.get());
    }


    private static <A> Codec<Set<A>> createSetCodec(Codec<A> codec) {
        return codec.listOf().xmap(HashSet::new, ArrayList::new);
    }
}
