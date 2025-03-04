package it.hurts.sskirillss.relics.init;

import com.mojang.serialization.Codec;
import it.hurts.sskirillss.relics.utils.Reference;
import it.hurts.sskirillss.relics.world.RelicLootModifier;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = Reference.MODID)
public class CodecRegistry {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> CODECS = DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Reference.MODID);

    public static final RegistryObject<Codec<RelicLootModifier>> RELIC_LOOT = CODECS.register("relic_gen", () -> RelicLootModifier.CODEC);

    public static void register() {
        CODECS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}