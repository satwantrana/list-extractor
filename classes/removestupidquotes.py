import sys

for line in sys.stdin:
    print line.strip().replace("''", '"').replace('``', '"').replace("...", ".").replace("--", "-")
