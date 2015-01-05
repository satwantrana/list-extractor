import sys

lines = [line for line in sys.stdin]

t= int(lines[0].strip())

i = 1 
for x in xrange(t):
    print lines[i].strip() 
    i += 1
    
    n = int(lines[i])
    i += 1
    for num in xrange(n):

        m = int(lines[i].split()[1])
        i += 1
        for _ in xrange(m):
            i += 1
    
            i += 1
    
