#!/bin/bash

set -ex

AI_DIR="$1/AI/Skirmish/SkynetAI/0.1/"

printf "\nEnsuring AI directory is created\n"
mkdir -p "$AI_DIR"
cp AIInfo.lua "$AI_DIR"
cp AIOptions.lua "$AI_DIR"
cp target/SkirmishAI.jar "$AI_DIR"

printf "\nCopying AI JAR into shared jlib\n"
printf "\nHopefully we can figure out how to avoid this\n"
cp target/SkirmishAI.jar "$1/AI/Interfaces/Java/0.1/jlib/skynet.jar"
