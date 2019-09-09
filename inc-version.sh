#!/bin/bash

# Increment a version string using Semantic Versioning (SemVer) terminology.

# Based off https://github.com/fmahnke/shell-semver/blob/master/increment_version.sh. 
# Licenseed under the MIT license.

# Parse command line options.

while getopts ":Mmp" Option
do
  case $Option in
    M ) major=true;;
    m ) minor=true;;
    p ) patch=true;;
  esac
done

shift $(($OPTIND - 1))

version=`cat version.txt`
echo Updating from version: $version

# Build array from version string.

a=( ${version//./ } )

# If version string is missing or has the wrong number of members, show usage message.

if [ ${#a[@]} -ne 4 ]
then
  version=0.0.1.0
  a=( ${version//./ } )
fi

# Increment version numbers as requested.

if [ ! -z $major ]
then
  ((a[0]++))
  a[1]=0
  a[2]=0
fi

if [ ! -z $minor ]
then
  ((a[1]++))
  a[2]=0
fi

if [ ! -z $patch ]
then
  ((a[2]++))
fi

((a[3]++))

echo "New version: ${a[0]}.${a[1]}.${a[2]}.${a[3]}"
echo "${a[0]}.${a[1]}.${a[2]}.${a[3]}" > version.txt
