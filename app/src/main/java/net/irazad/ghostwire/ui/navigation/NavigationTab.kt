package net.irazad.ghostwire.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import net.irazad.ghostwire.R

enum class NavigationTab(
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int
) {
    NEXUS(R.string.tab_nexus, R.drawable.ic_nexus),
    RADAR(R.string.tab_radar, R.drawable.ic_radar),
    TACTICAL(R.string.tab_tactical, R.drawable.ic_tactical),
    ENGINE(R.string.tab_engine, R.drawable.ic_engine)
}
