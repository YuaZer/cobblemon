/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.molang

import com.bedrockk.molang.runtime.struct.QueryStruct
import com.bedrockk.molang.runtime.struct.VariableStruct
import com.bedrockk.molang.runtime.value.DoubleValue
import com.bedrockk.molang.runtime.value.MoValue
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.util.server
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import java.nio.file.Path
import kotlin.io.path.pathString
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.storage.LevelResource

/**
 * A cache for loaded MoLang files to avoid redundant file I/O operations.
 * Provides functions to load, save, check existence, and clear cached files.
 *
 * @author Hiroku
 * @since November 1st, 2025
 */
object MoLangLoadedFilesCache {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val loadedFiles = mutableMapOf<String, VariableStruct>()
    
    lateinit var baseFolder: Path
    private lateinit var configFolder: Path
    private lateinit var dataFolder: Path

    fun initialize(server: MinecraftServer) {
        baseFolder = server.getWorldPath(LevelResource.ROOT)
        configFolder = baseFolder.resolve("config").normalize().toAbsolutePath()
        dataFolder = baseFolder.resolve("data").normalize().toAbsolutePath()
    }
    
    val struct = QueryStruct(
        hashMapOf(
            "load" to java.util.function.Function { params ->
                val fileName = params.params[0].asString()
                return@Function load(fileName)
            },
            "save" to java.util.function.Function { params ->
                val fileName = params.params[0].asString()
                val data = params.params[1] as VariableStruct
                save(fileName, data)
                return@Function DoubleValue.ONE
            },
            "exists" to java.util.function.Function { params ->
                val fileName = params.params[0].asString()
                val exists = exists(fileName)
                return@Function if (exists) DoubleValue.ONE else DoubleValue.ZERO
            },
            "clear" to java.util.function.Function { params ->
                val fileName = params.params[0].asString()
                clear(fileName)
                return@Function DoubleValue.ONE
            }
        )
    )

    private fun validatePath(fileName: String): Path? {
        val filePath = baseFolder.resolve(fileName).normalize().toAbsolutePath()
        if (!filePath.startsWith(configFolder) && !filePath.startsWith(dataFolder)) {
            Cobblemon.LOGGER.error("MoLang attempted to access file outside of config or data folder: $fileName")
            return null
        } else if ("/molang/" !in fileName) {
            Cobblemon.LOGGER.error("MoLang attempted to access file outside of a 'molang' subfolder: $fileName")
            return null
        } else if (!fileName.endsWith(".json")) {
            Cobblemon.LOGGER.error("MoLang attempted to access file without a .json extension: $fileName")
            return null
        }
        return filePath
    }

    fun exists(fileName: String): Boolean {
        val filePath = validatePath(fileName) ?: return false
        return filePath.toFile().exists()
    }

    fun clear(fileName: String) {
        loadedFiles.remove(fileName)
    }

    fun load(fileName: String): VariableStruct {
        return loadedFiles.getOrPut(fileName) {
            val filePath = validatePath(fileName) ?: return VariableStruct()
            val file = filePath.toFile()
            if (!file.exists()) {
                return VariableStruct()
            }
            val jsonElement = gson.fromJson(file.readText(), JsonElement::class.java)
            MoValue.of(jsonElement) as VariableStruct
        }
    }

    fun save(fileName: String, data: VariableStruct) {
        loadedFiles[fileName] = data
        val filePath = validatePath(fileName) ?: return
        val file = filePath.toFile()
        file.parentFile?.mkdirs()
        val jsonElement = MoValue.writeToJson(data)
        file.writeText(gson.toJson(jsonElement))
    }
}