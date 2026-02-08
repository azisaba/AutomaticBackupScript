package net.azisaba.automaticbackupscript.command

import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import kotlinx.cli.default
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.azisaba.automaticbackupscript.config.CoreConfig
import java.io.File

@OptIn(ExperimentalCli::class)
object MigrateCommand : Subcommand("migrate", "Migrate old config") {
    private val oldConfigPath by option(ArgType.String, "old-config-file", "o", "Old configuration file").default("config/backup.json")
    private val newConfigPath by option(ArgType.String, "new-config-file", "n", "New configuration file").default("config/backup.toml")

    @OptIn(ExperimentalSerializationApi::class)
    private val json =
        Json {
            encodeDefaults = true
            prettyPrint = true
            prettyPrintIndent = "  "
            ignoreUnknownKeys = true
        }

    override fun execute() {
        println("Config migrator")
        println("From: $oldConfigPath")
        println("To: $newConfigPath")
        val config = json.decodeFromString(CoreConfig.serializer(), File(oldConfigPath).readText())
        File(newConfigPath).writeText(CoreConfig.toml.encodeToString(config))
        println("Migration completed.")
    }
}
