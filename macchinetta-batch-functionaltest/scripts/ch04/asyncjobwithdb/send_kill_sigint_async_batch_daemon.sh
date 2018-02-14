#!/bin/bash

ps aux | grep AsyncBatchDaemon | grep -v grep | awk '{ print "kill -9 ", $2 }' | sh
