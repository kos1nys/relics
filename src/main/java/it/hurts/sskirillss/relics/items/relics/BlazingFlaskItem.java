package it.hurts.sskirillss.relics.items.relics;

import it.hurts.sskirillss.relics.client.particles.circle.CircleTintData;
import it.hurts.sskirillss.relics.client.tooltip.base.RelicStyleData;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.base.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.RelicAbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.RelicAbilityEntry;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.RelicAbilityStat;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.RelicLevelingData;
import it.hurts.sskirillss.relics.items.relics.base.utils.AbilityUtils;
import it.hurts.sskirillss.relics.items.relics.base.utils.LevelingUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import it.hurts.sskirillss.relics.utils.NBTUtils;
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import it.hurts.sskirillss.relics.utils.WorldUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.List;

public class BlazingFlaskItem extends RelicItem {
    public static final String TAG_POSITION = "pos";
    public static final String TAG_COUNT = "count";
    public static final String TAG_RADIUS = "radius";

    @Override
    public RelicData constructRelicData() {
        return RelicData.builder()
                .abilityData(RelicAbilityData.builder()
                        .ability("bonfire", RelicAbilityEntry.builder()
                                .stat("step", RelicAbilityStat.builder()
                                        .initialValue(1D, 2.5D)
                                        .upgradeModifier(RelicAbilityStat.Operation.MULTIPLY_BASE, 0.1D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .stat("speed", RelicAbilityStat.builder()
                                        .initialValue(0.01D, 0.05D)
                                        .upgradeModifier(RelicAbilityStat.Operation.MULTIPLY_BASE, 1.9D)
                                        .formatValue(value -> MathUtils.round(value * 8, 1))
                                        .build())
                                .stat("height", RelicAbilityStat.builder()
                                        .initialValue(3D, 5D)
                                        .upgradeModifier(RelicAbilityStat.Operation.ADD, 1D)
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
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean isSelected) {
        if (!(entity instanceof Player player))
            return;

        Level world = player.getCommandSenderWorld();

        int fire = getFireAround(stack, world);

        if (fire <= 0) {
            NBTUtils.clearTag(stack, TAG_POSITION);
            NBTUtils.clearTag(stack, TAG_COUNT);
        } else {
            NBTUtils.setInt(stack, TAG_COUNT, fire);
        }

        Vec3 center = NBTUtils.parsePosition(NBTUtils.getString(stack, TAG_POSITION, ""));

        if (center != null) {
            double radius = NBTUtils.getDouble(stack, TAG_RADIUS, 0D);

            if (!player.isCreative() && !player.isSpectator() && !player.getAbilities().flying && !player.getAbilities().mayfly) {
                if (new Vec3(player.getX(), center.y(), player.getZ()).distanceTo(center) <= radius + 0.5F) {
                    player.fallDistance = 0F;

                    if (player.tickCount % 100 == 0)
                        LevelingUtils.addExperience(player, stack, 1);

                    double speed = AbilityUtils.getAbilityValue(stack, "bonfire", "speed");

                    if (world.isClientSide()) {
                        if (!player.onGround() && (player.zza != 0 || player.xxa != 0))
                            player.move(MoverType.SELF, player.getDeltaMovement().multiply(speed, 0, speed));

                        if (player instanceof LocalPlayer localPlayer && localPlayer.input.jumping
                                && (getGroundHeight(player) + AbilityUtils.getAbilityValue(stack, "bonfire", "height")) - player.getY() > 0) {
                            Vec3 motion = player.getDeltaMovement();

                            if (motion.y() < 0)
                                player.setDeltaMovement(motion.x(), motion.y() * 0.9F, motion.z());

                            player.setDeltaMovement(player.getDeltaMovement().add(0F, 0.1F, 0F));
                        }
                    }
                }
            }

            double size = NBTUtils.getInt(stack, TAG_COUNT, 0) * AbilityUtils.getAbilityValue(stack, "bonfire", "step");
            double step = 0.1D;
            int time = 0;

            if (radius < size) {
                if (radius + step < size)
                    radius += step;
                else
                    radius = size;

                time = 10;

                NBTUtils.setDouble(stack, TAG_RADIUS, radius);
            }

            if (radius > size) {
                if (radius - step > size)
                    radius -= step;
                else
                    radius = size;

                time = 10;

                NBTUtils.setDouble(stack, TAG_RADIUS, radius);
            }

            if (radius <= step)
                ParticleUtils.createBall(new CircleTintData(new Color(255, 100, 0), 0.3F, 20, 0.9F, true),
                        center, level, 3, 0.2F);

            ParticleUtils.createCyl(new CircleTintData(new Color(255, 100, 0), 0.2F, time, 0.8F, true),
                    center, level, radius, 0.15F);
        }
    }

    protected double getGroundHeight(Player player) {
        HitResult result = player.level().clip(new ClipContext(player.position(), player.position().add(0, -64, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player));

        if (result.getType() == HitResult.Type.BLOCK)
            return result.getLocation().y();

        return -player.getCommandSenderWorld().getMaxBuildHeight();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        Vec3 view = player.getViewVector(0);
        Vec3 eyeVec = player.getEyePosition(0);

        float distance = (float) (8F + AbilityUtils.getAbilityValue(stack, "bonfire", "height"));

        Vec3 end = level.clip(new ClipContext(eyeVec, eyeVec.add(view.x * distance, view.y * distance,
                view.z * distance), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();

        if (getFireAround(stack, end, level) > 0) {
            Vec3 center = NBTUtils.parsePosition(NBTUtils.getString(stack, TAG_POSITION, ""));

            double radius = NBTUtils.getDouble(stack, TAG_RADIUS, 0D);

            NBTUtils.setDouble(stack, TAG_RADIUS, (center != null && end.distanceTo(center) <= radius) ? radius - center.distanceTo(end) : 0D);
            NBTUtils.setString(stack, TAG_POSITION, NBTUtils.writePosition(end));

            player.getCooldowns().addCooldown(this, 20);
        }

        return InteractionResultHolder.pass(stack);
    }

    public int getFireAround(ItemStack stack, Level level) {
        Vec3 center = NBTUtils.parsePosition(NBTUtils.getString(stack, TAG_POSITION, ""));

        if (center == null)
            return 0;

        return getFireAround(stack, center, level);
    }

    public int getFireAround(ItemStack stack, Vec3 center, Level level) {
        List<BlockPos> positions = WorldUtils.getBlockSphere(new BlockPos((int) center.x, (int) center.y, (int) center.z), AbilityUtils.getAbilityValue(stack, "bonfire", "step"))
                .stream().filter(pos -> (level.getBlockState(pos).getBlock() instanceof BaseFireBlock)).toList();

        return positions.size();
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }
}