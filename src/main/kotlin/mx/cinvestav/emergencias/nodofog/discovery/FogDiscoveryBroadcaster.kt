package mx.cinvestav.emergencias.nodofog.discovery

import com.google.gson.Gson
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

@Component
class FogDiscoveryBroadcaster(
    @Value("\${fog.node-id}") private val nodeId: String,
    @Value("\${server.port:8080}") private val puerto: Int
) {
    private val log = LoggerFactory.getLogger(FogDiscoveryBroadcaster::class.java)
    private val gson = Gson()
    private var job: Thread? = null
    @Volatile private var activo = true

    @PostConstruct
    fun iniciar() {
        job = Thread {
            val socket = DatagramSocket()
            socket.broadcast = true
            val payload = gson.toJson(mapOf(
                "tipo" to "SACES_FOG",
                "nodeId" to nodeId,
                "puerto" to puerto
            )).toByteArray()

            while (activo) {
                try {
                    val packet = DatagramPacket(
                        payload, payload.size,
                        InetAddress.getByName("255.255.255.255"), 9876
                    )
                    socket.send(packet)
                } catch (e: Exception) {
                    log.warn("Error en broadcast UDP: {}", e.message)
                }
                Thread.sleep(5000)
            }
            socket.close()
        }
        job?.isDaemon = true
        job?.start()
        log.info("FogDiscoveryBroadcaster iniciado — broadcast cada 5s en puerto 9876")
    }

    @PreDestroy
    fun detener() {
        activo = false
        log.info("FogDiscoveryBroadcaster detenido")
    }
}
