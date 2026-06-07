package ao.argosidps.capture

import ao.argosidps.ai.runAnalysis
import ao.argosidps.colors.BLUE
import ao.argosidps.colors.GREEN
import ao.argosidps.colors.RED
import ao.argosidps.colors.RESET
import ao.argosidps.colors.YELLOW
import ao.argosidps.configurations.Configuration
import ao.argosidps.smtp.sendAlert
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.pcap4j.packet.IcmpV4CommonPacket
import org.pcap4j.packet.IcmpV4EchoPacket
import org.pcap4j.packet.IcmpV4TimestampPacket
import org.pcap4j.packet.IpV4Packet
import org.pcap4j.packet.TcpPacket
import org.pcap4j.packet.UdpPacket
import org.pcap4j.packet.namednumber.IpNumber
import java.net.InetAddress
import javax.swing.plaf.multi.MultiTextUI
import javax.swing.text.FlowView
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

private val flows = ConcurrentHashMap<Int, Array<Double>>()
private val startTimestamps = ConcurrentHashMap<Int, Long>()
private val flowArrivalTimestamps = ConcurrentHashMap<Int, MutableList<Long>>()
private val flowBytesTotal = ConcurrentHashMap<Int, Long>()
private val flowPacketsTotal = ConcurrentHashMap<Int, Int>()
private val flowLocks = ConcurrentHashMap<Int, Mutex>()

fun isNetworkEquals(addr1: String, addr2: String, netmask: String): Boolean{
    val addr1ULong = addr1
        .split(".")
        .map { it.toULong() }
        .reduce {value, element -> (value shl 8) or element}
    val addr2ULong = addr2
        .split(".")
        .map { it.toULong() }
        .reduce {value, element -> (value shl 8) or element}
    val netmaskULong = netmask
        .split(".")
        .map { it.toULong() }
        .reduce {value, element -> (value shl 8) or element}

    return (addr1ULong and netmaskULong) == (addr2ULong and netmaskULong)
}

private fun <T: Number> List<T>.std(): Double {
    val double = this.map {it.toDouble()}
    val mean = double.average()
    val sum = double.sumOf { (it - mean).pow(2) }
    return (sqrt(sum / double.size))
}

private fun printFlow(flowId: Int, analysisResult: String){
    println("""${BLUE}
        |Flow Duration:             ${flows[flowId]!![0]} milliseconds
        |Bytes/s:                   ${flows[flowId]!![1]}
        |Packets/s:                 ${flows[flowId]!![2]}
        |Total FWD packages:        ${flows[flowId]!![3]}
        |Total BWD packages:        ${flows[flowId]!![4]}
        |Average Packet Size:       ${flows[flowId]!![5]}
        |FWD Package Length Max:    ${flows[flowId]!![6]}
        |BWD Package Length Max:    ${flows[flowId]!![7]}
        |SYN Flag Count:            ${flows[flowId]!![8]}
        |FIN Flag Count:            ${flows[flowId]!![9]}
        |RST Flag Count:            ${flows[flowId]!![10]}
        |Flow IAT Mean:             ${flows[flowId]!![11]}
        |Flow IAT Std:              ${flows[flowId]!![12]}
        |Flow Analysis Result:      ${if (analysisResult.contains("ANOMALIA")) RED else GREEN} $analysisResult
        |$RESET
    """.trimMargin())
}

