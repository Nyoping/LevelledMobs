package me.lokka30.levelledmobs.managers;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.Addition;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * TODO Describe...
 *
 * @author lokka30
 */
public class MobDataManager {

    private final LevelledMobs main;

    public MobDataManager(final LevelledMobs main) {
        this.main = main;
    }

    @Nullable
    public Object getAttributeDefaultValue(final EntityType entityType, final Attribute attribute) {
        final String path = entityType.toString() + "." + attribute.toString();

        if (main.attributesCfg.contains(path)) {
            return main.attributesCfg.get(path);
        } else {
            return null;
        }
    }

    public final boolean isLevelledDropManaged(final EntityType entityType, final Material material) {
        // Head drops
        if (material.toString().endsWith("_HEAD") || material.toString().endsWith("_SKULL")) {
            if (!main.settingsCfg.getBoolean("mobs-multiply-head-drops")) {
                return false;
            }
        }

        // Check list
        return main.dropsCfg.getStringList(entityType.toString()).contains(material.toString());
    }

    public void setAdditionsForLevel(final LivingEntityWrapper lmEntity, final Attribute attribute, final Addition addition, final boolean useBaseValue) {
        double defaultValue = Objects.requireNonNull(lmEntity.getLivingEntity().getAttribute(attribute)).getBaseValue();
        if (!useBaseValue){
            Object valueTemp = getAttributeDefaultValue(lmEntity.getLivingEntity().getType(), attribute);
            if (valueTemp != null) defaultValue = (double) valueTemp;
        }

        double tempValue = defaultValue + getAdditionsForLevel(lmEntity, addition);
        if (attribute.equals(Attribute.GENERIC_MAX_HEALTH) && tempValue > 2048.0){
            // max health has hard limit of 2048
            tempValue = 2048.0;
        }
        Objects.requireNonNull(lmEntity.getLivingEntity().getAttribute(attribute)).setBaseValue(tempValue);
    }

    public final double getAdditionsForLevel(final LivingEntityWrapper lmEntity, final Addition addition) {
        final int minLevel = main.settingsCfg.getInt("fine-tuning.min-level");
        final int maxLevel = main.settingsCfg.getInt("fine-tuning.max-level");
        final double range = (double) maxLevel - minLevel - 1;
        final double percent = (double) lmEntity.getMobLevel() / range;

        final boolean isAdult = !(lmEntity.getLivingEntity() instanceof Ageable) || ((Ageable) lmEntity.getLivingEntity()).isAdult();
        final String entityCheckName = isAdult ?
                lmEntity.getLivingEntity().getName().replace(" ", "_").toUpperCase() :
                "BABY_" + lmEntity.getLivingEntity().getName().replace(" ", "_").toUpperCase();
        final double maxOverridenEntity = main.settingsCfg.getDouble(addition.getMaxAdditionConfigPath(entityCheckName), -100.0); // in case negative number are allowed
        final double max = main.settingsCfg.getDouble(addition.getMaxAdditionConfigPath());
        //Utils.logger.info(String.format("cl: %s, lmin: %s, lmax: %s, max: %s, percent: %s, test: %s", currentLevel, minLevel, maxLevel, max, percent, addition.getMaxAdditionConfigPath()));

        return maxOverridenEntity > -100.0 ?
                percent * maxOverridenEntity :
                percent * max;
    }
}
