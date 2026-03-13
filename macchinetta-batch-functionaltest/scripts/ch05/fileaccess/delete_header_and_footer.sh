#!/bin/bash

if [ $# -ne 4 ]; then
  echo "The number of arguments must be 4, given is $#." 1>&2
  exit 1
fi

# Input file.
input=$1

# Output file.
output=$2

# Number of lines in header.
header=$3

# Number of lines in footer.
footer=$4

# Remove number of lines in header from the top of input file and number of lines in footer from the end, 
# and save to output file.
tail -n +`expr ${header} + 1` ${input} | head -n -${footer} > ${output}
