package it.hurts.sskirillss.relics.items.relics.ring;

import it.hurts.sskirillss.relics.client.particles.circle.CircleTintData;
import it.hurts.sskirillss.relics.client.tooltip.base.RelicStyleData;
import it.hurts.sskirillss.relics.init.ItemRegistry;
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
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.SlotContext;

import javax.annotation.Nullable;
import java.awt.*;

public class ChorusInhibitorItem extends RelicItem {
    @Override
    public RelicData constructRelicData() {
        return RelicData.builder()
                .abilityData(RelicAbilityData.builder()
                        .ability("blink", RelicAbilityEntry.builder()
                                .stat("distance", RelicAbilityStat.builder()
                                        .initialValue(16D, 32D)
                                        .upgradeModifier(RelicAbilityStat.Operation.MULTIPLY_BASE, 0.2D)
                                        .formatValue(value -> (int) (MathUtils.round(value, 0)))
                                        .build())
                                .stat("cooldown", RelicAbilityStat.builder()
                                        .initialValue(5D, 10D)
                                        .upgradeModifier(RelicAbilityStat.Operation.ADD, -0.5D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .build())
                        .build())
                .levelingData(new RelicLevelingData(100, 10, 100))
                .styleData(RelicStyleData.builder()
                        .borders("#eed551", "#dcbe1d")
                        .build())
                .build();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player)
                || player.getItemInHand(InteractionHand.MAIN_HAND).getItem() != Items.CHORUS_FRUIT
                || player.getCooldowns().isOnCooldown(Items.CHORUS_FRUIT) || !player.level().isClientSide())
            return;

        BlockPos pos = getEyesPos(player, stack);

        if (pos == null)
            return;

        Vec3 start = player.position().add(0, player.getBbHeight() * 0.65D, 0);
        Vec3 end = new Vec3(pos.getX() + 0.5F, pos.getY() - 0.5F, pos.getZ() + 0.5F);

        ParticleUtils.createLine(new CircleTintData(new Color(20, 0, 80), 0.15F, 0, 0.5F, false),
                player.level(), start, end, (int) Math.round(start.distanceTo(end) * 5));
    }

    @Nullable
    public static BlockPos getEyesPos(Player player, ItemStack stack) {
        Level world = player.getCommandSenderWorld();
        Vec3 view = player.getViewVector(0);
        Vec3 eyeVec = player.getEyePosition(0);

        double distance = AbilityUtils.getAbilityValue(stack, "blink", "distance");

        BlockHitResult ray = world.clip(new ClipContext(eyeVec, eyeVec.add(view.x * distance, view.y * distance,
                view.z * distance), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        BlockPos pos = ray.getBlockPos();

        if (!world.getBlockState(pos).blocksMotion())
            return null;

        pos = pos.above();

        for (int i = 0; i < 10; i++) {
            if (world.getBlockState(pos).blocksMotion() || world.getBlockState(pos.above()).blocksMotion()) {
                pos = pos.above();

                continue;
            }

            return pos;
        }

        return null;
    }

    @Mod.EventBusSubscriber
    public static class Events {
        @SubscribeEvent
        public static void onChorusTeleport(EntityTeleportEvent.ChorusFruit event) {
            if (!(event.getEntity() instanceof Player player))
                return;

            ItemStack stack = EntityUtils.findEquippedCurio(player, ItemRegistry.CHORUS_INHIBITOR.get());

            if (stack.isEmpty())
                return;

            event.setCanceled(true);

            BlockPos pos = getEyesPos(player, stack);

            if (pos == null)
                return;

            LevelingUtils.addExperience(player, stack, (int) Math.floor(player.position().distanceTo(new Vec3(pos.getX(), pos.getY(), pos.getZ())) / 10F));

            player.teleportTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
            player.level().playSound(null, pos, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1F, 1F);
            player.getCooldowns().addCooldown(Items.CHORUS_FRUIT, Math.max((int) Math.round(AbilityUtils.getAbilityValue(stack, "blink", "cooldown") * 20D), 0));
        }
    }
}