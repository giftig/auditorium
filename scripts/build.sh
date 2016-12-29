#!/bin/bash

DIR=$(readlink -f "$(dirname $0)")

usage() {
  echo ''
  echo 'usage: build.sh [--nocolor] [--tag extra-tag] [--maven-repo path/to/repo]'
  echo ''
  echo 'Options:'
  echo '--nocolor\t\t\tSuppress the use of giftig/maven image for colour'
  echo '--tag\t\t\tSpecify an optional extra tag with which to label the built docker image'
  echo '--maven-repo\t\t\tIf set, will share your maven repo with docker (no refetching deps)'
  echo ''
  echo '-t\t\t\tAlias for --tag'
  echo '-m\t\t\tAlias for --maven-repo'
  echo '-C\t\t\tAlias for --nocolor'
  echo ''
}

while [[ "$1" != '' ]]; do
  arg=$1
  shift

  case $arg in
    --nocolor|-C)
      NO_COLOUR=1
      ;;
    --tag|-t)
      EXTRA_TAG="$1"
      shift

      if [[ "$EXTRA_TAG" == '' ]]; then
        echo 'No tag specified!'
        usage
        exit 1
      fi
      ;;
    --maven-repo|-m)
      MAVEN_REPO="$1"
      shift

      if [[ "$MAVEN_REPO" == '' ]]; then
        echo 'No maven repo specified!'
        usage
        exit 1
      fi
      EXTRA_CONFIG="$EXTRA_CONFIG -v $MAVEN_REPO:/root/.m2"
      ;;
    --local-maven)
      LOCAL_MAVEN=1
      ;;
    *)
      echo "Unrecognised argument: $arg"
      usage
      exit 1
      ;;
  esac
done

build_maven() {
  if [[ "$LOCAL_MAVEN" == 1 ]]; then
    mvn clean test package
    return
  fi

  if [[ "$NO_COLOUR" == 1 ]]; then
    MAVEN_IMAGE='maven:latest'
    CMD='mvn'
  else
    if [[ "$(docker images -q giftig/maven)" == '' ]]; then
      echo 'giftig/maven not found in your docker environment.'
      echo 'If you would like to run maven through some colourisation, run:'
      echo '  docker pull giftig/maven'
      echo 'If you do not want to use that image or see this message, use --nocolor'
      MAVEN_IMAGE='maven:latest'
      CMD='mvn'
    else
      MAVEN_IMAGE='giftig/maven:latest'
      CMD=''
    fi
  fi

  cd $DIR

  docker run \
    --rm \
    -v $DIR:/usr/src \
    -w /usr/src \
    -u $(id -u):$(id -g) \
    -e TERM \
    $EXTRA_CONFIG \
    $MAVEN_IMAGE $CMD \
    clean test package || exit 1

}

build_maven || exit 1

mv build/auditorium-*.jar build/auditorium.jar || exit 1

docker build -t auditorium:snapshot . || exit 1

if [[ "$EXTRA_TAG" != '' ]]; then
  docker tag auditorium:snapshot "$EXTRA_TAG"
fi
