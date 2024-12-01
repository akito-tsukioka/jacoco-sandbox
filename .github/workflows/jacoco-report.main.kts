@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("com.google.code.gson:gson:2.8.9")

import com.google.gson.Gson
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import javax.xml.parsers.DocumentBuilderFactory


val commentUrl: String = System.getenv("COMMENT_URL")
val githubToken: String = System.getenv("GITHUB_TOKEN")
val reportPaths = System.getenv("REPORT_PATHS").toString().split(",")

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

    val body = reports.joinToString("\n") { it.toTable() }
    val json = Gson().toJson(body)
    println(json)

    val request = HttpRequest.newBuilder()
        .uri(URI.create(commentUrl))
        .header("Content-Type", "application/json")
        .header("Authorization", "token $githubToken")
        .POST(HttpRequest.BodyPublishers.ofString(json))
        .build()

    val response = HttpClient.newHttpClient()
        .send(request, HttpResponse.BodyHandlers.ofString())

    println(response)
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

    fun toTable(): String {
        val prefix =
            listOf(
                "<details>",
                "<summary>${name}</summary>",
                "| Package | Source | C0 | C1 |",
                "| --- | --- | --- | --- |",
            )
        val postfix = listOf("</details>")

        val table =
            prefix + packages.map { pkg ->
                pkg.sourceFiles.map { sourceFile ->
                    val c0Coverage =
                        "%3.2f".format(
                            sourceFile.coverages.firstOrNull { it.type.isInstruction() }?.coverage() ?: 0.0
                        )
                    val c1Coverage =
                        "%3.2f".format(
                            sourceFile.coverages.firstOrNull { it.type.isBranch() }?.coverage() ?: 0.0
                        )

                    "| ${pkg.name} | ${sourceFile.name} | ${c0Coverage} | ${c1Coverage} |"
                }
            }.flatten() + postfix

        return table.joinToString("\n")
    }
}