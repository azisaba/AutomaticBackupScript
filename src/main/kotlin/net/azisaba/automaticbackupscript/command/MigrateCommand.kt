package net.azisaba.automaticbackupscript.command

import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import kotlinx.cli.default
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.azisaba.automaticbackupscript.config.BackupConfig
import net.azisaba.automaticbackupscript.config.CoreConfig
import java.io.File
import kotlin.io.path.nameWithoutExtension

@OptIn(ExperimentalCli::class)
object MigrateCommand : Subcommand("migrate", "Migrate old config") {
    private val coreConfigPath by option(ArgType.String, "Core-config-file", "c", "Core configuration file").default("config/core.json")
    private val backupConfigPath by option(
        ArgType.String,
        "Backup-config-file",
        "b",
        "Backup configuration file",
    ).default("config/backup.json")

    @OptIn(ExperimentalSerializationApi::class)
    private val json =
        Json {
            encodeDefaults = true
            prettyPrint = true
            prettyPrintIndent = "  "
            ignoreUnknownKeys = true
        }

    fun changeExtToToml(old: File): File {
        val p = old.toPath()
        return File(p.parent.toFile(), p.nameWithoutExtension + ".toml")
    }

    override fun execute() {
        println("Config migrator")
        val oldCoreConfig: File = File(coreConfigPath)
        val newCoreConfig: File = changeExtToToml(oldCoreConfig)
        println("From: $oldCoreConfig")
        println("To: $newCoreConfig")
        val coreConfig = json.decodeFromString(CoreConfig.serializer(), oldCoreConfig.readText())
        newCoreConfig.writeText(CoreConfig.toml.encodeToString(coreConfig))
        println("Core config migrated.")

        val oldBackupConfig: File = File(backupConfigPath)
        val newBackupConfig: File = changeExtToToml(oldBackupConfig)
        println("From: $oldBackupConfig")
        println("To: $newBackupConfig")
        val backupConfig = json.decodeFromString(BackupConfig.serializer(), oldBackupConfig.readText())
        newBackupConfig.writeText(CoreConfig.toml.encodeToString(backupConfig))
        println("Backup config migrated.")

        println("All migration completed.")
    }
}
