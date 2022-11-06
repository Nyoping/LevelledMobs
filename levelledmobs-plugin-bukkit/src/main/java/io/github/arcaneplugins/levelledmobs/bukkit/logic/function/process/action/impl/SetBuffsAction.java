package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl;

import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.Action;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import io.github.arcaneplugins.levelledmobs.bukkit.util.modal.impl.ModalAttributeSet;
import io.github.arcaneplugins.levelledmobs.bukkit.util.modal.impl.ModalEntityTypeSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import redempt.crunch.Crunch;

/*
TODO Javadoc
 */
public class SetBuffsAction extends Action {

    /*
    TODO: Add buffs
        - CUSTOM_RANGED_ATTACK_DAMAGE
        - CUSTOM_CREEPER_BLAST_DAMAGE
        - CUSTOM_ITEM_DROP
        - CUSTOM_XP_DROP
     */

    private final Set<Buff> buffs = new HashSet<>();
    private final boolean enabled;


    /*
    TODO Javadoc
     */
    public SetBuffsAction(
        @NotNull Process parentProcess,
        @NotNull CommentedConfigurationNode actionNode
    ) {
        super(parentProcess, actionNode);

        this.enabled = getActionNode().node("enabled").getBoolean(true);

        for(final CommentedConfigurationNode buffNode : getActionNode()
            .node("buffs").childrenList()
        ) {
            getBuffs().add(new Buff(buffNode));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(Context context) {
        if(!isEnabled()) { return; }

        if(context.getEntity() == null) {
            throw new IllegalStateException(
                "SetBuffsAction at path '" + getActionNode().path() + "' attempted to run " +
                    "without an entity context."
            );
        }
        if(!(context.getEntity() instanceof final LivingEntity entity)) {
            throw new IllegalStateException(
                "SetBuffsAction at path '" + getActionNode().path() + "' attempted to run " +
                    "without a (living) entity context."
            );
        }

        getBuffs().forEach(buff -> buff.apply(context, entity));
    }

    public Set<Buff> getBuffs() {
        return buffs;
    }

    public boolean isEnabled() {
        return enabled;
    }

    private static class Buff {

        private final CommentedConfigurationNode node;
        private final boolean enabled;
        private final ModalEntityTypeSet affectedEntities;
        private final ModalAttributeSet attributes;
        private final String multiplierFormula;
        private final boolean adjustCurrentHealth;

        public Buff(
            final CommentedConfigurationNode node
        ) {
            this.node = node;

            this.enabled = getNode().node("enabled").getBoolean(true);

            this.affectedEntities =
                ModalEntityTypeSet.parseNode(getNode().node("affected-entities"));

            this.attributes =
                ModalAttributeSet.parseNode(getNode().node("attributes"));

            this.multiplierFormula =
                getNode().node("multiplier-formula").getString("1");

            this.adjustCurrentHealth =
                getNode().node("adjust-current-health").getBoolean(true);
        }

        public void apply(
            final @NotNull Context context,
            final @NotNull LivingEntity entity
        ) {
            if(!isEnabled()) return;

            if(!EntityDataUtil.isLevelled(entity, true)) {
                Log.sev("SetBuffsAction: Mob is not levelled!", true);
                return;
            }

            if(!getAffectedEntities().contains(entity.getType())) {
                return;
            }

            final double multiplier = Crunch.evaluateExpression(
                LogicHandler.replacePapiAndContextPlaceholders(getMultiplierFormula(), context)
            );

            for(final Attribute attribute : Attribute.values()) {
                if(!getAttributes().contains(attribute)) continue;

                final AttributeInstance attinstance = entity.getAttribute(attribute);
                if(attinstance == null) { continue; }

                final double currentValue = attinstance.getValue();

                final boolean shouldAdjustHealth =
                    attribute == Attribute.GENERIC_MAX_HEALTH && shouldAdjustCurrentHealth();

                double healthRatio = 0.0d;

                if(shouldAdjustHealth) {
                    healthRatio = entity.getHealth() / currentValue;
                }

            /*
            Bukkit doesn't have Operation.MULTIPLY, so we have to implement it ourselves.
            The formula Bukkit sees is: CurrentValue + (Multiplier * CurrentValue) - CurrentValue
            That simplifies to: Multiplier * CurrentValue
            That's how we essentially make Operation.MULTIPLIER out of Operation.ADD_NUMBER.
             */

                attinstance.addModifier(new AttributeModifier(
                    "levelledmobs:multiplier." + attribute.toString().toLowerCase(Locale.ROOT),
                    (multiplier * currentValue) - currentValue,
                    Operation.ADD_NUMBER
                ));

                if(shouldAdjustHealth) {
                    entity.setHealth(healthRatio * attinstance.getValue());
                }
            }
        }

        public CommentedConfigurationNode getNode() {
            return node;
        }

        public boolean isEnabled() {
            return enabled;
        }

        private ModalEntityTypeSet getAffectedEntities() {
            return affectedEntities;
        }

        private ModalAttributeSet getAttributes() {
            return attributes;
        }

        public String getMultiplierFormula() {
            return multiplierFormula;
        }

        public boolean shouldAdjustCurrentHealth() {
            return adjustCurrentHealth;
        }

    }

}
