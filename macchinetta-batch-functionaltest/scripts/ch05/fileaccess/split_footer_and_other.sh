#!/bin/bash

if [ $# -ne 4 ]; then
  echo "The number of arguments must be 4, given is $#." 1>&2
  exit 1
fi

# Input file.
input=$1

# Output file non-footer records are saved.
other_records=$2

# Output file footer records are saved.
footer_records=$3

# Number of lines in footer.
footer=$4

# Extract non-footer record from input file and save to output file.
head -n -${footer} ${input} > ${other_records}

# Extract footer record from input file and save to output file.
tail -n ${footer} ${input} > ${footer_records}
