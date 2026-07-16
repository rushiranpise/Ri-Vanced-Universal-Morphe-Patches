package app.morphe.patches.shared.layout.hide.general

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.all.misc.resources.addResources
import app.morphe.patches.all.misc.resources.addResourcesPatch
import app.morphe.patches.shared.misc.litho.filter.addLithoFilter
import app.morphe.patches.shared.misc.settings.preference.BasePreferenceScreen
import app.morphe.patches.shared.misc.settings.preference.InputType
import app.morphe.patches.shared.misc.settings.preference.PreferenceScreenPreference
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.patches.shared.misc.settings.preference.TextPreference
import kotlin.collections.toTypedArray

internal fun hideLayoutComponentsPatch(
    lithoFilterPatch: Patch<*>,
    settingsPatch: Patch<*>,
    generalSettingsScreen: BasePreferenceScreen.Screen,
    additionalDependencies: Set<Patch<*>> = emptySet(),
    filterClasses: Set<String>,
    vararg compatibleWithPackages: Pair<String, Set<String>?>,
    executeBlock: BytecodePatchContext.() -> Unit = {},
) = bytecodePatch(
    name = "Hide layout components",
    description = "Adds options to hide general layout components.",
) {
    dependsOn(
        lithoFilterPatch,
        settingsPatch,
        *additionalDependencies.toTypedArray(),
        addResourcesPatch,
    )

    compatibleWith(packages = compatibleWithPackages)

    execute {
        addResources("shared", "layout.hide.general.hideLayoutComponentsPatch")

        generalSettingsScreen.addPreferences(
            PreferenceScreenPreference(
                key = "rivanced_custom_filter_screen",
                sorting = PreferenceScreenPreference.Sorting.UNSORTED,
                preferences = setOf(
                    SwitchPreference("rivanced_custom_filter"),
                    TextPreference("rivanced_custom_filter_strings", inputType = InputType.TEXT_MULTI_LINE),
                ),
            ),
        )

        filterClasses.forEach { className -> addLithoFilter(className) }

        executeBlock()
    }
}
