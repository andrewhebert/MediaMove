new File("U:\\HebertMediaDropoff").eachFile{
	it.setLastModified(Date.parse("MM/dd/yyyy", "03/07/2009").getTime());
}