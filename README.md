# Overview

This repository contains a simple reproduction of a bug seen when using heap `ByteBuffer`s when using `AsynchronousFileChannel` on Windows systems where the second buffer written is written as '\0' characters.
An additional case using direct `ByteBuffer`s is given which doesn't run into this issue.

OpenJDK 26 early access build 28 was used to reproduce this issue.
