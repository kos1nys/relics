package it.hurts.sskirillss.relics.entities;

import it.hurts.sskirillss.relics.client.particles.circle.CircleTintData;
import it.hurts.sskirillss.relics.init.EntityRegistry;
import it.hurts.sskirillss.relics.init.ItemRegistry;
import it.hurts.sskirillss.relics.items.relics.ShadowGlaiveItem;
import it.hurts.sskirillss.relics.items.relics.base.utils.AbilityUtils;
import it.hurts.sskirillss.relics.utils.NBTUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nonnull;
import java.awt.*;

public class ShadowSawEntity extends ThrowableProjectile {
    @Getter
    @Setter
    private ItemStack stack = ItemStack.EMPTY;

    public boolean isReturning = false;

    public ShadowSawEntity(EntityType<? extends ShadowSawEntity> pEntityType, Level level) {
        super(pEntityType, level);
    }

    public ShadowSawEntity(ItemStack stack, LivingEntity owner) {
        super(EntityRegistry.SHADOW_SAW.get(), owner.getCommandSenderWorld());

        setStack(stack);
    }

    @Override
    public void tick() {
        this.move(MoverType.SELF, this.getDeltaMovement());

        if (level().isClientSide())
            return;

        if (this.tickCount >= 1200)
            this.isReturning = true;

        if (isReturning) {
            this.noPhysics = true;

            if (this.getOwner() instanceof Player player) {
                if (this.position().distanceTo(player.position()) > this.getBbWidth())
                    this.setDeltaMovement(player.position().add(0, player.getBbHeight() / 2, 0).subtract(this.position()).normalize().multiply(0.75, 0.75, 0.75));
                else {
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        ItemStack stack = player.getInventory().getItem(i);

                        if (stack.getItem() != ItemRegistry.SHADOW_GLAIVE.get())
                            continue;

                        if (NBTUtils.getString(stack, ShadowGlaiveItem.TAG_SAW, "").equals(this.getStringUUID())) {
                            NBTUtils.setInt(stack, ShadowGlaiveItem.TAG_CHARGES, 8);
                            NBTUtils.clearTag(stack, ShadowGlaiveItem.TAG_SAW);

                            break;
                        }
                    }

                    this.discard();
                }
            } else
                this.discard();
        } else {
            if (this.onGround())
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.75F, 0.75F, 0.75F));
            else
                this.setDeltaMovement(this.getDeltaMovement().add(0F, -0.05F, 0F));
        }

        for (LivingEntity entity : level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.5D))) {
            if ((this.getOwner() == null || (this.getOwner() != null && entity != this.getOwner()))
                    && entity.hurt(level().damageSources().magic(), (float) Math.max(AbilityUtils.getAbilityValue(stack, "saw", "damage"), 0.1D))) {
                entity.invulnerableTime = (int) Math.round(AbilityUtils.getAbilityValue(stack, "saw", "speed"));
            }
        }

        ServerLevel serverLevel = (ServerLevel) level();

        double radius = 1.5D;

        for (int i = 0; i < 5; i++) {
            float angle = -(this.tickCount * 20 + i * 120) * 0.0105F;

            double extraX = (radius * Mth.sin((float) (Math.PI + angle))) + this.getX();
            double extraZ = (radius * Mth.cos(angle)) + this.getZ();

            serverLevel.sendParticles(new CircleTintData(new Color(200, 0, 255), 0.075F, 20, 0.95F, true),
                    extraX, this.getY() + 0.25F, extraZ, 1, 0.01, 0.01, 0.01, 0.025F);
        }

        serverLevel.sendParticles(new CircleTintData(new Color(102, 0, 255), 0.1F, 20, 0.9F, true),
                this.getX(), this.getY() + 0.3F, this.getZ(), 2, 0.25, 0.1, 0.25, 0.02F);
    }

    @Override
    protected void onHitBlock(BlockHitResult pResult) {
        this.setDeltaMovement(this.getDeltaMovement().multiply(1, 0, 1));
    }

    @Override
    protected float getGravity() {
        return 0.05F;
    }

    @Override
    protected void defineSynchedData() {

    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {

    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Nonnull
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}