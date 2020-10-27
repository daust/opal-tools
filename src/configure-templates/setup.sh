#!/bin/bash
#--------------------------------------------------------------------------
# setting important environment variables for the scripts to be used. 
#--------------------------------------------------------------------------

java -cp "lib/*" de.opal.Main configure $*
