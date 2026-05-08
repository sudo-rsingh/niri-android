package com.niri.launcher

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import com.niri.launcher.data.NiriTile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NiriStateHolder(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("niri_tiles", Context.MODE_PRIVATE)
    private val pm: PackageManager = context.packageManager

    private val _tiles = MutableStateFlow<List<NiriTile>>(emptyList())
    val tiles: StateFlow<List<NiriTile>> = _tiles.asStateFlow()

    private val _focusedPackage = MutableStateFlow<String?>(null)
    val focusedPackage: StateFlow<String?> = _focusedPackage.asStateFlow()

    private val _isPickerOpen = MutableStateFlow(false)
    val isPickerOpen: StateFlow<Boolean> = _isPickerOpen.asStateFlow()

    init {
        loadTiles()
    }

    fun addTile(tile: NiriTile) {
        if (_tiles.value.none { it.packageName == tile.packageName }) {
            _tiles.value = _tiles.value + tile
            saveTiles()
        }
    }

    fun removeTile(id: String) {
        _tiles.value = _tiles.value.filter { it.id != id }
        saveTiles()
    }

    fun setFocusedPackage(pkg: String?) { _focusedPackage.value = pkg }
    fun openPicker()  { _isPickerOpen.value = true }
    fun closePicker() { _isPickerOpen.value = false }

    private fun saveTiles() {
        val value = _tiles.value.joinToString(",") { "${it.id}|${it.packageName}|${it.activityName}" }
        prefs.edit().putString("tiles", value).apply()
    }

    private fun loadTiles() {
        val raw = prefs.getString("tiles", "") ?: return
        if (raw.isBlank()) return
        _tiles.value = raw.split(",").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size != 3) return@mapNotNull null
            val (id, pkg, activity) = parts
            runCatching {
                val info = pm.getActivityInfo(
                    android.content.ComponentName(pkg, activity), 0
                )
                NiriTile(
                    id = id,
                    packageName = pkg,
                    activityName = activity,
                    label = info.loadLabel(pm).toString(),
                    icon = info.loadIcon(pm),
                )
            }.getOrNull()
        }
    }
}
