package com.bnpinnovation.reaver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ReaverApplication

fun main(args: Array<String>) {
    runApplication<ReaverApplication>(*args)
}
