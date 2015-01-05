import sys

lines = [line for line in sys.stdin]

t= int(lines[0].strip())
print t
i = 1 
for x in xrange(t):
    i += 1
    print lines[i].strip()
    n = int(lines[i])
    i += 1
    for num in xrange(n):
        print lines[i].split()[1]
        m = int(lines[i].split()[1])
        i += 1
        for _ in xrange(m):
            i += 1
            print lines[i].split()[0], lines[i].split()[-1]
            i += 1
    
