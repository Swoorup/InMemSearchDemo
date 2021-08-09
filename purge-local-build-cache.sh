#! /usr/bin/env bash
sbt clean
rm -rf .bloop/ .vscode/ .ionide/ .idea/ .metals/ .bsp/ target/
rm -rf project/project.* project/metals.sbt project/project project/target project/.bloop
rm -rf **/*/target/
