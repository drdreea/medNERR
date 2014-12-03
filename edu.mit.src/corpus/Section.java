package corpus;

public class Section {
	private String header;
	private String content;
	
	public Section(String header, String content) {
		this.setHeader(header);
		this.setContent(content);
	}

	public String getHeader() {
		return header;
	}

	public void setHeader(String header) {
		this.header = header;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
	
	

}
