plines = [x[:-1] for x in open("pout_large.txt").readlines()]
dlines = [x[:-1] for x in open("dout_large.txt").readlines()]
dscores, pscores = [], []
dsum, psum = 0, 0
for i in xrange(len(plines)):
	if((i%2) == 0): continue
	# print plines[i].split()
	plis = [float(x) for x in plines[i].split()]
	dlis = [float(x) for x in dlines[i].split()]
	if(len(plis)==0 or len(dlis)==0): continue
	pscores += plis
	dscores += dlis

for x in pscores:
	psum += x

for x in dscores:
	dsum += x

dsum /= len(dscores)
psum /= len(pscores)

print dsum, psum
print len(dscores), len(pscores)

#import numpy as np
# print len(pscores), len(dscores)
# pscores = np.array(pscores)
# dscores = np.array(dscores)

# print pscores.mean(), pscores.std()
# print dscores.mean(), dscores.std()