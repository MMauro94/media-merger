package com.github.mmauro94.media_merger.util.ask

import java.math.BigDecimal

object BigDecimalCliAsker : AbstractCliAsker<BigDecimal>() {
    override fun parse(str: String, default: BigDecimal?) = str.toBigDecimalOrNull()
}