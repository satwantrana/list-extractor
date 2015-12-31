# Extracts Penn Treebank from NLTK.
from nltk.corpus import treebank
from operator import itemgetter
import codecs
words = treebank.sents()
tagged_words = [map(itemgetter(1), sent) for sent in treebank.tagged_sents()]
parsed_sents = treebank.parsed_sents()

total_sents = len(parsed_sents)

f = codecs.open('../data/penn_treebank','w','utf-8')
assert (len(words) == len(tagged_words) and len(words) == len(parsed_sents)), ' '.join(map(str, [len(words), len(tagged_words), len(parsed_sents)]))
f.write(str(total_sents) + '\n')
for i in xrange(total_sents):
	sent_len = len(words[i])
	f.write(str(sent_len) + '\n')
	
	sent = ' '.join(words[i])
	pos = ' '.join(tagged_words[i])
	assert(sent.count('\n') == 0 and pos.count('\n') == 0 and len(sent.split(' ')) == sent_len and len(pos.split(' ')) == sent_len)
	f.write(sent + '\n')
	f.write(pos + '\n')
	
	tree = str(parsed_sents[i]).split('\n')
	f.write(str(len(tree)) + '\n')
	f.write('\n'.join(tree) + '\n')