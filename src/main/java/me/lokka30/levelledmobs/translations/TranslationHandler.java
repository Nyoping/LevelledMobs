/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.translations;

import me.lokka30.levelledmobs.LevelledMobs;
import org.apache.commons.lang.WordUtils;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

/**
 * @author lokka30
 * @since v4.0.0
 * This class handles the text that LevelledMobs
 * allows administrators to translate through the
 * <code>constants.yml</code> and <code>messages.yml</code> files.
 */
public class TranslationHandler {

    private final LevelledMobs main;
    public TranslationHandler(final @NotNull LevelledMobs main) {
        this.main = main;
    }

    /*
    TODO
        - Add javadoc comment.
     */
    public String getTranslatedEntityName(final @NotNull EntityType entityType) {
        return main.getFileHandler().getConstantsFile().getData().getOrDefault(
                "entity-names." + entityType,
                getDefaultEntityName(entityType)
        );
    }

    /*
    TODO
        - Add javadoc comment.
     */
    public String getDefaultEntityName(final @NotNull EntityType entityType) {
        return WordUtils.capitalizeFully(
                entityType.toString()
                        .replace("_", " ")
        );
    }

    /*
    TODO
        - Add javadoc comment.
     */
    public String getTranslatedInteger(final int i) {
        return main.getFileHandler().getConstantsFile().getData().getOrDefault(
                "integers." + i,
                Integer.toString(i)
        );
    }
}
