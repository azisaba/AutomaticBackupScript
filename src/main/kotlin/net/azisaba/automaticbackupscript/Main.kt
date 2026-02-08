package net.azisaba.automaticbackupscript

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.default
import net.azisaba.automaticbackupscript.command.BackupCommand
import net.azisaba.automaticbackupscript.command.MigrateCommand

object Main {
    init {
        System.setProperty("org.slf4j.simpleLogger.logFile", "System.out")
    }

    private val parser = ArgParser("AutomaticBackupScript")
    val configFile by parser.option(ArgType.String, "config-file", "c", "Config file (core)").default("config/core.json")

    @OptIn(ExperimentalCli::class)
    @JvmStatic
    fun main(args: Array<String>) {
        parser.subcommands(
            BackupCommand,
            MigrateCommand,
        )
        parser.parse(args)
    }
}
