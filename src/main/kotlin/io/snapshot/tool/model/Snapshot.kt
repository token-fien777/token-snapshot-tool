package io.snapshot.tool.model

/**
 * @author Igor Pahomov
 */
data class Snapshot(
        val address: String?,
        val decimals: Int?,
        val fromBlock: Int?,
        val toBlock: Int?
)