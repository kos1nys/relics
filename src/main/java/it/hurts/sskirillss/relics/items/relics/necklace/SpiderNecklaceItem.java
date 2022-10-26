package it.hurts.sskirillss.relics.items.relics.necklace;

import it.hurts.sskirillss.relics.client.tooltip.base.RelicStyleData;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.base.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.RelicAbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.RelicAbilityEntry;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.RelicAbilityStat;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.RelicLevelingData;
import it.hurts.sskirillss.relics.utils.DurabilityUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public class SpiderNecklaceItem extends RelicItem {
    public SpiderNecklaceItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.RARE));
    }

    @Override
    public RelicData getRelicData() {
        return RelicData.builder()
                .abilityData(RelicAbilityData.builder()
                        .ability("scramble", RelicAbilityEntry.builder()
                                .stat("speed", RelicAbilityStat.builder()
                                        .initialValue(0.05D, 0.2D)
                                        .upgradeModifier(RelicAbilityStat.Operation.ADD, 0.05D)
                                        .build())
                                .build())
                        .build())
                .levelingData(new RelicLevelingData(100, 10, 200))
                .styleData(RelicStyleData.builder()
                        .borders("#dc41ff", "#832698")
                        .build())
                .build();
    }

    @Override
    public void curioTick(String identifier, int index, LivingEntity livingEntity, ItemStack stack) {
        if (DurabilityUtils.isBroken(stack) || livingEntity.isSpectator())
            return;

        if (livingEntity.horizontalCollision && livingEntity.zza > 0) {
            livingEntity.setDeltaMovement(livingEntity.getDeltaMovement().x(),
                    getAbilityValue(stack, "scramble", "speed"), livingEntity.getDeltaMovement().z());
            livingEntity.fallDistance = 0F;
        }
    }
}