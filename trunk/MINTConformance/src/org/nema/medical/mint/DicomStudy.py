#!/usr/bin/python
# -----------------------------------------------------------------------------
# $Id$
#
# Copyright (C) 2010 MINT Working group. All rights reserved.
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
# Contact mint-user@googlegroups.com if any conditions of this
# licensing are not clear to you.
# -----------------------------------------------------------------------------

import getopt
import glob
import os
import string
import sys
import traceback

from org.nema.medical.mint.DicomInstance import DicomInstance

# -----------------------------------------------------------------------------
# DicomStudy
# -----------------------------------------------------------------------------
class DicomStudy():
   def __init__(self, dcmDir):
       self.__dcmDir = dcmDir
       self.__instances = []
       self.__read()

   def studyInstanceUID(self):
       if self.numInstances() > 0:
          return self.instances(0).studyInstanceUID()
       else:
          return ""
       
   def numInstances(self):
       return len(self.__instances)
       
   def instances(self, n):
       return self.__instances[n]
       
   def _print(self):
       numInstances = self.numInstances()
       for n in range(0, numInstances):
           instances = self.instances(n)
           instances._print()
       
   def __read(self):
       pattern = os.path.join(self.__dcmDir, "*.dcm")
       dcmNames = glob.glob(pattern)
       for dcmName in dcmNames:
           instances = DicomInstance(dcmName)
           self.__instances.append(instances)
       
# -----------------------------------------------------------------------------
# main
# -----------------------------------------------------------------------------
def main():
    progName = sys.argv[0]
    (options, args)=getopt.getopt(sys.argv[1:], "")
    
    try:
       if len(args) != 1:
          print "Usage", progName, "<dicom_dir>"
          sys.exit(1)
          
       # ---
       # Read dicom.
       # ---
       dcmDir = sys.argv[1];
       study = DicomStudy(dcmDir)
       study._print()
       
    except Exception, exception:
       traceback.print_exception(sys.exc_info()[0], 
                                 sys.exc_info()[1],
                                 sys.exc_info()[2])
       sys.exit(1)
       
if __name__ == "__main__":
   main()