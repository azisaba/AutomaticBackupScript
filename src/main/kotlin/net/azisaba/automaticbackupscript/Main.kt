package net.azisaba.automaticbackupscript

import kotlinx.cli.ArgParser
import kotlinx.cli.ExperimentalCli
import net.azisaba.automaticbackupscript.command.BackupCommand
import net.azisaba.automaticbackupscript.command.MigrateCommand

object Main {
    init {
        System.setProperty("org.slf4j.simpleLogger.logFile", "System.out")
    }

    val parser = ArgParser("AutomaticBackupScript")

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
