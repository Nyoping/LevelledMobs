package me.lokka30.levelledmobs.bukkit.logic.function.process.action.impl.setlevel;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import me.lokka30.levelledmobs.bukkit.LevelledMobs;
import me.lokka30.levelledmobs.bukkit.api.data.EntityDataUtil;
import me.lokka30.levelledmobs.bukkit.data.InternalEntityDataUtil;
import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import me.lokka30.levelledmobs.bukkit.logic.function.process.Process;
import me.lokka30.levelledmobs.bukkit.logic.function.process.action.Action;
import me.lokka30.levelledmobs.bukkit.logic.function.process.action.impl.setlevel.inheritance.DifferingFormulaResolveType;
import me.lokka30.levelledmobs.bukkit.logic.levelling.strategy.LevellingStrategy;
import me.lokka30.levelledmobs.bukkit.logic.levelling.strategy.LevellingStrategyRequestEvent;
import me.lokka30.levelledmobs.bukkit.util.Log;
import me.lokka30.levelledmobs.bukkit.util.StringUtils;
import me.lokka30.levelledmobs.bukkit.util.TriLevel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import redempt.crunch.Crunch;

public class SetLevelAction extends Action {

    /* vars */

    private final String formula;
    private final Set<LevellingStrategy> strategies = new HashSet<>();

    private final boolean useInheritanceIfAvailable;
    private final String inheritanceBreedingFormula;
    private final String inheritanceTransformationFormula;

    /* constructors */

    public SetLevelAction(
        @NotNull Process parentProcess,
        @NotNull CommentedConfigurationNode actionNode
    ) {
        super(parentProcess, actionNode);
        Log.tmpdebug("Found set level action at path: " + getActionNode().path().toString());

        this.formula = getActionNode().node("formula")
            .getString("no-level");

        this.useInheritanceIfAvailable = getActionNode()
            .node("inheritance", "use-if-available")
            .getBoolean(false);

        this.inheritanceBreedingFormula = getActionNode()
            .node("inheritance", "breeding", "formula")
            .getString("(%father-level% + %mother-level%) / 2");

        this.inheritanceTransformationFormula = getActionNode()
            .node("inheritance", "transformation", "formula")
            .getString("%mother-level%");

        /*
        Here we want to call out for all known levelling strategies to be registered to the
        SetLevelAction.
         */
        // Iterate through each strategyId specified under the strategies section
        for(var strategyNodeEntry : getActionNode()
            .node("strategies")
            .childrenMap().entrySet()
        ) {
            final CommentedConfigurationNode strategyNode = strategyNodeEntry.getValue();

            if(strategyNodeEntry.getKey() == null) {
                //TODO log error: strategy key must not be null
                new Throwable().printStackTrace();
                continue;
            }

            if(!(strategyNodeEntry.getKey() instanceof String strategyId)) {
                //TODO log error: strategy keys must be strings
                new Throwable().printStackTrace();
                continue;
            }

            // fire LevellingStrategyRequestEvent
            final var stratReqEvent = new LevellingStrategyRequestEvent(strategyId, strategyNode);
            Bukkit.getPluginManager().callEvent(stratReqEvent);

            if(stratReqEvent.isCancelled()) {
                continue;
            }

            // add all strategies from the events
            getStrategies().addAll(stratReqEvent.getStrategies());
        }
    }

    /* methods */

    @Override
    public void run(Context context) {
        Objects.requireNonNull(context, "context");

        if(context.getEntity() == null) {
            throw new IllegalStateException("Requires entity context; missing.");
        }

        if(!(context.getEntity() instanceof final LivingEntity lent)) {
            throw new IllegalStateException("Entity context is not a type of LivingEntity.");
        }

        if (EntityDataUtil.isLevelled(lent, true)) {
            // Looks like the mob was previously levelled, so we need to remove most of the LM stuff
            InternalEntityDataUtil.unlevelMob(lent);
        }

        TriLevel result;

        result = generateInheritedLevels(context);
        if(result == null) result = generateStandardLevels(context);

        // if the formula was invalid or returned 'no-level', skip
        if(result == null) {
            return;
        }

        InternalEntityDataUtil.setMinLevel(lent, result.getMinLevel(), true);
        InternalEntityDataUtil.setLevel(lent, result.getLevel(), true);
        InternalEntityDataUtil.setMaxLevel(lent, result.getMaxLevel(), true);

        // apply inheritance formulas to (parent) entity.
        if(useInheritanceIfAvailable()) {
            InternalEntityDataUtil.setInheritanceBreedingFormula(lent,
                getInheritanceBreedingFormula(), true);
            InternalEntityDataUtil.setInheritanceTransformationFormula(lent,
                getInheritanceTransformationFormula(), true);
        }

        Log.tmpdebug("Finished levelling a %s: Lvl=%s; MinLvl=%s; MaxLvl=%s.".formatted(
            lent.getType(), result.getLevel(), result.getMinLevel(), result.getMaxLevel()
        ));
    }

