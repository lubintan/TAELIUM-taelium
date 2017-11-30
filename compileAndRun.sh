#!/bin/sh

if [ $# == 0 ]
then
	rm -rf nxt_db
fi

./compile.sh

./run.sh
