package net.irazad.ghostwire.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.irazad.ghostwire.data.models.AppLanguage
import net.irazad.ghostwire.data.models.AppTheme
import net.irazad.ghostwire.data.models.SplitTunnelMode
import net.irazad.ghostwire.data.models.UserPreferences

class UserPreferencesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ghostwire_user_prefs", Context.MODE_PRIVATE)

    private val _userPreferences = MutableStateFlow(loadPreferences())
    val userPreferences: StateFlow<UserPreferences> = _userPreferences.asStateFlow()

    private fun loadPreferences(): UserPreferences {
        val langStr = prefs.getString("language", AppLanguage.FA.name) ?: AppLanguage.FA.name
        val themeStr = prefs.getString("theme", AppTheme.DARK_SHIELD.name) ?: AppTheme.DARK_SHIELD.name
        val splitModeStr = prefs.getString("split_tunnel_mode", SplitTunnelMode.ALL.name) ?: SplitTunnelMode.ALL.name
        val splitApps = prefs.getStringSet("split_tunnel_apps", emptySet()) ?: emptySet()

        return UserPreferences(
            language = runCatching { AppLanguage.valueOf(langStr) }.getOrDefault(AppLanguage.FA),
            theme = runCatching { AppTheme.valueOf(themeStr) }.getOrDefault(AppTheme.DARK_SHIELD),
            autoMtu = prefs.getBoolean("auto_mtu", true),
            manualMtu = prefs.getInt("manual_mtu", 1400),
            desyncEnabled = prefs.getBoolean("desync_enabled", true),
            resolverHopping = prefs.getBoolean("resolver_hopping", true),
            survivalMode = prefs.getBoolean("survival_mode", false),
            backgroundStripper = prefs.getBoolean("background_stripper", true),
            zstdCompression = prefs.getBoolean("zstd_compression", true),
            selectedProfileId = prefs.getString("selected_profile_id", "builtin_alpha_reality") ?: "builtin_alpha_reality",
            splitTunnelMode = runCatching { SplitTunnelMode.valueOf(splitModeStr) }.getOrDefault(SplitTunnelMode.ALL),
            splitTunnelApps = splitApps
        )
    }

    fun updateSplitTunnelMode(mode: SplitTunnelMode) {
        prefs.edit().putString("split_tunnel_mode", mode.name).apply()
        _userPreferences.value = _userPreferences.value.copy(splitTunnelMode = mode)
    }

    fun updateSplitTunnelApps(apps: Set<String>) {
        prefs.edit().putStringSet("split_tunnel_apps", apps).apply()
        _userPreferences.value = _userPreferences.value.copy(splitTunnelApps = apps)
    }

    fun toggleAppInSplitTunnel(packageName: String) {
        val current = _userPreferences.value.splitTunnelApps.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        updateSplitTunnelApps(current)
    }

    fun updateLanguage(language: AppLanguage) {
        prefs.edit().putString("language", language.name).apply()
        _userPreferences.value = _userPreferences.value.copy(language = language)
    }

    fun updateTheme(theme: AppTheme) {
        prefs.edit().putString("theme", theme.name).apply()
        _userPreferences.value = _userPreferences.value.copy(theme = theme)
    }

    fun updateAutoMtu(enabled: Boolean) {
        prefs.edit().putBoolean("auto_mtu", enabled).apply()
        _userPreferences.value = _userPreferences.value.copy(autoMtu = enabled)
    }

    fun updateManualMtu(mtu: Int) {
        prefs.edit().putInt("manual_mtu", mtu).apply()
        _userPreferences.value = _userPreferences.value.copy(manualMtu = mtu)
    }

    fun updateDesync(enabled: Boolean) {
        prefs.edit().putBoolean("desync_enabled", enabled).apply()
        _userPreferences.value = _userPreferences.value.copy(desyncEnabled = enabled)
    }

    fun updateResolverHopping(enabled: Boolean) {
        prefs.edit().putBoolean("resolver_hopping", enabled).apply()
        _userPreferences.value = _userPreferences.value.copy(resolverHopping = enabled)
    }

    fun updateSurvivalMode(enabled: Boolean) {
        prefs.edit().putBoolean("survival_mode", enabled).apply()
        _userPreferences.value = _userPreferences.value.copy(survivalMode = enabled)
    }

    fun updateBackgroundStripper(enabled: Boolean) {
        prefs.edit().putBoolean("background_stripper", enabled).apply()
        _userPreferences.value = _userPreferences.value.copy(backgroundStripper = enabled)
    }

    fun updateZstdCompression(enabled: Boolean) {
        prefs.edit().putBoolean("zstd_compression", enabled).apply()
        _userPreferences.value = _userPreferences.value.copy(zstdCompression = enabled)
    }

    fun updateSelectedProfileId(profileId: String) {
        prefs.edit().putString("selected_profile_id", profileId).apply()
        _userPreferences.value = _userPreferences.value.copy(selectedProfileId = profileId)
    }
}