suspend fun updateFlowStats(packet: IpV4Packet, timestampBeforeCapture: Long, timestampAfterCapture: Long){
    val tuple = getTuple(packet)
    val isFwd = isNetworkEquals(tuple[0], Configuration.nifAddr!!, Configuration.nifNetmask!!)
    tuple.sort()
    val flowId = tuple.contentHashCode()
    val flow = flows.get(flowId)
    val payload =  when (packet.header.protocol){
        IpNumber.UDP -> packet.get(UdpPacket::class.java)
        IpNumber.TCP -> packet.get(TcpPacket::class.java)
        IpNumber.ICMPV4 -> packet.get(IcmpV4CommonPacket::class.java)
        else -> null // for other protocols
    }
    var intervalInSeconds: Double

    if (payload == null) return
    if (flow == null){
        flowLocks.getOrPut(flowId) { Mutex() }.withLock {
            startTimestamps[flowId] = timestampAfterCapture
            flowBytesTotal[flowId] = packet.rawData.size.toLong()
            flowPacketsTotal[flowId] = 1
            flowArrivalTimestamps[flowId] = mutableListOf(timestampAfterCapture)
            flows[flowId] = arrayOf(
                (timestampAfterCapture - timestampBeforeCapture).toDouble(), // Flow Duration
                flowBytesTotal[flowId]!!.toDouble(), // Bytes/s
                flowPacketsTotal[flowId]!!.toDouble(), // Packets/s
                if (isFwd) 1.0 else .0, // Total FWD packets
                if (!isFwd) 1.0 else .0, // Total Backward Packets
                packet.rawData.size.toDouble(), // Average Packet Size
                if (isFwd) packet.rawData.size.toDouble() else .0, // FWD packet length max
                if (!isFwd) packet.rawData.size.toDouble() else .0, // BWD packet length max
                if (payload is TcpPacket && payload.header.syn) 1.0 else .0, // SYN flag count
                if (payload is TcpPacket && payload.header.fin) 1.0 else .0, // FIN flag count
                if (payload is TcpPacket && payload.header.rst) 1.0 else .0, // RST flag count
                .0, // Flow IAT mean
                .0, // FLOW IAT Std
            )
        }
        /*
        Why analyse a flow that just started?

        val analysisResult = runAnalysis(flows[flowId]!!)
        if (analysisResult.contains("ANOMALIA")){
            sendAlert(
                analysisResult
                    .substringAfter("\"attach_type\": \"")
                    .substringBefore("\""),
                packet.header.srcAddr.hostAddress,
                captureFilename!!)
        }
        printFlow(flowId, analysisResult)
        */
        return
    }
    flowLocks.getOrPut(flowId) { Mutex() }.withLock {
        intervalInSeconds = (timestampAfterCapture - startTimestamps[flowId]!!) / 1000.0
        intervalInSeconds = if (intervalInSeconds == 0.0) 1.0 else intervalInSeconds
        flows[flowId]!![0] = (timestampAfterCapture - startTimestamps[flowId]!!).toDouble() // Flow Duration
        flowBytesTotal[flowId] = flowBytesTotal.getValue(flowId) + packet.rawData.size.toLong()
        flows[flowId]!![1] = flowBytesTotal[flowId]!!.toDouble() / intervalInSeconds // Bytes/s
        flowPacketsTotal[flowId] = flowPacketsTotal[flowId]!! + 1
        flows[flowId]!![2] = flowPacketsTotal[flowId]!! / intervalInSeconds // Packets/s
        flows[flowId]!![3] = if (isFwd) flows[flowId]!![3] + 1 else flows[flowId]!![3] // Total FWD Packets
        flows[flowId]!![4] = if (!isFwd) flows[flowId]!![4] + 1 else flows[flowId]!![4] // Total Backward Packets
        flows[flowId]!![5] = flowBytesTotal[flowId]!!.toDouble() / flowPacketsTotal[flowId]!! // Average Packet sIZE
        flows[flowId]!![6] = if (isFwd) max(
            flows[flowId]!![6],
            packet.rawData.size.toDouble()
        ) else flows[flowId]!![6] // FWD packed length max
        flows[flowId]!![7] = if (!isFwd) max(
            flows[flowId]!![7],
            packet.rawData.size.toDouble()
        ) else flows[flowId]!![7] // BWD packed length max
        flows[flowId]!![8] =
            flows[flowId]!![8] + if (payload is TcpPacket && payload.header.syn) 1 else 0 // SYN flag count
        flows[flowId]!![9] =
            flows[flowId]!![9] + if (payload is TcpPacket && payload.header.fin) 1 else 0 // FIN flag count
        flows[flowId]!![10] =
            flows[flowId]!![10] + if (payload is TcpPacket && payload.header.rst) 1 else 0 // RST flag count
        flowArrivalTimestamps[flowId]!!.add(timestampAfterCapture)
        val totalArrivals = flowArrivalTimestamps[flowId]!!.size
        val iat = flowArrivalTimestamps[flowId]!!.zipWithNext { first, second -> second - first }
        flows[flowId]!![11] = iat.average() // Flow IAT mean
        flows[flowId]!![12] = iat.std() // Flow IAT Std
    }
    val analysisResult = runAnalysis(flows[flowId]!!)
    if (analysisResult.contains("ANOMALIA")){
        sendAlert(
            analysisResult
                .substringAfter("\"attach_type\": \"")
                .substringBefore("\""),
            packet.header.srcAddr.hostAddress,
            captureFilename!!)
    }
    printFlow(flowId, analysisResult)
}

suspend fun getTuple(packet: IpV4Packet): Array<String> {
    val srcAddr = packet.header.srcAddr
    val dstAddr = packet.header.dstAddr
    val protocol = packet.header.protocol

    return when (protocol){
        IpNumber.UDP -> {
            val udp = packet.get(UdpPacket::class.java)
            arrayOf(
                srcAddr.hostAddress,
                udp?.header?.srcPort.toString(),
                dstAddr.hostAddress,
                udp?.header?.dstPort.toString(),
                protocol.name()
            )
        }

        IpNumber.TCP -> {
            val tcp = packet.get(TcpPacket::class.java)
            arrayOf(
                srcAddr.hostAddress,
                tcp?.header?.srcPort.toString(),
                dstAddr.hostAddress,
                tcp?.header?.dstPort.toString(),
                protocol.name()
            )
        }

        IpNumber.ICMPV4 -> {
            val icmp = packet.get(IcmpV4CommonPacket::class.java)
            val type = icmp?.header?.type?.valueAsString() ?: "0"
            val id = when(val inner = icmp?.payload){
                is IcmpV4EchoPacket -> inner.header.identifier.toInt()
                is IcmpV4TimestampPacket -> inner.header.identifier.toInt()
                else -> 0
            }
            arrayOf(
                srcAddr.hostAddress,
                type,
                dstAddr.hostAddress,
                id.toString(), // why this is not showing error when .toString() is not used?
                protocol.name()
            )
        }

        else -> arrayOf(
            srcAddr.hostAddress,
            "0",
            dstAddr.hostAddress,
            "0",
            protocol.name()
            )
    }
}
