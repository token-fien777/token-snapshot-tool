package io.openfuture.snapshot.exporter

import io.openfuture.snapshot.wrapper.Web3jWrapper
import io.openfuture.snapshot.dto.ExportSnapshotRequest
import org.web3j.abi.datatypes.Address
import java.io.File
import java.io.PrintWriter
import java.math.BigDecimal
import java.util.concurrent.Executors
import kotlin.math.pow

class SnapshotExporter(private val web3j: Web3jWrapper) {

    private val executor = Executors.newFixedThreadPool(POOL_SIZE)

    fun export(request: ExportSnapshotRequest) {

        web3j.init(request.nodeAddress)

        val writer = PrintWriter(request.fileName, "UTF-8")

        writer.println(HEADER)

        for (blockNumber in request.toBlock downTo request.fromBlock step BATCH_SIZE) {
            var nextBatch = blockNumber - BATCH_SIZE + 1
            if (nextBatch <= request.fromBlock) nextBatch = request.fromBlock

            executor.execute {
                println("Batch snapshot from block $blockNumber to block $nextBatch")

                val addresses = web3j.getAddressesFromTransferEvents(request.address, nextBatch, blockNumber)
                println("Fetched addresses ${addresses.size}")

                val balances = getBalancesAtBlock(addresses, request.address, request.toBlock, request.decimals)

                writeResult(balances, writer)

                println("Added ${balances.size} balances from block $nextBatch to block $blockNumber")
            }
        }

        executor.shutdown()
        while (!executor.isTerminated) {
        }

        writer.close()
        checkDuplicateAddresses(request.fileName)
    }

    private fun getBalancesAtBlock(addresses: Set<Address>, tokenAddress: String, blockNumber: Int, decimals: Int): Map<String, BigDecimal> {
        return addresses
                .map {
                    val balance = web3j.getTokenBalanceAtBlock(it.value, tokenAddress, blockNumber)
                    it.value to BigDecimal(balance.toDouble() * 10.0.pow(-decimals.toDouble()))
                }
                .toMap()
    }

    private fun writeResult(results: Map<String, BigDecimal>, writer: PrintWriter) {
        results.forEach { writer.println("${it.key},${it.value}") }
        writer.flush()
    }

    private fun checkDuplicateAddresses(fileName: String) {
        val balances = read(fileName)
        writeResult(balances, fileName)
    }

    private fun read(path: String): Set<List<String>> {
        val lines = File("${System.getProperty("user.dir")}/$path").readLines()

        return lines.map {
            it.split(",")
        }.toSet()
    }

    private fun writeResult(results: Set<List<String?>>, fileName: String) {
        val writer = PrintWriter("clear_$fileName", "UTF-8")
        results.forEach { writer.println("${it[0]},${it[1]}") }
        writer.flush()
        writer.close()
    }

    companion object {
        private const val BATCH_SIZE = 20
        private const val POOL_SIZE = 10
        const val HEADER = "ADDRESS,BALANCE"
    }

}