package me.gilberto.essentials.config.configs

import me.gilberto.essentials.config.ConfigMain
import me.gilberto.essentials.config.ConfigMain.econf

object lang {
    lateinit var langname : String
    fun reload() {
        langname = ConfigMain.getString(econf, "Lang")
    }
}