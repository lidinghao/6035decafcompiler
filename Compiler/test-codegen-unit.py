#!/usr/bin/python
# MIT 6.035 project grading script
# by Jason Ansel <jansel@csail.mit.edu>, Spring 2010
# You may redistribute this file under the MIT license
import tarfile, sys, tempfile, subprocess, os, warnings, shutil, time
from os.path import isfile, isdir, join, dirname, abspath

class config:
  scanner_public_dir = join(dirname(abspath(sys.argv[0])), "tests/scanner/")
  scanner_hidden_dir = join(dirname(abspath(sys.argv[0])), "tests/scanner/hidden")
  scanner_skip_tests = []
  scanner_tests      = False

  parser_public_dir  = join(dirname(abspath(sys.argv[0])), "tests/parser/")
  parser_hidden_dir  = join(dirname(abspath(sys.argv[0])), "tests/parser/hidden")
  parser_tests       = False

  semantics_public_dir  = join(dirname(abspath(sys.argv[0])), "tests/semantics/")
  semantics_hidden_dir  = join(dirname(abspath(sys.argv[0])), "tests/semantics/hidden")
  semantics_tests       = False
  
  codegen_public_dir  = join(dirname(abspath(sys.argv[0])), "tests/codegen-unit/")
  codegen_hidden_dir  = join(dirname(abspath(sys.argv[0])), "tests/codegen/hidden")
  codegen_tests       = True

  pad = 20
  showscore=False
  verbose=True

def run_tmpfile(cmd, i):
  '''
  run cmd
  return rv and output
  write input to a temp file so students cant use the filename as a hint
  '''
  (fd, tmpfilename)=tempfile.mkstemp(dir=config.tmp, suffix=".dcf")
  os.close(fd)
  f = open(tmpfilename, "wb")
  f.write(i)
  f.flush()
  f.close()
  try:
    null=open("/dev/null", "w")
  except:
    null=tempfile.TemporaryFile(dir=config.tmp)
  cmd.append(tmpfilename)
  p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=null)
  cmd.pop()
  output = p.stdout.read()
  rv=p.wait()
  f.close()
  null.close()
  try:
    pass
    #os.unlink(tmpfilename)
  except:
    pass
  return rv, output, tmpfilename

def diff_answer(testname, correct, students):
  '''check the answer against correct'''
  if correct == students:
    print testname.ljust(config.pad), "CORRECT"
    grade=1
  else:
    print testname.ljust(config.pad), "INCORRECT, DIFFERENCE:"
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
    print testname.ljust(config.pad), "SKIPPED"
    return 1
  rv, students, tmpfilename = run_tmpfile(cmd, i)
  scannormalize = lambda txt: txt.replace(testname,'').replace(tmpfilename,'').replace("\r","").strip()
  return diff_answer(testname, scannormalize(correct), scannormalize(students))

def testcase_codegen(cmd, testname, i, correct):
  '''check if cmd produces assembly that produces the output correct'''
  rv, stdoutlog, filename = run_tmpfile(cmd, i)
  if rv!=0:
    print testname.ljust(config.pad), "INCORRECT, COMPILE FAILED returncode", rv
    return 0
  bfile = filename+".bin"

  if isfile(filename+".s"):
    filename+=".s"
  elif isfile(os.path.splitext(filename)[0]+".s"):
    filename=os.path.splitext(filename)[0]+".s"
  else:
    print testname.ljust(config.pad), "INCORRECT, NO output.s FILE PRODUCED"
    return 0

  if subprocess.call(["gcc", "-o", bfile, filename])!=0 or not isfile(bfile):
    print testname.ljust(config.pad), "INCORRECT, 'gcc output.s' FAILED"
    return 0

  p = subprocess.Popen([bfile], stdout=subprocess.PIPE)
  output = p.stdout.read()
  p.wait()

  def norm(s):
    '''
    normalize the look of runtime errors
    any line containing 'RUNTIME ERROR' gets turned into '*** RUNTIME ERROR ***'
    '''
    s=s.replace('\r','')
    lines=s.split('\n')
    for i, line in enumerate(lines):
      if "RUNTIME ERROR" in line:
        lines = lines[0:i]
        while len(lines) and lines[-1]=='':
          lines.pop()
        lines.append("*** RUNTIME ERROR ***")
        lines.append("(description normalized/removed by grading script  )")
        lines.append("(just make sure your errors contain 'RUNTIME ERROR')")
        break
    return '\n'.join(lines)
  return diff_answer(testname, norm(correct), norm(output))


