package com.github.mmauro94.media_merger.util.cli.type

import java.math.BigDecimal

object BigDecimalCliType : CliType<BigDecimal>() {
    override fun parse(str: String) = str.toBigDecimalOrNull()
}