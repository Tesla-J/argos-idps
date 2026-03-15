package ao.argosidps.capture

import ao.argosidps.colors.BLUE
import ao.argosidps.colors.RESET
import ao.argosidps.configurations.Configuration
import org.pcap4j.packet.IpV4Packet
import org.pcap4j.packet.TcpPacket
import org.pcap4j.packet.UdpPacket
import org.pcap4j.packet.namednumber.IpNumber
import java.net.InetAddress
import javax.swing.text.FlowView
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

private val flows = HashMap<Int, Array<Double>>()
private val startTimestamps = HashMap<Int, Long>()
private val flowArrivalTimestamps = HashMap<Int, MutableList<Long>>()
private val flowBytesTotal = HashMap<Int, Long>()
private val flowPacketsTotal = HashMap<Int, Int>()

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

private fun printFlow(flowId: Int){
    println("""${BLUE}
        |Flow Duration:             ${flows[flowId]!![0]} seconds
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
        else -> null // TODO add ICMP support later
    }
    val intervalInSeconds: Double

    if (payload == null) return
    if (flow == null){
        startTimestamps[flowId] = timestampAfterCapture
        flowBytesTotal[flowId] = packet.rawData.size.toLong()
        flowPacketsTotal[flowId] = 1
        flowArrivalTimestamps[flowId] = mutableListOf(timestampAfterCapture)
        flows[flowId] = arrayOf(
            (timestampAfterCapture - timestampBeforeCapture).toDouble() / 1000, // Flow Duration TODO I'n not sure
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
        printFlow(flowId)
        return
    }
    intervalInSeconds = (timestampAfterCapture - startTimestamps[flowId]!!) / 1000.0
    flows[flowId]!![0] = (timestampAfterCapture - startTimestamps[flowId]!!).toDouble() / 1000 // Flow Duration
    flowBytesTotal[flowId] = flowBytesTotal.getValue(flowId) + packet.rawData.size.toLong()
    flows[flowId]!![1] = flowBytesTotal[flowId]!!.toDouble() / intervalInSeconds // Bytes/s
    flowPacketsTotal[flowId] = flowPacketsTotal[flowId]!! + 1
    flows[flowId]!![2] =  flowPacketsTotal[flowId]!! / intervalInSeconds // Packets/s
    flows[flowId]!![3] = if (isFwd) flows[flowId]!![3] + 1 else flows[flowId]!![3] // Total FWD Packets
    flows[flowId]!![4] = if (!isFwd) flows[flowId]!![4] + 1 else flows[flowId]!![4] // Total Backward Packets
    flows[flowId]!![5] = flowBytesTotal[flowId]!!.toDouble() / flowPacketsTotal[flowId]!! // Average Packet sIZE
    flows[flowId]!![6] = if (isFwd) max(flows[flowId]!![6], packet.rawData.size.toDouble()) else flows[flowId]!![6] // FWD packed length max
    flows[flowId]!![7] = if (!isFwd) max(flows[flowId]!![7], packet.rawData.size.toDouble()) else flows[flowId]!![7] // BWD packed length max
    flows[flowId]!![8] = flows[flowId]!![8] + if (payload is TcpPacket && payload.header.syn) 1 else 0 // SYN flag count
    flows[flowId]!![9] = flows[flowId]!![9] + if (payload is TcpPacket && payload.header.fin) 1 else 0 // FIN flag count
    flows[flowId]!![10] = flows[flowId]!![10] + if (payload is TcpPacket && payload.header.rst) 1 else 0 // RST flag count
    flowArrivalTimestamps[flowId]!!.add(timestampAfterCapture)
    val totalArrivals = flowArrivalTimestamps[flowId]!!.size
    val iat = flowArrivalTimestamps[flowId]!!.zipWithNext { first, second -> second - first}
    flows[flowId]!![11] = iat.average() // Flow IAT mean
    flows[flowId]!![12] = iat.std() // Flow IAT Std
    printFlow(flowId)
}

suspend fun getTuple(packet: IpV4Packet): Array<String> {
    val srcAddr = packet.header.srcAddr
    val dstAddr = packet.header.dstAddr
    val protocol = packet.header.protocol
    val payload = if (protocol == IpNumber.UDP)
        packet.get(UdpPacket::class.java)
    else if (protocol == IpNumber.TCP)
        packet.get(TcpPacket::class.java)
    else
        null // TODO add ICMP support later
    val srcPort = payload?.header?.srcPort?.valueAsInt()
    val dstPort = payload?.header?.dstPort?.valueAsInt()
    //if (payload == null)
    //    return ""
    return arrayOf(srcAddr.hostAddress, srcPort.toString(), dstAddr.hostAddress, dstPort.toString(), protocol.name())
}
