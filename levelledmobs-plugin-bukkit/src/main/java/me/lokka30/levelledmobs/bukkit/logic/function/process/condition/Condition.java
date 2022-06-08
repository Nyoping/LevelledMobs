package me.lokka30.levelledmobs.bukkit.logic.function.process.condition;

import java.util.Objects;
import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import me.lokka30.levelledmobs.bukkit.logic.function.process.Process;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public abstract class Condition {

    /* vars */

    private final Process process;
    private final CommentedConfigurationNode conditionNode;

    /* constructors */

    public Condition(final Process process, final @NotNull CommentedConfigurationNode conditionNode) {
        this.process = Objects.requireNonNull(process, "process");
        this.conditionNode = Objects.requireNonNull(conditionNode, "node");
    }

    /* methods */

    public abstract boolean applies(final Context context);

    /* getters and setters */

    @NotNull
    public Process getProcess() { return process; }

    @NotNull
    public CommentedConfigurationNode getConditionNode() { return conditionNode; }

}
