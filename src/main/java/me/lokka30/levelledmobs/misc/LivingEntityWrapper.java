package me.lokka30.levelledmobs.misc;

import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class LivingEntityWrapper {
    public LivingEntityWrapper(final @NotNull LivingEntity livingEntity, final @NotNull LevelledMobs main){
        this.main = main;
        this.livingEntity = livingEntity;
        this.applicableGroups = new LinkedList<>();
    }

    private final LevelledMobs main;
    private final LivingEntity livingEntity;
    @NotNull
    private List<CustomUniversalGroups> applicableGroups;
    private boolean hasCache;
    private boolean groupsAreBuilt;
    private Integer mobLevel;

    private void buildCache(){
        if (main.levelInterface.isLevelled(livingEntity))
            this.mobLevel = main.levelInterface.getLevelOfMob(livingEntity);
        else
            this.mobLevel = null;

        this.hasCache = true;
    }

    public void invalidateCache(){
        this.hasCache = false;
        this.groupsAreBuilt = false;
        this.applicableGroups.clear();
    }

    @NotNull
    public LivingEntity getLivingEntity(){
        return this.livingEntity;
    }

    @NotNull
    public String getTypeName(){
        return this.livingEntity.getType().toString();
    }

    @NotNull
    public List<CustomUniversalGroups> getApplicableGroups(){
        if (!groupsAreBuilt){
            //Utils.logger.warning("building groups for " + getTypeName());
            this.applicableGroups = buildApllicableGroupsForMob();
            groupsAreBuilt = true;
        }

        return this.applicableGroups;
    }

    @Nullable
    public int getMobLevel(){
        if (!hasCache) buildCache();

        return this.mobLevel;
    }

    public boolean isLevelled(){
        if (!hasCache) buildCache();

        return this.mobLevel != null;
    }

    @NotNull
    public String getWorldName(){
        return livingEntity.getWorld().getName();
    }

    @NotNull
    public PersistentDataContainer getPDC(){
        return livingEntity.getPersistentDataContainer();
    }

    public boolean isBabyMob() {
        if (livingEntity instanceof Zombie) {
            // for backwards compatibility
            Zombie zombie = (Zombie) livingEntity;
            try {
                zombie.isAdult();
                return !zombie.isAdult();
            } catch (NoSuchMethodError err) {
                //noinspection deprecation
                return zombie.isBaby();
            }
        } else if (livingEntity instanceof Ageable){
            return !(((Ageable) livingEntity).isAdult());
        }

        return false;
    }

    @Nonnull
    private List<CustomUniversalGroups> buildApllicableGroupsForMob(){
        final List<CustomUniversalGroups> groups = new ArrayList<>();
        groups.add(CustomUniversalGroups.ALL_MOBS);

        //final boolean isLevellable = (main.levelInterface.getLevellableState(le, true) == LevelInterface.LevellableState.ALLOWED);
        final boolean isLevellable = true; // if we're here then it is levellable?

        //if (isLevelled || isLevellable)
        if (this.mobLevel != null)
            groups.add(CustomUniversalGroups.ALL_LEVELLABLE_MOBS);
        final EntityType eType = livingEntity.getType();

        if (livingEntity instanceof Monster || livingEntity instanceof Boss || main.companion.groups_HostileMobs.contains(eType)){
            groups.add(CustomUniversalGroups.ALL_HOSTILE_MOBS);
        }

        if (livingEntity instanceof WaterMob || main.companion.groups_AquaticMobs.contains(eType)){
            groups.add(CustomUniversalGroups.ALL_AQUATIC_MOBS);
        }

        if (livingEntity.getWorld().getEnvironment().equals(World.Environment.NORMAL)){
            groups.add(CustomUniversalGroups.ALL_OVERWORLD_MOBS);
        } else if (livingEntity.getWorld().getEnvironment().equals(World.Environment.NETHER)){
            groups.add(CustomUniversalGroups.ALL_NETHER_MOBS);
        }

        if (livingEntity instanceof Flying || eType.equals(EntityType.PARROT) || eType.equals(EntityType.BAT)){
            groups.add(CustomUniversalGroups.ALL_FLYING_MOBS);
        }

        // why bats aren't part of Flying interface is beyond me
        if (!(livingEntity instanceof Flying) && !(livingEntity instanceof WaterMob) && !(livingEntity instanceof Boss) && !(eType.equals(EntityType.BAT))){
            groups.add(CustomUniversalGroups.ALL_GROUND_MOBS);
        }

        if (livingEntity instanceof WaterMob || main.companion.groups_AquaticMobs.contains(eType)){
            groups.add(CustomUniversalGroups.ALL_AQUATIC_MOBS);
        }

        if (livingEntity instanceof Animals || livingEntity instanceof WaterMob || main.companion.groups_PassiveMobs.contains(eType)){
            groups.add(CustomUniversalGroups.ALL_PASSIVE_MOBS);
        }

        return groups;
    }
}
