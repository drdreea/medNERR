# i2b2eval.py
# version 1.04 patch
# Eithon Cadag, ecadag@uw.edu
# changes by Illes Solt, illes.solt@tmit.bme.hu
#
# 	Evaluation module for the 2009 i2b2 challenge; contents of this code are based on the Evaluation.Metrics.6.16.pdf file, available
# 	from the Google Groups site for the 2009 i2b2 challenge.
#	
#	Changelog:
#	8/26/2009 -- Tabs replaced with spaces for vector information.
#	8/9/2009 -- Speed improvements
#	8/7/2009 -- Structural data for record is now incorporated into the XML file (no longer any need to include the record directory during evaluation)
#	8/2/2009 -- Tags absent in the ground truth are now not included in the f-score calculations (bug report by Zuofeng Li)
#	7/28/09 -- Public release file completed.


from i2b2obj import *
import math,re,sets,os

SIG_DIGITS = 4

def average(lvars):
	"""Takes the average of a list of floats (lvars)."""
	try:
		ave_val = float(sum(lvars))/len(lvars)
		return ave_val
	except ZeroDivisionError:
		return 0.0

def stdev(lvars):
	"""Calculates the standard deviation of a list of floats (lvars)."""
	avg = average(lvars)
	sqdiff = sum(map(lambda x: (x-avg)**2,lvars))
	return math.sqrt(sqdiff/(len(lvars)-1))

def fmeasure(dval,sval,nval,beta=1.0):
	"""F-measure, specifications per i2b2 evaluation document."""
	try:
		ip = dval/float(nval)
		ir = dval/float(sval)
		numer = (beta**2 + 1)*ip*ir
		denom = (beta**2 * ip) + ir
	except ZeroDivisionError:
		return 0.0
	if denom == 0.0:
		return 0.0
	else:
		return round(numer/denom,SIG_DIGITS)

def prmeasure(dval,sval,nval):
	"""Pr and Re measures, per i2b2 eval document."""
	try:
		ip = dval/float(nval)
	except ZeroDivisionError:
		ip = 0.0
	try:
		ir = dval/float(sval)
	except ZeroDivisionError:
		ir = 0.09
	return (round(ip,SIG_DIGITS),round(ir,SIG_DIGITS))

def parse_i2b2_xml_entry(xml_file):
	"""Parses the content of an i2b2 XML entry file.
	xml_file: string path to target file."""
	xml_parse = re.compile("<doc id='(\d+)'>\s+<structure>(.*?)</structure>\s+<\!\[CDATA\[(.*?)\]\]>",re.S) # Regular expression to identify individual documents
	xml_fh = open(xml_file)
	xml_data = xml_parse.findall(xml_fh.read())
	xml_records = {}
	for x in xml_data:
		xml_records[x[0]] = (x[1],x[2])
	#xml_records = dict(xml_parse.findall(xml_fh.read()))
	xml_fh.close()
	return xml_records

