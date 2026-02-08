package net.azisaba.automaticbackupscript.command

import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import kotlinx.cli.default
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import net.azisaba.automaticbackupscript.Application
import net.azisaba.automaticbackupscript.config.BackupConfig
import net.azisaba.automaticbackupscript.config.CoreConfig
import net.azisaba.automaticbackupscript.config.CoreConfig.Companion.serializer
import net.azisaba.automaticbackupscript.config.CoreConfig.Companion.toml
import java.io.File

@OptIn(ExperimentalCli::class)
object BackupCommand : Subcommand("backup", "Backup files") {
    private val configFile by option(ArgType.String, "core-config-file", "c", "Config file (core)").default("config/core.toml")
    private val configPath by option(ArgType.String, "config-file", "f", "Configuration file").default("config/backup.toml")

    val config: CoreConfig by lazy {
        File(configFile).let { file ->
            if (!file.parentFile.exists()) file.parentFile.mkdirs()
            if (!file.exists()) file.writeText(toml.encodeToString(CoreConfig()))
            toml.decodeFromString(serializer(), file.readText())
        }
    }

    override fun execute() {
        config.executePreExecuteScript()
        val configFile = File(configPath)
        BackupConfig.load(configFile)
        if (BackupConfig.config.webhookUrl == "insert url here") {
            System.err.println("Please edit ${configFile.absolutePath} and run the application!")
            return
        }
        runBlocking {
            Application().backup()
        }
    }
}
