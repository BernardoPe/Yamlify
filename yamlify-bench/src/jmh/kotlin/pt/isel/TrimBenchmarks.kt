package pt.isel

import org.openjdk.jmh.annotations.*

/**
fun String.fastTrim(): String {
    var start = 0
    var end = length - 1
    while (start <= end && this[start].isWhitespace()) start++
    while (end >= start && this[end].isWhitespace()) end--
    return substring(start, end + 1)
}

fun getLineParts(line: String): List<String> {
    val parts = line.split(":")
    return if (parts.size == 1) {
        listOf(parts[0].fastTrim(), "")
    } else {
        listOf(parts[0].fastTrim(), parts[1].fastTrim())
    }
}

fun getLinePartsKotlinTrim(line: String): List<String> {
    val parts = line.split(":")
    return if (parts.size == 1) {
        listOf(parts[0].trim(), "")
    } else {
        listOf(parts[0].trim(), parts[1].trim())
    }
}

@BenchmarkMode(Mode.Throughput)
@State(Scope.Benchmark)
open class lineSplitBenchmarks {

    private val studentLines = listOf(
        "student:",
        "  name: John Doe",
        "  age: 20",
        "  grades:",
        "    - 10",
        "    - 12",
        "    - 14",
        "  address:",
        "    street: Unknown",
        "    city: Nowhere"
    )
    @Benchmark
    fun kotlinTrim(): Unit {
        studentLines.forEach { it.trim() }
    }

    @Benchmark
    fun fastTrim(): Unit {
        studentLines.forEach { it.fastTrim() }
    }

    @Benchmark
    fun normalSplitKotlinTrim(): Unit {
        studentLines.forEach { it.split(":").map { it.trim() } }
    }

    @Benchmark
    fun normalSplitFastTrim(): Unit {
        studentLines.forEach { it.split(":").map { it.fastTrim() } }
    }

    @Benchmark
    fun getLinePartsFastTrim(): Unit {
        studentLines.forEach { getLineParts(it) }
    }

    @Benchmark
    fun getLinePartsKotlinTrim(): Unit {
        studentLines.forEach { getLinePartsKotlinTrim(it) }
    }

}
*/