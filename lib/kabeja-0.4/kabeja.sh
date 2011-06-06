#!/bin/sh

JAVA_MEM=256m



java -Xmx$JAVA_MEM -jar launcher.jar $@
