-ignorewarnings

-keep class * implements org.slf4j.spi.SLF4JServiceProvider { *; }
-keep class ch.qos.logback.** { *; }
