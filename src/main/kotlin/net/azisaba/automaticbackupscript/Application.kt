package net.azisaba.automaticbackupscript

import net.azisaba.automaticbackupscript.config.BackupConfig
import net.azisaba.automaticbackupscript.config.BackupInfo
import net.azisaba.automaticbackupscript.config.DependOp
import net.azisaba.automaticbackupscript.config.DownloadInfo
import net.azisaba.automaticbackupscript.util.DurationLocale
import net.azisaba.automaticbackupscript.util.ProcessExecutor
import net.azisaba.automaticbackupscript.util.WebhookUtil
import net.azisaba.automaticbackupscript.util.toLocaleString
import net.azisaba.automaticbackupscript.util.use
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@OptIn(ExperimentalTime::class)
class Application {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(Application::class.java)
    }

    private val successfulDownloads = mutableListOf<String>()

    /**
     * Return true if unlocked; false otherwise
     */
    private fun ensureNotLocked(repo: File) = File(repo, "locks").listFiles().isNullOrEmpty()

    suspend fun backup() {
        val duration =
            measureTime {
                checkBinaries()
                forgetHosts()
                downloadAll()
                backupAll()
            }
        WebhookUtil.executeWebhook(
            BackupConfig.config.webhookUrl,
            ":white_check_mark: バックアップが完了しました(${duration.toLocaleString(DurationLocale.JAPANESE)})",
        )
    }

    private fun checkBinaries() {
        logger.info("Checking binaries")
        measureTime {
            ProcessExecutor.executeCommandCaptureOutput(File("."), BackupConfig.config.binaries.sshKeygen, "--help")
            ProcessExecutor.executeCommandCaptureOutput(File("."), BackupConfig.config.binaries.rsync, "--version")
            ProcessExecutor.executeCommandCaptureOutput(File("."), BackupConfig.config.binaries.restic, "version")
        }.apply {
            logger.info("Completed in ${toLocaleString(DurationLocale.ENGLISH)}")
        }
    }

    private suspend fun downloadAll() {
        logger.info("Downloading files")
        measureTime {
            BackupConfig.config.downloads.forEach { info ->
                download(info)
            }
        }.apply {
            logger.info("Downloaded all files in ${toLocaleString(DurationLocale.ENGLISH)}")
        }
    }

    private suspend fun download(info: DownloadInfo) {
        val command = mutableListOf<String>()
        if (BackupConfig.config.runAsSudo) command.add("sudo")
        command.add(BackupConfig.config.binaries.rsync)
        if (info.archive) command.add("-a")
        if (info.verbose) command.add("-v")
        if (info.compress) command.add("-z")
        if (info.delete) command.add("--delete")
        command.add("-L")
        if (info.rsh != null) {
            command.add("-e")
            command.add(info.rsh)
        }
        info.exclude.forEach {
            command.add("--exclude")
            command.add(it)
        }
        command.add(info.from)
        command.add(info.to)
        var exitCode: Int
        val duration =
            measureTime {
                exitCode = ProcessExecutor.executeCommandStreamedOutput(File("."), *command.toTypedArray())
                logger.info("rsync process exited with code $exitCode")
            }
        if (exitCode == 0 || exitCode == 23 || exitCode == 24) {
            successfulDownloads.add(info.from)
            successfulDownloads.add(info.to)
            WebhookUtil.executeWebhook(
                BackupConfig.config.webhookUrl,
                ":inbox_tray: `${info.webhookName}`のダウンロードが完了(${duration.toLocaleString(DurationLocale.JAPANESE)})",
            )
        } else {
            WebhookUtil.executeWebhook(
                BackupConfig.config.webhookUrl,
                "${BackupConfig.config.prefixIfWarning}`${info.webhookName}`のダウンロードに失敗しました(${duration.toLocaleString(
                    DurationLocale.JAPANESE,
                )}) [$exitCode]",
            )
        }
    }

    private suspend fun backupAll() {
        val duration =
            measureTime {
                BackupConfig.config.backups.forEach { info ->
                    backup(info)
                }
            }
        logger.info("Backup completed in ${duration.toLocaleString(DurationLocale.ENGLISH)}")
    }

    private suspend fun backup(info: BackupInfo) {
        if (!ensureNotLocked(File(info.repo))) {
            WebhookUtil.executeWebhook(
                BackupConfig.config.webhookUrl,
                ":warning: `${info.webhookName}`のリポジトリは他のプロセスによってロックされているためバックアップは作成されません。",
            )
            return
        }
        val effectiveDepends = info.depend.filter { !it.startsWith("#") }
        if ((info.dependOp == DependOp.OR && !effectiveDepends.any { successfulDownloads.contains(it) }) ||
            (info.dependOp == DependOp.AND && !effectiveDepends.all { successfulDownloads.contains(it) })
        ) {
            WebhookUtil.executeWebhook(
                BackupConfig.config.webhookUrl,
                ":warning: `${info.webhookName}`はデータのダウンロードに失敗しているためバックアップは作成されません。",
            )
            return
        }
        File.createTempFile("restic-password", ".tmp").use { tmp ->
            tmp.writeText(info.repoPassword)
            val command = mutableListOf<String>()
            if (BackupConfig.config.runAsSudo) command.add("sudo")
            command.add(BackupConfig.config.binaries.restic)
            command.add("-r")
            command.add(info.repo)
            command.add("-p")
            command.add(tmp.absolutePath)
            for (i in 1..info.verbose) {
                command.add("--verbose")
            }
            command.add("backup")
            val file = File(info.path)
            val duration: Duration
            val success =
                if (info.backupChildDirectoriesOnly) {
                    if (!file.exists()) {
                        logger.warn("${info.path} does not exist")
                        WebhookUtil.executeWebhook(
                            BackupConfig.config.webhookUrl,
                            "${BackupConfig.config.prefixIfWarning}`${info.webhookName}`のバックアップに失敗しました(`${info.path}`が見つかりません)",
                        )
                        return
                    }
                    if (!file.isDirectory) {
                        logger.warn("${info.path} is not a directory")
                        WebhookUtil.executeWebhook(
                            BackupConfig.config.webhookUrl,
                            "${BackupConfig.config.prefixIfWarning}`${info.webhookName}`のバックアップに失敗しました(`${info.path}`はディレクトリではありません)",
                        )
                        return
                    }
                    val result: Boolean
                    duration =
                        measureTime {
                            result =
                                file.listFiles { it.isDirectory }!!.all { child ->
                                    try {
                                        command.add(child.absolutePath)
                                        val exitCode =
                                            ProcessExecutor.executeCommandStreamedOutput(File("."), *command.toTypedArray())
                                        logger.info("restic process exited with code $exitCode")
                                        return@all exitCode == 0
                                    } finally {
                                        command.removeLast()
                                    }
                                }
                        }
                    result
                } else {
                    command.add(file.absolutePath)
                    val exitCode: Int
                    duration =
                        measureTime {
                            exitCode = ProcessExecutor.executeCommandStreamedOutput(File("."), *command.toTypedArray())
                        }
                    logger.info("restic process exited with code $exitCode")
                    exitCode == 0
                }
            if (success) {
                WebhookUtil.executeWebhook(
                    BackupConfig.config.webhookUrl,
                    ":pencil: `${info.webhookName}`のバックアップが完了(${duration.toLocaleString(DurationLocale.JAPANESE)})",
                )
            } else {
                WebhookUtil.executeWebhook(
                    BackupConfig.config.webhookUrl,
                    "${BackupConfig.config.prefixIfWarning}`${info.webhookName}`のバックアップに失敗しました(${duration.toLocaleString(
                        DurationLocale.JAPANESE,
                    )})",
                )
            }
        }
    }

    private fun forgetHosts() {
        BackupConfig.config.forgetHosts.forEach { forgetHost(it, BackupConfig.config.knownHostsFile) }
    }

    private fun forgetHost(
        host: String,
        file: String?,
    ) {
        val command = mutableListOf<String>()
        if (BackupConfig.config.runAsSudo) {
            command.add("sudo")
        }
        command.add(BackupConfig.config.binaries.sshKeygen)
        command.add("-R")
        command.add(host)
        if (file != null) {
            command.add("-f")
            command.add(file)
        }
        val exitCode = ProcessExecutor.executeCommandStreamedOutput(File("."), *command.toTypedArray())
        logger.info("ssh-keygen exited with code $exitCode")
    }
}
