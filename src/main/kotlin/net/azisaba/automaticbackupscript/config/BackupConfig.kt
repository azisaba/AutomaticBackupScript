package net.azisaba.automaticbackupscript.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.File

@Serializable
data class BackupConfig(
    val webhookUrl: String = "insert url here",
    val prefixIfWarning: String = ":warning: ",
    val runAsSudo: Boolean = false,
    val knownHostsFile: String? = null,
    val forgetHosts: List<String> = listOf("[github.com]:22"),
    val binaries: BinariesInfo = BinariesInfo(),
    val downloads: List<DownloadInfo> =
        listOf(
            DownloadInfo("server:/tmp/exampleA", "server:/tmp/exampleA", "/tmp/exampleB"),
        ),
    val backups: List<BackupInfo> =
        listOf(
            BackupInfo("server:/tmp/exampleA", "/mnt/backup", "plain-password", "/tmp/exampleB", false, 1),
        ),
) {
    companion object {
        lateinit var config: BackupConfig

        fun load(file: File) {
            if (!file.parentFile.exists()) file.parentFile.mkdirs()
            if (!file.exists()) file.writeText(CoreConfig.toml.encodeToString(BackupConfig()))
            config = CoreConfig.toml.decodeFromString(serializer(), file.readText())
            file.writeText(CoreConfig.toml.encodeToString(config))
        }
    }
}

@Serializable
data class BinariesInfo(
    val sshKeygen: String = "ssh-keygen",
    val rsync: String = "rsync",
    val restic: String = "restic",
)

@Serializable
data class DownloadInfo(
    val webhookName: String,
    val from: String,
    val to: String,
    val rsh: String? = null,
    val archive: Boolean = true,
    val verbose: Boolean = true,
    val compress: Boolean = true,
    val delete: Boolean = true,
    val exclude: List<String> = listOf("CoreProtect/database.db"),
)

@Serializable
data class BackupInfo(
    val webhookName: String,
    val repo: String,
    val repoPassword: String,
    val path: String,
    val backupChildDirectoriesOnly: Boolean,
    val verbose: Int,
    val dependOp: DependOp = DependOp.OR,
    val depend: List<String> = listOf("# 'from' or 'to' in downloads"),
)

enum class DependOp {
    OR,
    AND,
}