def Evaluate(gold_xml,sys_xml,exact,show_pr=False):
	"""Primary evaluation function.
	record_path: string path to raw records
	gold_xml: string path to XML ground truth
	sys_xml: string path to XML system
	exact: boolean True if using exact eval; False o.w."""
	gold_files = parse_i2b2_xml_entry(gold_xml)
	sys_files = parse_i2b2_xml_entry(sys_xml)
	sysname = sys_xml
	if exact:
		is_exact = "exact"
	else:
		is_exact = "inexact"
	d_sh,s_sh,n_sh = 0.0,0.0,0.0
	file_fms = []
	file_prms = []
	vert_sms = dict(zip(MentionTuple.OFFSET_MENTIONS,[{} for om in MentionTuple.OFFSET_MENTIONS])) # system level
	vert_rms = dict(zip(MentionTuple.OFFSET_MENTIONS,[[] for om in MentionTuple.OFFSET_MENTIONS])) # record level
	vert_pr_rms = dict(zip(MentionTuple.OFFSET_MENTIONS,[[] for om in MentionTuple.OFFSET_MENTIONS])) # record level
	for f in vert_sms: # Vertical evaluation metrics for F-measure
		vert_sms[f]['D'] = 0.0
		vert_sms[f]['N'] = 0.0
		vert_sms[f]['S'] = 0.0
	for g in gold_files:
		#if g in sys_files:
		f_d,f_s,f_n = 0.0,0.0,0.0
		##rec_h = open(record_path+g)
		##doc = VectorDocument(rec_h)
		#if hash(gold_files[g][0]) != hash(sys_files[g][0]): # infostring mismatch -- record contents do not match, despite sharing the same name
		#	print "# Warning: Content for `%s' do not match between gold and system!"%g
		doc = VectorDocument(infostring=gold_files[g][0])
		##rec_h.close()
		gold_rec = MentionTupleParser.parse_i2b2_string(gold_files[g][1],1)
		if g in sys_files:
			if hash(gold_files[g][0]) != hash(sys_files[g][0]):
				print "# Warning: Content for `%s' do not match between gold and system!"%g
			sys_rec = MentionTupleParser.parse_i2b2_string(sys_files[g][1],0)
		else:
			print "# Record `%s' not present in system!"%g
			sys_rec = []
		ms = MatchingScheme(doc,gold_rec,sys_rec,exact)
		curd, curs, curn = ms.microstats()
		for mtag in MentionTuple.OFFSET_MENTIONS:
			tagd_d,tagd_s,tagd_n = ms.microstats_tag(mtag)
			vert_sms[mtag]['D'] += tagd_d
			vert_sms[mtag]['S'] += tagd_s
			vert_sms[mtag]['N'] += tagd_n
			if tagd_s != 0.0: # If this is not an empty set
				vert_rms[mtag].append(fmeasure(tagd_d,tagd_s,tagd_n))
				vert_pr_rms[mtag].append(prmeasure(tagd_d,tagd_s,tagd_n))
		f_d += curd
		f_s += curs
		f_n += curn
		file_fms.append(fmeasure(f_d,f_s,f_n))
		file_prms.append(prmeasure(f_d,f_s,f_n))
#		else:
#			print "#WARNING: record %s not present in system file."%g
		d_sh += f_d
		s_sh += f_s
		n_sh += f_n
	print d_sh
	print s_sh
	print n_sh
	if show_pr:
		hsys = [str(x) for x in [sysname,is_exact,"horizontal","system-level",'X',fmeasure(d_sh,s_sh,n_sh),prmeasure(d_sh,s_sh,n_sh)[0],prmeasure(d_sh,s_sh,n_sh)[1]]]
		hpat = [str(x) for x in [sysname,is_exact,"horizontal","patient-level",'X',average(file_fms),average([y[0] for y in file_prms]),average([y[1] for y in file_prms])]]		
	else:
		hsys = [str(x) for x in [sysname,is_exact,"horizontal","system-level",'X',fmeasure(d_sh,s_sh,n_sh)]]
		hpat = [str(x) for x in [sysname,is_exact,"horizontal","patient-level",'X',average(file_fms)]]
	print '\t'.join(hsys)
	print '\t'.join(hpat)
	for mtag in vert_rms:
		if show_pr:
			vsys = [str(x) for x in [sysname,is_exact,"vertical","system-level",mtag,fmeasure(vert_sms[mtag]['D'],vert_sms[mtag]['S'],vert_sms[mtag]['N']),prmeasure(vert_sms[mtag]['D'],vert_sms[mtag]['S'],vert_sms[mtag]['N'])[0],prmeasure(vert_sms[mtag]['D'],vert_sms[mtag]['S'],vert_sms[mtag]['N'])[1]]]
			vpat = [str(x) for x in [sysname,is_exact,"vertical","patient-level",mtag,average(vert_rms[mtag]),average([y[0] for y in vert_pr_rms[mtag]]),average([y[1] for y in vert_pr_rms[mtag]])]]
		else:
			vsys = [str(x) for x in [sysname,is_exact,"vertical","system-level",mtag,fmeasure(vert_sms[mtag]['D'],vert_sms[mtag]['S'],vert_sms[mtag]['N'])]]
			vpat = [str(x) for x in [sysname,is_exact,"vertical","patient-level",mtag,average(vert_rms[mtag])]]
		print '\t'.join(vsys)
		print '\t'.join(vpat)
		

