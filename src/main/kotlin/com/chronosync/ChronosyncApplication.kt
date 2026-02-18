package com.chronosync

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableAsync
class ChronosyncApplication

fun main(args: Array<String>) {
    SpringApplication.run(ChronosyncApplication::class.java, *args)
}
