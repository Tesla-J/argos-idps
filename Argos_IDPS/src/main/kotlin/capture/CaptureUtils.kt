package ao.argosidps.capture

import ao.argosidps.configurations.Configuration
import ao.argosidps.display.DisplayState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import org.pcap4j.core.PcapNetworkInterface
import org.pcap4j.core.Pcaps
import org.pcap4j.packet.IpV4Packet
import org.pcap4j.packet.TcpPacket
import org.pcap4j.packet.UdpPacket
import org.pcap4j.packet.namednumber.IpNumber
import java.io.File
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter



suspend fun startCapture() {
    val nif: PcapNetworkInterface = Pcaps.getDevByAddress(InetAddress.getByName(Configuration.nifAddr))
    val snapLen = Int.MAX_VALUE
    val mode: PcapNetworkInterface.PromiscuousMode = PcapNetworkInterface.PromiscuousMode.PROMISCUOUS
    val timeout = 10
    val handle = nif.openLive(snapLen, mode, timeout)
    var beforeCaptureTimestamp: Long
    var afterCaptureTimestamp: Long
    val dumpPath = "/var/log/argos"
    File(dumpPath).mkdirs()
    val dateFormater = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")
    val dumpFile = "${dumpPath}/captures_${LocalDateTime.now().format(dateFormater)}.pcap"
    val dumper = handle.dumpOpen(dumpFile)

    println("Capturing from ${nif.name}")
    
    try {
        while (!DisplayState.quit.get()) {
            val packet = handle.nextPacket
            if (packet == null) continue
            
            try {
                // Get timestamp immediately after receiving packet
                var packetTimestamp = handle.timestamp
                // Use system timestamp if packet timestamp is null
                if (packetTimestamp == null) {
                    packetTimestamp = Timestamp(System.currentTimeMillis())
                }
                
                // Process packet synchronously to prevent coroutine accumulation
                beforeCaptureTimestamp = packetTimestamp.time
                val ipv4Packet = packet.get<IpV4Packet>(IpV4Packet::class.java)
                afterCaptureTimestamp = packetTimestamp.time
                
                if (ipv4Packet != null) {
                    withContext(Dispatchers.IO) {
                        updateFlowStats(ipv4Packet, beforeCaptureTimestamp, afterCaptureTimestamp)
                    }
                }
                
                withContext(Dispatchers.IO) {
                    dumper.dump(packet, packetTimestamp)
                }
            } catch (e: Exception) {
                // Log error but continue processing
                // Suppress verbose error output for cleaner display
                if (e.message?.contains("ts: null") != true) {
                    System.err.println("Error processing packet: ${e.message}")
                }
                // Continue processing next packet on error
            }
        }
    } finally {
        try {
            dumper.close()
        } catch (e: Exception) {
            System.err.println("Error closing dumper: ${e.message}")
        }
        try {
            handle.close()
        } catch (e: Exception) {
            System.err.println("Error closing handle: ${e.message}")
        }
    }
}