def build_batch_files(record_dir,entry_dir,outpath,entry_format="%s."):
	"""Creates XML-based files for performance matching."""
	records = os.listdir(record_dir)
	entries = os.listdir(entry_dir)
	entry_xml = []
	doc_info = {}
	for r in records:
		entry_file = entry_format%r
		record_found = False
		for e in entries:
			if e.startswith(entry_file):
				fh = open(entry_dir + e,'r')
				entry_xml.append((r,fh.read()))
				fh.close()
				vfh = open(record_dir + r,'r')
				tvd = VectorDocument(vfh)
				doc_info[r] = tvd.compress()
				tvd.decompress(doc_info[r])
				vfh.close()
				record_found=True
				break
		if not record_found:		
			print "Could not find entry file for %s."%r
	fh = open(outpath,'w')
	fh.write("<i2b2_data path='%s'>\n"%outpath)
	for rname,cdata in entry_xml:
		fh.write("<doc id='%s'>\n"%rname)
		fh.write("<structure>\n%s\n</structure>\n"%doc_info[rname])
		fh.write("<![CDATA[\n")
		fh.write(cdata)
		fh.write("]]>\n")
		fh.write("</doc>\n")
	fh.write("</i2b2_data>\n")
	fh.close()

def build_single_file(record_dir,entry_file,outpath):
	"""Creates XML-based file for specified entry."""
	records = os.listdir(record_dir)
	doc_info = None
	record_found = False
	entry_xml = None
	rname = None
	for r in records:
		if os.path.basename(entry_file).startswith('%s.'%r):
			rname = r
			fh = open(entry_file,'r')
			entry_xml = fh.read()
			fh.close()
			vfh = open(record_dir + r,'r')
			tvd = VectorDocument(vfh)
			doc_info = tvd.compress()
			vfh.close()
			record_found=True
			break
	if not record_found:
		print "Could not find note file for %s."%entry_file
		return
	fh = open(outpath,'w')
	fh.write("<i2b2_data path='%s'>\n"%outpath)
	fh.write("<doc id='%s'>\n"%rname)	
	fh.write("<structure>\n%s\n</structure>\n"%doc_info)
	fh.write("<![CDATA[\n")
	fh.write(entry_xml)
	fh.write("]]>\n")
	fh.write("</doc>\n")
	fh.write("</i2b2_data>\n")
	fh.close()


class InformativeMention:
	def __init__(self,mention,token_dict):
		self.mention = mention
		self.token_dict = token_dict
	
	def __eq__(self,other):
		return self.mention == other.mention
		
	def __str__(self):
		return "im:%s"%(self.mention)
	
	def __repr__(self):
		return self.__str__()

class Match:
	def __init__(self,gold_imention,tgt_imention,overlap_tokens):
		self.gold = gold_imention
		self.tgt = tgt_imention
		self.overlap_tokens = overlap_tokens
		self.dval = float(sum([self.overlap_tokens[x] for x in self.overlap_tokens]))
		self.nval = float(sum([self.tgt.token_dict[x] for x in self.tgt.token_dict]))
		self.sval = float(sum([self.gold.token_dict[x] for x in self.gold.token_dict]))
		self.fmeasure = fmeasure(self.dval,self.sval,self.nval)
	
	def match_tag_stats(self,tag):
		"""Given a tag, returns the number of overlaps, system count, and gold count in the match.
		D -> overlaps
		N -> system count
		S -> gold count """
		dval = float(self.overlap_tokens[tag])
		nval = float(self.tgt.token_dict[tag])
		sval = float(self.gold.token_dict[tag])
		return dval,sval,nval
	
	def __cmp__(self,other):
		return self.fmeasure*(10**(SIG_DIGITS-1)) - other.fmeasure*(10**(SIG_DIGITS-1))		

