#!/usr/bin/python
# MIT 6.035 project 1 grading script
# by Jason Ansel <jansel@csail.mit.edu> on Feb 6, 2010
# You may redistribute this file under the MIT license
import tarfile, sys, tempfile, subprocess, os, warnings, shutil
from os.path import isfile, isdir, join, dirname, abspath

class config:
  scanner_public_dir = join(dirname(abspath(sys.argv[0])), "tests/scanner/")
  scanner_hidden_dir = None
  scanner_skip_tests = ['ws1' # assignment disagreed with test on handling of pagebreak
                       ]
  scanner_tests      = False

  parser_public_dir  = join(dirname(abspath(sys.argv[0])), "tests/parser/")
  parser_hidden_dir  = None
  parser_tests       = False

  semantics_public_dir  = join(dirname(abspath(sys.argv[0])), "tests/semantics/")
  semantics_hidden_dir  = join(dirname(abspath(sys.argv[0])), "tests/semantics/hidden")
  semantics_tests       = True

  verbose=True

def run_tmpfile(cmd, i):
  '''
  run cmd
  return rv and output
  write input to a temp file so students cant use the filename as a hint
  '''
  f = tempfile.NamedTemporaryFile()
  f.write(i)
  f.flush()
  try:
    null=open("/dev/null", "w")
  except:
    null=tempfile.TemporaryFile()
  cmd.append(f.name)
  p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=null)
  cmd.pop()
  output = p.stdout.read()
  rv=p.wait()
  tmpfilename=f.name
  f.close()
  null.close()
  return rv, output, tmpfilename

def diff_answer(testname, correct, students):
  '''check the answer against correct'''
  if correct == students:
    print testname.ljust(10), "CORRECT"
    grade=1
  else:
    print testname.ljust(10), "INCORRECT, DIFFERENCE:"
    grade=0
    if config.verbose:
      #pretty output
      cor=correct.split('\n')
      stu=students.split('\n')
      while len(cor)<len(stu):
        cor.append('')
      while len(cor)>len(stu):
        stu.append('')
      pad = max(map(len, cor))
      print "      |" ,"CORRECT".ljust(pad),"| STUDENT"
      for i in xrange(len(cor)):
        if cor[i]==stu[i]:
          print "      |",
        else:
          print "  *** |",
        print cor[i].ljust(pad),'|', stu[i]
  return grade

def testcase_scanner(cmd, testname, i, correct):
  '''check if cmd produces the output correct given input i'''
  if testname in config.scanner_skip_tests:
    print testname.ljust(10), "SKIPPED"
    return 1
  rv, students, tmpfilename = run_tmpfile(cmd, i)
  scannormalize = lambda txt: txt.replace(testname,'').replace(tmpfilename,'').strip()
  return diff_answer(testname, scannormalize(correct), scannormalize(students))

def testcase_legalillegal(cmd, testname, i):
  '''check the return value of cmd given input i against testname'''
  rv, students, tmpfilename = run_tmpfile(cmd, i)
  if testname[0:5]=="legal":
    islegal=True
  else:
    assert testname[0:7]=="illegal"
    islegal=False
  if islegal == (rv==0):
    print testname.ljust(10), "CORRECT"
    return 1
  else:
    if not config.verbose:
      print testname.ljust(10), "INCORRECT"
    else:
      if islegal:
        print testname.ljust(10), "INCORRECT -- expect exit code 0"
      else:
        print testname.ljust(10), "INCORRECT -- expect exit code other than 0"
    return 0

def test_scanner(cmd, testdir):
  '''run all tests in testdir'''
  correct=0
  total=0
  for f in sorted(os.listdir(testdir)):
    fi=os.path.join(testdir, f)
    fo=os.path.join(testdir,"output",f+".out")
    if isfile(fi) and isfile(fo):
      correct+=testcase_scanner(cmd, f, open(fi).read(), open(fo).read())
      total+=1
  return correct,total

def test_legalillegal(cmd, testdir):
  '''run all tests in testdir'''
  correct=0
  total=0
  for f in sorted(os.listdir(testdir)):
    fi=os.path.join(testdir, f)
    if isfile(fi):
      correct+=testcase_legalillegal(cmd, f, open(fi).read())
      total+=1
  return correct,total

def main(filename, tmpdir):
  #untar filename to tmpdir/
  if isfile(filename):
    tarfile.open(filename).extractall(tmpdir)
  elif isdir(filename):
    shutil.copytree(filename, os.path.join(tmpdir,'t'))
  else:
    raise Exception("invalid input file")

  #chdir to tmpdir/joestudent-parser/
  os.chdir(tmpdir)
  while len(os.listdir('.'))==1:
    os.chdir(os.listdir('.')[0])

  #figure out where Compiler.jar is
  if isfile("./dist/Compiler.jar"):
    cmd=["java", "-jar", "./dist/Compiler.jar"]
  elif isfile("./Compiler.jar"):
    cmd=["java", "-jar", "./Compiler.jar"]
  elif isfile("./dist/run.sh"):
    cmd=["bash", "./dist/run.sh"]
  elif isfile("./run.sh"):
    cmd=["bash", "./run.sh"]
  else:
    raise Exception("dist/Compiler.jar is missing")

  #test the scanner
  cmd.extend(["-target","scan"])
  if config.scanner_tests:
    scanresult = test_scanner(cmd, config.scanner_public_dir)

  #test the parser
  cmd[-1]="parse"
  if config.parser_tests:
    parseresult = test_legalillegal(cmd, config.parser_public_dir)
  
  cmd[-1]="inter"
  if config.semantics_tests:
    s1 = test_legalillegal(cmd, config.semantics_public_dir)
    s2 = test_legalillegal(cmd, config.semantics_hidden_dir)
    semanticsresult = (s1[0] + s2[0], s1[1] + s2[1])
   

  #print results
  print
  if config.scanner_tests:
    print "SCANNER:    %d of %d correct"%scanresult
  if config.parser_tests:
    print "PARSER:     %d of %d correct"%parseresult
  if config.semantics_tests:
    print "SEMANTICS:  %d of %d correct"%semanticsresult
  #print "TOTAL:   %d of %d correct"%tuple(map(sum,zip(scanresult,parseresult)))

if __name__ == "__main__":
  if len(sys.argv)!=2:
    print "USAGE:", sys.argv[0], "joestudent-parser.tar.gz"
    print "  or"
    print "USAGE:", sys.argv[0], "janestudent-parser/"
    sys.exit(1)
  tmpdir=tempfile.mkdtemp("6035P1GRADER")
  try:
    main(sys.argv[1], tmpdir)
  finally:
    shutil.rmtree(tmpdir)