def testcase_legalillegal(cmd, testname, i):
  '''check the return value of cmd given input i against testname'''
  rv, students, tmpfilename = run_tmpfile(cmd, i)
  if testname[0:5]=="legal":
    islegal=True
  else:
    assert testname[0:7]=="illegal"
    islegal=False
  if islegal == (rv==0):
    print testname.ljust(config.pad), "CORRECT"
    return 1
  else:
    if not config.verbose:
      print testname.ljust(config.pad), "INCORRECT"
    else:
      if islegal:
        print testname.ljust(config.pad), "INCORRECT -- expect exit code 0"
      else:
        print testname.ljust(config.pad), "INCORRECT -- expect exit code other than 0"
    return 0

def test_diffoutput(cmd, testdir, subtest):
  '''run all tests in testdir'''
  if testdir is None:
    return 0,0
  correct=0
  total=0
  for f in sorted(os.listdir(testdir)):
    fi=os.path.join(testdir, f)
    fo=os.path.join(testdir,"output",os.path.splitext(f)[0]+".out")
    if isfile(fi) and isfile(fo):
      correct+=subtest(cmd, f, open(fi).read(), open(fo).read())
      total+=1
  return correct,total

test_scanner=lambda c,d: test_diffoutput(c, d, testcase_scanner)
test_codegen=lambda c,d: test_diffoutput(c, d, testcase_codegen)

def test_legalillegal(cmd, testdir):
  '''run all tests in testdir'''
  if testdir is None:
    return 0,0
  correct=0
  total=0
  for f in sorted(os.listdir(testdir)):
    fi=os.path.join(testdir, f)
    if isfile(fi):
      correct+=testcase_legalillegal(cmd, f, open(fi).read())
      total+=1
  return correct,total

def weightedScore(rx):
  h = [0,0] #public
  p = [0,0] #hidden
  for k,v in filter(lambda x: x[1][1]>0, rx.iteritems()):
    if k.lower().endswith('hidden'):
      h[0] += v[0]/float(v[1])
      h[1] += 1
    else:
      p[0] += v[0]/float(v[1])
      p[1] += 1
  p = p[0]/float(p[1])
  if h[1] == 0:
    return p
  else:
    h = h[0]/float(h[1])
    return (2.0*h+p)/3.0

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

  rx = dict()

  def testwrap(name, tester, dir):
    if dir is not None and os.path.isdir(dir):
      print name
      rx[name] = tester(cmd, dir)
      print
    else:
      print name," tests missing"
      print

  cmd.extend(["-target","scan"])
  if config.scanner_tests:
    testwrap('SCANNER PUBLIC', test_scanner, config.scanner_public_dir)
    testwrap('SCANNER HIDDEN', test_scanner, config.scanner_hidden_dir)

  cmd[-1]="parse"
  if config.parser_tests:
    testwrap('PARSER PUBLIC', test_legalillegal, config.parser_public_dir)
    testwrap('PARSER HIDDEN', test_legalillegal, config.parser_hidden_dir)
  
  cmd[-1]="inter"
  if config.semantics_tests:
    testwrap('SEMANTICS PUBLIC', test_legalillegal, config.semantics_public_dir)
    testwrap('SEMANTICS HIDDEN', test_legalillegal, config.semantics_hidden_dir)
  
  cmd[-1]="codegen"
  if config.codegen_tests:
    testwrap('CODEGEN PUBLIC', test_codegen, config.codegen_public_dir)
    testwrap('CODEGEN HIDDEN', test_codegen, config.codegen_hidden_dir)

  #print results
  for k,v in sorted(rx.items()):
    if sum(v)>0:
      print k.ljust(config.pad)," %d of %d"%v
  print "TOTAL".ljust(config.pad), " %d of %d"%tuple(map(sum,zip(*rx.values())))

  if config.showscore:
    print "SCORE".ljust(config.pad), weightedScore(rx)

if __name__ == "__main__":
  if len(sys.argv)!=2:
    print "USAGE:", sys.argv[0], "joestudent-parser.tar.gz"
    print "  or"
    print "USAGE:", sys.argv[0], "janestudent-parser/"
    sys.exit(1)
  if config.codegen_tests:
    if os.uname()[4] != "x86_64":
      print "Script must be run on a x86_64 linux machine."
      sys.exit(1)

  config.tmp=tempfile.mkdtemp("6035GRADER")
  try:
    main(sys.argv[1], config.tmp)
  finally:
    shutil.rmtree(config.tmp)