class MatchingScheme:
	def __init__(self,vecdoc,gs_list,tgt_list,exact=False):
		self.doc = vecdoc
		self.gs = [x for x in gs_list]
		self.tgt = [x for x in tgt_list]
		self.matches = None
		self.do_exact = exact
		goldi,tgti = self._match()
		self.gold_fn,self.tgt_fp = self._unpaired(goldi,tgti) # gold false negatives, system false positives
	
	def microstats(self):
		"""Obtain the statistics for F-measure calculation."""
		dms,sms,nms = 0.0,0.0,0.0
		for x in MentionTuple.OFFSET_MENTIONS:
			dmt,smt,nmt = self.microstats_tag(x)
			dms += dmt
			sms += smt
			nms += nmt
		return (dms,sms,nms)
	
	def microstats_tag(self,tag):
		"""Obtain the statistics for F-measure for a particular tag.
		tag: string tag for which to collect statistics."""
		stats_func = self.tag_stats
		if self.do_exact: stats_func = self.exact_tag_stats
		dt_ms, st_ms,nt_ms = stats_func(tag)
		return dt_ms,st_ms,nt_ms
	
	def exact_tag_stats(self,tag):
		"""Converts inexact tags to exact tag counts (ignores `nm' entries)."""
		d_all,s_all,n_all = 0.0,0.0,0.0
		for x in self.matches:
			dx,sx,nx = x.match_tag_stats(tag)
			if dx == sx == nx and dx != 0.0: # This is a perfect overlap; N == S, and N == D, thus S == D.
				d_all += 1
			if sx != 0.0: # Present only in gold
				s_all += 1
			if nx != 0.0: # Present only in system
				n_all += 1
		# Iterate through gold and system entries that were found to have no matches, and add them to S and N
		for x in self.gold_fn:
			if x.token_dict[tag] != 0.0: s_all += 1.0
		for x in self.tgt_fp:
			if x.token_dict[tag] != 0.0: n_all += 1.0
		return d_all,s_all,n_all
	
	def tag_stats(self,tag):
		"""Inexact tag statistics (use exact_tag_stats for exact tag counts)."""
		d_all,s_all,n_all = 0.0,0.0,0.0
		for x in self.matches:
			dx,sx,nx = x.match_tag_stats(tag)
			d_all += dx
			s_all += sx
			n_all += nx
		else:
			for x in self.gold_fn: s_all += x.token_dict[tag]
			for x in self.tgt_fp: n_all += x.token_dict[tag]
		return d_all,s_all,n_all
	
	def _unpaired(self,gi,ti):
		gold_matched = [x.gold for x in self.matches]
		tgt_matched = [x.tgt for x in self.matches]
		gi_unpaired = [x for x in gi if x not in gold_matched]
		ti_unpaired = [x for x in ti if x not in tgt_matched]
		return gi_unpaired,ti_unpaired
	
	def _match(self):
		"""Identifies the mentions in the gold standard that have a possible match to the target; uses greedy
		F-measure methods to resolve multiple overlaps."""
		self.doc.clear_vector()
		matches = []
		gold_imentions_list = []
		tgt_imentions_list = []
		for ix,x in enumerate(self.gs):
			if len(self.tgt) == 0:
				gs_mcc = self.doc.add_mentiontuple(x,1)
				gs_imention = InformativeMention(x,gs_mcc)
				if gs_imention not in gold_imentions_list: gold_imentions_list.append(gs_imention)	
				ov = self.doc.overlap('m')
				if ov > 0:
					overlap_dict = {}
					for m in x.mentions:
						overlap_dict[m] = self.doc.overlap(m)
					curmatch = Match(gs_imention,tgt_imention,overlap_dict)
					matches.append(curmatch)
				self.doc.clear_vector()							
			for iy,y in enumerate(self.tgt):
				gs_mcc = self.doc.add_mentiontuple(x,1)
				tgt_mcc = self.doc.add_mentiontuple(y,0)
				gs_imention = InformativeMention(x,gs_mcc)
				tgt_imention = InformativeMention(y,tgt_mcc)
				if gs_imention not in gold_imentions_list: gold_imentions_list.append(gs_imention)
				if tgt_imention not in tgt_imentions_list: tgt_imentions_list.append(tgt_imention)
				#if not self.doc.has_overlap: continue
				ov = self.doc.overlap('m')
				if ov > 0:
					overlap_dict = {}
					for m in x.mentions:
						overlap_dict[m] = self.doc.overlap(m)
					curmatch = Match(gs_imention,tgt_imention,overlap_dict)
					matches.append(curmatch)
					#if curmatch.fmeasure == 1.0: break
				self.doc.clear_vector()
		gold_matches = []
		tgt_matches = []
		gold_tgt_corresp = []
		sorted_matches = sorted(matches,reverse=True)
		for sm in sorted_matches:
			if sm.gold not in gold_matches and sm.tgt not in tgt_matches:
				gold_matches.append(sm.gold)
				tgt_matches.append(sm.tgt)
				gold_tgt_corresp.append(sm)
		gold_no_match = []
		target_no_match = []
		self.matches = gold_tgt_corresp
		return gold_imentions_list,tgt_imentions_list


