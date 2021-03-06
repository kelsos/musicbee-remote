package com.kelsos.mbrc.networking.connections

import androidx.lifecycle.LiveData
import arrow.core.Option
import com.kelsos.mbrc.networking.discovery.DiscoveryStop

interface ConnectionRepository {
  suspend fun save(settings: ConnectionSettingsEntity)

  suspend fun delete(settings: ConnectionSettingsEntity)

  fun getAll(): LiveData<List<ConnectionSettingsEntity>>

  suspend fun count(): Long

  fun getDefault(): Option<ConnectionSettingsEntity>

  fun setDefault(settings: ConnectionSettingsEntity)

  fun defaultSettings(): LiveData<ConnectionSettingsEntity?>

  suspend fun discover(): DiscoveryStop
}