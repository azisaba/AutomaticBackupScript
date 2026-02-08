package net.azisaba.automaticbackupscript.config

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlIndentation
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import net.azisaba.automaticbackupscript.Main
import net.azisaba.automaticbackupscript.util.ProcessExecutor
import java.io.File

@Serializable
data class CoreConfig(
    val preExecuteScript: List<List<String>> = listOf(listOf("echo", "pre-execute script!")),
) {
    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        val toml =
            Toml(
                inputConfig =
                    TomlInputConfig(
                        ignoreUnknownNames = true,
                        allowEmptyValues = true,
                        allowNullValues = true,
                        allowEscapedQuotesInLiteralStrings = true,
                        allowEmptyToml = true,
                    ),
                outputConfig =
                    TomlOutputConfig(
                        indentation = TomlIndentation.NONE,
                    ),
            )

        val config: CoreConfig =
            File(Main.configFile).let { file ->
                if (!file.parentFile.exists()) file.parentFile.mkdirs()
                if (!file.exists()) file.writeText(toml.encodeToString(CoreConfig()))
                toml.decodeFromString(serializer(), file.readText())
            }
    }

    fun executePreExecuteScript() {
        preExecuteScript.forEach { script ->
            ProcessExecutor.executeCommandStreamedOutput(File("."), *script.toTypedArray())
        }
    }
}