class VectorDocument:
	def __init__(self,readable=None,infostring=None): 
		self.raw = None
		self.lines = None
		if readable is not None:
			self.raw = readable.read()
			self.raw = self.raw.replace('\t',' ')
			self.lines = [x.split(' ') for x in self.raw.split('\n')]
		elif infostring is not None:
			self.decompress(infostring)
		self.active_line_info = [] 	# active mention count per line
		self.span_vector = []
		self.clear_vector()
	
	def compress(self):
		"""Shrink the structurally-informative data for this document."""
		infostring = []
		for ix,x in enumerate(self.span_vector):
			infostring.append((str(ix+1)+":"+str(len(self.lines[ix]))))
		return ' '.join(infostring)
	
	def decompress(self,infostring):
		"""Prepare a document according to the given compression string."""
		self.raw = None
		self.lines = []
		self.span_vector = []
		line_info = infostring.strip().split()
		token_info = [x.split(':') for x in line_info]
		for ix,line in enumerate(token_info):
			self.span_vector.append([[] for x in xrange(0,int(line[1]))])
			self.lines.append([' ' for x in xrange(0,int(line[1]))])
	
	def clear_vector_ori(self):
		self.span_vector = [[[] for t in line] for line in self.lines]
		self.active_line_info = [0 for line in self.lines]

	def clear_vector(self):
		if self.span_vector == []:
			# called from __init__
			self.span_vector = [[[] for t in line] for line in self.lines]
		else:
			# reset only lines with active mentions
			for line_index,mentions in enumerate(self.active_line_info):
				if mentions > 0:
					# reset line
					for token_mentions in self.span_vector[line_index]:
						del token_mentions[:]
		# reset active mention counts
		self.active_line_info = [0 for line in self.lines]
	
	def has_overlap(self,tag):
		for line in self.span_vector:
			for tok in line:
				if len(tok) > 1: return True
		return False
	
	def overlap(self,tag):
		ov_count = 0
		lines = self.min_active_lines(2)
		for lpos in lines:
			curline = self.span_vector[lpos]
			for tokpos,token in enumerate(curline):
				if len(token) > 1:
					relevant_mentions = [sid for sid,m in token if m.tag == tag]
					if len(relevant_mentions) == 2: ov_count += 1
		return ov_count

	def min_active_lines_ori(self,min_active=1):
		active_lines = []
		for ix,x in enumerate(self.span_vector):
			line_total = 0
			for iy,y in enumerate(x):
				line_total += len(y)
			if line_total >= min_active: active_lines.append(ix)
		return active_lines

	def min_active_lines(self,min_active=1):
		active_lines = []
		for il,line_total in enumerate(self.active_line_info):
			if line_total >= min_active: active_lines.append(il)
		return active_lines

	def add_mentiontuple(self,mtuple,set_id):
		mention_count = {}
		for m in mtuple.mentions:
			mention_count[m] = 0
			for submention in mtuple.mentions[m]:
				if submention.start_line != []:
					mention_count[m] += self.add_mention(submention,set_id)
		return mention_count
	
	def add_mention(self,mention,set_id):
		add_count = 0
		spl = mention.spans_lines()
		for x in spl:
			linepos = x-1
			for tokenpos,token in enumerate(self.span_vector[linepos]):
				if mention.contains(linepos+1,tokenpos):
					self.span_vector[linepos][tokenpos].append((set_id,mention))
					self.active_line_info[linepos] += 1
					add_count += 1
		#print self.active_line_info
		return add_count

