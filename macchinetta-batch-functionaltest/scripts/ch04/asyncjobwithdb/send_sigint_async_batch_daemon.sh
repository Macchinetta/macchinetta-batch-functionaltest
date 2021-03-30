#!/bin/bash

ps aux | grep AsyncBatchDaemon | grep -v grep | awk '{ print "kill -2 ", $2 }' | sh
