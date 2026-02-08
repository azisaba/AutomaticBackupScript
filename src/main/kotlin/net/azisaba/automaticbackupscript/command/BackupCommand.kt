package net.azisaba.automaticbackupscript.command

import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import kotlinx.cli.default
import kotlinx.coroutines.runBlocking
import net.azisaba.automaticbackupscript.Application
import net.azisaba.automaticbackupscript.config.BackupConfig
import net.azisaba.automaticbackupscript.config.CoreConfig
import java.io.File

@OptIn(ExperimentalCli::class)
object BackupCommand : Subcommand("backup", "Backup files") {
    private val configPath by option(ArgType.String, "config-file", "f", "Configuration file").default("config/backup.toml")

    override fun execute() {
        CoreConfig.config.executePreExecuteScript()
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