#def main():
if __name__ == "__main__":
	from optparse import OptionParser
	parser = OptionParser()
	parser.add_option("-x","--xmlmake",action="store_true",dest="do_xmlgen"
					,default=False,help="Generates XML file from specified record and entry directories.")
	parser.add_option("-f","--entryfile",action="store_true",dest="do_single_xml",
					default=False,help="Generates XML file from specified record directory and single entry file.")
	parser.add_option("-p","--pandr",action="store_true",dest="show_pr",
					default=False,help="Prints entry-level precision and recall values")	
	parser.add_option("-r","--recordpath",dest="recordpath",
					help="Path to the directory containing i2b2 records (only applies if -x is specified)")
	parser.add_option("-z","--entrypath",dest="entrypath",
					help="Path to the directory containing i2b2 entries (only applies if -x is specified) -- format of entry files expected is: <record_name>.i2b2.entries; if the -f flag is present, then this corresponds to the specific entry file.")	
	parser.add_option("-o","--xmlout",dest="xmlout",
					help="File from which to create an XML entry file (only applies if -x is specified).")
	parser.add_option("-g","--goldxml",dest="goldxmlp",
					help="Path to the ground truth XML i2b2 entry file (only applies if -x is omitted).")
	parser.add_option("-s","--sysxml",dest="sysxmlp",
					help="Path to the system XML i2b2 entry file (only applies if -x is omitted).")
	#parser.add_option("-e","--exactmatch",dest="use_exact",action="store_true",help="Exact matches only (only applies if -x is omitted).",default=False)
	
	(options, args) = parser.parse_args()
	
	if options.do_xmlgen and not options.do_single_xml:
		build_batch_files(options.recordpath,options.entrypath,options.xmlout)
	elif options.do_xmlgen and options.do_single_xml:
		build_single_file(options.recordpath,options.entrypath,options.xmlout)	
	else:
		headers = ['#in/exact','vert/horiz','sys/pat','tag/X','fmeasure']
		if options.show_pr:
			headers.extend(['precision','recall'])
		print options.sysxmlp
		print '\t'.join(headers)
		#import cProfile
		#cProfile.run('Evaluate(options.recordpath,options.goldxmlp,options.sysxmlp,False)')
		Evaluate(options.goldxmlp,options.sysxmlp,False,options.show_pr)
		#cProfile.run('Evaluate(options.recordpath,options.goldxmlp,options.sysxmlp,True)')
		Evaluate(options.goldxmlp,options.sysxmlp,True,options.show_pr)

