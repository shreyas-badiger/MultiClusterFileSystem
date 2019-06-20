#!/bin/sh
cat hostsEntry /etc/hosts > temp
cp temp /etc/hosts