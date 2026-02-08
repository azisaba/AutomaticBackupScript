package net.azisaba.automaticbackupscript.util

import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

object ProcessExecutor {
    private val logger = LoggerFactory.getLogger(ProcessExecutor::class.java)!!
    private var streamThreadIndex = 0

    fun executeCommandCaptureOutput(
        workingDir: File,
        vararg command: String,
        logCommand: Boolean = true,
        mergeStderr: Boolean = false,
    ): Result {
        if (logCommand) {
            logger.info("Executing command: ${command.contentToString()}")
        }
        val proc =
            ProcessBuilder(*command)
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .apply { if (mergeStderr) redirectErrorStream(true) }
                .start()

        if (!proc.waitFor(60, TimeUnit.MINUTES)) {
            error("Timeout")
        }
        return Result(
            proc.inputStream.bufferedReader().readText(),
            proc.errorStream.bufferedReader().readText(),
            proc.exitValue(),
        )
    }

    fun executeCommandStreamedOutput(
        workingDir: File,
        vararg command: String,
        logCommand: Boolean = true,
        mergeStderr: Boolean = false,
    ): Int {
        if (logCommand) {
            logger.info("Executing command: ${command.contentToString()}")
        }
        val proc =
            ProcessBuilder(*command)
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .apply { if (mergeStderr) redirectErrorStream(true) }
                .start()
                .setupPrinter()

        if (!proc.waitFor(60, TimeUnit.MINUTES)) {
            error("Timeout")
        }
        return proc.exitValue()
    }

    private fun setupPrinter(
        input: InputStream,
        log: (String) -> Unit,
    ) {
        Thread({
            InputStreamReader(input).use { isr ->
                BufferedReader(isr).use { br ->
                    var line: String?
                    while (br.readLine().also { line = it } != null) {
                        line?.let(log)
                    }
                }
            }
        }, "Printer Thread #${streamThreadIndex++}").apply { isDaemon = true }.start()
    }

    private fun Process.setupPrinter(): Process {
        setupPrinter(this.inputStream, logger::info)
        setupPrinter(this.errorStream, logger::warn)
        return this
    }

    data class Result(
        val stdout: String,
        val stderr: String,
        val exitValue: Int,
    )
}
