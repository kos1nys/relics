package it.hurts.sskirillss.relics.items.relics.feet;

import it.hurts.sskirillss.relics.client.tooltip.base.RelicStyleData;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.base.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.RelicAbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.RelicAbilityEntry;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.RelicAbilityStat;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.RelicLevelingData;
import it.hurts.sskirillss.relics.items.relics.base.utils.AbilityUtils;
import it.hurts.sskirillss.relics.items.relics.base.utils.LevelingUtils;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import it.hurts.sskirillss.relics.utils.NBTUtils;
import it.hurts.sskirillss.relics.utils.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeMod;
import top.theillusivec4.curios.api.SlotContext;

public class IceSkatesItem extends RelicItem {
    private static final String TAG_SKATING_DURATION = "duration";

    @Override
    public RelicData constructRelicData() {
        return RelicData.builder()
                .abilityData(RelicAbilityData.builder()
                        .ability("skating", RelicAbilityEntry.builder()
                                .stat("speed", RelicAbilityStat.builder()
                                        .initialValue(0.01D, 0.035D)
                                        .upgradeModifier(RelicAbilityStat.Operation.MULTIPLY_BASE, 0.15D)
                                        .formatValue(value -> (int) (MathUtils.round(value, 3) * 10 * 100))
                                        .build())
                                .stat("duration", RelicAbilityStat.builder()
                                        .initialValue(25D, 50D)
                                        .upgradeModifier(RelicAbilityStat.Operation.MULTIPLY_BASE, 0.1D)
                                        .formatValue(value -> MathUtils.round(value / 10, 1))
                                        .build())
                                .build())
                        .ability("ram", RelicAbilityEntry.builder()
                                .requiredLevel(5)
                                .stat("damage", RelicAbilityStat.builder()
                                        .initialValue(0.05D, 0.25D)
                                        .upgradeModifier(RelicAbilityStat.Operation.MULTIPLY_BASE, 0.1D)
                                        .formatValue(value -> MathUtils.round(value * 10, 1))
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
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player))
            return;

        Level level = player.getCommandSenderWorld();
        BlockPos pos = WorldUtils.getSolidBlockUnderFeet(level, player.blockPosition());

        int duration = NBTUtils.getInt(stack, TAG_SKATING_DURATION, 0);

        int maxDuration = (int) Math.round(AbilityUtils.getAbilityValue(stack, "skating", "duration"));

        if (player.isSprinting() && !player.isShiftKeyDown() && !player.isInWater() && !player.isInLava()
                && (pos != null && level.getBlockState(pos).is(BlockTags.ICE))) {
            if (player.tickCount % 20 == 0)
                LevelingUtils.addExperience(player, stack, 1);

            if (duration < maxDuration && player.tickCount % 2 == 0) {
                NBTUtils.setInt(stack, TAG_SKATING_DURATION, ++duration);
            }

            if (level.getRandom().nextInt(maxDuration) < duration)
                level.addParticle(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.15F,
                        player.getZ(), 0, 0.25F, 0);
        } else if (duration > 0)
            NBTUtils.setInt(stack, TAG_SKATING_DURATION, Math.max(0, duration - 2));

        if (AbilityUtils.canUseAbility(stack, "ram") && duration >= 10) {
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox())) {
                if (entity == player || entity.hurtTime > 0)
                    continue;

                entity.hurt(level.damageSources().playerAttack(player), (float) (duration * AbilityUtils.getAbilityValue(stack, "ram", "damage")));

                double factor = Mth.clamp(duration * 0.025D, 1D, 2D);

                entity.setDeltaMovement(entity.position().add(0, 0.5F, 0).subtract(player.position()).normalize().multiply(factor, Math.max(1, factor / 2), factor));
            }
        }

        EntityUtils.removeAttribute(player, stack, Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.MULTIPLY_TOTAL);

        if (duration > 0) {
            EntityUtils.applyAttribute(player, stack, Attributes.MOVEMENT_SPEED, (float) (duration * AbilityUtils.getAbilityValue(stack, "skating", "speed")), AttributeModifier.Operation.MULTIPLY_TOTAL);
            EntityUtils.applyAttribute(player, stack, ForgeMod.STEP_HEIGHT_ADDITION.get(), 0.6F, AttributeModifier.Operation.ADDITION);
        } else
            EntityUtils.removeAttribute(player, stack, ForgeMod.STEP_HEIGHT_ADDITION.get(), AttributeModifier.Operation.ADDITION);
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (stack.getItem() == newStack.getItem())
            return;

        LivingEntity entity = slotContext.entity();

        EntityUtils.removeAttribute(entity, stack, Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.MULTIPLY_TOTAL);
        EntityUtils.removeAttribute(entity, stack, ForgeMod.STEP_HEIGHT_ADDITION.get(), AttributeModifier.Operation.ADDITION);
    }
}