#!/usr/bin/env kotlin

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

val reportPaths = System.getenv("REPORT_PATHS").toString().split(",")
val outputFilePath: String = System.getenv("OUTPUT_FILE_PATH").toString()
val passThreshold: Double = System.getenv("PASS_THRESHOLD").toDouble()

main(reportPaths)

fun main(reportPaths: List<String>) {
    val factory = DocumentBuilderFactory.newInstance()
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false) // report.dtdエラースキップ
    val builder = factory.newDocumentBuilder()

    val reports = reportPaths.map { path ->
        val document: Document = builder.parse(path)

        val root = document.documentElement
        val layerName = root.getAttribute("name")

        val packageElements = root.getElementsByTagName("package")
        val packageCoverages = (0 until packageElements.length).map { packageIndex ->
            val packageElement = packageElements.item(packageIndex) as Element
            val packageName = packageElement.getAttribute("name").split("/").last()
            val sourceFileElements = packageElement.getElementsByTagName("sourcefile")

            val sourceFileCoverages = (0 until sourceFileElements.length).map { sourceFileIndex ->
                val sourceFileElement = sourceFileElements.item(sourceFileIndex) as Element
                val sourceFileName = sourceFileElement.getAttribute("name")
                val counterElements = sourceFileElement.getElementsByTagName("counter")

                val coverages = (0 until counterElements.length).map { counterIndex ->
                    val counterElement = counterElements.item(counterIndex) as Element
                    val missed = counterElement.getAttribute("missed").toDouble()
                    val covered = counterElement.getAttribute("covered").toDouble()
                    val coverageType = CoverageType.of(counterElement.getAttribute("type"))

                    Coverage(
                        type = coverageType,
                        missed = missed,
                        covered = covered,
                    )
                }

                SourceFile(sourceFileName, coverages)
            }

            Package(packageName, sourceFileCoverages)
        }

        Layer(layerName, packageCoverages)
    }

    val prefix =
        listOf(
            "## Summary\n",
            "| layer | c0 | c1 | pass/fail |",
            "| --- | ---: | ---: | :---: |",
        )
    val summary = prefix + reports.joinToString("\n") { it.overAll() }
    val detail = reports.joinToString("\n") { it.toTable() }

    val file = File(outputFilePath)
    file.writeText(listOf(summary, detail).joinToString("\n"), Charsets.UTF_8)
}

enum class CoverageType(
    private val type: String,
) {
    INSTRUCTION("INSTRUCTION"),
    BRANCH("BRANCH"),
    LINE("LINE"),
    COMPLEXITY("COMPLEXITY"),
    METHOD("METHOD"),
    CLASS("CLASS"),
    UNDEFINED(""),
    ;

    fun isInstruction() = this == INSTRUCTION
    fun isBranch() = this == BRANCH
    fun isUndefined() = this == UNDEFINED

    companion object {
        fun of(type: String?) =
            entries.find { it.type == type } ?: UNDEFINED
    }
}

data class Coverage(
    val type: CoverageType,
    val missed: Double,
    val covered: Double,
) {
    fun coverage(): Double =
        if (missed + covered > 0) (covered / (missed + covered)) * 100
        else 0.0

    fun isPassed(threshold: Double) = coverage() > threshold

    companion object {
        val EMPTY = Coverage(
            type = CoverageType.UNDEFINED,
            missed = 0.0,
            covered = 0.0,
        )
    }
}

data class SourceFile(
    val name: String,
    val coverages: List<Coverage>,
)

data class Package(
    val name: String,
    val sourceFiles: List<SourceFile>,
)

data class Layer(
    val name: String,
    val packages: List<Package>,
) {
    fun overAll(): String {
        val c0Missed =
            packages.sumOf { pkg ->
                pkg.sourceFiles.sumOf { sourceFile ->
                    sourceFile.coverages.firstOrNull { it.type.isInstruction() }?.missed ?: 0.0
                }
            }
        val c0Covered =
            packages.sumOf { pkg ->
                pkg.sourceFiles.sumOf { sourceFile ->
                    sourceFile.coverages.firstOrNull { it.type.isInstruction() }?.covered ?: 0.0
                }
            }

        val c1Missed =
            packages.sumOf { pkg ->
                pkg.sourceFiles.sumOf { sourceFile ->
                    sourceFile.coverages.firstOrNull { it.type.isBranch() }?.missed ?: 0.0
                }
            }
        val c1Covered =
            packages.sumOf { pkg ->
                pkg.sourceFiles.sumOf { sourceFile ->
                    sourceFile.coverages.firstOrNull { it.type.isBranch() }?.covered ?: 0.0
                }
            }

        val c0Coverage =
            if (c0Missed + c0Covered > 0) (c0Covered / (c0Missed + c0Covered)) * 100
            else 0.0

        val c1Coverage =
            if (c1Missed + c1Covered > 0) (c1Covered / (c1Missed + c1Covered)) * 100
            else 0.0

        val isPassed =
            if (c0Coverage > passThreshold) ":green_circle:"
            else ":red_circle:"

        return "| $name | ${"%3.2f".format(c0Coverage)} | ${"%3.2f".format(c1Coverage)} | $isPassed |"
    }

    fun toTable(): String {
        val prefix =
            listOf(
                "<details>",
                "<summary>${name}</summary>\n",
                "| package | source | c0 | c1 | pass/fail |",
                "| --- | --- | ---: | ---: | :---: |",
            )
        val postfix = listOf("</details>")

        val table =
            prefix + packages.map { pkg ->
                pkg.sourceFiles.map { sourceFile ->
                    val c0Coverage = sourceFile.coverages.firstOrNull { it.type.isInstruction() } ?: Coverage.EMPTY
                    val c1Coverage = sourceFile.coverages.firstOrNull { it.type.isBranch() } ?: Coverage.EMPTY

                    val isPassed =
                        if (c0Coverage.isPassed(passThreshold)) ":green_circle:"
                        else ":red_circle:"

                    "| ${pkg.name} " +
                            "| ${sourceFile.name} " +
                            "| ${"%3.2f".format(c0Coverage.coverage())} " +
                            "| ${"%3.2f".format(c1Coverage.coverage())} " +
                            "| $isPassed " +
                            "|"
                }
            }.flatten() + postfix

        return table.joinToString("\n")
    }
}