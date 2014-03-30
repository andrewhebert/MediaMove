package com.andrewhebert.hebertmedia

import java.text.SimpleDateFormat
import java.util.Date;

class GroovyFind {

	static main(args) {
		
		String test = "MyMovive.mov";
		String firstPart = test.find(~/.+\./);
		println firstPart;
		
		String test2 = "MyMovie (100).mov";
		String secondPart = test2.find(~/.+ \(\d+\)./)
		println secondPart;
		
		SimpleDateFormat sdf = new SimpleDateFormat("zzz yyyy-MM-dd HH:mm:ss");
		Date date = sdf.parse("UTC 2012-09-20 17:19:31");	
		println date;
	}

}
