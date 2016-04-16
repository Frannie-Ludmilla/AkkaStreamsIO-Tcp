#!/bin/sh
#NOTE: This script is taken from Scala Cookbook by Alvin Alexander 
mkdir -p src/{main,test}/{resources,scala}
mkdir lib project target
# create an initial build.sbt file
echo 'name := "AkkaFileTransfer"
version := "1.0"
scalaVersion := "2.11.8"' > build.sbt
