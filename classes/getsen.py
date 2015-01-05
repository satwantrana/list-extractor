raw_input()
m, n = 557, 550


a = [0 for x in xrange(n)]
b = [0 for x in xrange(m)]


for i in xrange(n):
    a[i] = int(raw_input())

for i in xrange(m):
    b[i] = int(raw_input())

i, j = 0, 0

while i <> n:
    if b[j] *1.1 < a[i]:
        j += 1
        print i
    i += 1
    j += 1
