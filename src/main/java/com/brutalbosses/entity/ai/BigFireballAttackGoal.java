package com.brutalbosses.entity.ai;

import com.brutalbosses.entity.IOnProjectileHit;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

public class BigFireballAttackGoal extends SimpleRangedAttackGoal
{
    public static ResourceLocation ID = new ResourceLocation("brutalbosses:shootbigfireballs");

    public BigFireballAttackGoal(final MobEntity mob, final IAIParams params)
    {
        super(mob, params);
    }

    @Override
    protected ResourceLocation getID()
    {
        return ID;
    }

    @Override
    protected void doRangedAttack(final ProjectileEntity projectileEntity, final LivingEntity target)
    {
        projectileEntity.remove();
        double xDiff = target.getX() - mob.getX();
        double yDiff = target.getY(0.5D) - mob.getY(0.5D);
        double zDiff = target.getZ() - mob.getZ();

        float distVariance = MathHelper.sqrt(MathHelper.sqrt(this.mob.distanceToSqr(this.target.getX(), this.target.getY(), this.target.getZ()))) * 0.5F;

        final FireballEntity fireballEntity = new FireballEntity(mob.level,
          mob,
          xDiff + mob.getRandom().nextGaussian() * (double) distVariance,
          yDiff,
          zDiff + mob.getRandom().nextGaussian() * (double) distVariance);

        fireballEntity.xPower *= params.speed;
        fireballEntity.yPower *= params.speed;
        fireballEntity.zPower *= params.speed;

        fireballEntity.setPos(mob.getX(), mob.getY() + mob.getEyeHeight() - 0.5, mob.getZ());
        fireballEntity.setOwner(mob);
        fireballEntity.setRemainingFireTicks(10000);
        ((IOnProjectileHit) fireballEntity).setMaxLifeTime(mob.level.getGameTime() + 20 * 20);
        mob.level.addFreshEntity(fireballEntity);
    }

    @Override
    protected ProjectileEntity createProjectile()
    {
        final FireballEntity fireballEntity = new FireballEntity(mob.level,
          mob,
          0,
          0,
          0);
        fireballEntity.setRemainingFireTicks(10000);
        ((IOnProjectileHit) fireballEntity).setMaxLifeTime(mob.level.getGameTime() + 20 * 40);
        return fireballEntity;
    }
}
