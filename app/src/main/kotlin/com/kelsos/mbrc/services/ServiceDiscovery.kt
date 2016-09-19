package com.kelsos.mbrc.services

import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import com.fasterxml.jackson.databind.ObjectMapper
import com.kelsos.mbrc.constants.Const
import com.kelsos.mbrc.constants.Protocol
import com.kelsos.mbrc.data.ConnectionSettings
import com.kelsos.mbrc.data.DiscoveryMessage
import com.kelsos.mbrc.enums.DiscoveryStop
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.events.ui.DiscoveryStopped
import com.kelsos.mbrc.mappers.ConnectionMapper
import com.kelsos.mbrc.repository.ConnectionRepository
import rx.Observable
import rx.Subscriber
import rx.functions.Action1
import rx.functions.Func0
import rx.functions.Func1
import rx.schedulers.Schedulers
import timber.log.Timber
import java.io.IOException
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ServiceDiscovery
@Inject
internal constructor(private val manager: WifiManager,
                     private val connectivityManager: ConnectivityManager,
                     private val mapper: ObjectMapper,
                     private val bus: RxBus,
                     private val connectionRepository: ConnectionRepository) {
  private var mLock: WifiManager.MulticastLock? = null
  private var group: InetAddress? = null

  @JvmOverloads fun startDiscovery(callback: OnDiscoveryComplete? = null) {
    if (!isWifiConnected) {
      bus.post(DiscoveryStopped(DiscoveryStop.NO_WIFI))
      return
    }
    mLock = manager.createMulticastLock("locked")
    mLock!!.setReferenceCounted(true)
    mLock!!.acquire()

    Timber.v("Starting remote service discovery")

    val mapper = ConnectionMapper()
    discoveryObservable().subscribeOn(Schedulers.io())
        .unsubscribeOn(Schedulers.io())
        .doOnTerminate({
          this.stopDiscovery()
        }).map<ConnectionSettings>(Func1<DiscoveryMessage, ConnectionSettings> { mapper.map(it) }).subscribe({ settings ->
      bus.post(DiscoveryStopped(DiscoveryStop.COMPLETE))
      connectionRepository.save(settings)

      callback?.complete()

    }) {
      Timber.v(it, "Discovery incomplete")
      bus.post(DiscoveryStopped(DiscoveryStop.NOT_FOUND))
    }
  }

  private fun stopDiscovery() {
    if (mLock != null) {
      mLock!!.release()
      mLock = null
    }
  }

  private val wifiAddress: String
    get() {
      val mInfo = manager.connectionInfo
      val address = mInfo.ipAddress
      return String.format(Locale.getDefault(),
          "%d.%d.%d.%d",
          address and 0xff,
          address shr 8 and 0xff,
          address shr 16 and 0xff,
          address shr 24 and 0xff)
    }

  private val isWifiConnected: Boolean
    get() {
      val current = connectivityManager.activeNetworkInfo
      return current != null && current.type == ConnectivityManager.TYPE_WIFI
    }

  private fun discoveryObservable(): Observable<DiscoveryMessage> {
    return Observable.using<DiscoveryMessage, MulticastSocket>(Func0<MulticastSocket> { this.getResource() }, Func1<MulticastSocket, Observable<out DiscoveryMessage>> { this.getObservable(it) }, Action1<MulticastSocket> { this.cleanup(it) })
  }

  private fun cleanup(resource: MulticastSocket) {
    try {
      resource.leaveGroup(group)
      resource.close()
      stopDiscovery()
    } catch (e: IOException) {
      throw RuntimeException(e)
    }

  }

  private fun getObservable(socket: MulticastSocket): Observable<DiscoveryMessage> {
    return Observable.interval(600, TimeUnit.MILLISECONDS).take(6).flatMap<DiscoveryMessage>({ aLong ->
      Observable.create { subscriber: Subscriber<in DiscoveryMessage> ->
        try {
          val mPacket: DatagramPacket
          val buffer = ByteArray(512)
          mPacket = DatagramPacket(buffer, buffer.size)
          socket.receive(mPacket)
          val incoming = String(mPacket.data, Const.UTF_8)
          val node = mapper.readValue(incoming, DiscoveryMessage::class.java)
          Timber.v("Discovery received -> %s", node)
          subscriber.onNext(node)
          subscriber.onCompleted()
        } catch (e: IOException) {
          subscriber.onError(e)
        }
      }
    }).filter { message -> NOTIFY == message.context }.first()
  }

  private val resource: MulticastSocket
    get() {
      try {
        val multicastSocket = MulticastSocket(MULTICASTPORT)
        multicastSocket.soTimeout = SO_TIMEOUT
        group = InetAddress.getByName(DISCOVERY_ADDRESS)
        multicastSocket.joinGroup(group)
        val message = DiscoveryMessage()
        message.context = Protocol.DISCOVERY
        message.address = wifiAddress
        val discovery = mapper.writeValueAsBytes(message)
        multicastSocket.send(DatagramPacket(discovery, discovery.size, group, MULTICASTPORT))
        return multicastSocket
      } catch (e: IOException) {
        Timber.v(e, "Failed to open multicast socket")
        throw RuntimeException(e)
      }

    }

  interface OnDiscoveryComplete {
    fun complete()
  }

  companion object {
    private val NOTIFY = "notify"
    private val SO_TIMEOUT = 10 * 1000
    private val MULTICASTPORT = 45345
    private val DISCOVERY_ADDRESS = "239.1.5.10" //NOPMD
  }
}
