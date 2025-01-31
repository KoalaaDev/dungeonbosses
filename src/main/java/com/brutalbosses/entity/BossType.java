package com.brutalbosses.entity;

import com.brutalbosses.BrutalBosses;
import com.brutalbosses.entity.ai.IAIParams;
import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.entity.monster.SpellcastingIllagerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Iterator;
import java.util.Map;

import static com.brutalbosses.entity.CustomAttributes.REMOVESPELLAI;
import static com.brutalbosses.entity.capability.BossCapability.BOSS_CAP;

/**
 * Represents one boss type
 */
public class BossType
{
    /**
     * The bosses entity used
     */
    private final EntityType entityToUse;

    /**
     * Additional data for the boss entity on creation
     */
    private CompoundNBT creationData = null;

    /**
     * ID of this boss set
     */
    private final ResourceLocation id;

    private ImmutableMap<Effect, Integer>              potionEffects    = ImmutableMap.of();
    private ImmutableMap<EquipmentSlotType, ItemStack> gearMap          = ImmutableMap.of();
    private ImmutableMap<ResourceLocation, IAIParams>  aiData           = ImmutableMap.of();
    private ImmutableMap<Attribute, Float>             attributes       = ImmutableMap.of();
    private ImmutableMap<ResourceLocation, Integer>    spawnTables      = ImmutableMap.of();
    private ImmutableMap<String, Float>                customAttributes = ImmutableMap.of();

    /**
     * Name desc
     */
    private String  desc;
    private float   scale             = 1.0f;
    private int     experienceDropped = 1;
    private int     itemLootCount     = 3;
    private boolean showBossBar       = true;
    private boolean nameVisible       = true;

    public BossType(final EntityType entityToUse, final ResourceLocation id)
    {
        this.entityToUse = entityToUse;
        this.id = id;
    }

    /**
     * Creates a new boss entity of this type
     *
     * @param world
     * @return
     */
    public MobEntity createBossEntity(final World world)
    {
        final Entity entity = entityToUse.create(world);

        if (!(entity instanceof MobEntity))
        {
            if (creationData.contains("Pos"))
            {
                entity.load(creationData);
            }
            else
            {
                if (creationData.contains("ForgeCaps", 10) && entity instanceof IEntityCapReader)
                {
                    ((IEntityCapReader) entity).readCapsFrom(creationData.getCompound("ForgeCaps"));
                }
                if (entity instanceof LivingEntity)
                {
                    ((LivingEntity) entity).readAdditionalSaveData(creationData);
                }
            }
        }

        if (!(entity instanceof MobEntity))
        {
            BrutalBosses.LOGGER.warn("Not supported boss entity:" + entityToUse.getRegistryName());
            return null;
        }

        entity.getCapability(BOSS_CAP).orElse(null).setBossType(this);
        initForEntity((MobEntity) entity);

        return (MobEntity) entity;
    }

    /**
     * Init for client entities
     *
     * @param entity
     */
    public void initForClientEntity(final LivingEntity entity)
    {
        if (entity instanceof CustomEntityRenderData)
        {
            ((CustomEntityRenderData) entity).setVisualScale(scale);
            ((CustomEntityRenderData) entity).setDimension(entityToUse.getDimensions().scale(scale));
        }
    }

    /**
     * Init for server entities
     *
     * @param boss
     */
    public void initForEntity(final LivingEntity boss)
    {
        initGear(boss);
        initStats(boss);
        initAI(boss);
        if (boss instanceof MobEntity)
        {
            ((MobEntity) boss).setPersistenceRequired();
            if (boss.getAttributes().hasAttribute(Attributes.FOLLOW_RANGE))
            {
                // ((MobEntity) boss).restrictTo(boss.getCapability(BOSS_CAP).orElse(null).getSpawnPos(), (int) boss.getAttribute(Attributes.FOLLOW_RANGE).getValue());
            }
        }

        boss.setCustomName(new TranslationTextComponent(desc));
        if (nameVisible)
        {
            boss.setCustomNameVisible(true);
        }
    }

    /**
     * Inits stats and effects
     *
     * @param boss
     */
    private void initStats(final LivingEntity boss)
    {
        final float healthPct = boss.getHealth() / boss.getMaxHealth();

        for (Map.Entry<Attribute, Float> attributeEntry : attributes.entrySet())
        {
            if (boss.getAttributes().hasAttribute(attributeEntry.getKey()))
            {
                if (attributeEntry.getKey() == Attributes.MAX_HEALTH || attributeEntry.getKey() == Attributes.ATTACK_DAMAGE)
                {
                    boss.getAttribute(attributeEntry.getKey()).setBaseValue(attributeEntry.getValue() * BrutalBosses.config.getCommonConfig().globalDifficultyMultiplier.get());
                }
                else
                {
                    boss.getAttribute(attributeEntry.getKey()).setBaseValue(attributeEntry.getValue());
                }
            }
            else
            {
                BrutalBosses.LOGGER.debug(
                  "Boss:" + id.toString() + " Attribute: " + attributeEntry.getKey().getDescriptionId() + " is not applicable to: " + entityToUse.getRegistryName());
            }
        }

        boss.setHealth(boss.getMaxHealth() * healthPct);

        for (final Map.Entry<Effect, Integer> effectEntry : potionEffects.entrySet())
        {
            boss.addEffect(new EffectInstance(effectEntry.getKey(), 10000000, effectEntry.getValue()));
        }
    }

    /**
     * Inits gear used
     *
     * @param boss
     */
    private void initGear(final LivingEntity boss)
    {
        if (boss instanceof MobEntity)
        {
            for (final Map.Entry<EquipmentSlotType, ItemStack> gearEntry : gearMap.entrySet())
            {
                boss.setItemSlot(gearEntry.getKey(), gearEntry.getValue());
            }
        }
    }

