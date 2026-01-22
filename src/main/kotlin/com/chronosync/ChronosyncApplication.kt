package com.chronosync

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class ChronosyncApplication

fun main(args: Array<String>) {
    SpringApplication.run(ChronosyncApplication::class.java, *args)
}
