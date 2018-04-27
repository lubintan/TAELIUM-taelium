#!/bin/sh

if [ $# == 0 ]
then
	rm -rf taelium_db
fi

./compile.sh

./run.sh
