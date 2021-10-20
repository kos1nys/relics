package it.hurts.sskirillss.relics.items.relics;

import it.hurts.sskirillss.relics.init.ItemRegistry;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicAttribute;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicLoot;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicStats;
import it.hurts.sskirillss.relics.items.relics.renderer.DrownedBeltModel;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.Reference;
import it.hurts.sskirillss.relics.utils.RelicUtils;
import it.hurts.sskirillss.relics.utils.tooltip.RelicTooltip;
import it.hurts.sskirillss.relics.utils.tooltip.ShiftTooltip;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.Rarity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.tuple.MutablePair;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class DrownedBeltItem extends RelicItem<DrownedBeltItem.Stats> implements ICurioItem {
    public static DrownedBeltItem INSTANCE;

    public DrownedBeltItem() {
        super(RelicData.builder()
                .rarity(Rarity.RARE)
                .config(Stats.class)
                .loot(RelicLoot.builder()
                        .table(RelicUtils.Worldgen.AQUATIC)
                        .chance(0.1F)
                        .build())
                .loot(RelicLoot.builder()
                        .table(EntityType.DROWNED.getDefaultLootTable().toString())
                        .chance(0.01F)
                        .build())
                .build());

        INSTANCE = this;
    }

    @Override
    public RelicTooltip getTooltip(ItemStack stack) {
        return RelicTooltip.builder()
                .shift(ShiftTooltip.builder()
                        .build())
                .shift(ShiftTooltip.builder()
                        .arg("+" + (int) (config.dealtDamageMultiplier * 100 - 100) + "%")
                        .build())
                .shift(ShiftTooltip.builder()
                        .arg("+" + (int) (config.incomingDamageMultiplier * 100 - 100) + "%")
                        .negative()
                        .build())
                .build();
    }

    @Override
    public RelicAttribute getAttributes(ItemStack stack) {
        return RelicAttribute.builder()
                .slot(new MutablePair<>("talisman", 1))
                .build();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public BipedModel<LivingEntity> getModel() {
        return new DrownedBeltModel();
    }

    @Mod.EventBusSubscriber(modid = Reference.MODID)
    public static class DrownedBeltServerEvents {
        @SubscribeEvent
        public static void onEntityHurt(LivingHurtEvent event) {
            Stats config = INSTANCE.config;

            PlayerEntity player = null;
            float value = 1.0F;

            if (event.getEntityLiving() instanceof PlayerEntity) {
                player = (PlayerEntity) event.getEntityLiving();
                value = config.incomingDamageMultiplier;
            } else if (event.getSource().getEntity() instanceof PlayerEntity) {
                player = (PlayerEntity) event.getSource().getEntity();
                value = config.dealtDamageMultiplier;
            }

            if (player != null && player.isUnderWater()
                    && !EntityUtils.findEquippedCurio(player, ItemRegistry.DROWNED_BELT.get()).isEmpty())
                event.setAmount(event.getAmount() * value);
        }

        @SubscribeEvent
        public static void onItemUseStart(LivingEntityUseItemEvent.Start event) {
            ItemStack stack = event.getItem();

            if (!(event.getEntityLiving() instanceof PlayerEntity) || stack.getItem() != Items.TRIDENT)
                return;

            PlayerEntity player = (PlayerEntity) event.getEntityLiving();

            if (player.getCooldowns().isOnCooldown(stack.getItem()))
                event.setCanceled(true);
        }

        @SubscribeEvent
        public static void onItemUseFinish(LivingEntityUseItemEvent.Stop event) {
            Stats config = INSTANCE.config;

            ItemStack stack = event.getItem();

            if (!(event.getEntityLiving() instanceof PlayerEntity) || stack.getItem() != Items.TRIDENT)
                return;

            PlayerEntity player = (PlayerEntity) event.getEntityLiving();

            if (EntityUtils.findEquippedCurio(player, ItemRegistry.DROWNED_BELT.get()).isEmpty())
                return;

            int duration = stack.getItem().getUseDuration(stack) - event.getDuration();
            int enchantment = EnchantmentHelper.getRiptide(stack);

            if (duration < 10 || enchantment <= 0)
                return;

            player.getCooldowns().addCooldown(stack.getItem(), (config.riptideCooldown / (enchantment + 1)) * 20);
        }
    }

    public static class Stats extends RelicStats {
        public float incomingDamageMultiplier = 1.5F;
        public float dealtDamageMultiplier = 2.0F;
        public int riptideCooldown = 10;
    }
}