    private TriLevel generateStandardLevels(Context context) {
        return processFormula(context);
    }

    private TriLevel generateInheritedLevels(Context context) {
        if(!useInheritanceIfAvailable()) return null;

        final LivingEntity lent = Objects.requireNonNull(
            (LivingEntity) context.getEntity(),
            "LivingEntity"
        );

        final @Nullable LivingEntity father = EntityDataUtil.getFather(lent, false);
        final @Nullable LivingEntity mother = EntityDataUtil.getMother(lent, false);

        /*
        Entity Breeding Level Inheritance
         */
        if(Boolean.TRUE.equals(EntityDataUtil.wasBred(lent, true))) {


            if(father == null || mother == null) return null;
            context
                .withFather(father)
                .withMother(mother);

            final String fatherFormula = StringUtils.emptyIfNull(
                EntityDataUtil.getInheritanceBreedingFormula(father, true)
            );
            final String motherFormula = StringUtils.emptyIfNull(
                EntityDataUtil.getInheritanceBreedingFormula(mother, true)
            );

            // skip if both are null
            if(fatherFormula.isBlank() && motherFormula.isBlank()) {
                return null;
            }

            // skip if both formulas are 'no-level'
            if(fatherFormula.equalsIgnoreCase("no-level") &&
                motherFormula.equalsIgnoreCase("no-level")
            ) {
                return null;
            }

            final Function<String, Integer> levelEvaluator = (formula) -> {
                if(formula.isBlank() || formula.equalsIgnoreCase("no-level"))
                    return getMinPossibleLevel();

                try {
                    return (int) Math.floor(
                        Crunch.evaluateExpression(
                            LevelledMobs.getInstance()
                                .getLogicHandler()
                                .getContextPlaceholderHandler()
                                .replace(formula, context)
                        )
                    );
                } catch(Exception ex) {
                    throw new RuntimeException(ex);
                }
            };

            final int fatherInheritedLevel = levelEvaluator.apply(fatherFormula);
            final int motherInheritedLevel = levelEvaluator.apply(motherFormula);

            final int minLevel;
            final @Nullable Integer fatherMinLevel = EntityDataUtil
                .getMinLevel(father, true);
            final @Nullable Integer motherMinLevel = EntityDataUtil
                .getMinLevel(mother, true);

            final int maxLevel;
            final @Nullable Integer fatherMaxLevel = EntityDataUtil
                .getMaxLevel(father, true);
            final @Nullable Integer motherMaxLevel = EntityDataUtil
                .getMaxLevel(mother, true);

            if(fatherMinLevel == null && motherMinLevel == null) {
                return null;
            } else if(fatherMinLevel != null && motherMinLevel != null) {
                minLevel = Math.min(fatherMinLevel, motherMinLevel);
            } else {
                minLevel = Objects.requireNonNullElse(fatherMinLevel, motherMinLevel);
            }

            if(fatherMaxLevel == null && motherMaxLevel == null) {
                return null;
            } else if(fatherMaxLevel != null && motherMaxLevel != null) {
                maxLevel = Math.min(fatherMaxLevel, motherMaxLevel);
            } else {
                maxLevel = Objects.requireNonNullElse(fatherMaxLevel, motherMaxLevel);
            }

            // resolve differing formulas
            if(!fatherFormula.equalsIgnoreCase(motherFormula)) {
                return switch(DifferingFormulaResolveType.getFromAdvancedSettings()) {
                    case USE_AVERAGE -> new TriLevel(
                        minLevel,
                        (fatherInheritedLevel + motherInheritedLevel) / 2,
                        maxLevel
                    );
                    case USE_RANDOM -> new TriLevel(
                        minLevel,
                        ThreadLocalRandom.current().nextBoolean() ?
                            fatherInheritedLevel : motherInheritedLevel,
                        maxLevel
                    );
                    case USE_NEITHER -> null;
                };
            }

            // yes, we are ignoring fatherLevel since it should be the same
            return new TriLevel(minLevel, motherInheritedLevel, maxLevel);

        }

        /*
        Entity Transformation Level Inheritance
         */
        if(Boolean.TRUE.equals(EntityDataUtil.wasTransformed(lent, true))) {

            // during transformation, mother == father. we only check for one.
            if(mother == null) return null;

            // yes: it is intentional the father is the same as the mother during transformation.
            context
                .withFather(mother)
                .withMother(mother);

            if(!EntityDataUtil.isLevelled(mother, true)) return null;

            final String formula = StringUtils.emptyIfNull(EntityDataUtil
                .getInheritanceTransformationFormula(mother, true));

            if(formula.isBlank() || formula.equalsIgnoreCase("no-level")) {
                return null;
            }

            //noinspection ConstantConditions
            return new TriLevel(
                EntityDataUtil.getMinLevel(father, true),

                (int) Math.floor(
                    Crunch.evaluateExpression(
                        LevelledMobs.getInstance()
                            .getLogicHandler()
                            .getContextPlaceholderHandler()
                            .replace(formula, context)
                    )
                ),

                EntityDataUtil.getMaxLevel(father, true)
            );
        }

        /*
        Passenger/Vehicle Level Inheritance
         */
        Entity vehicleEntity = lent;
        while(lent.isInsideVehicle()) {
            if(vehicleEntity instanceof LivingEntity vehicleLentity) {
                if(EntityDataUtil.isLevelled(vehicleLentity, true)) {
                    //noinspection ConstantConditions
                    return new TriLevel(
                        EntityDataUtil.getMinLevel(vehicleLentity, true),
                        EntityDataUtil.getLevel(vehicleLentity, true),
                        EntityDataUtil.getMaxLevel(vehicleLentity, true)
                    );
                }
            }

            if(!vehicleEntity.isInsideVehicle()) continue;

            vehicleEntity = Objects.requireNonNull(vehicleEntity.getVehicle(), "vehicle");
        }

        // No level could be inherited, so return null.
        return null;
    }