    /**
     * Inits AI used
     *
     * @param boss
     */
    private void initAI(final LivingEntity boss)
    {
        if (customAttributes.containsKey(REMOVESPELLAI) && boss instanceof MobEntity)
        {
            for (final Iterator<PrioritizedGoal> iterator = ((MobEntity) boss).goalSelector.availableGoals.iterator(); iterator.hasNext(); )
            {
                final PrioritizedGoal goal = iterator.next();
                if (goal.getGoal() instanceof SpellcastingIllagerEntity.UseSpellGoal)
                {
                    goal.stop();
                    iterator.remove();
                }
            }
        }

        for (final Map.Entry<ResourceLocation, IAIParams> data : aiData.entrySet())
        {
            if (BossTypeManager.instance.aiCreatorRegistry.containsKey(data.getKey()))
            {
                BossTypeManager.instance.aiCreatorRegistry.get(data.getKey()).accept(boss, data.getValue());
            }
        }
    }

    public ResourceLocation getID()
    {
        return id;
    }

    /**
     * Sets the Potion effects
     *
     * @param potionEffects
     */
    public void setEffects(final ImmutableMap<Effect, Integer> potionEffects)
    {
        this.potionEffects = potionEffects;
    }

    /**
     * Set the gear to use
     *
     * @param gearMap
     */
    public void setGear(final ImmutableMap<EquipmentSlotType, ItemStack> gearMap)
    {
        this.gearMap = gearMap;
    }

    /**
     * Set the attributes to apply
     *
     * @param attributes
     */
    public void setAttributes(final ImmutableMap<Attribute, Float> attributes)
    {
        this.attributes = attributes;
    }

    /**
     * sets the list of AI's
     *
     * @param aiData
     */
    public void setAIData(final ImmutableMap<ResourceLocation, IAIParams> aiData)
    {
        this.aiData = aiData;
    }

    /**
     * Gets the entity type used for this boss
     *
     * @return
     */
    public EntityType getEntityType()
    {
        return entityToUse;
    }

    /**
     * Sets the desc after name
     */
    public void setDesc(final String desc)
    {
        this.desc = desc;
    }

    /**
     * Sets the spawn loottables of chests
     *
     * @param spawnData
     */
    public void setSpawnTable(final ImmutableMap<ResourceLocation, Integer> spawnData)
    {
        this.spawnTables = spawnData;
    }

    @Override
    public int hashCode()
    {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object other)
    {
        if (other instanceof BossType)
        {
            return id.equals(((BossType) other).id);
        }

        return false;
    }

    /**
     * Get the spawn tables
     *
     * @return
     */
    public ImmutableMap<ResourceLocation, Integer> getSpawnTables()
    {
        return spawnTables;
    }

    /**
     * Set the visual scale
     *
     * @param scale
     */
    public void setVisualScale(final float scale)
    {
        this.scale = scale;
    }

    /**
     * Get the visual scale
     *
     * @return
     */
    public float getVisualScale()
    {
        return scale;
    }

    /**
     * Get the configured json value of this attribute
     *
     * @return
     */
    public float getCustomAttributeValueOrDefault(final String attributeID, float defaultValue)
    {
        return customAttributes.getOrDefault(attributeID, defaultValue);
    }

    /**
     * Set the custom attributes
     *
     * @param customAttributes
     */
    public void setCustomAttributes(final ImmutableMap<String, Float> customAttributes)
    {
        this.customAttributes = customAttributes;
    }

    /**
     * Get the exp dropped
     *
     * @return
     */
    public int getExperienceDrop()
    {
        return experienceDropped;
    }

    /**
     * Get the exp dropped
     *
     * @return
     */
    public void setExperienceDrop(final int exp)
    {
        experienceDropped = exp;
    }

    /**
     * Get the params for an AI
     *
     * @param id
     * @return
     */
    public IAIParams getAIParams(final ResourceLocation id)
    {
        return aiData.get(id);
    }

    public int getItemLootCount()
    {
        return itemLootCount;
    }

    public void setItemLootCount(final int itemLootCount)
    {
        this.itemLootCount = itemLootCount;
    }

    /**
     * Serialize nbt to client
     *
     * @return
     */
    public CompoundNBT serializeToClient()
    {
        final CompoundNBT compoundNBT = new CompoundNBT();
        compoundNBT.putString("id", id.toString());
        compoundNBT.putString("etype", entityToUse.getRegistryName().toString());
        compoundNBT.putFloat("scale", scale);


        return compoundNBT;
    }

    /**
     * Serialize nbt to client
     *
     * @return
     */
    public static BossType deserializeAtClient(final CompoundNBT compoundNBT)
    {
        final ResourceLocation id = ResourceLocation.tryParse(compoundNBT.getString("id"));
        final ResourceLocation entity = ResourceLocation.tryParse(compoundNBT.getString("etype"));
        final EntityType type = ForgeRegistries.ENTITIES.getValue(entity);

        if (type == null)
        {
            return null;
        }

        final BossType bossType = new BossType(type, id);
        bossType.setVisualScale(compoundNBT.getFloat("scale"));
        return bossType;
    }

    public void setBossBar(final boolean show)
    {
        this.showBossBar = show;
    }

    public boolean showBossBar()
    {
        return showBossBar;
    }

    public void setEntityNBT(final CompoundNBT entityNBT)
    {
        creationData = entityNBT;
    }

    public void setNameVisible(final boolean visible)
    {
        nameVisible = visible;
    }
}
