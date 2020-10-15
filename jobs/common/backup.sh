#!/bin/bash


##
## Functions that will backup binaries to a specific
## location in the target host and retain at most
## five copies at a time.
##
## This script is meant to be "imported" as follows:
##
## source "$(dirname $0)/../common/backup.sh"
##

function backup {
    FILE="${1?Specify a binary}"
    HOST="${2?Specify a host}"
    DEST="${3?Specify a destination directory in $HOST}"

    #################################################

    NAME="${FILE/.zip/}"
    SHA=$(sha256sum "$FILE" | sed 's, .*,,')
    DATE="$(date +"%Y-%m-%d")"

    SSH_OPTS="-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null"

    # Delete all but the last 4 most recent binaries
    ssh $SSH_OPTS "$HOST" ls -t "$DEST" | tail -n +5 | while read n; do
	ssh $SSH_OPTS "$HOST" rm "$DEST/$n"
    done

    # Upload the latest binary with correct date and sha
    scp $SSH_OPTS "$FILE" "$HOST:$DEST/$NAME-$DATE-$SHA.zip"
}

function backup_no_delete {
    FILE="${1?Specify a binary}"
    HOST="${2?Specify a host}"
    DEST="${3?Specify a destination directory in $HOST}"

    #################################################

    NAME="${FILE/.zip/}"
    SHA=$(sha256sum "$FILE" | sed 's, .*,,')
    DATE="$(date +"%Y-%m-%d")"

    SSH_OPTS="-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null"

    # Upload the latest binary with correct date and sha
    scp $SSH_OPTS "$FILE" "$HOST:$DEST/$NAME-$DATE-$SHA.zip"
}
