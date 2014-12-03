# i2b2obj.py
# version 1.0b
# Eithon Cadag, ecadag@uw.edu
#
# 	Data structure files for i2b2eval.py for the 2009 i2b2 challenge.
#	
#	Changelog:
#	7/28/09 -- Public release file completed.

import re

class Mention:
	def __init__(self,tag,mindex,content,start_line=None,start_token=None,end_line=None,end_token=None):
		self.tag = tag
		self.index = mindex
		self.content = content
		if start_line is not None and start_token is not None and end_line is not None and end_token is not None:
			self.start_line = start_line
			self.start_token = start_token
			self.end_line = end_line
			self.end_token = end_token
		else:
			self.start_line = []
			self.start_token = []
			self.end_line = []
			self.end_token = []
	
	def spans_lines(self):
		"""Returns a list of lines that this mention includes."""
		linelist = []
		if self.start_line != [] and self.end_line != []:
			linelist = [x for x in xrange(self.start_line[0],self.end_line[-1]+1)]
		else:
			return []
		return linelist

	def contains(self,line,pos):
		for ix,x in enumerate(self.start_line):
			if (self.end_line[ix] > line and self.start_line[ix] < line) or \
				(self.start_line[ix] == line and self.start_token[ix] <= pos and self.end_token[ix] >= pos) or \
				(self.end_line[ix] == line and self.end_token[ix] >= pos and self.start_token[ix] <= pos) or \
				(self.start_line[ix] == line and self.start_token[ix] <= pos and self.end_line[ix] != line) or \
				(self.end_line[ix] == line and self.end_token[ix] >= pos and self.start_line[ix] != line):
				return True
		return False


	def __cmp__(self,other):
		if self == other:
			return 0
		else: # Ugly...
			if self.tag == other.tag:
				comparator = self.start_line[0] - other.start_line[0]
				if comparator == 0:
					comparator = self.start_token[0] - other.start_token[0]
					if comparator == 0:
						comparator = other.end_line[0] - self.end_line[0]
						if comparator == 0:
							comparator = other.end_token[0] - self.end_token[0]
			else:
				return hash(self.tag) - hash(other.tag)
			return comparator

	def __eq__(self,other):
		if self.tag == other.tag and \
			self.start_line == other.start_line and \
			self.start_token == other.start_token and \
			self.end_line == other.end_line and \
			self.end_token == other.end_token:
			return True
		else:
			return False

	def __str__(self):
		mention_tag = ['%s="%s" '%(self.tag,self.content)]
		if len(self.start_line) > 0:
			for ix,x in enumerate(self.start_line):
				mention_tag.append("%s:%s %s:%s"%(self.start_line[ix],self.start_token[ix],self.end_line[ix],self.end_token[ix]))
				if ix < len(self.start_line)-1: # Not the last entry; add a comma
					mention_tag[-1] = mention_tag[-1]+","
		return ''.join(mention_tag)

	def __repr__(self):
		return self.__str__()

	def __hash__(self):
		return hash(self.__str__())


class MentionTuple:
	MENTION_ORDER = ['m','do','mo','f','du','r','e','t','c','ln','modifier','output']
	OFFSET_MENTIONS = ['m','do','mo','f','du','r']

	def __init__(self,mid,mentions):
		self.mentions = {}
		for x in MentionTuple.MENTION_ORDER:
			self.mentions[x] = []
		self.mid = mid
		for m in mentions:
			m.index = self.mid
			self.mentions[m.tag].append(m)
	
	def spans_lines(self):
		line_span = []
		for m in self.mentions:
			for y in self.mentions[m]:
				line_span.extend(y.spans_lines())
		if line_span != []:
			return [x for x in xrange(min(line_span),max(line_span)+1)]
		else:
			return line_span

	def verbose(self):
		verbose_mentions = []
		for x in MentionTuple.MENTION_ORDER:
			if x in self.mentions and len(self.mentions[x]) > 0:
				for y in self.mentions[x]:
					verbose_mentions.append(str(y).lower())
			else:
				verbose_mentions.append('%s="nm"'%x)
		return '||'.join(verbose_mentions)

	def __eq__(self,other):
		self_tags = [x for x in self.mentions.keys()]
		self_tags.sort()
		other_tags = [x for x in other.mentions.keys()]
		other_tags.sort()
		if self_tags != other_tags: return False
		for k in self.mentions:
			if self.mentions[k] != other.mentions[k]: return False
		return True

	def __getitem__(self,index):
		return self.mentions[index]

	def __setitem__(self,key,item):
		self.mentions[key] = item

	def __str__(self):
		all_mention_types = []
		for x in MentionTuple.MENTION_ORDER:
			if x in self.mentions:
				all_mention_types.extend(self.mentions[x])
			else:
				all_mention_types.extend('%s="nm"||'%x)
		outstr = [str(x) for x in all_mention_types]
		return '||'.join(outstr)

	def __repr__(self):
		return "<"+self.__str__()+">"

	def __hash__(self):
		return hash(self.__str__())

	def __cmp__(self,other):
		return self.mentions['m'][0].start_line[0] - other.mentions['m'][0].start_line[0]


class MentionTupleParser:
	P_DOCSET = re.compile(r'<doc id="(\d+)">(.*?)</doc>',re.S)	

	P_I2B2_MENTION = re.compile(r'(m|do|mo|f|du|r|e|t|c|ln|modifier|output)="([^|]*?)"(.*?)(\|\||$)')
	#1: tag, 2: content, 3: offsets (comma-delim), 4: divider

	P_MENTION_ATTR = re.compile(r'(do|mo|f|du|r|e|t|c|ln|modifier|output)="([^|]*?)"\s*(\|\||$)')
	#1: tag, 2: content, 3: divider

	P_OFFSET = re.compile(r'(\d+):(\d+)')

	@staticmethod
	def parse_i2b2_file(fpath):
		fh = open(fpath)
		contents = fh.read().split('\n')
		parsed_mentions = []
		for ix,x in enumerate(contents):
			parsed_mentions.append(MentionTupleParser.parse_i2b2(x,ix))
		return parsed_mentions
	
	@staticmethod
	def parse_i2b2_string(pstring,pid):
		contents = pstring.split('\n')
		parsed_mentions = []
		for ix,x in enumerate(contents):
			parsed_mentions.append(MentionTupleParser.parse_i2b2(x,pid))
		return parsed_mentions

	@staticmethod
	def parse_i2b2(mention_line,idx):
		#mentions = mention_line.split('||')
		mention_list = []
		mentions_full = MentionTupleParser.P_I2B2_MENTION.findall(mention_line)
		for m in mentions_full:
			tag,content,offsets = m[0:3]
			all_offsets = offsets.strip().split(',')
			offset_ints = []
			for ix,x in enumerate(all_offsets):
				curpos = MentionTupleParser.P_OFFSET.findall(x)
				for y in curpos:
					offset_ints.append((int(y[0]),int(y[1])))
			start_lines = [x[0] for x in offset_ints[::2]]
			start_tokens = [x[1] for x in offset_ints[::2]]
			end_lines = [x[0] for x in offset_ints[1::2]]
			end_tokens = [x[1] for x in offset_ints[1::2]]
			mention_list.append(Mention(tag,idx,content,start_lines,start_tokens,end_lines,end_tokens))
		return MentionTuple(idx,mention_list)