    /**
     * TODO document.
     *
     * @param context TODO doc
     * @return TODO doc
     */
    @Nullable
    public TriLevel processFormula(final @NotNull Context context) {
        Objects.requireNonNull(context, "context");

        // check if the mob should have no level
        if(getFormula().equalsIgnoreCase("no-level")) {
            // null = no level
            return null;
        }

        // replace context placeholders in the formula
        String formula = context.replacePlaceholders(getFormula());

        int minLevel = getMinPossibleLevel();
        int maxLevel = getMinPossibleLevel();

        // replace levelling strategy placeholders in the formula
        for(final LevellingStrategy strategy : getStrategies()) {
            formula = strategy.replaceInFormula(formula, context);

            minLevel = Math.min(minLevel, strategy.getMinLevel());
            maxLevel = Math.max(maxLevel, strategy.getMaxLevel());
        }

        // evaluate the formula with Crunch, don't allow values below the min possible level
        final int level = Math.max(
            (int) Math.round(Crunch.evaluateExpression(formula)),
            getMinPossibleLevel()
        );

        return new TriLevel(minLevel, level, maxLevel);
    }

    /* getters and setters */

    public Set<LevellingStrategy> getStrategies() {
        return strategies;
    }

    // TODO let's move this into a more accessible area - SettingsCfg class?
    public static int getMinPossibleLevel() {
        // we don't want negative values as they create undefined game behaviour
        return Math.max(0, LevelledMobs.getInstance()
            .getConfigHandler().getSettingsCfg()
            .getRoot().node("advanced", "minimum-level").getInt(1)
        );
    }

    @NotNull
    public String getFormula() {
        return formula;
    }

    public boolean useInheritanceIfAvailable() {
        return useInheritanceIfAvailable;
    }

    public String getInheritanceTransformationFormula() {
        return inheritanceTransformationFormula;
    }

    public String getInheritanceBreedingFormula() {
        return inheritanceBreedingFormula;
    }
}
