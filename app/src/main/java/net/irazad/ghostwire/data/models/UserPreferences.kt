package net.irazad.ghostwire.data.models

enum class AppTheme {
    DARK_SHIELD,
    LIGHT_CYBER
}

enum class AppLanguage {
    EN,
    FA
}

enum class SplitTunnelMode {
    ALL,
    PROXY_SELECTED,
    BYPASS_SELECTED
}

data class UserPreferences(
    val language: AppLanguage = AppLanguage.FA,
    val theme: AppTheme = AppTheme.DARK_SHIELD,
    val autoMtu: Boolean = true,
    val manualMtu: Int = 1400,
    val desyncEnabled: Boolean = true,
    val resolverHopping: Boolean = true,
    val survivalMode: Boolean = false,
    val backgroundStripper: Boolean = true,
    val zstdCompression: Boolean = true,
    val selectedProfileId: String = "builtin_alpha_reality",
    val splitTunnelMode: SplitTunnelMode = SplitTunnelMode.ALL,
    val splitTunnelApps: Set<String> = emptySet()
)
